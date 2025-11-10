package com.hmis.workflow.controller;

import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.dto.ApiResponse;
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
     * Create a new workflow instance for a patient
     * POST /workflows/instances
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowInstanceDTO>> createWorkflowInstance(
            @RequestBody CreateWorkflowInstanceRequest request) {
        log.info("Creating workflow instance for patient: {} using template: {}",
                request.getPatientId(), request.getTemplateId());

        WorkflowInstance instance = workflowInstanceService.createWorkflowInstance(
                request.getPatientId(), request.getTemplateId());

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
                .templateId(instance.getTemplate().getId())
                .templateName(instance.getTemplate().getName())
                .progressPercentage(instance.getProgressPercentage())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();
    }

    // ==================== Request DTOs ====================

    static class CreateWorkflowInstanceRequest {
        public UUID patientId;
        public UUID templateId;

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
}
