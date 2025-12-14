package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.Order;
import com.hmis.workflow.domain.enums.OrderStatus;
import com.hmis.workflow.domain.enums.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderId(String orderId);
    List<Order> findByWorkflowInstanceId(UUID workflowInstanceId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByOrderType(OrderType orderType);
    List<Order> findByDepartmentTarget(String department);

    @Query("SELECT o FROM Order o WHERE o.status != 'CLOSED' AND o.status != 'CANCELLED'")
    List<Order> findOpenOrders();

    @Query("SELECT o FROM Order o WHERE o.status = 'IN_PROGRESS' AND o.workflowInstance.id = :workflowInstanceId")
    List<Order> findInProgressByWorkflow(@Param("workflowInstanceId") UUID workflowInstanceId);

    @Query("SELECT o FROM Order o WHERE o.authorizedAt BETWEEN :startDate AND :endDate")
    List<Order> findByAuthorizationDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.status = 'RESULTED' OR o.status = 'DISPENSED'")
    List<Order> findOrdersWithResults();
}
