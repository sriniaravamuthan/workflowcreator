package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.OrderAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderAdjustmentRepository extends JpaRepository<OrderAdjustment, UUID> {

    @Query("SELECT oa FROM OrderAdjustment oa WHERE oa.order.id = :orderId")
    List<OrderAdjustment> findByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT oa FROM OrderAdjustment oa WHERE oa.workflowInstance.id = :workflowInstanceId")
    List<OrderAdjustment> findByWorkflowInstanceId(@Param("workflowInstanceId") UUID workflowInstanceId);

    @Query("SELECT oa FROM OrderAdjustment oa WHERE oa.workflowInstance.id = :workflowInstanceId AND oa.isApplied = false")
    List<OrderAdjustment> findPendingByWorkflowInstanceId(@Param("workflowInstanceId") UUID workflowInstanceId);

    @Query("SELECT oa FROM OrderAdjustment oa WHERE oa.order.id = :orderId AND oa.isApplied = true")
    List<OrderAdjustment> findAppliedByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT oa FROM OrderAdjustment oa WHERE oa.workflowInstance.id = :workflowInstanceId AND oa.adjustmentType = :adjustmentType")
    List<OrderAdjustment> findByWorkflowInstanceAndType(
            @Param("workflowInstanceId") UUID workflowInstanceId,
            @Param("adjustmentType") String adjustmentType);

    @Query("SELECT COUNT(oa) FROM OrderAdjustment oa WHERE oa.workflowInstance.id = :workflowInstanceId AND oa.isApplied = false")
    long countPendingAdjustmentsByWorkflowInstanceId(@Param("workflowInstanceId") UUID workflowInstanceId);
}
