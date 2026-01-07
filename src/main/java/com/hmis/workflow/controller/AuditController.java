package com.hmis.workflow.controller;

import com.hmis.workflow.dto.AuditLogDTO;
import com.hmis.workflow.dto.LegalHoldRequest;
import com.hmis.workflow.dto.LegalHoldResponse;
import com.hmis.workflow.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for audit trail operations.
 * Provides endpoints for querying audit history and managing legal holds.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit", description = "Audit trail and compliance operations")
public class AuditController {

    private final AuditService auditService;

    // ==================== Audit History Endpoints ====================

    @GetMapping("/entity/{entityId}")
    @Operation(summary = "Get audit history for a specific entity",
               description = "Returns all audit log entries for the specified entity ID")
    public ResponseEntity<List<AuditLogDTO>> getEntityHistory(
            @PathVariable String entityId) {
        log.info("Getting audit history for entity: {}", entityId);
        List<AuditLogDTO> history = auditService.getEntityHistory(entityId).stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/workflow/{workflowInstanceId}")
    @Operation(summary = "Get audit history for a workflow instance",
               description = "Returns all audit log entries for tasks and events in the workflow")
    public ResponseEntity<List<AuditLogDTO>> getWorkflowHistory(
            @PathVariable UUID workflowInstanceId) {
        log.info("Getting audit history for workflow: {}", workflowInstanceId);
        List<AuditLogDTO> history = auditService.getWorkflowHistory(workflowInstanceId).stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get audit history for a patient",
               description = "Returns all audit log entries related to the specified patient")
    public ResponseEntity<List<AuditLogDTO>> getPatientHistory(
            @PathVariable UUID patientId) {
        log.info("Getting audit history for patient: {}", patientId);
        List<AuditLogDTO> history = auditService.getPatientHistory(patientId).stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/actor/{actor}")
    @Operation(summary = "Get audit history by actor",
               description = "Returns all actions performed by the specified user")
    public ResponseEntity<List<AuditLogDTO>> getActorHistory(
            @PathVariable String actor) {
        log.info("Getting audit history for actor: {}", actor);
        List<AuditLogDTO> history = auditService.getActorHistory(actor).stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get audit history by date range",
               description = "Returns all audit log entries within the specified date range")
    public ResponseEntity<List<AuditLogDTO>> getHistoryByDateRange(
            @Parameter(description = "Start date (ISO format: yyyy-MM-ddTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format: yyyy-MM-ddTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Getting audit history from {} to {}", startDate, endDate);
        List<AuditLogDTO> history = auditService.getHistoryByDateRange(startDate, endDate).stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    // ==================== Legal Hold Endpoints ====================

    @GetMapping("/legal-hold")
    @Operation(summary = "Get all logs under legal hold",
               description = "Returns all audit log entries that are currently under legal hold")
    public ResponseEntity<List<AuditLogDTO>> getLegalHoldLogs() {
        log.info("Getting all logs under legal hold");
        List<AuditLogDTO> logs = auditService.getLegalHoldLogs().stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/legal-hold/patient/{patientId}")
    @Operation(summary = "Set legal hold for a patient",
               description = "Places all audit logs related to the patient under legal hold, preventing deletion")
    public ResponseEntity<LegalHoldResponse> setLegalHoldForPatient(
            @PathVariable String patientId,
            @RequestBody LegalHoldRequest request) {
        log.info("Setting legal hold for patient: {} by actor: {}", patientId, request.getActor());

        if (request.getReason() == null || request.getReason().isBlank()) {
            return ResponseEntity.badRequest().body(LegalHoldResponse.builder()
                    .entityId(patientId)
                    .entityType("PATIENT")
                    .message("Reason is required for legal hold operations")
                    .build());
        }

        int affected = auditService.setLegalHoldForPatient(patientId, request.getActor(), request.getReason());

        return ResponseEntity.ok(LegalHoldResponse.builder()
                .entityId(patientId)
                .entityType("PATIENT")
                .action("SET")
                .affectedRecords(affected)
                .reason(request.getReason())
                .actor(request.getActor())
                .message("Legal hold set successfully for " + affected + " records")
                .build());
    }

    @DeleteMapping("/legal-hold/patient/{patientId}")
    @Operation(summary = "Release legal hold for a patient",
               description = "Releases legal hold from all audit logs related to the patient")
    public ResponseEntity<LegalHoldResponse> releaseLegalHoldForPatient(
            @PathVariable String patientId,
            @RequestBody LegalHoldRequest request) {
        log.info("Releasing legal hold for patient: {} by actor: {}", patientId, request.getActor());

        if (request.getReason() == null || request.getReason().isBlank()) {
            return ResponseEntity.badRequest().body(LegalHoldResponse.builder()
                    .entityId(patientId)
                    .entityType("PATIENT")
                    .message("Reason is required for legal hold operations")
                    .build());
        }

        int affected = auditService.releaseLegalHoldForPatient(patientId, request.getActor(), request.getReason());

        return ResponseEntity.ok(LegalHoldResponse.builder()
                .entityId(patientId)
                .entityType("PATIENT")
                .action("RELEASED")
                .affectedRecords(affected)
                .reason(request.getReason())
                .actor(request.getActor())
                .message("Legal hold released for " + affected + " records")
                .build());
    }

    @PostMapping("/legal-hold/workflow/{workflowInstanceId}")
    @Operation(summary = "Set legal hold for a workflow",
               description = "Places all audit logs related to the workflow under legal hold")
    public ResponseEntity<LegalHoldResponse> setLegalHoldForWorkflow(
            @PathVariable String workflowInstanceId,
            @RequestBody LegalHoldRequest request) {
        log.info("Setting legal hold for workflow: {} by actor: {}", workflowInstanceId, request.getActor());

        if (request.getReason() == null || request.getReason().isBlank()) {
            return ResponseEntity.badRequest().body(LegalHoldResponse.builder()
                    .entityId(workflowInstanceId)
                    .entityType("WORKFLOW_INSTANCE")
                    .message("Reason is required for legal hold operations")
                    .build());
        }

        int affected = auditService.setLegalHoldForWorkflow(workflowInstanceId, request.getActor(), request.getReason());

        return ResponseEntity.ok(LegalHoldResponse.builder()
                .entityId(workflowInstanceId)
                .entityType("WORKFLOW_INSTANCE")
                .action("SET")
                .affectedRecords(affected)
                .reason(request.getReason())
                .actor(request.getActor())
                .message("Legal hold set successfully for " + affected + " records")
                .build());
    }

    // ==================== Task History Shortcuts ====================

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Get audit history for a task",
               description = "Returns all audit log entries for the specified task instance")
    public ResponseEntity<List<AuditLogDTO>> getTaskHistory(
            @PathVariable String taskId) {
        log.info("Getting audit history for task: {}", taskId);
        List<AuditLogDTO> history = auditService.getEntityHistory(taskId).stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }
}
