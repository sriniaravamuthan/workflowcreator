package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for configuring external notifications on a task definition.
 *
 * Notification Types:
 * - NONE: No external notification (default)
 * - KAFKA: Publish to configured Kafka topic
 * - API: Call configured HTTP API endpoint
 * - BOTH: Both Kafka and API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskNotificationConfigRequest {

    /**
     * Type of external notification to send when task completes.
     * Values: NONE, KAFKA, API, BOTH
     */
    private String notificationType;

    /**
     * Kafka topic name for task completion events.
     * Required when notificationType is KAFKA or BOTH.
     * Example: "lab-orders-completed", "pharmacy-dispense-requests"
     */
    private String notificationKafkaTopic;

    /**
     * External API endpoint URL for task completion notification.
     * Required when notificationType is API or BOTH.
     * Example: "https://lab-system.hospital.com/api/orders/complete"
     */
    private String notificationApiEndpoint;

    /**
     * HTTP method for API notification.
     * Values: POST, PUT (default: POST)
     */
    private String notificationApiMethod;

    /**
     * Message/payload template for notifications (JSON format).
     * Supports placeholders for dynamic values:
     * - ${taskInstanceId} - Task instance UUID
     * - ${taskName} - Name of the task
     * - ${taskResult} - Result string from task completion
     * - ${workflowInstanceId} - Workflow instance UUID
     * - ${patientId} - Patient UUID
     * - ${patientMrn} - Patient MRN
     * - ${completedAt} - Completion timestamp
     * - ${completedBy} - User who completed the task
     * - ${status} - Task status (COMPLETED, FAILED, SKIPPED)
     * - ${orderId} - Associated order ID (if any)
     * - ${orderCode} - Associated order code (if any)
     *
     * Example template:
     * {
     *   "eventType": "TASK_COMPLETED",
     *   "taskId": "${taskInstanceId}",
     *   "taskName": "${taskName}",
     *   "result": "${taskResult}",
     *   "patientMrn": "${patientMrn}",
     *   "completedAt": "${completedAt}",
     *   "completedBy": "${completedBy}"
     * }
     */
    private String notificationMessageTemplate;

    /**
     * Additional HTTP headers for API notification (JSON format).
     * Example: {"X-API-Key": "secret123", "X-Source": "workflow-engine"}
     */
    private String notificationApiHeaders;

    /**
     * Whether to send notification on task failure.
     * Default: true
     */
    private Boolean notifyOnFailure;

    /**
     * Whether to send notification on task skip.
     * Default: false
     */
    private Boolean notifyOnSkip;
}
