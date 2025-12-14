package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for adding a task to a workflow template.
 *
 * Task Dependency Model:
 * - predecessorTaskIds: Optional list of task definition IDs that must complete before this task.
 *   If not provided or empty, this is an "entry task" that starts immediately.
 * - nextTaskId: Legacy field for successor-based chaining (optional).
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

    /**
     * Optional list of task definition IDs that must complete before this task can start.
     * If null or empty, this task is an "entry task" and will start immediately when workflow begins.
     *
     * Example: ["task-def-1", "task-def-2"] - both tasks must complete before this task starts.
     */
    private List<String> predecessorTaskIds;

    /**
     * Legacy: The task definition ID to activate after this task completes.
     * Consider using predecessorTaskIds on the target task instead for more flexibility.
     */
    private String nextTaskId;

    private String failureTaskId;
    private Map<String, Object> metadata;
}
