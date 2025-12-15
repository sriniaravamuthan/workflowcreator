package com.hmis.workflow.controller;

import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.dto.ApiResponse;
import com.hmis.workflow.dto.TaskInstanceDTO;
import com.hmis.workflow.dto.WorkflowInstanceDTO;
import com.hmis.workflow.service.WorkflowInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for workflow instance management
 * Provides endpoints for creating and managing patient workflow instances
 */
@RestController
@RequestMapping("/workflows/instances")
@RequiredArgsConstructor
@Slf4j
public class WorkflowInstanceController {

    private final WorkflowInstanceService workflowInstanceService;

    /**
     * Create a new workflow instance for a patient.
     *
     * Required parameters:
     * - patientId: The patient UUID
     * - templateId: The workflow template UUID
     *
     * Optional but recommended parameters:
     * - encounterId: The clinical encounter ID (e.g., ER visit, admission)
     * - visitId: The ADT visit tracking ID
     *
     * Uniqueness: Only one active workflow per patient + encounter + template combination.
     *
     * POST /workflows/instances
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowInstanceDTO>> createWorkflowInstance(
            @RequestBody CreateWorkflowInstanceRequest request) {
        log.info("Creating workflow instance for patient: {} using template: {} (encounter: {}, visit: {})",
                request.getPatientId(), request.getTemplateId(),
                request.getEncounterId(), request.getVisitId());

        WorkflowInstance instance = workflowInstanceService.createWorkflowInstance(
                request.getPatientId(),
                request.getTemplateId(),
                request.getEncounterId(),
                request.getVisitId());

        WorkflowInstanceDTO dto = mapToDTO(instance);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Workflow instance created successfully"));
    }

    /**
     * Get workflow instance by ID
     * GET /workflows/instances/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowInstanceDTO>> getWorkflowInstance(@PathVariable UUID id) {
        log.info("Fetching workflow instance: {}", id);

        WorkflowInstance instance = workflowInstanceService.getWorkflowInstance(id);
        WorkflowInstanceDTO dto = mapToDTO(instance);

        return ResponseEntity.ok(ApiResponse.success(dto, "Workflow instance retrieved successfully"));
    }

    /**
     * Get all workflows for a patient
     * GET /workflows/instances/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<WorkflowInstanceDTO>>> getPatientWorkflows(
            @PathVariable UUID patientId) {
        log.info("Fetching all workflows for patient: {}", patientId);

        List<WorkflowInstance> instances = workflowInstanceService.getWorkflowsByPatient(patientId);
        List<WorkflowInstanceDTO> dtos = instances.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos,
                "Patient workflows retrieved successfully"));
    }

    /**
     * Get active workflows for a patient
     * GET /workflows/instances/patient/{patientId}/active
     */
    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<ApiResponse<List<WorkflowInstanceDTO>>> getActivePatientWorkflows(
            @PathVariable UUID patientId) {
        log.info("Fetching active workflows for patient: {}", patientId);

        List<WorkflowInstance> instances = workflowInstanceService.getActiveWorkflowsForPatient(patientId);
        List<WorkflowInstanceDTO> dtos = instances.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos,
                "Active workflows retrieved successfully"));
    }

    /**
     * Get escalated workflows
     * GET /workflows/instances/escalated
     */
    @GetMapping("/escalated")
    public ResponseEntity<ApiResponse<List<WorkflowInstanceDTO>>> getEscalatedWorkflows() {
        log.info("Fetching escalated workflows");

        List<WorkflowInstance> instances = workflowInstanceService.getEscalatedWorkflows();
        List<WorkflowInstanceDTO> dtos = instances.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos,
                "Escalated workflows retrieved successfully"));
    }

    /**
     * Pause workflow instance
     * POST /workflows/instances/{id}/pause
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<WorkflowInstanceDTO>> pauseWorkflow(@PathVariable UUID id) {
        log.info("Pausing workflow instance: {}", id);

        WorkflowInstance instance = workflowInstanceService.pauseWorkflow(id);
        WorkflowInstanceDTO dto = mapToDTO(instance);

        return ResponseEntity.ok(ApiResponse.success(dto, "Workflow paused successfully"));
    }

    /**
     * Resume workflow instance
     * POST /workflows/instances/{id}/resume
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<WorkflowInstanceDTO>> resumeWorkflow(@PathVariable UUID id) {
        log.info("Resuming workflow instance: {}", id);

        WorkflowInstance instance = workflowInstanceService.resumeWorkflow(id);
        WorkflowInstanceDTO dto = mapToDTO(instance);

        return ResponseEntity.ok(ApiResponse.success(dto, "Workflow resumed successfully"));
    }

    /**
     * Cancel workflow instance
     * POST /workflows/instances/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<WorkflowInstanceDTO>> cancelWorkflow(
            @PathVariable UUID id,
            @RequestBody CancelWorkflowRequest request) {
        log.info("Cancelling workflow instance: {} - Reason: {}", id, request.getReason());

        WorkflowInstance instance = workflowInstanceService.cancelWorkflow(id, request.getReason());
        WorkflowInstanceDTO dto = mapToDTO(instance);

        return ResponseEntity.ok(ApiResponse.success(dto, "Workflow cancelled successfully"));
    }

    /**
     * Complete workflow instance
     * POST /workflows/instances/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<WorkflowInstanceDTO>> completeWorkflow(@PathVariable UUID id) {
        log.info("Completing workflow instance: {}", id);

        WorkflowInstance instance = workflowInstanceService.completeWorkflow(id);
        WorkflowInstanceDTO dto = mapToDTO(instance);

        return ResponseEntity.ok(ApiResponse.success(dto, "Workflow completed successfully"));
    }

    /**
     * Escalate workflow instance
     * POST /workflows/instances/{id}/escalate
     */
    @PostMapping("/{id}/escalate")
    public ResponseEntity<ApiResponse<WorkflowInstanceDTO>> escalateWorkflow(
            @PathVariable UUID id,
            @RequestBody EscalateWorkflowRequest request) {
        log.info("Escalating workflow instance: {} - Reason: {}", id, request.getReason());

        WorkflowInstance instance = workflowInstanceService.escalateWorkflow(id, request.getReason());
        WorkflowInstanceDTO dto = mapToDTO(instance);

        return ResponseEntity.ok(ApiResponse.success(dto, "Workflow escalated successfully"));
    }

    // ==================== Ad-hoc Task Endpoints ====================

    /**
     * Add an ad-hoc task to a workflow instance.
     *
     * Ad-hoc tasks are dynamically created tasks not in the original template,
     * such as when a doctor orders "administer saline" during treatment.
     *
     * POST /workflows/instances/{id}/adhoc-task
     */
    @PostMapping("/{id}/adhoc-task")
    public ResponseEntity<ApiResponse<TaskInstanceDTO>> addAdhocTask(
            @PathVariable UUID id,
            @RequestBody AddAdhocTaskRequest request) {
        log.info("Adding ad-hoc task '{}' to workflow instance: {} by user: {}",
                request.getTaskName(), id, request.getCreatedByUser());

        com.hmis.workflow.domain.entity.TaskInstance task = workflowInstanceService.addAdhocTask(
                id,
                request.getTaskName(),
                request.getTaskDescription(),
                request.getAssignTo(),
                request.getCreatedByUser(),
                request.getSlaMinutes()
        );

        TaskInstanceDTO dto = mapTaskToDTO(task);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Ad-hoc task created successfully"));
    }

    /**
     * Get all ad-hoc tasks for a workflow instance
     * GET /workflows/instances/{id}/adhoc-tasks
     */
    @GetMapping("/{id}/adhoc-tasks")
    public ResponseEntity<ApiResponse<List<TaskInstanceDTO>>> getAdhocTasks(@PathVariable UUID id) {
        log.info("Fetching ad-hoc tasks for workflow instance: {}", id);

        List<com.hmis.workflow.domain.entity.TaskInstance> tasks = workflowInstanceService.getAdhocTasks(id);
        List<TaskInstanceDTO> dtos = tasks.stream()
                .map(this::mapTaskToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Ad-hoc tasks retrieved successfully"));
    }

    // ==================== Helper Methods ====================

    private WorkflowInstanceDTO mapToDTO(WorkflowInstance instance) {
        return WorkflowInstanceDTO.builder()
                .id(instance.getId())
                .workflowInstanceId(instance.getWorkflowInstanceId())
                .status(instance.getStatus())
                .notes(instance.getNotes())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .patientId(instance.getPatient().getId())
                .patientName(instance.getPatient().getFullName())
                .encounterId(instance.getEncounterId())
                .visitId(instance.getVisitId())
                .templateId(instance.getTemplate().getId())
                .templateName(instance.getTemplate().getName())
                .progressPercentage(instance.getProgressPercentage())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();
    }

    private TaskInstanceDTO mapTaskToDTO(TaskInstance task) {
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
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    // ==================== Request DTOs ====================

    static class CreateWorkflowInstanceRequest {
        public UUID patientId;
        public UUID templateId;
        public String encounterId; // Optional: clinical encounter ID (e.g., ER visit, admission)
        public String visitId;     // Optional: ADT visit tracking ID

        public UUID getPatientId() {
            return patientId;
        }

        public void setPatientId(UUID patientId) {
            this.patientId = patientId;
        }

        public UUID getTemplateId() {
            return templateId;
        }

        public void setTemplateId(UUID templateId) {
            this.templateId = templateId;
        }

        public String getEncounterId() {
            return encounterId;
        }

        public void setEncounterId(String encounterId) {
            this.encounterId = encounterId;
        }

        public String getVisitId() {
            return visitId;
        }

        public void setVisitId(String visitId) {
            this.visitId = visitId;
        }
    }

    static class CancelWorkflowRequest {
        public String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    static class EscalateWorkflowRequest {
        public String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    static class AddAdhocTaskRequest {
        public String taskName;
        public String taskDescription;
        public String assignTo;
        public String createdByUser;
        public Integer slaMinutes;

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getTaskDescription() {
            return taskDescription;
        }

        public void setTaskDescription(String taskDescription) {
            this.taskDescription = taskDescription;
        }

        public String getAssignTo() {
            return assignTo;
        }

        public void setAssignTo(String assignTo) {
            this.assignTo = assignTo;
        }

        public String getCreatedByUser() {
            return createdByUser;
        }

        public void setCreatedByUser(String createdByUser) {
            this.createdByUser = createdByUser;
        }

        public Integer getSlaMinutes() {
            return slaMinutes;
        }

        public void setSlaMinutes(Integer slaMinutes) {
            this.slaMinutes = slaMinutes;
        }
    }
}
