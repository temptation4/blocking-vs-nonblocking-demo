package com.example.blocking.repository;

import com.example.blocking.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * pg_sleep runs *inside Postgres*, on the database side - it stands in
     * for a genuinely slow query (large scan, lock wait, etc). The JDBC
     * driver reads the response with a blocking socket read, so the
     * calling thread (Tomcat worker) is parked for the whole duration.
     */
    @Query(value = "SELECT *, pg_sleep(:seconds) FROM app_user WHERE id = :id", nativeQuery = true)
    Optional<AppUser> findByIdSlow(@Param("id") long id, @Param("seconds") double seconds);
}
