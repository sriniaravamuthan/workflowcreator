package com.hmis.workflow.kafka.producer;

import com.hmis.workflow.domain.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Producer for publishing order lifecycle events to Kafka
 * Events: ORDER_CREATED, ORDER_AUTHORIZED, ORDER_ACTIVATED, ORDER_RESULTED, ORDER_CANCELLED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private static final String TOPIC = "workflow-order-events";
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    /**
     * Publish order event to Kafka
     */
    public void publishOrderEvent(OrderEvent event) {
        try {
            Message<OrderEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, event.getOrderId())
                    .setHeader("eventType", event.getEventType())
                    .setHeader("correlationId", event.getCorrelationId())
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish order event: {} for order: {}", event.getEventType(), event.getOrderId(), ex);
                } else {
                    log.info("Published order event: {} for order: {} to partition: {}",
                            event.getEventType(),
                            event.getOrderId(),
                            result.getRecordMetadata().partition());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing order event: {}", event.getEventType(), e);
        }
    }

    /**
     * Publish order created event
     */
    public void publishOrderCreated(String orderId, String workflowInstanceId, String patientId,
                                    String orderDescription) {
        OrderEvent event = OrderEvent.orderCreated(orderId, workflowInstanceId, patientId,
                orderDescription, null, null);
        publishOrderEvent(event);
    }

    /**
     * Publish order authorized event
     */
    public void publishOrderAuthorized(String orderId, String workflowInstanceId, String patientId,
                                       String actor) {
        OrderEvent event = OrderEvent.orderAuthorized(orderId, workflowInstanceId, patientId, actor);
        publishOrderEvent(event);
    }

    /**
     * Publish order activated event
     */
    public void publishOrderActivated(String orderId, String workflowInstanceId, String patientId,
                                      String departmentTarget) {
        OrderEvent event = OrderEvent.orderActivated(orderId, workflowInstanceId, patientId, departmentTarget);
        publishOrderEvent(event);
    }

    /**
     * Publish order resulted event
     */
    public void publishOrderResulted(String orderId, String workflowInstanceId, String patientId,
                                     String result) {
        OrderEvent event = OrderEvent.orderResulted(orderId, workflowInstanceId, patientId, result, null);
        publishOrderEvent(event);
    }

    /**
     * Publish order cancelled event
     */
    public void publishOrderCancelled(String orderId, String workflowInstanceId, String patientId,
                                      String reason) {
        OrderEvent event = OrderEvent.orderCancelled(orderId, workflowInstanceId, patientId, reason);
        publishOrderEvent(event);
    }
}
