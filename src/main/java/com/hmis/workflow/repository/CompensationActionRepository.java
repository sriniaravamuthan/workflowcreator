package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.CompensationAction;
import com.hmis.workflow.domain.enums.CompensationActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompensationActionRepository extends JpaRepository<CompensationAction, UUID> {
    List<CompensationAction> findByOrderId(UUID orderId);
    List<CompensationAction> findByActionType(CompensationActionType actionType);

    @Query("SELECT ca FROM CompensationAction ca WHERE ca.executed = false AND ca.retryCount < ca.maxRetries")
    List<CompensationAction> findPendingActions();

    @Query("SELECT ca FROM CompensationAction ca WHERE ca.order.id = :orderId AND ca.executed = false")
    List<CompensationAction> findPendingByOrder(@Param("orderId") UUID orderId);
}
