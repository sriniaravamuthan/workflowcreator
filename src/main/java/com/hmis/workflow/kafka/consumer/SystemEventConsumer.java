package com.hmis.workflow.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Kafka event consumer for system-level events.
 * Handles operational and diagnostic events that don't fit into workflow, task, or order categories.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemEventConsumer {

    /**
     * Listens for system events for operational monitoring
     * Tracks system health, performance, and diagnostic information
     */
    @KafkaListener(topics = "system-events", groupId = "workflow-engine-system-consumer")
    public void handleSystemEvent(
            @Payload Map<String, Object> eventPayload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        try {
            String eventType = (String) eventPayload.get("eventType");
            String eventId = (String) eventPayload.get("eventId");

            log.debug("Received system event: {} [eventId: {}, topic: {}, partition: {}, offset: {}]",
                    eventType, eventId, topic, partition, offset);

            switch (eventType) {
                case "HEALTH_CHECK":
                    handleHealthCheck(eventPayload);
                    break;
                case "PERFORMANCE_METRIC":
                    handlePerformanceMetric(eventPayload);
                    break;
                case "ERROR_REPORT":
                    handleErrorReport(eventPayload);
                    break;
                case "AUDIT_EVENT":
                    handleAuditEvent(eventPayload);
                    break;
                default:
                    log.debug("Unknown system event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing system event", e);
        }
    }

    /**
     * Handles system health check events
     */
    private void handleHealthCheck(Map<String, Object> event) {
        try {
            String component = (String) event.get("component");
            String status = (String) event.get("status");
            LocalDateTime timestamp = (LocalDateTime) event.get("timestamp");

            log.info("Health check - Component: {}, Status: {}, Timestamp: {}",
                    component, status, timestamp);

            if ("UNHEALTHY".equals(status)) {
                log.warn("Component {} is unhealthy: {}", component, event.get("details"));
                // Trigger alerting mechanism
            }
        } catch (Exception e) {
            log.error("Error handling health check event", e);
        }
    }

    /**
     * Handles performance metric events
     */
    private void handlePerformanceMetric(Map<String, Object> event) {
        try {
            String metricName = (String) event.get("metricName");
            Double metricValue = (Double) event.get("metricValue");
            String unit = (String) event.get("unit");

            log.debug("Performance metric - Name: {}, Value: {}{}, Component: {}",
                    metricName, metricValue, unit, event.get("component"));

            // Check if metric is within acceptable thresholds
            checkMetricThresholds(metricName, metricValue);

        } catch (Exception e) {
            log.error("Error handling performance metric event", e);
        }
    }

    /**
     * Handles error report events
     */
    private void handleErrorReport(Map<String, Object> event) {
        try {
            String errorCode = (String) event.get("errorCode");
            String errorMessage = (String) event.get("errorMessage");
            String source = (String) event.get("source");
            String severity = (String) event.get("severity");

            log.warn("Error report - Code: {}, Severity: {}, Source: {}, Message: {}",
                    errorCode, severity, source, errorMessage);

            // Route to appropriate error handling based on severity
            handleErrorBySeverity(errorCode, errorMessage, severity);

        } catch (Exception e) {
            log.error("Error handling error report event", e);
        }
    }

    /**
     * Handles audit events for compliance tracking
     */
    private void handleAuditEvent(Map<String, Object> event) {
        try {
            String action = (String) event.get("action");
            String actor = (String) event.get("actor");
            String resource = (String) event.get("resource");
            String timestamp = (String) event.get("timestamp");

            log.info("Audit event - Action: {}, Actor: {}, Resource: {}, Timestamp: {}",
                    action, actor, resource, timestamp);

            // In production: Store in audit log table with immutable flag for compliance

        } catch (Exception e) {
            log.error("Error handling audit event", e);
        }
    }

    /**
     * Checks if metric values exceed acceptable thresholds
     */
    private void checkMetricThresholds(String metricName, Double value) {
        if (value == null) {
            return;
        }

        // Define thresholds for various metrics
        switch (metricName) {
            case "task_completion_time_ms":
                if (value > 300000) { // > 5 minutes
                    log.warn("Task completion time threshold exceeded: {}ms", value);
                }
                break;
            case "kafka_lag":
                if (value > 1000) { // > 1000 messages
                    log.warn("Kafka consumer lag threshold exceeded: {}", value);
                }
                break;
            case "database_query_time_ms":
                if (value > 5000) { // > 5 seconds
                    log.warn("Database query time threshold exceeded: {}ms", value);
                }
                break;
            case "active_workflows":
                if (value > 10000) {
                    log.warn("High number of active workflows: {}", value);
                }
                break;
            default:
                // No specific thresholds for this metric
                break;
        }
    }

    /**
     * Routes error handling based on severity level
     */
    private void handleErrorBySeverity(String errorCode, String errorMessage, String severity) {
        switch (severity) {
            case "CRITICAL":
                log.error("CRITICAL ERROR [{}]: {}", errorCode, errorMessage);
                // Trigger immediate escalation and alerts
                // Potentially trigger failover or recovery procedures
                break;
            case "HIGH":
                log.error("HIGH SEVERITY ERROR [{}]: {}", errorCode, errorMessage);
                // Escalate to support team
                // Create incident ticket
                break;
            case "MEDIUM":
                log.warn("MEDIUM SEVERITY ERROR [{}]: {}", errorCode, errorMessage);
                // Log and monitor
                break;
            case "LOW":
                log.debug("LOW SEVERITY ERROR [{}]: {}", errorCode, errorMessage);
                // Log for diagnostics
                break;
            default:
                log.warn("UNKNOWN SEVERITY ERROR [{}]: {}", errorCode, errorMessage);
        }
    }
}
