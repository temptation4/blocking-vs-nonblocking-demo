package com.example.reactive.repository;

import com.example.reactive.entity.AppUser;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface AppUserRepository extends ReactiveCrudRepository<AppUser, Long> {

    /**
     * Same idea as the blocking side's native query: pg_sleep runs on the
     * Postgres server to simulate a slow query. The difference is how the
     * *driver* waits for the reply - r2dbc-postgresql is built on Reactor
     * Netty, so awaiting the response never blocks an event-loop thread;
     * it registers a callback and the thread goes back to serving other
     * requests until Postgres has bytes ready.
     */
    @Query("SELECT *, pg_sleep(:seconds) FROM app_user WHERE id = :id")
    Mono<AppUser> findByIdSlow(long id, double seconds);
}
