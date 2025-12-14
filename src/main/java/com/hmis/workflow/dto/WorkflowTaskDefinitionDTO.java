package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * DTO for WorkflowTaskDefinition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTaskDefinitionDTO {

    private UUID id;
    private String name;
    private String description;
    private Integer taskOrder;
    private String assignTo;
    private Integer estimatedDurationMinutes;
    private String instructions;
    private Boolean isParallel;
    private Boolean isOptional;
    private String nextTaskId;
    private String failureTaskId;
    private Map<String, Object> metadata;
}
