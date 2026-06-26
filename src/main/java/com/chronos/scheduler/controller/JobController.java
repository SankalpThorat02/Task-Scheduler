package com.chronos.scheduler.controller;

import com.chronos.scheduler.model.dto.JobSubmitRequest;
import com.chronos.scheduler.service.RedisQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController  {

    private final RedisQueueService redisQueueService;

    @PostMapping
    public ResponseEntity<Map<String, String>> submitJob(@Valid @RequestBody JobSubmitRequest request) {

        String jobId = UUID.randomUUID().toString();

        redisQueueService.enqueueJob(jobId, request);

        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "status", "SUBMITTED",
                "message", "Job accepted and queued for execution."
        ));
    }
}
