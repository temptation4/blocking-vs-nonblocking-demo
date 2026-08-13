package com.example.blocking.controller;

import com.example.blocking.entity.AppUser;
import com.example.blocking.model.TimingResult;
import com.example.blocking.repository.AppUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Three ways to fetch `count` users, all hitting the same real Postgres
 * query (pg_sleep(seconds) makes each call take about `seconds` so the
 * timing differences are easy to see). Compare the totalTimeMs each one
 * returns.
 */
@RestController
@RequestMapping("/api/compare")
public class CompareController {

    private final AppUserRepository repository;

    public CompareController(AppUserRepository repository) {
        this.repository = repository;
    }

    /**
     * java.util.concurrent.Future: submit each call to a thread pool, then
     * block on future.get() one by one. The calls run concurrently, but
     * this thread still waits for each result explicitly.
     */
    @GetMapping("/future")
    public TimingResult withFuture(@RequestParam(defaultValue = "5") int count,
                                    @RequestParam(defaultValue = "1") double seconds) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        long start = System.currentTimeMillis();

        List<Future<AppUser>> futures = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            long id = i;
            futures.add(executor.submit(() -> repository.findByIdSlow(id, seconds).orElseThrow()));
        }
        for (Future<AppUser> future : futures) {
            future.get();
        }

        executor.shutdown();
        long totalTimeMs = System.currentTimeMillis() - start;
        return new TimingResult("Future", count, totalTimeMs);
    }

    /**
     * CompletableFuture: same concurrent calls, but composed with
     * supplyAsync + allOf instead of manually looping over futures.
     */
    @GetMapping("/completable-future")
    public TimingResult withCompletableFuture(@RequestParam(defaultValue = "5") int count,
                                               @RequestParam(defaultValue = "1") double seconds) {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        long start = System.currentTimeMillis();

        List<CompletableFuture<AppUser>> futures = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            long id = i;
            futures.add(CompletableFuture.supplyAsync(() -> repository.findByIdSlow(id, seconds).orElseThrow(), executor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown();
        long totalTimeMs = System.currentTimeMillis() - start;
        return new TimingResult("CompletableFuture", count, totalTimeMs);
    }

    /**
     * Plain blocking REST + JPA: no concurrency at all - `count` calls run
     * one after another on this request thread. This is the baseline the
     * two concurrent approaches above should beat.
     */
    @GetMapping("/blocking-jpa")
    public TimingResult blockingJpa(@RequestParam(defaultValue = "5") int count,
                                     @RequestParam(defaultValue = "1") double seconds) {
        long start = System.currentTimeMillis();

        for (int i = 1; i <= count; i++) {
            repository.findByIdSlow(i, seconds).orElseThrow();
        }

        long totalTimeMs = System.currentTimeMillis() - start;
        return new TimingResult("Blocking JPA (sequential)", count, totalTimeMs);
    }
}
