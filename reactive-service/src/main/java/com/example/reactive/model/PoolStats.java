package com.example.reactive.model;

public record PoolStats(
        String pool,
        int maxAllocatedSize,
        int acquiredSize,
        int idleSize,
        int pendingAcquireSize,
        int liveThreadCount
) {
}
