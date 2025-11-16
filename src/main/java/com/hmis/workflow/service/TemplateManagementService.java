package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.TemplateOrder;
import com.hmis.workflow.domain.entity.WorkflowTaskDefinition;
import com.hmis.workflow.domain.entity.WorkflowTemplate;
import com.hmis.workflow.repository.TemplateOrderRepository;
import com.hmis.workflow.repository.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for advanced template management operations
 * Handles: EDIT (archive + clone), CLONE, SOFT DELETE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateManagementService {

    private final WorkflowTemplateRepository templateRepository;
    private final TemplateOrderRepository templateOrderRepository;
    private final NotificationService notificationService;

    /**
     * EDIT operation: Archives current template and creates new one with cloned data
     *
     * @param templateId ID of template to edit
     * @param updatedName New name for the edited template
     * @param updatedDescription New description
     * @return New edited template with cloned tasks and orders
     */
    @Transactional
    public WorkflowTemplate editTemplate(UUID templateId, String updatedName, String updatedDescription) {
        log.info("Editing template: {}", templateId);

        // Load the current template
        WorkflowTemplate currentTemplate = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        // Validate it's not already archived
        if (currentTemplate.isArchived()) {
            throw new RuntimeException("Cannot edit an already archived template: " + templateId);
        }

        // Step 1: Archive the current template
        log.info("Archiving current template: {}", templateId);
        currentTemplate.archiveForEdit(templateId);  // Will be updated with new ID below
        templateRepository.save(currentTemplate);

        // Step 2: Create new template with cloned data
        log.info("Creating new edited version of template: {}", templateId);
        UUID newTemplateId = UUID.randomUUID();

        WorkflowTemplate newTemplate = WorkflowTemplate.builder()
                .name(updatedName != null ? updatedName : currentTemplate.getName() + " (v" + (currentTemplate.getVersion() + 1) + ")")
                .description(updatedDescription != null ? updatedDescription : currentTemplate.getDescription())
                .category(currentTemplate.getCategory())
                .version(currentTemplate.getVersion() + 1)
                .reviewStatus("DRAFT")  // New version starts as DRAFT
                .active(true)
                .isDeleted(false)
                .clonedFromTemplateId(templateId)
                .parentTemplateId(templateId)
                .tasks(new HashSet<>())
                .orders(new HashSet<>())
                .gates(new HashSet<>())
                .decisionLogics(new HashSet<>())
                .build();

        // Step 3: Clone all tasks
        log.info("Cloning tasks from template: {} to new template: {}", templateId, newTemplateId);
        Set<WorkflowTaskDefinition> clonedTasks = currentTemplate.getTasks().stream()
                .map(task -> cloneTask(task, newTemplate))
                .collect(Collectors.toSet());
        newTemplate.setTasks(clonedTasks);

        // Step 4: Clone all orders
        log.info("Cloning orders from template: {} to new template: {}", templateId, newTemplateId);
        Set<TemplateOrder> clonedOrders = currentTemplate.getOrders().stream()
                .map(order -> cloneOrder(order, newTemplateId.toString()))
                .collect(Collectors.toSet());
        newTemplate.setOrders(clonedOrders);

        // Step 5: Save new template
        WorkflowTemplate savedTemplate = templateRepository.save(newTemplate);

        // Update parent template with new ID reference
        currentTemplate.setParentTemplateId(newTemplateId);
        templateRepository.save(currentTemplate);

        log.info("Template successfully edited. Old template {} archived, new template {} created",
                templateId, newTemplateId);

        // Send notification
        notifyTemplateEdit(templateId, newTemplateId, currentTemplate.getName());

        return savedTemplate;
    }

    /**
     * CLONE operation: Creates an exact copy of a template
     *
     * @param templateId ID of template to clone
     * @param newName Name for the cloned template
     * @return Cloned template
     */
    @Transactional
    public WorkflowTemplate cloneTemplate(UUID templateId, String newName) {
        log.info("Cloning template: {}", templateId);

        // Load the template to clone
        WorkflowTemplate sourceTemplate = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        UUID newTemplateId = UUID.randomUUID();

        // Create new template with cloned data
        WorkflowTemplate clonedTemplate = WorkflowTemplate.builder()
                .name(newName != null ? newName : sourceTemplate.getName() + " (Clone)")
                .description(sourceTemplate.getDescription())
                .category(sourceTemplate.getCategory())
                .version(1)  // Clone starts at version 1
                .reviewStatus("DRAFT")
                .active(true)
                .isDeleted(false)
                .clonedFromTemplateId(templateId)
                .tasks(new HashSet<>())
                .orders(new HashSet<>())
                .gates(new HashSet<>())
                .decisionLogics(new HashSet<>())
                .build();

        // Clone all tasks
        log.info("Cloning tasks from template: {}", templateId);
        Set<WorkflowTaskDefinition> clonedTasks = sourceTemplate.getTasks().stream()
                .map(task -> cloneTask(task, clonedTemplate))
                .collect(Collectors.toSet());
        clonedTemplate.setTasks(clonedTasks);

        // Clone all orders
        log.info("Cloning orders from template: {}", templateId);
        Set<TemplateOrder> clonedOrders = sourceTemplate.getOrders().stream()
                .map(order -> cloneOrder(order, newTemplateId.toString()))
                .collect(Collectors.toSet());
        clonedTemplate.setOrders(clonedOrders);

        // Save cloned template
        WorkflowTemplate savedTemplate = templateRepository.save(clonedTemplate);

        log.info("Template successfully cloned. Source: {}, Clone: {}", templateId, newTemplateId);

        // Send notification
        notifyTemplateCloned(templateId, newTemplateId, sourceTemplate.getName());

        return savedTemplate;
    }

    /**
     * SOFT DELETE operation: Marks template as deleted without removing from database
     *
     * @param templateId ID of template to delete
     */
    @Transactional
    public void softDeleteTemplate(UUID templateId) {
        log.info("Soft deleting template: {}", templateId);

        WorkflowTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        // Cannot delete if template is published or being used
        if (template.isPublished()) {
            throw new RuntimeException("Cannot delete a published template: " + templateId);
        }

        // Soft delete
        template.softDelete();
        templateRepository.save(template);

        log.info("Template successfully soft deleted: {}", templateId);

        // Send notification
        notifyTemplateDeleted(templateId, template.getName());
    }

    /**
     * Adds an order to a template
     */
    @Transactional
    public TemplateOrder addOrderToTemplate(UUID templateId, TemplateOrder order) {
        log.info("Adding order {} to template: {}", order.getOrderCode(), templateId);

        WorkflowTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        // Validate order code is unique in template
        boolean orderExists = template.getOrders().stream()
                .anyMatch(o -> o.getOrderCode().equals(order.getOrderCode()));

        if (orderExists) {
            throw new RuntimeException("Order with code " + order.getOrderCode() + " already exists in template");
        }

        order.setTemplateId(templateId.toString());
        TemplateOrder savedOrder = templateOrderRepository.save(order);

        template.getOrders().add(savedOrder);
        templateRepository.save(template);

        log.info("Order {} successfully added to template: {}", order.getOrderCode(), templateId);

        return savedOrder;
    }

    /**
     * Removes an order from a template
     */
    @Transactional
    public void removeOrderFromTemplate(UUID templateId, String orderId) {
        log.info("Removing order {} from template: {}", orderId, templateId);

        WorkflowTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        template.getOrders().removeIf(o -> o.getId().equals(orderId));
        templateRepository.save(template);

        templateOrderRepository.deleteById(orderId);

        log.info("Order {} successfully removed from template: {}", orderId, templateId);
    }

    /**
     * Helper: Clone a task to a new template
     */
    private WorkflowTaskDefinition cloneTask(WorkflowTaskDefinition task, WorkflowTemplate newTemplate) {
        WorkflowTaskDefinition clonedTask = new WorkflowTaskDefinition();
        clonedTask.setId(UUID.randomUUID());
        clonedTask.setTemplate(newTemplate);
        clonedTask.setName(task.getName());
        clonedTask.setDescription(task.getDescription());
        clonedTask.setTaskOrder(task.getTaskOrder());
        clonedTask.setAssignTo(task.getAssignTo());
        clonedTask.setEstimatedDurationMinutes(task.getEstimatedDurationMinutes());
        clonedTask.setInstructions(task.getInstructions());
        clonedTask.setIsParallel(task.getIsParallel());
        clonedTask.setIsOptional(task.getIsOptional());
        clonedTask.setNextTaskId(task.getNextTaskId());
        clonedTask.setFailureTaskId(task.getFailureTaskId());
        clonedTask.setMetadata(task.getMetadata());

        return clonedTask;
    }

    /**
     * Helper: Clone an order to a new template
     */
    private TemplateOrder cloneOrder(TemplateOrder order, String newTemplateId) {
        TemplateOrder clonedOrder = new TemplateOrder();
        clonedOrder.setId(UUID.randomUUID().toString());
        clonedOrder.setTemplateId(newTemplateId);
        clonedOrder.setOrderCode(order.getOrderCode());
        clonedOrder.setOrderName(order.getOrderName());
        clonedOrder.setDescription(order.getDescription());
        clonedOrder.setExternalApiEndpoint(order.getExternalApiEndpoint());
        clonedOrder.setApiMethod(order.getApiMethod());
        clonedOrder.setApiRequestPayload(order.getApiRequestPayload());
        clonedOrder.setIsRequired(order.getIsRequired());
        clonedOrder.setIsAutomatic(order.getIsAutomatic());
        clonedOrder.setOrderSequence(order.getOrderSequence());
        clonedOrder.setMetadata(order.getMetadata());
        clonedOrder.setIsActive(true);

        return clonedOrder;
    }

    /**
     * Notification helpers
     */
    private void notifyTemplateEdit(UUID oldTemplateId, UUID newTemplateId, String templateName) {
        try {
            log.info("Sending template edit notification");
            // In production, send notification to admins about template edit
        } catch (Exception e) {
            log.error("Error sending template edit notification", e);
        }
    }

    private void notifyTemplateCloned(UUID sourceTemplateId, UUID cloneTemplateId, String templateName) {
        try {
            log.info("Sending template clone notification");
            // In production, send notification to admins about template clone
        } catch (Exception e) {
            log.error("Error sending template clone notification", e);
        }
    }

    private void notifyTemplateDeleted(UUID templateId, String templateName) {
        try {
            log.info("Sending template delete notification");
            // In production, send notification to admins about template deletion
        } catch (Exception e) {
            log.error("Error sending template delete notification", e);
        }
    }
}
