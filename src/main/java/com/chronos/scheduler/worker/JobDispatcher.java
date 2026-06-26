package com.chronos.scheduler.worker;

import com.chronos.scheduler.JobStatus;
import com.chronos.scheduler.repository.JobExecutionLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class JobDispatcher {

    private final RedisTemplate<String, String> redisTemplate;
    private final JobExecutionLogRepository logRepository;
    private final ObjectMapper objectMapper;

    private static final String ZSET_QUEUE = "chronos:delayed_tasks";
    private static final String PAYLOAD_PREFIX = "job_data:";

    @Scheduled(fixedDelay = 1000)
    public void pollQueue() {

        long currentTimestamp = System.currentTimeMillis() / 1000;

        Set<String> dueJobs = redisTemplate.opsForZSet().rangeByScore(ZSET_QUEUE, 0, currentTimestamp, 0, 1);

        if (dueJobs == null || dueJobs.isEmpty()) {
            return;
        }

        String jobId = dueJobs.iterator().next();

        // 2. Atomic Claim: Try to remove it from the ZSET.
        // Only one node in a cluster will get a return value of 1.
        Long removed = redisTemplate.opsForZSet().remove(ZSET_QUEUE, jobId);

        if (removed != null && removed > 0) {
            log.info("Claimed Job ID: {}. Starting execution flow.", jobId);
            processJob(jobId);
        }

    }

    private void processJob(String jobId) {
        String payloadKey = PAYLOAD_PREFIX + jobId;

        try {
            // 3. Update status to PROCESSING in Oracle
            updateJobStatus(jobId, JobStatus.PROCESSING, null);

            // 4. Fetch the actual heavy JSON payload from Redis
            String rawJson = redisTemplate.opsForValue().get(payloadKey);
            if (rawJson == null) {
                throw new IllegalStateException("Payload missing in Redis for Job ID: " + jobId);
            }

            // 5. Simulate task execution processing
            log.info("Executing task data for Job: {}", rawJson);
            Thread.sleep(2000); // Simulating actual work (e.g., parsing Excel, calling external API)

            // 6. Update status to COMPLETED in Oracle
            updateJobStatus(jobId, JobStatus.COMPLETED, null);

            // 7. Clean up the payload from Redis to prevent storage leakage
            redisTemplate.delete(payloadKey);
            log.info("Successfully processed and closed Job ID: {}", jobId);

        } catch (Exception e) {
            log.error("Execution failed for Job ID: {}", jobId, e);
            updateJobStatus(jobId, JobStatus.FAILED_RETRYING, e.getMessage());
        }
    }

    private void updateJobStatus(String jobId, JobStatus status, String errorMessage) {
        logRepository.findById(Long.valueOf(jobId)).ifPresent(executionLog -> {
            executionLog.setStatus(status);
            if (errorMessage != null) {
                executionLog.setErrorMessage(errorMessage);
            }
            logRepository.save(executionLog);
        });
    }
}
