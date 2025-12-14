package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for WorkflowTaskDefinition.
 *
 * Task Dependency Model:
 * - predecessorTaskIds: List of task definition IDs that must complete before this task starts.
 *   If empty or null, this is an "entry task" that can start immediately.
 * - nextTaskId: The task to activate after this task completes (legacy successor pattern).
 * - failureTaskId: The task to activate if this task fails.
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

    /**
     * List of task definition IDs that must complete before this task can start.
     * If null or empty, this is an "entry task" that starts immediately when workflow begins.
     *
     * Example: ["task-def-1", "task-def-2"] means both tasks must complete first.
     */
    private List<String> predecessorTaskIds;

    /**
     * The task definition ID to activate after this task completes (legacy pattern).
     * Consider using predecessorTaskIds on the target task instead.
     */
    private String nextTaskId;

    private String failureTaskId;
    private Map<String, Object> metadata;

    /**
     * Indicates if this is an entry task (no predecessors).
     * Entry tasks start immediately when the workflow begins.
     */
    private Boolean isEntryTask;
}
