package com.hmis.workflow.domain.event;

import com.hmis.workflow.domain.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * TaskEvent represents an event that is published to Kafka when a task status changes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String taskInstanceId;
    private String workflowInstanceId;
    private String patientId;
    private TaskStatus status;
    private String taskName;
    private LocalDateTime eventTime;
    private String errorMessage;
    private Map<String, Object> metadata;
    private String eventType; // TASK_CREATED, TASK_STARTED, TASK_COMPLETED, TASK_FAILED

    public static TaskEvent taskCreated(String taskInstanceId, String workflowInstanceId,
                                        String patientId, String taskName, Map<String, Object> metadata) {
        return TaskEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .taskInstanceId(taskInstanceId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(TaskStatus.PENDING)
                .taskName(taskName)
                .eventTime(LocalDateTime.now())
                .eventType("TASK_CREATED")
                .metadata(metadata)
                .build();
    }

    public static TaskEvent taskStarted(String taskInstanceId, String workflowInstanceId,
                                       String patientId, String taskName) {
        return TaskEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .taskInstanceId(taskInstanceId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(TaskStatus.IN_PROGRESS)
                .taskName(taskName)
                .eventTime(LocalDateTime.now())
                .eventType("TASK_STARTED")
                .build();
    }

    public static TaskEvent taskCompleted(String taskInstanceId, String workflowInstanceId,
                                         String patientId, String taskName, Map<String, Object> metadata) {
        return TaskEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .taskInstanceId(taskInstanceId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(TaskStatus.COMPLETED)
                .taskName(taskName)
                .eventTime(LocalDateTime.now())
                .eventType("TASK_COMPLETED")
                .metadata(metadata)
                .build();
    }

    public static TaskEvent taskFailed(String taskInstanceId, String workflowInstanceId,
                                      String patientId, String taskName, String errorMessage) {
        return TaskEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .taskInstanceId(taskInstanceId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(TaskStatus.FAILED)
                .taskName(taskName)
                .eventTime(LocalDateTime.now())
                .eventType("TASK_FAILED")
                .errorMessage(errorMessage)
                .build();
    }
}
