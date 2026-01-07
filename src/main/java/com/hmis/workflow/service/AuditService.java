package com.hmis.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmis.workflow.config.AuditContext;
import com.hmis.workflow.domain.entity.AuditLog;
import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.entity.Order;
import com.hmis.workflow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing audit logs.
 * Provides methods for logging all entity changes, status transitions,
 * and user actions in the workflow system.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // Entity type constants
    public static final String ENTITY_WORKFLOW_TEMPLATE = "WORKFLOW_TEMPLATE";
    public static final String ENTITY_WORKFLOW_INSTANCE = "WORKFLOW_INSTANCE";
    public static final String ENTITY_TASK_INSTANCE = "TASK_INSTANCE";
    public static final String ENTITY_TASK_DEFINITION = "TASK_DEFINITION";
    public static final String ENTITY_ORDER = "ORDER";
    public static final String ENTITY_PATIENT = "PATIENT";

    // Action type constants
    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_UPDATED = "UPDATED";
    public static final String ACTION_DELETED = "DELETED";
    public static final String ACTION_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String ACTION_ASSIGNED = "ASSIGNED";
    public static final String ACTION_STARTED = "STARTED";
    public static final String ACTION_COMPLETED = "COMPLETED";
    public static final String ACTION_FAILED = "FAILED";
    public static final String ACTION_SKIPPED = "SKIPPED";
    public static final String ACTION_CANCELLED = "CANCELLED";
    public static final String ACTION_NOTIFICATION_CONFIGURED = "NOTIFICATION_CONFIGURED";
    public static final String ACTION_LEGAL_HOLD_SET = "LEGAL_HOLD_SET";
    public static final String ACTION_LEGAL_HOLD_RELEASED = "LEGAL_HOLD_RELEASED";

    /**
     * Log a workflow instance status change.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logWorkflowStatusChange(WorkflowInstance workflow, String previousStatus, String newStatus, String actor) {
        Map<String, Object> details = new HashMap<>();
        details.put("workflowName", workflow.getWorkflowTemplate() != null ? workflow.getWorkflowTemplate().getName() : null);
        details.put("patientId", workflow.getPatient() != null ? workflow.getPatient().getId().toString() : null);
        details.put("previousStatus", previousStatus);
        details.put("newStatus", newStatus);

        return createAuditLog(
                ENTITY_WORKFLOW_INSTANCE,
                workflow.getId().toString(),
                ACTION_STATUS_CHANGED,
                actor,
                details,
                previousStatus,
                newStatus,
                workflow.getPatient() != null ? workflow.getPatient().getId().toString() : null,
                workflow.getId().toString()
        );
    }

    /**
     * Log a workflow instance creation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logWorkflowCreated(WorkflowInstance workflow, String actor) {
        Map<String, Object> details = new HashMap<>();
        details.put("workflowName", workflow.getWorkflowTemplate() != null ? workflow.getWorkflowTemplate().getName() : null);
        details.put("patientId", workflow.getPatient() != null ? workflow.getPatient().getId().toString() : null);
        details.put("status", workflow.getStatus() != null ? workflow.getStatus().name() : null);

        return createAuditLog(
                ENTITY_WORKFLOW_INSTANCE,
                workflow.getId().toString(),
                ACTION_CREATED,
                actor,
                details,
                null,
                workflow.getStatus() != null ? workflow.getStatus().name() : null,
                workflow.getPatient() != null ? workflow.getPatient().getId().toString() : null,
                workflow.getId().toString()
        );
    }

    /**
     * Log a task instance status change.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logTaskStatusChange(TaskInstance task, String previousStatus, String newStatus, String actor) {
        Map<String, Object> details = new HashMap<>();
        details.put("taskName", task.getTaskName());
        details.put("isAdhoc", task.getIsAdhoc());
        details.put("previousStatus", previousStatus);
        details.put("newStatus", newStatus);
        details.put("workflowInstanceId", task.getWorkflowInstance() != null ? task.getWorkflowInstance().getId().toString() : null);

        String patientId = null;
        String workflowId = null;
        if (task.getWorkflowInstance() != null) {
            workflowId = task.getWorkflowInstance().getId().toString();
            if (task.getWorkflowInstance().getPatient() != null) {
                patientId = task.getWorkflowInstance().getPatient().getId().toString();
            }
        }

        return createAuditLog(
                ENTITY_TASK_INSTANCE,
                task.getId().toString(),
                ACTION_STATUS_CHANGED,
                actor,
                details,
                previousStatus,
                newStatus,
                patientId,
                workflowId
        );
    }

    /**
     * Log a task completion with result data.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logTaskCompleted(TaskInstance task, String result, String actor) {
        Map<String, Object> details = new HashMap<>();
        details.put("taskName", task.getTaskName());
        details.put("result", result);
        details.put("completedAt", task.getCompletedAt() != null ? task.getCompletedAt().toString() : null);

        String patientId = null;
        String workflowId = null;
        if (task.getWorkflowInstance() != null) {
            workflowId = task.getWorkflowInstance().getId().toString();
            if (task.getWorkflowInstance().getPatient() != null) {
                patientId = task.getWorkflowInstance().getPatient().getId().toString();
            }
        }

        return createAuditLog(
                ENTITY_TASK_INSTANCE,
                task.getId().toString(),
                ACTION_COMPLETED,
                actor,
                details,
                task.getStatus() != null ? task.getStatus().name() : "IN_PROGRESS",
                "COMPLETED",
                patientId,
                workflowId
        );
    }

    /**
     * Log a task failure.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logTaskFailed(TaskInstance task, String errorMessage, String actor) {
        Map<String, Object> details = new HashMap<>();
        details.put("taskName", task.getTaskName());
        details.put("errorMessage", errorMessage);
        details.put("failedAt", LocalDateTime.now().toString());

        String patientId = null;
        String workflowId = null;
        if (task.getWorkflowInstance() != null) {
            workflowId = task.getWorkflowInstance().getId().toString();
            if (task.getWorkflowInstance().getPatient() != null) {
                patientId = task.getWorkflowInstance().getPatient().getId().toString();
            }
        }

        return createAuditLog(
                ENTITY_TASK_INSTANCE,
                task.getId().toString(),
                ACTION_FAILED,
                actor,
                details,
                task.getStatus() != null ? task.getStatus().name() : "IN_PROGRESS",
                "FAILED",
                patientId,
                workflowId
        );
    }

    /**
     * Log a task skip.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logTaskSkipped(TaskInstance task, String reason, String actor) {
        Map<String, Object> details = new HashMap<>();
        details.put("taskName", task.getTaskName());
        details.put("skipReason", reason);
        details.put("skippedAt", LocalDateTime.now().toString());

        String patientId = null;
        String workflowId = null;
        if (task.getWorkflowInstance() != null) {
            workflowId = task.getWorkflowInstance().getId().toString();
            if (task.getWorkflowInstance().getPatient() != null) {
                patientId = task.getWorkflowInstance().getPatient().getId().toString();
            }
        }

        return createAuditLog(
                ENTITY_TASK_INSTANCE,
                task.getId().toString(),
                ACTION_SKIPPED,
                actor,
                details,
                task.getStatus() != null ? task.getStatus().name() : "PENDING",
                "SKIPPED",
                patientId,
                workflowId
        );
    }

    /**
     * Log a task assignment.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logTaskAssigned(TaskInstance task, String previousAssignee, String newAssignee, String actor) {
        Map<String, Object> details = new HashMap<>();
        details.put("taskName", task.getTaskName());
        details.put("previousAssignee", previousAssignee);
        details.put("newAssignee", newAssignee);

        String patientId = null;
        String workflowId = null;
        if (task.getWorkflowInstance() != null) {
            workflowId = task.getWorkflowInstance().getId().toString();
            if (task.getWorkflowInstance().getPatient() != null) {
                patientId = task.getWorkflowInstance().getPatient().getId().toString();
            }
        }

        return createAuditLog(
                ENTITY_TASK_INSTANCE,
                task.getId().toString(),
                ACTION_ASSIGNED,
                actor,
                details,
                previousAssignee,
                newAssignee,
                patientId,
                workflowId
        );
    }

    /**
     * Log an order status change.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logOrderStatusChange(Order order, String previousStatus, String newStatus, String actor) {
        Map<String, Object> details = new HashMap<>();
        details.put("orderCode", order.getCode());
        details.put("orderType", order.getOrderType());
        details.put("previousStatus", previousStatus);
        details.put("newStatus", newStatus);

        String patientId = order.getPatient() != null ? order.getPatient().getId().toString() : null;
        String workflowId = order.getWorkflowInstance() != null ? order.getWorkflowInstance().getId().toString() : null;

        return createAuditLog(
                ENTITY_ORDER,
                order.getId().toString(),
                ACTION_STATUS_CHANGED,
                actor,
                details,
                previousStatus,
                newStatus,
                patientId,
                workflowId
        );
    }

    /**
     * Log a generic entity change with field-level tracking.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logEntityChange(String entityType, String entityId, String action,
                                     Map<String, Object> changes, String actor,
                                     String patientId, String workflowInstanceId) {
        return createAuditLog(
                entityType,
                entityId,
                action,
                actor,
                changes,
                null,
                null,
                patientId,
                workflowInstanceId
        );
    }

    /**
     * Log a field-level change.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logFieldChange(String entityType, String entityId, String fieldName,
                                    String previousValue, String newValue, String actor,
                                    String patientId, String workflowInstanceId) {
        Map<String, Object> details = new HashMap<>();
        details.put("fieldName", fieldName);
        details.put("previousValue", previousValue);
        details.put("newValue", newValue);

        return createAuditLog(
                entityType,
                entityId,
                ACTION_UPDATED,
                actor,
                details,
                previousValue,
                newValue,
                patientId,
                workflowInstanceId
        );
    }

    /**
     * Set legal hold on audit logs for a patient.
     */
    @Transactional
    public int setLegalHoldForPatient(String patientId, String actor, String reason) {
        List<AuditLog> logs = auditLogRepository.findByPatientIdOrderByActionTimestampDesc(UUID.fromString(patientId));
        int count = 0;
        for (AuditLog auditLog : logs) {
            if (!Boolean.TRUE.equals(auditLog.getIsLegalHold())) {
                auditLog.setIsLegalHold(true);
                auditLogRepository.save(auditLog);
                count++;
            }
        }

        // Log the legal hold action itself
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("affectedRecords", count);
        createAuditLog(
                ENTITY_PATIENT,
                patientId,
                ACTION_LEGAL_HOLD_SET,
                actor,
                details,
                "false",
                "true",
                patientId,
                null
        );

        log.info("Legal hold set for patient {} by {}. {} records affected.", patientId, actor, count);
        return count;
    }

    /**
     * Release legal hold for a patient's audit logs.
     */
    @Transactional
    public int releaseLegalHoldForPatient(String patientId, String actor, String reason) {
        List<AuditLog> logs = auditLogRepository.findByPatientIdOrderByActionTimestampDesc(UUID.fromString(patientId));
        int count = 0;
        for (AuditLog auditLog : logs) {
            if (Boolean.TRUE.equals(auditLog.getIsLegalHold())) {
                auditLog.setIsLegalHold(false);
                auditLogRepository.save(auditLog);
                count++;
            }
        }

        // Log the legal hold release
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("affectedRecords", count);
        createAuditLog(
                ENTITY_PATIENT,
                patientId,
                ACTION_LEGAL_HOLD_RELEASED,
                actor,
                details,
                "true",
                "false",
                patientId,
                null
        );

        log.info("Legal hold released for patient {} by {}. {} records affected.", patientId, actor, count);
        return count;
    }

    /**
     * Set legal hold on a specific workflow instance's audit logs.
     */
    @Transactional
    public int setLegalHoldForWorkflow(String workflowInstanceId, String actor, String reason) {
        List<AuditLog> logs = auditLogRepository.findByWorkflowInstanceIdOrderByActionTimestampDesc(UUID.fromString(workflowInstanceId));
        int count = 0;
        for (AuditLog auditLog : logs) {
            if (!Boolean.TRUE.equals(auditLog.getIsLegalHold())) {
                auditLog.setIsLegalHold(true);
                auditLogRepository.save(auditLog);
                count++;
            }
        }

        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("affectedRecords", count);
        createAuditLog(
                ENTITY_WORKFLOW_INSTANCE,
                workflowInstanceId,
                ACTION_LEGAL_HOLD_SET,
                actor,
                details,
                "false",
                "true",
                null,
                workflowInstanceId
        );

        log.info("Legal hold set for workflow {} by {}. {} records affected.", workflowInstanceId, actor, count);
        return count;
    }

    // Query methods

    /**
     * Get audit history for a specific entity.
     */
    public List<AuditLog> getEntityHistory(String entityId) {
        return auditLogRepository.findByEntityIdOrderByActionTimestampDesc(entityId);
    }

    /**
     * Get audit history for a workflow instance.
     */
    public List<AuditLog> getWorkflowHistory(UUID workflowInstanceId) {
        return auditLogRepository.findByWorkflowInstanceIdOrderByActionTimestampDesc(workflowInstanceId);
    }

    /**
     * Get audit history for a patient.
     */
    public List<AuditLog> getPatientHistory(UUID patientId) {
        return auditLogRepository.findByPatientIdOrderByActionTimestampDesc(patientId);
    }

    /**
     * Get audit history by actor (user).
     */
    public List<AuditLog> getActorHistory(String actor) {
        return auditLogRepository.findByActor(actor);
    }

    /**
     * Get audit logs within a date range.
     */
    public List<AuditLog> getHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Get all logs under legal hold.
     */
    public List<AuditLog> getLegalHoldLogs() {
        return auditLogRepository.findLegalHoldLogs();
    }

    /**
     * Get paginated audit history for a workflow.
     */
    public Page<AuditLog> getWorkflowHistoryPaginated(UUID workflowInstanceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "actionTimestamp"));
        return auditLogRepository.findAll(pageable);
    }

    // Private helper methods

    private AuditLog createAuditLog(String entityType, String entityId, String action,
                                     String actor, Map<String, Object> details,
                                     String previousValue, String newValue,
                                     String patientId, String workflowInstanceId) {
        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit details: {}", e.getMessage());
            detailsJson = "{}";
        }

        // Use provided actor, fall back to context, then to SYSTEM
        String effectiveActor = actor;
        if (effectiveActor == null || effectiveActor.isBlank()) {
            effectiveActor = AuditContext.getCurrentUser();
        }
        if (effectiveActor == null || effectiveActor.isBlank()) {
            effectiveActor = "SYSTEM";
        }

        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(effectiveActor)
                .actionTimestamp(LocalDateTime.now())
                .details(detailsJson)
                .previousValue(truncateValue(previousValue))
                .newValue(truncateValue(newValue))
                .correlationId(AuditContext.getCorrelationId())
                .patientId(patientId)
                .workflowInstanceId(workflowInstanceId)
                .isLegalHold(false)
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} {} {} by {}", entityType, entityId, action, effectiveActor);
        return saved;
    }

    private String truncateValue(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 100 ? value.substring(0, 97) + "..." : value;
    }
}
