package com.example.blocking.model;

public record PoolStats(
        String pool,
        int totalConnections,
        int activeConnections,
        int idleConnections,
        int threadsAwaitingConnection,
        int liveThreadCount
) {
}
