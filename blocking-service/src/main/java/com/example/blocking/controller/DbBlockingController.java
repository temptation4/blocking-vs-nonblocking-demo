package com.example.blocking.controller;

import com.example.blocking.entity.AppUser;
import com.example.blocking.model.CreateUserRequest;
import com.example.blocking.model.PoolStats;
import com.example.blocking.repository.AppUserRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Talks to a real Postgres instance through Spring Data JPA / JDBC - a
 * blocking driver. Every method here parks the calling thread until
 * Postgres replies.
 */
@RestController
@RequestMapping("/api/blocking/db")
public class DbBlockingController {

    private final AppUserRepository repository;
    private final HikariDataSource dataSource;
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public DbBlockingController(AppUserRepository repository, HikariDataSource dataSource) {
        this.repository = repository;
        this.dataSource = dataSource;
    }

    @GetMapping("/users/{id}")
    public AppUser getUser(@PathVariable long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No user with id " + id));
    }

    /**
     * Same lookup, but the query runs pg_sleep(seconds) on the DB side
     * first - simulates a slow query using real database-side latency.
     */
    @GetMapping("/users/{id}/slow")
    public AppUser getUserSlow(@PathVariable long id, @RequestParam(defaultValue = "1") double seconds) {
        return repository.findByIdSlow(id, seconds)
                .orElseThrow(() -> new EntityNotFoundException("No user with id " + id));
    }

    @GetMapping("/users")
    public List<AppUser> getUsers(@RequestParam(defaultValue = "5") int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(id -> repository.findById((long) id)
                        .orElseThrow(() -> new EntityNotFoundException("No user with id " + id)))
                .toList();
    }

    @PostMapping("/users")
    public ResponseEntity<AppUser> createUser(@RequestBody CreateUserRequest request) {
        AppUser saved = repository.save(new AppUser(request.name(), request.email()));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/pool-stats")
    public PoolStats poolStats() {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        return new PoolStats(
                dataSource.getPoolName(),
                pool.getTotalConnections(),
                pool.getActiveConnections(),
                pool.getIdleConnections(),
                pool.getThreadsAwaitingConnection(),
                threadMXBean.getThreadCount()
        );
    }
}
