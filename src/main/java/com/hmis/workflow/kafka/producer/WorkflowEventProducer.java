package com.hmis.workflow.kafka.producer;

import com.hmis.workflow.domain.event.WorkflowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Producer for publishing workflow instance events to Kafka
 * Events: WORKFLOW_STARTED, WORKFLOW_COMPLETED, WORKFLOW_FAILED, WORKFLOW_ESCALATED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventProducer {

    private static final String TOPIC = "workflow-state-events";
    private final KafkaTemplate<String, WorkflowEvent> kafkaTemplate;

    /**
     * Publish workflow event to Kafka
     */
    public void publishWorkflowEvent(WorkflowEvent event) {
        try {
            Message<WorkflowEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, event.getWorkflowInstanceId())
                    .setHeader("eventType", event.getEventType())
                    .setHeader("correlationId", event.getCorrelationId())
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish workflow event: {} for workflow: {}", event.getEventType(), event.getWorkflowInstanceId(), ex);
                } else {
                    log.info("Published workflow event: {} for workflow: {} to partition: {}",
                            event.getEventType(),
                            event.getWorkflowInstanceId(),
                            result.getRecordMetadata().partition());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing workflow event: {}", event.getEventType(), e);
        }
    }

    /**
     * Publish workflow started event
     */
    public void publishWorkflowStarted(String workflowInstanceId, String patientId, String templateName) {
        WorkflowEvent event = WorkflowEvent.workflowStarted(workflowInstanceId, patientId, templateName);
        publishWorkflowEvent(event);
    }

    /**
     * Publish workflow completed event
     */
    public void publishWorkflowCompleted(String workflowInstanceId, String patientId) {
        WorkflowEvent event = WorkflowEvent.workflowCompleted(workflowInstanceId, patientId);
        publishWorkflowEvent(event);
    }

    /**
     * Publish workflow failed event
     */
    public void publishWorkflowFailed(String workflowInstanceId, String patientId, String reason) {
        WorkflowEvent event = WorkflowEvent.workflowFailed(workflowInstanceId, patientId, reason);
        publishWorkflowEvent(event);
    }

    /**
     * Publish workflow escalated event
     */
    public void publishWorkflowEscalated(String workflowInstanceId, String patientId, String reason) {
        WorkflowEvent event = WorkflowEvent.workflowEscalated(workflowInstanceId, patientId, reason);
        publishWorkflowEvent(event);
    }
}
