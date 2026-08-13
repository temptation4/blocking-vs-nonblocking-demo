package com.example.reactive.controller;

import com.example.reactive.model.TimingResult;
import com.example.reactive.repository.AppUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebFlux + R2DBC counterpart to the blocking service's CompareController:
 * fetches `count` users against the same real Postgres query
 * (pg_sleep(seconds) makes each call take about `seconds`). flatMap
 * subscribes to all `count` calls at once - no thread pool, no manual
 * futures, just non-blocking I/O.
 */
@RestController
@RequestMapping("/api/compare")
public class CompareController {

    private final AppUserRepository repository;

    public CompareController(AppUserRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/webflux-r2dbc")
    public Mono<TimingResult> webfluxR2dbc(@RequestParam(defaultValue = "5") int count,
                                            @RequestParam(defaultValue = "1") double seconds) {
        long start = System.currentTimeMillis();

        return Flux.range(1, count)
                .flatMap(id -> repository.findByIdSlow(id, seconds))
                .then(Mono.fromSupplier(() ->
                        new TimingResult("WebFlux + R2DBC", count, System.currentTimeMillis() - start)));
    }
}
