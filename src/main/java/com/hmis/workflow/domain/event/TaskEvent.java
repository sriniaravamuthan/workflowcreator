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
 * TaskEvent represents an event that is published to Kafka when a task status changes.
 *
 * Enhanced with result data, order details, and patient information for downstream system integration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEvent implements Serializable {

    private static final long serialVersionUID = 2L;

    private String eventId;
    private String taskInstanceId;
    private String workflowInstanceId;
    private String patientId;
    private TaskStatus status;
    private String taskName;
    private LocalDateTime eventTime;
    private String errorMessage;
    private Map<String, Object> metadata;
    private String eventType; // TASK_CREATED, TASK_STARTED, TASK_COMPLETED, TASK_FAILED, TASK_SKIPPED

    // Enhanced fields for downstream system integration
    private String taskResult;           // The result/output of the completed task
    private String taskDescription;      // Description of the task
    private String completedByUser;      // User who completed/failed/skipped the task
    private LocalDateTime startedAt;     // When the task was started
    private LocalDateTime completedAt;   // When the task was completed/failed/skipped

    // Patient details for downstream systems
    private String patientMrn;           // Patient Medical Record Number
    private String patientFirstName;
    private String patientLastName;

    // Order details (if task is associated with an order)
    private String orderId;              // Associated order ID
    private String orderCode;            // Order code (e.g., LAB-CBC, PHARM-DISPENSE)
    private String orderType;            // Order type (LAB, PHARMACY, IMAGING, etc.)

    // Skip reason (for TASK_SKIPPED events)
    private String skipReason;

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
                .startedAt(LocalDateTime.now())
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
                .completedAt(LocalDateTime.now())
                .metadata(metadata)
                .build();
    }

    /**
     * Create a task completed event with full result data for downstream integration.
     */
    public static TaskEvent taskCompletedWithDetails(String taskInstanceId, String workflowInstanceId,
                                                     String patientId, String taskName, String taskResult,
                                                     String completedByUser, String patientMrn,
                                                     String patientFirstName, String patientLastName,
                                                     String orderId, String orderCode, String orderType,
                                                     LocalDateTime startedAt, LocalDateTime completedAt,
                                                     Map<String, Object> metadata) {
        return TaskEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .taskInstanceId(taskInstanceId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(TaskStatus.COMPLETED)
                .taskName(taskName)
                .taskResult(taskResult)
                .completedByUser(completedByUser)
                .eventTime(LocalDateTime.now())
                .eventType("TASK_COMPLETED")
                .patientMrn(patientMrn)
                .patientFirstName(patientFirstName)
                .patientLastName(patientLastName)
                .orderId(orderId)
                .orderCode(orderCode)
                .orderType(orderType)
                .startedAt(startedAt)
                .completedAt(completedAt)
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
                .completedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create a task failed event with full details for downstream integration.
     */
    public static TaskEvent taskFailedWithDetails(String taskInstanceId, String workflowInstanceId,
                                                  String patientId, String taskName, String errorMessage,
                                                  String failedByUser, String patientMrn,
                                                  String patientFirstName, String patientLastName,
                                                  String orderId, String orderCode, String orderType,
                                                  LocalDateTime startedAt, LocalDateTime completedAt) {
        return TaskEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .taskInstanceId(taskInstanceId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(TaskStatus.FAILED)
                .taskName(taskName)
                .errorMessage(errorMessage)
                .completedByUser(failedByUser)
                .eventTime(LocalDateTime.now())
                .eventType("TASK_FAILED")
                .patientMrn(patientMrn)
                .patientFirstName(patientFirstName)
                .patientLastName(patientLastName)
                .orderId(orderId)
                .orderCode(orderCode)
                .orderType(orderType)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .build();
    }

    /**
     * Create a task skipped event.
     */
    public static TaskEvent taskSkipped(String taskInstanceId, String workflowInstanceId,
                                        String patientId, String taskName, String skipReason,
                                        String skippedByUser) {
        return TaskEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .taskInstanceId(taskInstanceId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(TaskStatus.SKIPPED)
                .taskName(taskName)
                .skipReason(skipReason)
                .completedByUser(skippedByUser)
                .eventTime(LocalDateTime.now())
                .eventType("TASK_SKIPPED")
                .completedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create a task skipped event with full details for downstream integration.
     */
    public static TaskEvent taskSkippedWithDetails(String taskInstanceId, String workflowInstanceId,
                                                   String patientId, String taskName, String skipReason,
                                                   String skippedByUser, String patientMrn,
                                                   String patientFirstName, String patientLastName,
                                                   String orderId, String orderCode, String orderType) {
        return TaskEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .taskInstanceId(taskInstanceId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(TaskStatus.SKIPPED)
                .taskName(taskName)
                .skipReason(skipReason)
                .completedByUser(skippedByUser)
                .eventTime(LocalDateTime.now())
                .eventType("TASK_SKIPPED")
                .completedAt(LocalDateTime.now())
                .patientMrn(patientMrn)
                .patientFirstName(patientFirstName)
                .patientLastName(patientLastName)
                .orderId(orderId)
                .orderCode(orderCode)
                .orderType(orderType)
                .build();
    }
}
