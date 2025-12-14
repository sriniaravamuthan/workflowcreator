package com.hmis.workflow.kafka.producer;

import com.hmis.workflow.domain.event.TaskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Producer for publishing task lifecycle events to Kafka
 * Events: TASK_CREATED, TASK_STARTED, TASK_COMPLETED, TASK_FAILED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventProducer {

    private static final String TOPIC = "workflow-task-events";
    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;

    /**
     * Publish task event to Kafka
     */
    public void publishTaskEvent(TaskEvent event) {
        try {
            Message<TaskEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, event.getTaskInstanceId())
                    .setHeader("eventType", event.getEventType())
                    .setHeader("correlationId", event.getEventId())
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish task event: {} for task: {}", event.getEventType(), event.getTaskInstanceId(), ex);
                } else {
                    log.info("Published task event: {} for task: {} to partition: {}",
                            event.getEventType(),
                            event.getTaskInstanceId(),
                            result.getRecordMetadata().partition());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing task event: {}", event.getEventType(), e);
        }
    }

    /**
     * Publish task created event
     */
    public void publishTaskCreated(String taskInstanceId, String workflowInstanceId,
                                   String patientId, String taskName) {
        TaskEvent event = TaskEvent.taskCreated(taskInstanceId, workflowInstanceId, patientId, taskName, null);
        publishTaskEvent(event);
    }

    /**
     * Publish task started event
     */
    public void publishTaskStarted(String taskInstanceId, String workflowInstanceId,
                                   String patientId, String taskName) {
        TaskEvent event = TaskEvent.taskStarted(taskInstanceId, workflowInstanceId, patientId, taskName);
        publishTaskEvent(event);
    }

    /**
     * Publish task completed event
     */
    public void publishTaskCompleted(String taskInstanceId, String workflowInstanceId,
                                     String patientId, String taskName) {
        TaskEvent event = TaskEvent.taskCompleted(taskInstanceId, workflowInstanceId, patientId, taskName, null);
        publishTaskEvent(event);
    }

    /**
     * Publish task failed event
     */
    public void publishTaskFailed(String taskInstanceId, String workflowInstanceId,
                                  String patientId, String taskName, String errorMessage) {
        TaskEvent event = TaskEvent.taskFailed(taskInstanceId, workflowInstanceId, patientId, taskName, errorMessage);
        publishTaskEvent(event);
    }
}
