package com.hmis.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmis.workflow.domain.entity.Patient;
import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.entity.WorkflowTaskDefinition;
import com.hmis.workflow.domain.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending external notifications when tasks complete, fail, or are skipped.
 * Supports both Kafka topic publishing and HTTP API calls based on task definition configuration.
 *
 * Notification Types:
 * - NONE: No external notification (default)
 * - KAFKA: Publish to configured Kafka topic
 * - API: Call configured HTTP API endpoint
 * - BOTH: Both Kafka and API
 *
 * Message Templates:
 * Supports placeholder substitution for dynamic values like ${taskInstanceId}, ${patientMrn}, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalNotificationService {

    private final KafkaTemplate<String, String> kafkaStringTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${external.notification.enabled:true}")
    private boolean notificationEnabled;

    @Value("${external.notification.api.timeout:5000}")
    private long apiTimeout;

    @Value("${external.notification.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${external.notification.retry.max-attempts:3}")
    private int maxRetryAttempts;

    private static final String NOTIFICATION_TYPE_NONE = "NONE";
    private static final String NOTIFICATION_TYPE_KAFKA = "KAFKA";
    private static final String NOTIFICATION_TYPE_API = "API";
    private static final String NOTIFICATION_TYPE_BOTH = "BOTH";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Send external notification for task completion.
     *
     * @param task The completed task instance
     * @param result The task result
     * @param completedByUser The user who completed the task
     */
    @Async
    public void notifyTaskCompleted(TaskInstance task, String result, String completedByUser) {
        if (!notificationEnabled) {
            log.debug("External notifications are disabled");
            return;
        }

        WorkflowTaskDefinition taskDef = task.getTaskDefinition();
        if (taskDef == null) {
            log.debug("Task {} is an ad-hoc task, skipping external notification", task.getId());
            return;
        }

        String notificationType = taskDef.getNotificationType();
        if (notificationType == null || NOTIFICATION_TYPE_NONE.equalsIgnoreCase(notificationType)) {
            log.debug("No external notification configured for task definition: {}", taskDef.getName());
            return;
        }

        Map<String, String> variables = buildVariables(task, result, completedByUser, TaskStatus.COMPLETED);
        String payload = buildPayload(taskDef.getNotificationMessageTemplate(), variables);

        log.info("Sending external notification for completed task: {} (type: {})",
                task.getTaskName(), notificationType);

        sendNotification(taskDef, payload, task, "TASK_COMPLETED");
    }

    /**
     * Send external notification for task failure.
     *
     * @param task The failed task instance
     * @param errorMessage The error message
     * @param failedByUser The user who marked the task as failed
     */
    @Async
    public void notifyTaskFailed(TaskInstance task, String errorMessage, String failedByUser) {
        if (!notificationEnabled) {
            return;
        }

        WorkflowTaskDefinition taskDef = task.getTaskDefinition();
        if (taskDef == null) {
            return;
        }

        // Check if failure notification is enabled
        if (!Boolean.TRUE.equals(taskDef.getNotifyOnFailure())) {
            log.debug("Failure notification disabled for task definition: {}", taskDef.getName());
            return;
        }

        String notificationType = taskDef.getNotificationType();
        if (notificationType == null || NOTIFICATION_TYPE_NONE.equalsIgnoreCase(notificationType)) {
            return;
        }

        Map<String, String> variables = buildVariables(task, errorMessage, failedByUser, TaskStatus.FAILED);
        variables.put("errorMessage", errorMessage != null ? errorMessage : "");
        String payload = buildPayload(taskDef.getNotificationMessageTemplate(), variables);

        log.info("Sending external notification for failed task: {} (type: {})",
                task.getTaskName(), notificationType);

        sendNotification(taskDef, payload, task, "TASK_FAILED");
    }

    /**
     * Send external notification for task skip.
     *
     * @param task The skipped task instance
     * @param reason The skip reason
     * @param skippedByUser The user who skipped the task
     */
    @Async
    public void notifyTaskSkipped(TaskInstance task, String reason, String skippedByUser) {
        if (!notificationEnabled) {
            return;
        }

        WorkflowTaskDefinition taskDef = task.getTaskDefinition();
        if (taskDef == null) {
            return;
        }

        // Check if skip notification is enabled
        if (!Boolean.TRUE.equals(taskDef.getNotifyOnSkip())) {
            log.debug("Skip notification disabled for task definition: {}", taskDef.getName());
            return;
        }

        String notificationType = taskDef.getNotificationType();
        if (notificationType == null || NOTIFICATION_TYPE_NONE.equalsIgnoreCase(notificationType)) {
            return;
        }

        Map<String, String> variables = buildVariables(task, reason, skippedByUser, TaskStatus.SKIPPED);
        variables.put("skipReason", reason != null ? reason : "");
        String payload = buildPayload(taskDef.getNotificationMessageTemplate(), variables);

        log.info("Sending external notification for skipped task: {} (type: {})",
                task.getTaskName(), notificationType);

        sendNotification(taskDef, payload, task, "TASK_SKIPPED");
    }

    /**
     * Send notification based on task definition configuration.
     */
    private void sendNotification(WorkflowTaskDefinition taskDef, String payload,
                                  TaskInstance task, String eventType) {
        String notificationType = taskDef.getNotificationType();

        if (NOTIFICATION_TYPE_KAFKA.equalsIgnoreCase(notificationType) ||
            NOTIFICATION_TYPE_BOTH.equalsIgnoreCase(notificationType)) {
            sendKafkaNotification(taskDef, payload, task, eventType);
        }

        if (NOTIFICATION_TYPE_API.equalsIgnoreCase(notificationType) ||
            NOTIFICATION_TYPE_BOTH.equalsIgnoreCase(notificationType)) {
            sendApiNotification(taskDef, payload, task, eventType);
        }
    }

    /**
     * Send notification to Kafka topic.
     */
    private void sendKafkaNotification(WorkflowTaskDefinition taskDef, String payload,
                                       TaskInstance task, String eventType) {
        String topic = taskDef.getNotificationKafkaTopic();
        if (topic == null || topic.trim().isEmpty()) {
            log.warn("Kafka topic not configured for task definition: {}", taskDef.getName());
            return;
        }

        try {
            Message<String> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.KEY, task.getId().toString())
                    .setHeader("eventType", eventType)
                    .setHeader("taskInstanceId", task.getId().toString())
                    .setHeader("workflowInstanceId", task.getWorkflowInstance().getId().toString())
                    .setHeader("correlationId", UUID.randomUUID().toString())
                    .setHeader("timestamp", LocalDateTime.now().toString())
                    .build();

            kafkaStringTemplate.send(message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} notification to Kafka topic {} for task {}: {}",
                            eventType, topic, task.getId(), ex.getMessage());
                    // Could implement retry or dead-letter queue here
                } else {
                    log.info("Published {} notification to Kafka topic {} for task {} (partition: {})",
                            eventType, topic, task.getId(),
                            result.getRecordMetadata().partition());
                }
            });

        } catch (Exception e) {
            log.error("Error sending Kafka notification for task {}: {}", task.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send notification to external API endpoint.
     */
    private void sendApiNotification(WorkflowTaskDefinition taskDef, String payload,
                                     TaskInstance task, String eventType) {
        String endpoint = taskDef.getNotificationApiEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            log.warn("API endpoint not configured for task definition: {}", taskDef.getName());
            return;
        }

        int attempts = 0;
        boolean success = false;

        while (!success && attempts < (retryEnabled ? maxRetryAttempts : 1)) {
            attempts++;
            try {
                HttpHeaders headers = buildApiHeaders(taskDef);
                HttpEntity<String> entity = new HttpEntity<>(payload, headers);

                String method = taskDef.getNotificationApiMethod();
                HttpMethod httpMethod = "PUT".equalsIgnoreCase(method) ? HttpMethod.PUT : HttpMethod.POST;

                log.debug("Calling external API (attempt {}): {} {} - Task: {}",
                        attempts, httpMethod, endpoint, task.getId());

                ResponseEntity<String> response = restTemplate.exchange(
                        endpoint,
                        httpMethod,
                        entity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Successfully sent {} notification to API {} for task {} (status: {})",
                            eventType, endpoint, task.getId(), response.getStatusCode());
                    success = true;
                } else {
                    log.warn("API notification returned non-success status {} for task {}: {}",
                            response.getStatusCode(), task.getId(), response.getBody());
                }

            } catch (Exception e) {
                log.error("Error calling notification API (attempt {}/{}) for task {}: {}",
                        attempts, maxRetryAttempts, task.getId(), e.getMessage());

                if (attempts < maxRetryAttempts && retryEnabled) {
                    try {
                        // Exponential backoff: 1s, 2s, 4s
                        Thread.sleep((long) Math.pow(2, attempts - 1) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!success) {
            log.error("Failed to send {} notification to API {} for task {} after {} attempts",
                    eventType, endpoint, task.getId(), attempts);
            // Could implement dead-letter queue or alert here
        }
    }

    /**
     * Build HTTP headers for API notification.
     */
    private HttpHeaders buildApiHeaders(WorkflowTaskDefinition taskDef) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        headers.set("X-Source", "workflow-engine");
        headers.set("X-Correlation-Id", UUID.randomUUID().toString());

        // Parse custom headers from configuration
        String customHeaders = taskDef.getNotificationApiHeaders();
        if (customHeaders != null && !customHeaders.trim().isEmpty()) {
            try {
                Map<String, String> headerMap = objectMapper.readValue(
                        customHeaders, new TypeReference<Map<String, String>>() {});
                headerMap.forEach(headers::set);
            } catch (Exception e) {
                log.warn("Failed to parse custom API headers for task definition {}: {}",
                        taskDef.getName(), e.getMessage());
            }
        }

        return headers;
    }

    /**
     * Build template variables from task instance data.
     */
    private Map<String, String> buildVariables(TaskInstance task, String result,
                                               String actionByUser, TaskStatus status) {
        Map<String, String> variables = new HashMap<>();

        // Task instance data
        variables.put("taskInstanceId", task.getId().toString());
        variables.put("taskName", task.getTaskName());
        variables.put("taskDescription", task.getTaskDescription() != null ? task.getTaskDescription() : "");
        variables.put("taskResult", result != null ? result : "");
        variables.put("status", status.name());
        variables.put("completedAt", task.getCompletedAt() != null ?
                task.getCompletedAt().format(TIMESTAMP_FORMATTER) : LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        variables.put("completedBy", actionByUser != null ? actionByUser : "");
        variables.put("startedAt", task.getStartedAt() != null ?
                task.getStartedAt().format(TIMESTAMP_FORMATTER) : "");

        // Workflow data
        WorkflowInstance workflow = task.getWorkflowInstance();
        if (workflow != null) {
            variables.put("workflowInstanceId", workflow.getId().toString());
            variables.put("workflowName", workflow.getWorkflowTemplate() != null ?
                    workflow.getWorkflowTemplate().getName() : "");

            // Patient data
            Patient patient = workflow.getPatient();
            if (patient != null) {
                variables.put("patientId", patient.getId().toString());
                variables.put("patientMrn", patient.getMrn() != null ? patient.getMrn() : "");
                variables.put("patientFirstName", patient.getFirstName() != null ? patient.getFirstName() : "");
                variables.put("patientLastName", patient.getLastName() != null ? patient.getLastName() : "");
            }
        }

        // Order data (if task has an associated order)
        if (task.getAssociatedOrderId() != null) {
            variables.put("orderId", task.getAssociatedOrderId().toString());
        } else {
            variables.put("orderId", "");
        }

        // Task definition data
        WorkflowTaskDefinition taskDef = task.getTaskDefinition();
        if (taskDef != null) {
            variables.put("taskDefinitionId", taskDef.getId().toString());
            variables.put("orderCode", ""); // Would come from linked order
        }

        // Event metadata
        variables.put("eventId", UUID.randomUUID().toString());
        variables.put("eventTime", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return variables;
    }

    /**
     * Build payload by substituting template variables.
     * If no template is configured, returns a default JSON payload.
     */
    private String buildPayload(String template, Map<String, String> variables) {
        if (template == null || template.trim().isEmpty()) {
            // Generate default payload
            return buildDefaultPayload(variables);
        }

        // Substitute variables in template
        String payload = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? escapeJson(entry.getValue()) : "";
            payload = payload.replace(placeholder, value);
        }

        return payload;
    }

    /**
     * Build default JSON payload when no template is configured.
     */
    private String buildDefaultPayload(Map<String, String> variables) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", variables.get("eventId"));
            payload.put("eventType", "TASK_" + variables.get("status"));
            payload.put("eventTime", variables.get("eventTime"));
            payload.put("taskInstanceId", variables.get("taskInstanceId"));
            payload.put("taskName", variables.get("taskName"));
            payload.put("taskResult", variables.get("taskResult"));
            payload.put("status", variables.get("status"));
            payload.put("workflowInstanceId", variables.get("workflowInstanceId"));
            payload.put("patientId", variables.get("patientId"));
            payload.put("patientMrn", variables.get("patientMrn"));
            payload.put("completedAt", variables.get("completedAt"));
            payload.put("completedBy", variables.get("completedBy"));

            if (variables.get("orderId") != null && !variables.get("orderId").isEmpty()) {
                payload.put("orderId", variables.get("orderId"));
            }

            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Error building default payload: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Escape special characters for JSON string values.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Validate notification configuration for a task definition.
     *
     * @param taskDef The task definition to validate
     * @return Map containing validation results
     */
    public Map<String, Object> validateConfiguration(WorkflowTaskDefinition taskDef) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);

        String notificationType = taskDef.getNotificationType();
        if (notificationType == null || NOTIFICATION_TYPE_NONE.equalsIgnoreCase(notificationType)) {
            result.put("message", "No notification configured");
            return result;
        }

        if (NOTIFICATION_TYPE_KAFKA.equalsIgnoreCase(notificationType) ||
            NOTIFICATION_TYPE_BOTH.equalsIgnoreCase(notificationType)) {
            if (taskDef.getNotificationKafkaTopic() == null ||
                taskDef.getNotificationKafkaTopic().trim().isEmpty()) {
                result.put("valid", false);
                result.put("kafkaError", "Kafka topic is required when notification type is KAFKA or BOTH");
            }
        }

        if (NOTIFICATION_TYPE_API.equalsIgnoreCase(notificationType) ||
            NOTIFICATION_TYPE_BOTH.equalsIgnoreCase(notificationType)) {
            if (taskDef.getNotificationApiEndpoint() == null ||
                taskDef.getNotificationApiEndpoint().trim().isEmpty()) {
                result.put("valid", false);
                result.put("apiError", "API endpoint is required when notification type is API or BOTH");
            }
        }

        // Validate message template JSON if provided
        String template = taskDef.getNotificationMessageTemplate();
        if (template != null && !template.trim().isEmpty()) {
            try {
                objectMapper.readTree(template);
            } catch (Exception e) {
                result.put("valid", false);
                result.put("templateError", "Invalid JSON template: " + e.getMessage());
            }
        }

        // Validate custom headers JSON if provided
        String headers = taskDef.getNotificationApiHeaders();
        if (headers != null && !headers.trim().isEmpty()) {
            try {
                objectMapper.readValue(headers, new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                result.put("valid", false);
                result.put("headersError", "Invalid JSON headers: " + e.getMessage());
            }
        }

        return result;
    }
}
