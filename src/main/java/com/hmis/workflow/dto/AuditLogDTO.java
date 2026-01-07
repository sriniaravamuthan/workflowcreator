package com.hmis.workflow.dto;

import com.hmis.workflow.domain.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for audit log entries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {

    private UUID id;
    private String entityType;
    private String entityId;
    private String action;
    private String actor;
    private LocalDateTime actionTimestamp;
    private String details;
    private String previousValue;
    private String newValue;
    private String correlationId;
    private String patientId;
    private String workflowInstanceId;
    private Boolean isLegalHold;

    /**
     * Create DTO from entity.
     */
    public static AuditLogDTO fromEntity(AuditLog entity) {
        return AuditLogDTO.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .actor(entity.getActor())
                .actionTimestamp(entity.getActionTimestamp())
                .details(entity.getDetails())
                .previousValue(entity.getPreviousValue())
                .newValue(entity.getNewValue())
                .correlationId(entity.getCorrelationId())
                .patientId(entity.getPatientId())
                .workflowInstanceId(entity.getWorkflowInstanceId())
                .isLegalHold(entity.getIsLegalHold())
                .build();
    }
}
