package com.hmis.workflow.controller;

import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.entity.TaskNote;
import com.hmis.workflow.domain.entity.TaskNote.NoteType;
import com.hmis.workflow.domain.entity.TaskResult;
import com.hmis.workflow.domain.entity.TaskResult.ResultType;
import com.hmis.workflow.dto.ApiResponse;
import com.hmis.workflow.dto.TaskInstanceDTO;
import com.hmis.workflow.dto.TaskNoteDTO;
import com.hmis.workflow.dto.TaskNoteRequest;
import com.hmis.workflow.dto.TaskResultDTO;
import com.hmis.workflow.dto.TaskResultRequest;
import com.hmis.workflow.service.TaskInstanceService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
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
     * Skip optional task (backward compatible endpoint)
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
     * Skip task with reason and optional force flag.
     *
     * Supports skipping both optional and required tasks:
     * - Optional tasks can be skipped without requiring a reason
     * - Required tasks can only be skipped with forceSkip=true and require a reason
     *
     * Use cases for skipping required tasks:
     * - Blood test already performed elsewhere
     * - Patient refused the procedure
     * - Clinical judgment overrides standard protocol
     *
     * POST /workflows/tasks/{id}/skip-with-reason
     */
    @PostMapping("/{id}/skip-with-reason")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> skipTaskWithReason(
            @PathVariable UUID id,
            @RequestBody SkipTaskRequest request) {
        log.info("Skipping task {} with reason: {} by user: {} (forceSkip: {})",
                id, request.getReason(), request.getSkippedByUser(), request.isForceSkip());

        TaskInstance task = taskInstanceService.skipTaskWithReason(
                id,
                request.getReason(),
                request.getSkippedByUser(),
                request.isForceSkip()
        );
        TaskInstanceDTO dto = mapToDTO(task);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task skipped successfully"));
    }

    /**
     * Get skipped tasks for a workflow
     * GET /workflows/tasks/workflow/{workflowInstanceId}/skipped
     */
    @GetMapping("/workflow/{workflowInstanceId}/skipped")
    public ResponseEntity<ApiResponse<List<TaskInstanceDTO>>> getSkippedTasks(
            @PathVariable UUID workflowInstanceId) {
        log.info("Fetching skipped tasks for workflow instance: {}", workflowInstanceId);

        List<TaskInstance> tasks = taskInstanceService.getSkippedTasks(workflowInstanceId);
        List<TaskInstanceDTO> dtos = tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Skipped tasks retrieved successfully"));
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

    // ==================== TASK NOTES (APPEND-ONLY) ====================

    /**
     * Add a note to a task (append-only for compliance).
     * POST /workflows/tasks/{id}/notes
     */
    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<TaskNoteDTO>> addTaskNote(
            @PathVariable UUID id,
            @Valid @RequestBody TaskNoteRequest request) {
        log.info("Adding {} note to task {} by {}", request.getNoteType(), id, request.getAuthorUser());

        TaskNote note;
        if (request.getAddendumToNoteId() != null) {
            note = taskInstanceService.addAddendum(
                    id,
                    request.getAddendumToNoteId(),
                    request.getContent(),
                    request.getAuthorUser(),
                    request.getAuthorRole()
            );
        } else if (request.getPriority() != null && request.getPriority() > 0) {
            note = taskInstanceService.addFlaggedNote(
                    id,
                    request.getNoteType(),
                    request.getContent(),
                    request.getAuthorUser(),
                    request.getAuthorRole(),
                    request.getPriority()
            );
        } else {
            note = taskInstanceService.addNote(
                    id,
                    request.getNoteType(),
                    request.getContent(),
                    request.getAuthorUser(),
                    request.getAuthorRole()
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TaskNoteDTO.fromEntity(note), "Note added successfully"));
    }

    /**
     * Get all notes for a task (chronological order).
     * GET /workflows/tasks/{id}/notes
     */
    @GetMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<List<TaskNoteDTO>>> getTaskNotes(@PathVariable UUID id) {
        log.info("Fetching notes for task: {}", id);

        List<TaskNote> notes = taskInstanceService.getTaskNotes(id);
        List<TaskNoteDTO> dtos = notes.stream()
                .map(TaskNoteDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Task notes retrieved successfully"));
    }

    /**
     * Get notes by type for a task.
     * GET /workflows/tasks/{id}/notes/type/{noteType}
     */
    @GetMapping("/{id}/notes/type/{noteType}")
    public ResponseEntity<ApiResponse<List<TaskNoteDTO>>> getTaskNotesByType(
            @PathVariable UUID id,
            @PathVariable NoteType noteType) {
        log.info("Fetching {} notes for task: {}", noteType, id);

        List<TaskNote> notes = taskInstanceService.getTaskNotesByType(id, noteType);
        List<TaskNoteDTO> dtos = notes.stream()
                .map(TaskNoteDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Task notes retrieved successfully"));
    }

    /**
     * Get flagged notes for a task.
     * GET /workflows/tasks/{id}/notes/flagged
     */
    @GetMapping("/{id}/notes/flagged")
    public ResponseEntity<ApiResponse<List<TaskNoteDTO>>> getFlaggedNotes(@PathVariable UUID id) {
        log.info("Fetching flagged notes for task: {}", id);

        List<TaskNote> notes = taskInstanceService.getFlaggedNotes(id);
        List<TaskNoteDTO> dtos = notes.stream()
                .map(TaskNoteDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Flagged notes retrieved successfully"));
    }

    // ==================== TASK RESULTS (APPEND-ONLY) ====================

    /**
     * Add a result to a task (append-only for compliance).
     * POST /workflows/tasks/{id}/results
     */
    @PostMapping("/{id}/results")
    public ResponseEntity<ApiResponse<TaskResultDTO>> addTaskResult(
            @PathVariable UUID id,
            @Valid @RequestBody TaskResultRequest request) {
        log.info("Adding {} result '{}' to task {} by {}",
                request.getResultType(), request.getResultName(), id, request.getRecordedByUser());

        TaskResult result;
        if (request.getCorrectsResultId() != null) {
            // This is a correction to a previous result
            result = taskInstanceService.addResultCorrection(
                    id,
                    request.getCorrectsResultId(),
                    request.getResultValue(),
                    request.getComments(),
                    request.getRecordedByUser(),
                    request.getRecordedByRole()
            );
        } else if (request.getResultCode() != null) {
            // Coded result with standard code (e.g., LOINC)
            result = taskInstanceService.addCodedResult(
                    id,
                    request.getResultType(),
                    request.getResultName(),
                    request.getResultCode(),
                    request.getCodeSystem(),
                    request.getResultValue(),
                    request.getUnit(),
                    request.getReferenceRange(),
                    request.getInterpretationCode(),
                    request.getRecordedByUser(),
                    request.getRecordedByRole()
            );
        } else if (Boolean.TRUE.equals(request.getIsCritical())) {
            // Critical result
            result = taskInstanceService.addCriticalResult(
                    id,
                    request.getResultType(),
                    request.getResultName(),
                    request.getResultValue(),
                    request.getUnit(),
                    request.getRecordedByUser(),
                    request.getRecordedByRole(),
                    request.getComments()
            );
        } else {
            // Basic result
            result = taskInstanceService.addResult(
                    id,
                    request.getResultType(),
                    request.getResultName(),
                    request.getResultValue(),
                    request.getUnit(),
                    request.getRecordedByUser(),
                    request.getRecordedByRole()
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TaskResultDTO.fromEntity(result), "Result recorded successfully"));
    }

    /**
     * Verify a result.
     * POST /workflows/tasks/results/{resultId}/verify
     */
    @PostMapping("/results/{resultId}/verify")
    public ResponseEntity<ApiResponse<TaskResultDTO>> verifyResult(
            @PathVariable UUID resultId,
            @RequestBody VerifyResultRequest request) {
        log.info("Verifying result {} by {}", resultId, request.getVerifiedByUser());

        TaskResult result = taskInstanceService.verifyResult(resultId, request.getVerifiedByUser());

        return ResponseEntity.ok(ApiResponse.success(TaskResultDTO.fromEntity(result), "Result verified successfully"));
    }

    /**
     * Get all results for a task (chronological order).
     * GET /workflows/tasks/{id}/results
     */
    @GetMapping("/{id}/results")
    public ResponseEntity<ApiResponse<List<TaskResultDTO>>> getTaskResults(@PathVariable UUID id) {
        log.info("Fetching results for task: {}", id);

        List<TaskResult> results = taskInstanceService.getTaskResults(id);
        List<TaskResultDTO> dtos = results.stream()
                .map(TaskResultDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Task results retrieved successfully"));
    }

    /**
     * Get results by type for a task.
     * GET /workflows/tasks/{id}/results/type/{resultType}
     */
    @GetMapping("/{id}/results/type/{resultType}")
    public ResponseEntity<ApiResponse<List<TaskResultDTO>>> getTaskResultsByType(
            @PathVariable UUID id,
            @PathVariable ResultType resultType) {
        log.info("Fetching {} results for task: {}", resultType, id);

        List<TaskResult> results = taskInstanceService.getTaskResultsByType(id, resultType);
        List<TaskResultDTO> dtos = results.stream()
                .map(TaskResultDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Task results retrieved successfully"));
    }

    /**
     * Get critical results for a task.
     * GET /workflows/tasks/{id}/results/critical
     */
    @GetMapping("/{id}/results/critical")
    public ResponseEntity<ApiResponse<List<TaskResultDTO>>> getCriticalResults(@PathVariable UUID id) {
        log.info("Fetching critical results for task: {}", id);

        List<TaskResult> results = taskInstanceService.getCriticalResults(id);
        List<TaskResultDTO> dtos = results.stream()
                .map(TaskResultDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Critical results retrieved successfully"));
    }

    /**
     * Get unverified results for a task.
     * GET /workflows/tasks/{id}/results/unverified
     */
    @GetMapping("/{id}/results/unverified")
    public ResponseEntity<ApiResponse<List<TaskResultDTO>>> getUnverifiedResults(@PathVariable UUID id) {
        log.info("Fetching unverified results for task: {}", id);

        List<TaskResult> results = taskInstanceService.getUnverifiedResults(id);
        List<TaskResultDTO> dtos = results.stream()
                .map(TaskResultDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Unverified results retrieved successfully"));
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
                .taskName(task.getTaskName())
                .taskDescription(task.getTaskDescription())
                .isAdhoc(task.getIsAdhoc())
                .createdByUser(task.getCreatedByUser())
                .skipReason(task.getSkipReason())
                .skippedByUser(task.getSkippedByUser())
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

    static class SkipTaskRequest {
        public String reason;
        public String skippedByUser;
        public boolean forceSkip = false;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getSkippedByUser() {
            return skippedByUser;
        }

        public void setSkippedByUser(String skippedByUser) {
            this.skippedByUser = skippedByUser;
        }

        public boolean isForceSkip() {
            return forceSkip;
        }

        public void setForceSkip(boolean forceSkip) {
            this.forceSkip = forceSkip;
        }
    }

    static class VerifyResultRequest {
        public String verifiedByUser;

        public String getVerifiedByUser() {
            return verifiedByUser;
        }

        public void setVerifiedByUser(String verifiedByUser) {
            this.verifiedByUser = verifiedByUser;
        }
    }
}
