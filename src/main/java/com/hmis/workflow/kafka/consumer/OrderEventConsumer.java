package com.hmis.workflow.kafka.consumer;

import com.hmis.workflow.domain.entity.Order;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.enums.OrderStatus;
import com.hmis.workflow.domain.event.OrderEvent;
import com.hmis.workflow.service.OrderService;
import com.hmis.workflow.service.WorkflowInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Kafka event consumer for order lifecycle events.
 * Automatically processes order state transitions and triggers related actions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;
    private final WorkflowInstanceService workflowInstanceService;

    /**
     * Listens for order events and processes them
     * Automatically handles order state transitions and notifications
     */
    @KafkaListener(topics = "workflow-order-events", groupId = "workflow-engine-order-consumer")
    public void handleOrderEvent(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received order event: {} for order: {} [topic: {}, partition: {}, offset: {}]",
                event.getEventType(), event.getOrderId(), topic, partition, offset);

        try {
            switch (event.getEventType()) {
                case "ORDER_CREATED":
                    handleOrderCreated(event);
                    break;
                case "ORDER_AUTHORIZED":
                    handleOrderAuthorized(event);
                    break;
                case "ORDER_ACTIVATED":
                    handleOrderActivated(event);
                    break;
                case "ORDER_RESULTED":
                    handleOrderResulted(event);
                    break;
                case "ORDER_CANCELLED":
                    handleOrderCancelled(event);
                    break;
                default:
                    log.warn("Unknown order event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", event.getEventId(), e);
            // In production, send to dead-letter queue
        }
    }

    /**
     * Handles order creation event
     * Initializes order tracking and state machine
     */
    private void handleOrderCreated(OrderEvent event) {
        log.info("Processing ORDER_CREATED event for order: {} created by: {}",
                event.getOrderId(), event.getActor());

        try {
            UUID orderId = UUID.fromString(event.getOrderId());
            Order order = orderService.getOrder(orderId);

            if (order == null) {
                log.warn("Order not found: {}", orderId);
                return;
            }

            log.info("Order created: {} with type: {} in status: {}",
                    orderId, order.getOrderType(), order.getStatus());

            // In production: Send notifications, update dashboards, trigger workflows
        } catch (Exception e) {
            log.error("Error handling order created event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handles order authorization event
     * Validates authorization and triggers next step in order lifecycle
     */
    private void handleOrderAuthorized(OrderEvent event) {
        log.info("Processing ORDER_AUTHORIZED event for order: {} authorized by: {}",
                event.getOrderId(), event.getActor());

        try {
            UUID orderId = UUID.fromString(event.getOrderId());
            Order order = orderService.getOrder(orderId);

            if (order == null) {
                log.warn("Order not found: {}", orderId);
                return;
            }

            // Verify order is in correct state
            if (order.getStatus() != OrderStatus.AUTHORIZED) {
                log.warn("Order {} is in unexpected state: {} for authorization event",
                        orderId, order.getStatus());
                return;
            }

            log.info("Order {} successfully authorized. Estimated cost: {}",
                    orderId, order.getEstimatedCost());

            // Check if order can be auto-activated based on business rules
            if (shouldAutoActivateOrder(order)) {
                log.info("Order {} is eligible for auto-activation, triggering activation",
                        orderId);
                try {
                    orderService.activateOrder(orderId);
                } catch (Exception e) {
                    log.warn("Failed to auto-activate order {}: {}", orderId, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error handling order authorized event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handles order activation event
     * Confirms activation and prepares for execution
     */
    private void handleOrderActivated(OrderEvent event) {
        log.info("Processing ORDER_ACTIVATED event for order: {}", event.getOrderId());

        try {
            UUID orderId = UUID.fromString(event.getOrderId());
            Order order = orderService.getOrder(orderId);

            if (order == null) {
                log.warn("Order not found: {}", orderId);
                return;
            }

            // Verify order is in correct state
            if (order.getStatus() != OrderStatus.ACTIVATED) {
                log.warn("Order {} is in unexpected state: {} for activation event",
                        orderId, order.getStatus());
                return;
            }

            log.info("Order {} activated and ready for execution. Department target: {}",
                    orderId, order.getDepartmentTarget());

            // In production: Send to department systems, notify relevant staff, update fulfillment systems
        } catch (Exception e) {
            log.error("Error handling order activated event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handles order result event
     * Records result and triggers verification workflow
     */
    private void handleOrderResulted(OrderEvent event) {
        log.info("Processing ORDER_RESULTED event for order: {}", event.getOrderId());

        try {
            UUID orderId = UUID.fromString(event.getOrderId());
            Order order = orderService.getOrder(orderId);

            if (order == null) {
                log.warn("Order not found: {}", orderId);
                return;
            }

            // Verify order is in correct state for result
            if (order.getStatus() != OrderStatus.RESULTED &&
                    order.getStatus() != OrderStatus.DISPENSED &&
                    order.getStatus() != OrderStatus.COMPLETED) {
                log.warn("Order {} is in unexpected state: {} for result event",
                        orderId, order.getStatus());
                return;
            }

            log.info("Order {} resulted with result: {}. Actual cost: {}",
                    orderId, order.getResult(), order.getActualCost());

            // Trigger verification workflow - may require clinician review
            log.info("Order {} is awaiting verification", orderId);

            // In production: Queue for clinician review, send notifications

        } catch (Exception e) {
            log.error("Error handling order resulted event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handles order cancellation event
     * Executes compensation actions and triggers cleanup
     */
    private void handleOrderCancelled(OrderEvent event) {
        log.info("Processing ORDER_CANCELLED event for order: {}", event.getOrderId());

        try {
            UUID orderId = UUID.fromString(event.getOrderId());
            Order order = orderService.getOrder(orderId);

            if (order == null) {
                log.warn("Order not found: {}", orderId);
                return;
            }

            if (order.getStatus() != OrderStatus.CANCELLED) {
                log.warn("Order {} is in unexpected state: {} for cancellation event",
                        orderId, order.getStatus());
                return;
            }

            log.info("Order {} cancelled. Executing compensation actions...", orderId);

            // Execute any pending compensation actions (charge reversals, notifications, etc.)
            try {
                orderService.executePendingCompensations(orderId);
                log.info("Compensation actions executed for cancelled order: {}", orderId);
            } catch (Exception e) {
                log.error("Failed to execute compensation actions for order {}: {}", orderId, e.getMessage());
                // These should be retried with backoff
            }

            // In production: Update billing systems, send notifications to patient/departments

        } catch (Exception e) {
            log.error("Error handling order cancelled event: {}", event.getEventId(), e);
        }
    }

    /**
     * Determines if an order should be automatically activated
     * Based on order type, department, and business rules
     */
    private boolean shouldAutoActivateOrder(Order order) {
        // Default to false - manual activation required for safety in healthcare
        // Override based on specific order types that can be auto-activated
        // For example: laboratory tests, routine imaging might be auto-activated
        // But: surgeries, controlled substances require manual authorization

        switch (order.getOrderType()) {
            case LAB_TEST:
            case IMAGING:
                // These can often be auto-activated
                return true;
            case MEDICATION:
            case SURGERY:
            case PROCEDURE:
            case CONTROLLED_SUBSTANCE:
                // These require manual activation
                return false;
            default:
                // Conservative default to false
                return false;
        }
    }
}
