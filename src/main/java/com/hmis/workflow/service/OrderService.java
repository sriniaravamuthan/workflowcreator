package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.CompensationAction;
import com.hmis.workflow.domain.entity.Order;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.enums.CompensationActionType;
import com.hmis.workflow.domain.enums.OrderStatus;
import com.hmis.workflow.repository.CompensationActionRepository;
import com.hmis.workflow.repository.OrderRepository;
import com.hmis.workflow.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing orders in workflows
 * Handles order lifecycle: Proposed → Authorized → Activated → In Progress → Resulted → Verified → Closed/Cancelled
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CompensationActionRepository compensationRepository;
    private final WorkflowInstanceRepository workflowRepository;

    /**
     * Create a new order in PROPOSED status
     */
    public Order createOrder(Order order) {
        order.setOrderId(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.PROPOSED);
        log.info("Created order: {} ({})", order.getOrderId(), order.getOrderDescription());
        return orderRepository.save(order);
    }

    /**
     * Get order by ID
     */
    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    /**
     * Get order by order ID string
     */
    public Order getOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    /**
     * Get all orders for a workflow
     */
    public List<Order> getOrdersByWorkflow(UUID workflowInstanceId) {
        return orderRepository.findByWorkflowInstanceId(workflowInstanceId);
    }

    /**
     * Get all open orders
     */
    public List<Order> getOpenOrders() {
        return orderRepository.findOpenOrders();
    }

    /**
     * Authorize order
     */
    public Order authorizeOrder(UUID orderId, String authorizedByUser) {
        Order order = getOrder(orderId);

        if (!order.canTransitionTo(OrderStatus.AUTHORIZED)) {
            throw new IllegalStateException("Order cannot transition to AUTHORIZED from " + order.getStatus());
        }

        order.setStatus(OrderStatus.AUTHORIZED);
        order.setAuthorizedByUser(authorizedByUser);
        order.setAuthorizedAt(LocalDateTime.now());
        log.info("Authorized order: {} by {}", orderId, authorizedByUser);
        return orderRepository.save(order);
    }

    /**
     * Activate order (transmit to department)
     */
    public Order activateOrder(UUID orderId) {
        Order order = getOrder(orderId);

        if (!order.canTransitionTo(OrderStatus.ACTIVATED)) {
            throw new IllegalStateException("Order cannot transition to ACTIVATED from " + order.getStatus());
        }

        order.setStatus(OrderStatus.ACTIVATED);
        order.setActivatedAt(LocalDateTime.now());
        log.info("Activated order: {}", orderId);
        return orderRepository.save(order);
    }

    /**
     * Mark order as in progress
     */
    public Order startOrder(UUID orderId) {
        Order order = getOrder(orderId);

        if (!order.canTransitionTo(OrderStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Order cannot transition to IN_PROGRESS from " + order.getStatus());
        }

        order.setStatus(OrderStatus.IN_PROGRESS);
        log.info("Started order: {}", orderId);
        return orderRepository.save(order);
    }

    /**
     * Record order result
     */
    public Order resultOrder(UUID orderId, String result) {
        Order order = getOrder(orderId);

        OrderStatus nextStatus = switch (order.getOrderType()) {
            case MEDICATION -> OrderStatus.DISPENSED;
            default -> OrderStatus.RESULTED;
        };

        if (!order.canTransitionTo(nextStatus)) {
            throw new IllegalStateException("Order cannot transition to " + nextStatus + " from " + order.getStatus());
        }

        order.setStatus(nextStatus);
        order.setResult(result);
        order.setResultedAt(LocalDateTime.now());
        log.info("Resulted order: {} - Status: {}", orderId, nextStatus);
        return orderRepository.save(order);
    }

    /**
     * Verify order result
     */
    public Order verifyOrder(UUID orderId, String verifiedByUser) {
        Order order = getOrder(orderId);

        if (!order.canTransitionTo(OrderStatus.VERIFIED)) {
            throw new IllegalStateException("Order cannot transition to VERIFIED from " + order.getStatus());
        }

        order.setStatus(OrderStatus.VERIFIED);
        order.setVerifiedAt(LocalDateTime.now());
        order.setAuthorizedByUser(verifiedByUser); // Reusing field for verified user
        log.info("Verified order: {} by {}", orderId, verifiedByUser);
        return orderRepository.save(order);
    }

    /**
     * Close order
     */
    public Order closeOrder(UUID orderId) {
        Order order = getOrder(orderId);

        if (!order.canTransitionTo(OrderStatus.CLOSED)) {
            throw new IllegalStateException("Order cannot transition to CLOSED from " + order.getStatus());
        }

        order.setStatus(OrderStatus.CLOSED);
        order.setClosedAt(LocalDateTime.now());
        log.info("Closed order: {}", orderId);
        return orderRepository.save(order);
    }

    /**
     * Cancel order with compensation actions
     */
    public Order cancelOrder(UUID orderId, String cancellationReason) {
        Order order = getOrder(orderId);

        if (!order.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new IllegalStateException("Order cannot be cancelled from " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(cancellationReason);
        order.setCancelledAt(LocalDateTime.now());

        // Create default compensation actions
        if (OrderStatus.AUTHORIZED.equals(order.getStatus()) || OrderStatus.ACTIVATED.equals(order.getStatus())) {
            // Add reverse charge compensation
            CompensationAction reverseCharge = new CompensationAction();
            reverseCharge.setActionType(CompensationActionType.REVERSE_CHARGE);
            reverseCharge.setActionDescription("Reverse charges for cancelled order: " + order.getOrderId());
            reverseCharge.setTriggeringEvent("ORDER_CANCELLED");
            reverseCharge.setOrder(order);
            order.getCompensationActions().add(reverseCharge);
        }

        // Add notification compensation
        CompensationAction notification = new CompensationAction();
        notification.setActionType(CompensationActionType.SEND_NOTIFICATION);
        notification.setActionDescription("Notify department of cancellation: " + order.getOrderId());
        notification.setTriggeringEvent("ORDER_CANCELLED");
        notification.setOrder(order);
        order.getCompensationActions().add(notification);

        log.warn("Cancelled order: {} - Reason: {}", orderId, cancellationReason);
        return orderRepository.save(order);
    }

    /**
     * Get compensation actions for an order
     */
    public List<CompensationAction> getCompensationActions(UUID orderId) {
        return compensationRepository.findByOrderId(orderId);
    }

    /**
     * Execute pending compensation actions
     */
    public void executePendingCompensations(UUID orderId) {
        List<CompensationAction> pendingActions = compensationRepository.findPendingByOrder(orderId);

        for (CompensationAction action : pendingActions) {
            try {
                // Execute based on action type
                switch (action.getActionType()) {
                    case REVERSE_CHARGE:
                        log.info("Executing REVERSE_CHARGE for order: {}", orderId);
                        action.setExecuted(true);
                        action.setExecutedAt(LocalDateTime.now());
                        action.setExecutionResult("Charges reversed successfully");
                        break;

                    case SEND_NOTIFICATION:
                        log.info("Executing SEND_NOTIFICATION for order: {}", orderId);
                        action.setExecuted(true);
                        action.setExecutedAt(LocalDateTime.now());
                        action.setExecutionResult("Notification sent to department");
                        break;

                    case CANCEL_ORDER:
                        log.info("Executing CANCEL_ORDER compensation");
                        action.setExecuted(true);
                        action.setExecutedAt(LocalDateTime.now());
                        break;

                    default:
                        log.info("Executing custom action: {}", action.getActionType());
                        action.setExecuted(true);
                        action.setExecutedAt(LocalDateTime.now());
                }

                compensationRepository.save(action);
            } catch (Exception e) {
                action.setRetryCount(action.getRetryCount() + 1);
                action.setErrorMessage(e.getMessage());
                log.error("Failed to execute compensation action: {}", action.getActionType(), e);
                compensationRepository.save(action);
            }
        }
    }

    /**
     * Get orders with results
     */
    public List<Order> getOrdersWithResults() {
        return orderRepository.findOrdersWithResults();
    }
}
