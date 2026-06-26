package com.chronos.scheduler.service;


import com.chronos.scheduler.JobStatus;
import com.chronos.scheduler.model.dto.JobSubmitRequest;
import com.chronos.scheduler.model.entity.JobExecutionLog;
import com.chronos.scheduler.repository.JobExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisQueueService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final JobExecutionLogRepository logRepository;

    private static final String ZSET_QUEUE = "chronos:delayed_tasks";
    private static final String PAYLOAD_PREFIX = "job_data:";

    //test

    public void enqueueJob(String jobId, JobSubmitRequest request) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(request);

            try {
                redisTemplate.opsForValue().set(PAYLOAD_PREFIX + jobId, jsonPayload);
                redisTemplate.opsForSet().add(ZSET_QUEUE, jobId, request.getExecuteAt());
            } catch (Exception e) {
                log.warn("OFFICE MODE: Redis connection failed. Skipping queue push, but continuing to save to Oracle.");
            }

            JobExecutionLog executionLog = JobExecutionLog.builder()
                    .jobId(jobId)
                    .taskType(request.getTaskType())
                    .status(JobStatus.SUBMITTED)
                    .executeAt(request.getExecuteAt())
                    .build();

            logRepository.save(executionLog);

            log.info("Successfully queued Job ID: {} for execution at {}", jobId, request.getExecuteAt());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize job payload for Job ID: {}", jobId, e);
            throw new RuntimeException("System error: Unable to process job payload");
        }
    }
}
