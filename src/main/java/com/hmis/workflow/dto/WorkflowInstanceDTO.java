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
    private UUID patientId;
    private String patientName;
    private UUID templateId;
    private String templateName;
    private List<TaskInstanceDTO> taskInstances;
    private Integer progressPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
