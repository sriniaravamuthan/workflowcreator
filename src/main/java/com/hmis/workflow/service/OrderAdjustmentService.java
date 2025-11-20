package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.Order;
import com.hmis.workflow.domain.entity.OrderAdjustment;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.dto.OrderAdjustmentRequest;
import com.hmis.workflow.repository.OrderAdjustmentRepository;
import com.hmis.workflow.repository.OrderRepository;
import com.hmis.workflow.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service for managing order parameter adjustments during workflow review phase
 * Enables doctors to modify order dosages, timings, frequencies, etc. before workflow signing
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderAdjustmentService {

    private final OrderAdjustmentRepository orderAdjustmentRepository;
    private final OrderRepository orderRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;

    /**
     * Record an order parameter adjustment
     * @param workflowInstanceId Workflow instance being reviewed
     * @param request Adjustment details
     * @return Created adjustment
     */
    public OrderAdjustment recordOrderAdjustment(UUID workflowInstanceId, OrderAdjustmentRequest request) {
        log.info("Recording adjustment for workflow instance: {} parameter: {}", workflowInstanceId, request.getParameterName());

        WorkflowInstance workflowInstance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new NoSuchElementException("Workflow instance not found: " + workflowInstanceId));

        Order order = orderRepository.findById(UUID.fromString(request.getOrderId()))
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + request.getOrderId()));

        // Verify order belongs to this workflow instance
        if (!order.getWorkflowInstance().getId().equals(workflowInstanceId)) {
            throw new IllegalArgumentException("Order does not belong to this workflow instance");
        }

        OrderAdjustment adjustment = OrderAdjustment.builder()
                .adjustmentCode(generateAdjustmentCode())
                .parameterName(request.getParameterName())
                .originalValue(request.getOriginalValue())
                .adjustedValue(request.getAdjustedValue())
                .adjustmentReason(request.getAdjustmentReason())
                .adjustmentType(request.getAdjustmentType() != null ? request.getAdjustmentType() : "PARAMETER")
                .isApplied(false)
                .adjustedByUser(request.getAdjustedByUser())
                .adjustedAt(LocalDateTime.now())
                .order(order)
                .workflowInstance(workflowInstance)
                .metadata(request.getMetadata())
                .build();

        OrderAdjustment saved = orderAdjustmentRepository.save(adjustment);
        log.info("Adjustment recorded with code: {}", saved.getAdjustmentCode());
        return saved;
    }

    /**
     * Get all adjustments for a workflow instance
     * @param workflowInstanceId Workflow instance ID
     * @return List of adjustments
     */
    public List<OrderAdjustment> getAdjustmentsByWorkflowInstance(UUID workflowInstanceId) {
        return orderAdjustmentRepository.findByWorkflowInstanceId(workflowInstanceId);
    }

    /**
     * Get pending (unapplied) adjustments for a workflow instance
     * @param workflowInstanceId Workflow instance ID
     * @return List of pending adjustments
     */
    public List<OrderAdjustment> getPendingAdjustmentsByWorkflowInstance(UUID workflowInstanceId) {
        return orderAdjustmentRepository.findPendingByWorkflowInstanceId(workflowInstanceId);
    }

    /**
     * Get all adjustments for a specific order
     * @param orderId Order ID
     * @return List of adjustments
     */
    public List<OrderAdjustment> getAdjustmentsByOrder(UUID orderId) {
        return orderAdjustmentRepository.findByOrderId(orderId);
    }

    /**
     * Apply an adjustment (mark as applied)
     * @param adjustmentId Adjustment ID
     * @return Updated adjustment
     */
    public OrderAdjustment applyAdjustment(UUID adjustmentId) {
        log.info("Applying adjustment: {}", adjustmentId);

        OrderAdjustment adjustment = orderAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new NoSuchElementException("Adjustment not found: " + adjustmentId));

        adjustment.setIsApplied(true);
        OrderAdjustment updated = orderAdjustmentRepository.save(adjustment);
        log.info("Adjustment applied: {}", adjustmentId);
        return updated;
    }

    /**
     * Discard an adjustment
     * @param adjustmentId Adjustment ID
     */
    public void discardAdjustment(UUID adjustmentId) {
        log.info("Discarding adjustment: {}", adjustmentId);
        orderAdjustmentRepository.deleteById(adjustmentId);
    }

    /**
     * Apply all pending adjustments for a workflow instance
     * @param workflowInstanceId Workflow instance ID
     * @return Number of adjustments applied
     */
    public int applyAllPendingAdjustments(UUID workflowInstanceId) {
        log.info("Applying all pending adjustments for workflow instance: {}", workflowInstanceId);
        List<OrderAdjustment> pending = getPendingAdjustmentsByWorkflowInstance(workflowInstanceId);

        pending.forEach(adj -> {
            adj.setIsApplied(true);
            orderAdjustmentRepository.save(adj);
        });

        log.info("Applied {} adjustments for workflow instance: {}", pending.size(), workflowInstanceId);
        return pending.size();
    }

    /**
     * Check if workflow instance has pending adjustments
     * @param workflowInstanceId Workflow instance ID
     * @return True if pending adjustments exist
     */
    public boolean hasPendingAdjustments(UUID workflowInstanceId) {
        return orderAdjustmentRepository.countPendingAdjustmentsByWorkflowInstanceId(workflowInstanceId) > 0;
    }

    /**
     * Get adjustment summary for a workflow instance
     * @param workflowInstanceId Workflow instance ID
     * @return Summary info
     */
    public AdjustmentSummary getAdjustmentSummary(UUID workflowInstanceId) {
        List<OrderAdjustment> all = getAdjustmentsByWorkflowInstance(workflowInstanceId);
        List<OrderAdjustment> pending = getPendingAdjustmentsByWorkflowInstance(workflowInstanceId);

        return AdjustmentSummary.builder()
                .totalAdjustments(all.size())
                .appliedAdjustments(all.size() - pending.size())
                .pendingAdjustments(pending.size())
                .hasPending(pending.size() > 0)
                .adjustmentsByType(groupByType(all))
                .build();
    }

    /**
     * Generate unique adjustment code
     */
    private String generateAdjustmentCode() {
        return "ADJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Group adjustments by type
     */
    private java.util.Map<String, Integer> groupByType(List<OrderAdjustment> adjustments) {
        return adjustments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        OrderAdjustment::getAdjustmentType,
                        java.util.stream.Collectors.summingInt(a -> 1)));
    }

    /**
     * Summary of adjustments for a workflow instance
     */
    @lombok.Data
    @lombok.Builder
    public static class AdjustmentSummary {
        private int totalAdjustments;
        private int appliedAdjustments;
        private int pendingAdjustments;
        private boolean hasPending;
        private java.util.Map<String, Integer> adjustmentsByType;
    }
}
