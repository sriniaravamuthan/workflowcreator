package com.hmis.workflow.domain.event;

import com.hmis.workflow.domain.enums.OrderStatus;
import com.hmis.workflow.domain.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * OrderEvent represents order lifecycle events published to Kafka
 * Supports events: CREATED, AUTHORIZED, ACTIVATED, STARTED, RESULTED, VERIFIED, CLOSED, CANCELLED
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String orderId;
    private String workflowInstanceId;
    private String patientId;
    private OrderStatus status;
    private OrderType orderType;
    private String orderDescription;
    private String departmentTarget;
    private LocalDateTime eventTime;
    private String actor; // User who triggered the event
    private String result; // For RESULTED/COMPLETED events
    private String errorMessage;
    private BigDecimal actualCost;
    private Map<String, Object> metadata;
    private String eventType; // ORDER_CREATED, ORDER_AUTHORIZED, ORDER_ACTIVATED, etc.
    private String correlationId; // For tracing across services

    public static OrderEvent orderCreated(String orderId, String workflowInstanceId, String patientId,
                                         String orderDescription, OrderType orderType, Map<String, Object> metadata) {
        return OrderEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .orderId(orderId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(OrderStatus.PROPOSED)
                .orderType(orderType)
                .orderDescription(orderDescription)
                .eventTime(LocalDateTime.now())
                .eventType("ORDER_CREATED")
                .metadata(metadata)
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    public static OrderEvent orderAuthorized(String orderId, String workflowInstanceId, String patientId,
                                            String actor) {
        return OrderEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .orderId(orderId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(OrderStatus.AUTHORIZED)
                .eventTime(LocalDateTime.now())
                .eventType("ORDER_AUTHORIZED")
                .actor(actor)
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    public static OrderEvent orderActivated(String orderId, String workflowInstanceId, String patientId,
                                           String departmentTarget) {
        return OrderEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .orderId(orderId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(OrderStatus.ACTIVATED)
                .departmentTarget(departmentTarget)
                .eventTime(LocalDateTime.now())
                .eventType("ORDER_ACTIVATED")
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    public static OrderEvent orderResulted(String orderId, String workflowInstanceId, String patientId,
                                          String result, Map<String, Object> metadata) {
        return OrderEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .orderId(orderId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(OrderStatus.RESULTED)
                .result(result)
                .eventTime(LocalDateTime.now())
                .eventType("ORDER_RESULTED")
                .metadata(metadata)
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    public static OrderEvent orderCancelled(String orderId, String workflowInstanceId, String patientId,
                                           String errorMessage) {
        return OrderEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .orderId(orderId)
                .workflowInstanceId(workflowInstanceId)
                .patientId(patientId)
                .status(OrderStatus.CANCELLED)
                .eventTime(LocalDateTime.now())
                .eventType("ORDER_CANCELLED")
                .errorMessage(errorMessage)
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }
}
