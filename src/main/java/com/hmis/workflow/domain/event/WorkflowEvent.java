package com.hmis.workflow.domain.event;

import com.hmis.workflow.domain.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * WorkflowEvent represents workflow instance lifecycle events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String workflowInstanceId;
    private String patientId;
    private WorkflowStatus status;
    private String templateName;
    private LocalDateTime eventTime;
    private String escalationReason;
    private Integer progressPercentage;
    private Map<String, Object> metadata;
    private String eventType; // WORKFLOW_CREATED, WORKFLOW_STARTED, WORKFLOW_COMPLETED, etc.
    private String correlationId;

    public static WorkflowEvent workflowStarted(String workflowInstanceId, String patientId,
                                               String templateName) {
        return WorkflowEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(WorkflowStatus.ACTIVE)
                .templateName(templateName)
                .eventTime(LocalDateTime.now())
                .eventType("WORKFLOW_STARTED")
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    public static WorkflowEvent workflowCompleted(String workflowInstanceId, String patientId) {
        return WorkflowEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(WorkflowStatus.COMPLETED)
                .progressPercentage(100)
                .eventTime(LocalDateTime.now())
                .eventType("WORKFLOW_COMPLETED")
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    public static WorkflowEvent workflowFailed(String workflowInstanceId, String patientId,
                                              String reason) {
        return WorkflowEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(WorkflowStatus.FAILED)
                .eventTime(LocalDateTime.now())
                .eventType("WORKFLOW_FAILED")
                .escalationReason(reason)
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    public static WorkflowEvent workflowEscalated(String workflowInstanceId, String patientId,
                                                 String reason) {
        return WorkflowEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .eventTime(LocalDateTime.now())
                .eventType("WORKFLOW_ESCALATED")
                .escalationReason(reason)
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }
}
