package com.hmis.workflow.dto;

import com.hmis.workflow.domain.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for WorkflowInstance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstanceDTO {

    private UUID id;
    private String workflowInstanceId;
    private WorkflowStatus status;
    private String notes;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Patient context
    private UUID patientId;
    private String patientName;

    // Encounter/Visit context
    private String encounterId;  // Clinical encounter ID (e.g., ER visit, admission)
    private String visitId;      // ADT visit tracking ID

    // Template info
    private UUID templateId;
    private String templateName;

    // Task details
    private List<TaskInstanceDTO> taskInstances;
    private Integer progressPercentage;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
