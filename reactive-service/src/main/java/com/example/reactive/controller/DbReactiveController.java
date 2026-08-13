package com.example.reactive.controller;

import com.example.reactive.entity.AppUser;
import com.example.reactive.model.CreateUserRequest;
import com.example.reactive.model.PoolStats;
import com.example.reactive.repository.AppUserRepository;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.PoolMetrics;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Talks to the same real Postgres instance as the blocking service, but
 * through r2dbc-postgresql - a non-blocking driver. No method here ever
 * parks a thread waiting on the DB.
 */
@RestController
@RequestMapping("/api/reactive/db")
public class DbReactiveController {

    private final AppUserRepository repository;
    private final ConnectionFactory connectionFactory;
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public DbReactiveController(AppUserRepository repository, ConnectionFactory connectionFactory) {
        this.repository = repository;
        this.connectionFactory = connectionFactory;
    }

    @GetMapping("/users/{id}")
    public Mono<AppUser> getUser(@PathVariable long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
    }

    /**
     * Same lookup, but pg_sleep(seconds) runs on the DB side first - real
     * database-side latency, directly comparable to the blocking
     * service's /slow endpoint.
     */
    @GetMapping("/users/{id}/slow")
    public Mono<AppUser> getUserSlow(@PathVariable long id, @RequestParam(defaultValue = "1") double seconds) {
        return repository.findByIdSlow(id, seconds)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
    }

    @GetMapping("/users")
    public Flux<AppUser> getUsers(@RequestParam(defaultValue = "5") int count) {
        return Flux.range(1, count)
                .flatMap(id -> repository.findById((long) id)
                        .switchIfEmpty(Mono.error(new UserNotFoundException(id))));
    }

    @PostMapping("/users")
    public Mono<ResponseEntity<AppUser>> createUser(@RequestBody CreateUserRequest request) {
        return repository.save(new AppUser(request.name(), request.email()))
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    @GetMapping("/pool-stats")
    public PoolStats poolStats() {
        if (connectionFactory instanceof ConnectionPool pool) {
            PoolMetrics metrics = pool.getMetrics().orElse(null);
            if (metrics != null) {
                return new PoolStats(
                        "reactive-r2dbc-pool",
                        metrics.getMaxAllocatedSize(),
                        metrics.acquiredSize(),
                        metrics.idleSize(),
                        metrics.pendingAcquireSize(),
                        threadMXBean.getThreadCount()
                );
            }
        }
        return new PoolStats("reactive-r2dbc-pool", -1, -1, -1, -1, threadMXBean.getThreadCount());
    }

    static class UserNotFoundException extends RuntimeException {
        UserNotFoundException(long id) {
            super("No user with id " + id);
        }
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
