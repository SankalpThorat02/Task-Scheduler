package com.chronos.scheduler;

public enum JobStatus {
    SUBMITTED,        // API accepted it, sitting in Redis
    PROCESSING,       // Worker grabbed the lock and is working
    COMPLETED,        // Worker finished successfully
    FAILED_RETRYING,  // Worker crashed/failed, waiting for backoff
    DEAD_LETTER       // Failed 3 times, requires human intervention
}