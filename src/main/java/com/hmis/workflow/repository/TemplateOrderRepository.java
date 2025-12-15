package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.TemplateOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TemplateOrder entities
 * Manages orders that are part of workflow templates
 */
@Repository
public interface TemplateOrderRepository extends JpaRepository<TemplateOrder, String> {

    /**
     * Find all orders for a template
     */
    List<TemplateOrder> findByTemplateId(String templateId);

    /**
     * Find all active orders for a template
     */
    @Query("SELECT to FROM TemplateOrder to WHERE to.templateId = :templateId AND to.isActive = true")
    List<TemplateOrder> findActiveOrdersByTemplateId(@Param("templateId") String templateId);

    /**
     * Find all required orders for a template
     */
    @Query("SELECT to FROM TemplateOrder to WHERE to.templateId = :templateId AND to.isRequired = true AND to.isActive = true")
    List<TemplateOrder> findRequiredOrdersByTemplateId(@Param("templateId") String templateId);

    /**
     * Find all automatic orders for a template (should be triggered automatically)
     */
    @Query("SELECT to FROM TemplateOrder to WHERE to.templateId = :templateId AND to.isAutomatic = true AND to.isActive = true ORDER BY to.orderSequence ASC")
    List<TemplateOrder> findAutomaticOrdersByTemplateId(@Param("templateId") String templateId);

    /**
     * Find order by code in template
     */
    @Query("SELECT to FROM TemplateOrder to WHERE to.templateId = :templateId AND to.orderCode = :orderCode")
    Optional<TemplateOrder> findByTemplateIdAndOrderCode(
        @Param("templateId") String templateId,
        @Param("orderCode") String orderCode
    );

    /**
     * Delete all orders for a template
     */
    void deleteByTemplateId(String templateId);

    /**
     * Count orders in a template
     */
    Long countByTemplateId(String templateId);

    /**
     * Count active orders in a template
     */
    @Query("SELECT COUNT(to) FROM TemplateOrder to WHERE to.templateId = :templateId AND to.isActive = true")
    Long countActiveOrdersByTemplateId(@Param("templateId") String templateId);
}
