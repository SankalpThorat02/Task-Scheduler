package com.chronos.scheduler.service;


import com.chronos.scheduler.model.dto.JobSubmitRequest;
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

    private static final String ZSET_QUEUE = "chronos:delayed_tasks";
    private static final String PAYLOAD_PREFIX = "job_data:";

    public void enqueueJob(String jobId, JobSubmitRequest request) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(request);

            redisTemplate.opsForValue().set(PAYLOAD_PREFIX + jobId, jsonPayload);
            redisTemplate.opsForSet().add(ZSET_QUEUE, jobId, request.getExecuteAt());

            log.info("Successfully queued Job ID: {} for execution at {}", jobId, request.getExecuteAt());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize job payload for Job ID: {}", jobId, e);
            throw new RuntimeException("System error: Unable to process job payload");
        }
    }
}
