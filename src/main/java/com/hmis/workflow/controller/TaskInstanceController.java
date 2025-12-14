package com.hmis.workflow.controller;

import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.dto.ApiResponse;
import com.hmis.workflow.dto.TaskInstanceDTO;
import com.hmis.workflow.service.TaskInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for task instance management
 * Provides endpoints for managing individual task instances within workflows
 */
@RestController
@RequestMapping("/workflows/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskInstanceController {

    private final TaskInstanceService taskInstanceService;

    /**
     * Get task instance by ID
     * GET /workflows/tasks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> getTaskInstance(@PathVariable UUID id) {
        log.info("Fetching task instance: {}", id);

        TaskInstance task = taskInstanceService.getTaskInstance(id);
        TaskInstanceDTO dto = mapToDTO(task);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task instance retrieved successfully"));
    }

    /**
     * Get all tasks for a workflow instance
     * GET /workflows/tasks/workflow/{workflowInstanceId}
     */
    @GetMapping("/workflow/{workflowInstanceId}")
    public ResponseEntity<ApiResponse<List<TaskInstanceDTO>>> getWorkflowTasks(
            @PathVariable UUID workflowInstanceId) {
        log.info("Fetching tasks for workflow instance: {}", workflowInstanceId);

        List<TaskInstance> tasks = taskInstanceService.getTasksByWorkflow(workflowInstanceId);
        List<TaskInstanceDTO> dtos = tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Workflow tasks retrieved successfully"));
    }

    /**
     * Get all tasks assigned to a user
     * GET /workflows/tasks/assigned-to/{assignedTo}
     */
    @GetMapping("/assigned-to/{assignedTo}")
    public ResponseEntity<ApiResponse<List<TaskInstanceDTO>>> getAssignedTasks(
            @PathVariable String assignedTo) {
        log.info("Fetching tasks assigned to: {}", assignedTo);

        List<TaskInstance> tasks = taskInstanceService.getAssignedTasks(assignedTo);
        List<TaskInstanceDTO> dtos = tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Assigned tasks retrieved successfully"));
    }

    /**
     * Assign task to a user
     * POST /workflows/tasks/{id}/assign
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> assignTask(
            @PathVariable UUID id,
            @RequestBody AssignTaskRequest request) {
        log.info("Assigning task {} to: {}", id, request.getAssignedTo());

        TaskInstance task = taskInstanceService.assignTask(id, request.getAssignedTo());
        TaskInstanceDTO dto = mapToDTO(task);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task assigned successfully"));
    }

    /**
     * Start task
     * POST /workflows/tasks/{id}/start
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> startTask(
            @PathVariable UUID id,
            @RequestBody(required = false) StartTaskRequest request) {
        log.info("Starting task: {}", id);

        String startedByUser = request != null ? request.getStartedByUser() : "SYSTEM";
        TaskInstance task = taskInstanceService.startTask(id, startedByUser);
        TaskInstanceDTO dto = mapToDTO(task);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task started successfully"));
    }

    /**
     * Complete task
     * POST /workflows/tasks/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> completeTask(
            @PathVariable UUID id,
            @RequestBody CompleteTaskRequest request) {
        log.info("Completing task: {}", id);

        TaskInstance task = taskInstanceService.completeTask(id, request.getResult(),
                request.getCompletedByUser());
        TaskInstanceDTO dto = mapToDTO(task);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task completed successfully"));
    }

    /**
     * Fail task
     * POST /workflows/tasks/{id}/fail
     */
    @PostMapping("/{id}/fail")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> failTask(
            @PathVariable UUID id,
            @RequestBody FailTaskRequest request) {
        log.info("Failing task: {} - Error: {}", id, request.getErrorMessage());

        TaskInstance task = taskInstanceService.failTask(id, request.getErrorMessage(),
                request.getFailedByUser());
        TaskInstanceDTO dto = mapToDTO(task);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task marked as failed"));
    }

    /**
     * Retry failed task
     * POST /workflows/tasks/{id}/retry
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> retryTask(@PathVariable UUID id) {
        log.info("Retrying task: {}", id);

        TaskInstance task = taskInstanceService.retryTask(id);
        TaskInstanceDTO dto = mapToDTO(task);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task retry initiated successfully"));
    }

    /**
     * Escalate task
     * POST /workflows/tasks/{id}/escalate
     */
    @PostMapping("/{id}/escalate")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> escalateTask(
            @PathVariable UUID id,
            @RequestBody EscalateTaskRequest request) {
        log.info("Escalating task: {} to: {}", id, request.getEscalatedToUser());

        TaskInstance task = taskInstanceService.escalateTask(id, request.getEscalatedToUser(),
                request.getReason());
        TaskInstanceDTO dto = mapToDTO(task);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task escalated successfully"));
    }

    /**
     * Skip optional task
     * POST /workflows/tasks/{id}/skip
     */
    @PostMapping("/{id}/skip")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> skipTask(@PathVariable UUID id) {
        log.info("Skipping optional task: {}", id);

        TaskInstance task = taskInstanceService.skipTask(id);
        TaskInstanceDTO dto = mapToDTO(task);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task skipped successfully"));
    }

    /**
     * Get SLA-breached tasks
     * GET /workflows/tasks/sla/breached
     */
    @GetMapping("/sla/breached")
    public ResponseEntity<ApiResponse<List<TaskInstanceDTO>>> getSLABreachedTasks() {
        log.info("Fetching SLA-breached tasks");

        List<TaskInstance> tasks = taskInstanceService.getSLABreachedTasks();
        List<TaskInstanceDTO> dtos = tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "SLA-breached tasks retrieved successfully"));
    }

    /**
     * Get retryable tasks
     * GET /workflows/tasks/retryable
     */
    @GetMapping("/retryable")
    public ResponseEntity<ApiResponse<List<TaskInstanceDTO>>> getRetryableTasks() {
        log.info("Fetching retryable tasks");

        List<TaskInstance> tasks = taskInstanceService.getRetryableTasks();
        List<TaskInstanceDTO> dtos = tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Retryable tasks retrieved successfully"));
    }

    /**
     * Update task comments
     * PUT /workflows/tasks/{id}/comments
     */
    @PutMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> updateTaskComments(
            @PathVariable UUID id,
            @RequestBody UpdateCommentsRequest request) {
        log.info("Updating comments for task: {}", id);

        TaskInstance task = taskInstanceService.updateTaskComments(id, request.getComments());
        TaskInstanceDTO dto = mapToDTO(task);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task comments updated successfully"));
    }

    // ==================== Helper Methods ====================

    private TaskInstanceDTO mapToDTO(TaskInstance task) {
        return TaskInstanceDTO.builder()
                .id(task.getId())
                .taskInstanceId(task.getTaskInstanceId())
                .status(task.getStatus())
                .assignedTo(task.getAssignedTo())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .comments(task.getComments())
                .result(task.getResult())
                .retryCount(task.getRetryCount())
                .maxRetries(task.getMaxRetries())
                .errorMessage(task.getErrorMessage())
                .workflowInstanceId(task.getWorkflowInstance().getId())
                .taskName(task.getTaskDefinition().getName())
                .taskDescription(task.getTaskDefinition().getDescription())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    // ==================== Request DTOs ====================

    static class AssignTaskRequest {
        public String assignedTo;

        public String getAssignedTo() {
            return assignedTo;
        }

        public void setAssignedTo(String assignedTo) {
            this.assignedTo = assignedTo;
        }
    }

    static class StartTaskRequest {
        public String startedByUser;

        public String getStartedByUser() {
            return startedByUser;
        }

        public void setStartedByUser(String startedByUser) {
            this.startedByUser = startedByUser;
        }
    }

    static class CompleteTaskRequest {
        public String result;
        public String completedByUser;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getCompletedByUser() {
            return completedByUser;
        }

        public void setCompletedByUser(String completedByUser) {
            this.completedByUser = completedByUser;
        }
    }

    static class FailTaskRequest {
        public String errorMessage;
        public String failedByUser;

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getFailedByUser() {
            return failedByUser;
        }

        public void setFailedByUser(String failedByUser) {
            this.failedByUser = failedByUser;
        }
    }

    static class EscalateTaskRequest {
        public String escalatedToUser;
        public String reason;

        public String getEscalatedToUser() {
            return escalatedToUser;
        }

        public void setEscalatedToUser(String escalatedToUser) {
            this.escalatedToUser = escalatedToUser;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    static class UpdateCommentsRequest {
        public String comments;

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }
    }
}
