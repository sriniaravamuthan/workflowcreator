package com.hmis.workflow.dto;

import com.hmis.workflow.domain.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for TaskInstance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskInstanceDTO {

    private UUID id;
    private String taskInstanceId;
    private TaskStatus status;
    private String assignedTo;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String comments;
    private String result;
    private Integer retryCount;
    private Integer maxRetries;
    private String errorMessage;
    private UUID workflowInstanceId;
    private String taskName;
    private String taskDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
