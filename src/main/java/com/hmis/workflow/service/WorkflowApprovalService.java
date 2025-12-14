package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.dto.WorkflowApprovalRequest;
import com.hmis.workflow.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service for managing workflow review and approval lifecycle
 * Enables doctors to review orders/tasks before final workflow execution
 * Workflow states: PENDING_REVIEW → IN_REVIEW → APPROVED/REJECTED → EXECUTED
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class WorkflowApprovalService {

    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final OrderAdjustmentService orderAdjustmentService;

    /**
     * Mark a workflow instance as being reviewed
     * @param workflowInstanceId Workflow instance ID
     * @param reviewedByUser User beginning review
     * @return Updated workflow instance
     */
    public WorkflowInstance startReview(UUID workflowInstanceId, String reviewedByUser) {
        log.info("Starting review for workflow instance: {} by user: {}", workflowInstanceId, reviewedByUser);

        WorkflowInstance instance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new NoSuchElementException("Workflow instance not found: " + workflowInstanceId));

        // Can only start review if in PENDING_REVIEW status
        if (!"PENDING_REVIEW".equals(instance.getReviewStatus())) {
            throw new IllegalStateException("Workflow is not in PENDING_REVIEW status. Current status: " + instance.getReviewStatus());
        }

        instance.setReviewStatus("IN_REVIEW");
        instance.setReviewedByUser(reviewedByUser);

        WorkflowInstance updated = workflowInstanceRepository.save(instance);
        log.info("Review started for workflow instance: {}", workflowInstanceId);
        return updated;
    }

    /**
     * Approve a workflow instance for execution
     * Applies any pending adjustments and marks workflow as executable
     * @param workflowInstanceId Workflow instance ID
     * @param request Approval details
     * @return Updated workflow instance
     */
    public WorkflowInstance approveWorkflow(UUID workflowInstanceId, WorkflowApprovalRequest request) {
        log.info("Approving workflow instance: {} by user: {}", workflowInstanceId, request.getReviewedByUser());

        WorkflowInstance instance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new NoSuchElementException("Workflow instance not found: " + workflowInstanceId));

        // Can only approve from IN_REVIEW or PENDING_REVIEW status
        if (!"IN_REVIEW".equals(instance.getReviewStatus()) && !"PENDING_REVIEW".equals(instance.getReviewStatus())) {
            throw new IllegalStateException("Workflow cannot be approved from status: " + instance.getReviewStatus());
        }

        // Apply all pending adjustments
        int adjustmentsApplied = orderAdjustmentService.applyAllPendingAdjustments(workflowInstanceId);
        log.info("Applied {} adjustments before workflow approval", adjustmentsApplied);

        // Mark as approved and executable
        instance.setReviewStatus("APPROVED");
        instance.setReviewedByUser(request.getReviewedByUser());
        instance.setReviewedAt(LocalDateTime.now());
        instance.setReviewNotes(request.getReviewNotes());
        instance.setCanExecute(true);

        WorkflowInstance updated = workflowInstanceRepository.save(instance);
        log.info("Workflow instance approved and marked for execution: {}", workflowInstanceId);
        return updated;
    }

    /**
     * Reject a workflow instance and return to pending review
     * @param workflowInstanceId Workflow instance ID
     * @param request Rejection details
     * @return Updated workflow instance
     */
    public WorkflowInstance rejectWorkflow(UUID workflowInstanceId, WorkflowApprovalRequest request) {
        log.info("Rejecting workflow instance: {} by user: {}", workflowInstanceId, request.getReviewedByUser());

        WorkflowInstance instance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new NoSuchElementException("Workflow instance not found: " + workflowInstanceId));

        // Can only reject from IN_REVIEW or APPROVED status
        if (!"IN_REVIEW".equals(instance.getReviewStatus()) && !"APPROVED".equals(instance.getReviewStatus())) {
            throw new IllegalStateException("Workflow cannot be rejected from status: " + instance.getReviewStatus());
        }

        // Reset to pending review for modifications
        instance.setReviewStatus("PENDING_REVIEW");
        instance.setReviewedByUser(request.getReviewedByUser());
        instance.setReviewedAt(LocalDateTime.now());
        instance.setReviewNotes(request.getReviewNotes());
        instance.setCanExecute(false);

        WorkflowInstance updated = workflowInstanceRepository.save(instance);
        log.info("Workflow instance rejected and returned to pending review: {}", workflowInstanceId);
        return updated;
    }

    /**
     * Mark workflow as executed (after approval and task/order execution begins)
     * @param workflowInstanceId Workflow instance ID
     * @return Updated workflow instance
     */
    public WorkflowInstance markExecuted(UUID workflowInstanceId) {
        log.info("Marking workflow instance as executed: {}", workflowInstanceId);

        WorkflowInstance instance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new NoSuchElementException("Workflow instance not found: " + workflowInstanceId));

        if (!"APPROVED".equals(instance.getReviewStatus())) {
            throw new IllegalStateException("Only approved workflows can be marked as executed");
        }

        instance.setReviewStatus("EXECUTED");
        WorkflowInstance updated = workflowInstanceRepository.save(instance);
        log.info("Workflow instance marked as executed: {}", workflowInstanceId);
        return updated;
    }

    /**
     * Check if workflow can be executed
     * @param workflowInstanceId Workflow instance ID
     * @return true if approved and no pending adjustments
     */
    public boolean canExecuteWorkflow(UUID workflowInstanceId) {
        WorkflowInstance instance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new NoSuchElementException("Workflow instance not found: " + workflowInstanceId));

        boolean isApproved = "APPROVED".equals(instance.getReviewStatus());
        boolean hasPendingAdjustments = orderAdjustmentService.hasPendingAdjustments(workflowInstanceId);

        return isApproved && !hasPendingAdjustments && instance.getCanExecute();
    }

    /**
     * Get detailed review status and summary
     * @param workflowInstanceId Workflow instance ID
     * @return Review status summary
     */
    public ReviewStatusSummary getReviewStatus(UUID workflowInstanceId) {
        WorkflowInstance instance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new NoSuchElementException("Workflow instance not found: " + workflowInstanceId));

        OrderAdjustmentService.AdjustmentSummary adjustmentSummary =
                orderAdjustmentService.getAdjustmentSummary(workflowInstanceId);

        return ReviewStatusSummary.builder()
                .workflowInstanceId(workflowInstanceId)
                .reviewStatus(instance.getReviewStatus())
                .canExecute(instance.getCanExecute())
                .reviewedByUser(instance.getReviewedByUser())
                .reviewedAt(instance.getReviewedAt())
                .reviewNotes(instance.getReviewNotes())
                .totalOrders(instance.getOrders().size())
                .totalTasks(instance.getTaskInstances().size())
                .adjustmentSummary(adjustmentSummary)
                .build();
    }

    /**
     * Summary of workflow review status
     */
    @lombok.Data
    @lombok.Builder
    public static class ReviewStatusSummary {
        private UUID workflowInstanceId;
        private String reviewStatus;
        private Boolean canExecute;
        private String reviewedByUser;
        private LocalDateTime reviewedAt;
        private String reviewNotes;
        private int totalOrders;
        private int totalTasks;
        private OrderAdjustmentService.AdjustmentSummary adjustmentSummary;
    }
}
