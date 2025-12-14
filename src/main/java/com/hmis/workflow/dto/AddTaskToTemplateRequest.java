package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for adding a task to a workflow template
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddTaskToTemplateRequest {
    private String name;
    private String description;
    private String assignTo;
    private Integer estimatedDurationMinutes;
    private String instructions;
    private Boolean isParallel = false;
    private Boolean isOptional = false;
    private String nextTaskId;
    private String failureTaskId;
    private Map<String, Object> metadata;
}
