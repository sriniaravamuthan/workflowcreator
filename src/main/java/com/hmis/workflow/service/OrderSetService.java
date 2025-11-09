package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.OrderSet;
import com.hmis.workflow.domain.entity.OrderSetCondition;
import com.hmis.workflow.domain.entity.OrderSetItem;
import com.hmis.workflow.repository.OrderSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing order sets
 * Bundles orders, tasks, and instructions for specific clinical conditions
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderSetService {

    private final OrderSetRepository orderSetRepository;

    /**
     * Create a new order set
     */
    public OrderSet createOrderSet(OrderSet orderSet) {
        orderSet.setOrderSetId(UUID.randomUUID().toString());
        orderSet.setVersion(1);
        orderSet.setActive(true);
        log.info("Created order set: {} ({})", orderSet.getOrderSetId(), orderSet.getName());
        return orderSetRepository.save(orderSet);
    }

    /**
     * Get order set by ID
     */
    public OrderSet getOrderSet(UUID orderSetId) {
        return orderSetRepository.findById(orderSetId)
                .orElseThrow(() -> new IllegalArgumentException("OrderSet not found: " + orderSetId));
    }

    /**
     * Get order set by name
     */
    public OrderSet getOrderSetByName(String name) {
        return orderSetRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("OrderSet not found: " + name));
    }

    /**
     * Get all active order sets
     */
    public List<OrderSet> getActiveOrderSets() {
        return orderSetRepository.findByActiveTrue();
    }

    /**
     * Get order sets by category
     */
    public List<OrderSet> getOrderSetsByCategory(String category) {
        return orderSetRepository.findByCategory(category);
    }

    /**
     * Get order sets for a clinical condition
     */
    public List<OrderSet> getOrderSetsByCondition(String condition) {
        return orderSetRepository.findByCondition(condition);
    }

    /**
     * Get order sets by access level
     */
    public List<OrderSet> getOrderSetsByAccessLevel(String accessLevel) {
        return orderSetRepository.findByAccessLevel(accessLevel);
    }

    /**
     * Get latest versions of all order sets
     */
    public List<OrderSet> getLatestVersions() {
        return orderSetRepository.findLatestVersions();
    }

    /**
     * Update order set
     */
    public OrderSet updateOrderSet(UUID orderSetId, OrderSet updates) {
        OrderSet orderSet = getOrderSet(orderSetId);

        orderSet.setName(updates.getName());
        orderSet.setDescription(updates.getDescription());
        orderSet.setClinicalCondition(updates.getClinicalCondition());
        orderSet.setCategory(updates.getCategory());
        orderSet.setInstructions(updates.getInstructions());

        log.info("Updated order set: {}", orderSetId);
        return orderSetRepository.save(orderSet);
    }

    /**
     * Add item to order set
     */
    public OrderSet addItemToOrderSet(UUID orderSetId, OrderSetItem item) {
        OrderSet orderSet = getOrderSet(orderSetId);

        item.setOrderSet(orderSet);
        item.setSequenceNumber(orderSet.getItemCount() + 1);
        orderSet.getItems().add(item);

        log.info("Added item to order set {}: {}", orderSetId, item.getItemName());
        return orderSetRepository.save(orderSet);
    }

    /**
     * Add condition to order set
     */
    public OrderSet addConditionToOrderSet(UUID orderSetId, OrderSetCondition condition) {
        OrderSet orderSet = getOrderSet(orderSetId);

        condition.setOrderSet(orderSet);
        orderSet.getConditions().add(condition);

        log.info("Added condition to order set {}: {}", orderSetId, condition.getConditionName());
        return orderSetRepository.save(orderSet);
    }

    /**
     * Check if order set can be activated for a patient
     * Based on conditions evaluation
     */
    public boolean canActivateOrderSet(UUID orderSetId, Object patientData) {
        OrderSet orderSet = getOrderSet(orderSetId);

        if (!orderSet.canBeActivated()) {
            return false;
        }

        // Evaluate all conditions
        for (OrderSetCondition condition : orderSet.getConditions()) {
            // Placeholder for condition evaluation logic
            // In real implementation, would evaluate against patient data
            if (condition.getRequired()) {
                // If required condition not met, cannot activate
                return false;
            }
        }

        return true;
    }

    /**
     * Create a new version of an order set
     */
    public OrderSet createNewVersion(UUID orderSetId) {
        OrderSet original = getOrderSet(orderSetId);

        OrderSet newVersion = new OrderSet();
        newVersion.setOrderSetId(UUID.randomUUID().toString());
        newVersion.setName(original.getName());
        newVersion.setDescription(original.getDescription());
        newVersion.setClinicalCondition(original.getClinicalCondition());
        newVersion.setCategory(original.getCategory());
        newVersion.setVersion(original.getVersion() + 1);
        newVersion.setAccessLevel(original.getAccessLevel());

        // Copy items
        for (OrderSetItem item : original.getItems()) {
            OrderSetItem newItem = new OrderSetItem();
            newItem.setSequenceNumber(item.getSequenceNumber());
            newItem.setItemType(item.getItemType());
            newItem.setOrderType(item.getOrderType());
            newItem.setItemName(item.getItemName());
            newItem.setItemDescription(item.getItemDescription());
            newItem.setMandatory(item.getMandatory());
            newItem.setIsParallel(item.getIsParallel());
            newItem.setDefaultParameters(item.getDefaultParameters());
            newItem.setOrderSet(newVersion);
            newVersion.getItems().add(newItem);
        }

        log.info("Created new version {} of order set: {}", newVersion.getVersion(), orderSetId);
        return orderSetRepository.save(newVersion);
    }

    /**
     * Deactivate order set
     */
    public OrderSet deactivateOrderSet(UUID orderSetId) {
        OrderSet orderSet = getOrderSet(orderSetId);
        orderSet.setActive(false);
        log.info("Deactivated order set: {}", orderSetId);
        return orderSetRepository.save(orderSet);
    }

    /**
     * Change access level of order set
     */
    public OrderSet changeAccessLevel(UUID orderSetId, String newAccessLevel) {
        OrderSet orderSet = getOrderSet(orderSetId);
        orderSet.setAccessLevel(newAccessLevel);
        log.info("Changed access level of order set {} to: {}", orderSetId, newAccessLevel);
        return orderSetRepository.save(orderSet);
    }
}
