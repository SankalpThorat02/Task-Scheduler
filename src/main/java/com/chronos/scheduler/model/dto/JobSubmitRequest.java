package com.chronos.scheduler.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSubmitRequest {

    @NotBlank(message = "Task type cannot be blank")
    private String taskType;

    @NotNull(message = "Execution timestamp is required")
    private String executeAt;

    @NotBlank(message = "Priority level is required")
    private String priority;

    @NotNull(message = "Payload cannot be null")
    private Map<String, Object> payload;

    private String callbackUrl;
}
