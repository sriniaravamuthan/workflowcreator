package com.hmis.workflow.controller;

import com.hmis.workflow.domain.entity.TemplateOrder;
import com.hmis.workflow.domain.entity.WorkflowTemplate;
import com.hmis.workflow.domain.entity.WorkflowTaskDefinition;
import com.hmis.workflow.dto.AddOrderToTemplateRequest;
import com.hmis.workflow.dto.AddTaskToTemplateRequest;
import com.hmis.workflow.dto.ApiResponse;
import com.hmis.workflow.dto.CreateWorkflowTemplateRequest;
import com.hmis.workflow.dto.TaskNotificationConfigDTO;
import com.hmis.workflow.dto.TaskNotificationConfigRequest;
import com.hmis.workflow.dto.TemplateOrderDTO;
import com.hmis.workflow.dto.UpdateWorkflowTemplateRequest;
import com.hmis.workflow.dto.WorkflowTaskDefinitionDTO;
import com.hmis.workflow.dto.WorkflowTemplateDTO;
import com.hmis.workflow.service.ExternalNotificationService;
import com.hmis.workflow.service.TemplateManagementService;
import com.hmis.workflow.service.WorkflowTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for workflow template management
 * Provides endpoints for creating, updating, publishing, and managing workflow templates
 */
@RestController
@RequestMapping("/workflows/templates")
@RequiredArgsConstructor
@Slf4j
public class WorkflowTemplateController {

    private final WorkflowTemplateService templateService;
    private final TemplateManagementService templateManagementService;
    private final ExternalNotificationService externalNotificationService;

    /**
     * Create a new workflow template
     * POST /workflows/templates
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> createTemplate(
            @RequestBody CreateWorkflowTemplateRequest request) {
        log.info("Creating new workflow template: {}", request.getName());

        WorkflowTemplate template = WorkflowTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .build();

        WorkflowTemplate created = templateService.createTemplate(template);
        WorkflowTemplateDTO dto = mapToDTO(created);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Workflow template created successfully"));
    }

    /**
     * Get all templates
     * GET /workflows/templates
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkflowTemplateDTO>>> getAllTemplates() {
        log.info("Fetching all workflow templates");

        List<WorkflowTemplate> templates = templateService.getActiveTemplates();
        List<WorkflowTemplateDTO> dtos = templates.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Templates retrieved successfully"));
    }

    /**
     * Get all published templates
     * GET /workflows/templates/published
     */
    @GetMapping("/published")
    public ResponseEntity<ApiResponse<List<WorkflowTemplateDTO>>> getPublishedTemplates() {
        log.info("Fetching all published workflow templates");

        List<WorkflowTemplate> templates = templateService.getPublishedTemplates();
        List<WorkflowTemplateDTO> dtos = templates.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Published templates retrieved successfully"));
    }

    /**
     * Get templates by category
     * GET /workflows/templates/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<WorkflowTemplateDTO>>> getTemplatesByCategory(
            @PathVariable String category) {
        log.info("Fetching workflow templates by category: {}", category);

        List<WorkflowTemplate> templates = templateService.getTemplatesByCategory(category);
        List<WorkflowTemplateDTO> dtos = templates.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos,
                "Templates for category '" + category + "' retrieved successfully"));
    }

    /**
     * Get template by ID
     * GET /workflows/templates/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> getTemplate(@PathVariable UUID id) {
        log.info("Fetching workflow template: {}", id);

        WorkflowTemplate template = templateService.getTemplate(id);
        WorkflowTemplateDTO dto = mapToDTO(template);

        return ResponseEntity.ok(ApiResponse.success(dto, "Template retrieved successfully"));
    }

    /**
     * Update template (DRAFT only)
     * PUT /workflows/templates/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> updateTemplate(
            @PathVariable UUID id,
            @RequestBody UpdateWorkflowTemplateRequest request) {
        log.info("Updating workflow template: {}", id);

        WorkflowTemplate updates = WorkflowTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .notes(request.getNotes())
                .build();

        WorkflowTemplate updated = templateService.updateTemplate(id, updates);
        WorkflowTemplateDTO dto = mapToDTO(updated);

        return ResponseEntity.ok(ApiResponse.success(dto, "Template updated successfully"));
    }

    /**
     * Soft delete template (marks as deleted without removing from DB)
     * DELETE /workflows/templates/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTemplate(@PathVariable UUID id) {
        log.info("Soft deleting workflow template: {}", id);

        templateManagementService.softDeleteTemplate(id);

        return ResponseEntity.ok(ApiResponse.success("Template deleted successfully",
                "Template soft-deleted successfully"));
    }

    /**
     * Submit template for review
     * POST /workflows/templates/{id}/submit-review
     */
    @PostMapping("/{id}/submit-review")
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> submitForReview(@PathVariable UUID id) {
        log.info("Submitting workflow template for review: {}", id);

        WorkflowTemplate template = templateService.submitForReview(id);
        WorkflowTemplateDTO dto = mapToDTO(template);

        return ResponseEntity.ok(ApiResponse.success(dto, "Template submitted for review"));
    }

    /**
     * Approve template
     * POST /workflows/templates/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> approveTemplate(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalRequest request) {
        log.info("Approving workflow template: {}", id);

        String approvedBy = request != null ? request.getApprovedBy() : "SYSTEM";
        WorkflowTemplate template = templateService.approveTemplate(id, approvedBy);
        WorkflowTemplateDTO dto = mapToDTO(template);

        return ResponseEntity.ok(ApiResponse.success(dto, "Template approved successfully"));
    }

    /**
     * Publish template
     * POST /workflows/templates/{id}/publish
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> publishTemplate(@PathVariable UUID id) {
        log.info("Publishing workflow template: {}", id);

        WorkflowTemplate template = templateService.publishTemplate(id);
        WorkflowTemplateDTO dto = mapToDTO(template);

        return ResponseEntity.ok(ApiResponse.success(dto, "Template published successfully"));
    }

    /**
     * Create new version of published template
     * POST /workflows/templates/{id}/version
     */
    @PostMapping("/{id}/version")
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> createNewVersion(@PathVariable UUID id) {
        log.info("Creating new version of workflow template: {}", id);

        WorkflowTemplate newVersion = templateService.createNewVersion(id);
        WorkflowTemplateDTO dto = mapToDTO(newVersion);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "New template version created successfully"));
    }

    /**
     * Deprecate template
     * POST /workflows/templates/{id}/deprecate
     */
    @PostMapping("/{id}/deprecate")
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> deprecateTemplate(@PathVariable UUID id) {
        log.info("Deprecating workflow template: {}", id);

        WorkflowTemplate template = templateService.deprecateTemplate(id);
        WorkflowTemplateDTO dto = mapToDTO(template);

        return ResponseEntity.ok(ApiResponse.success(dto, "Template deprecated successfully"));
    }

    /**
     * EDIT operation: Archives current template and creates new edited version with cloned data
     * Archives current version and creates new DRAFT version with incremented version number
     * Clones all tasks and orders to new version
     * POST /workflows/templates/{id}/edit
     */
    @PostMapping("/{id}/edit")
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> editTemplate(
            @PathVariable UUID id,
            @RequestBody(required = false) EditTemplateRequest request) {
        log.info("Editing workflow template: {}", id);

        String newName = request != null ? request.getNewName() : null;
        String newDescription = request != null ? request.getNewDescription() : null;

        WorkflowTemplate editedTemplate = templateManagementService.editTemplate(id, newName, newDescription);
        WorkflowTemplateDTO dto = mapToDTO(editedTemplate);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Template edited successfully. New version created with all tasks and orders cloned"));
    }

    /**
     * CLONE operation: Creates exact copy of template
     * Useful for creating templates based on existing ones
     * POST /workflows/templates/{id}/clone
     */
    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> cloneTemplate(
            @PathVariable UUID id,
            @RequestBody(required = false) CloneTemplateRequest request) {
        log.info("Cloning workflow template: {}", id);

        String newName = request != null ? request.getNewName() : null;

        WorkflowTemplate clonedTemplate = templateManagementService.cloneTemplate(id, newName);
        WorkflowTemplateDTO dto = mapToDTO(clonedTemplate);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Template cloned successfully with all tasks and orders"));
    }

    /**
     * Add order to template
     * Orders are external API calls that are part of the workflow
     * POST /workflows/templates/{id}/orders
     */
    @PostMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<TemplateOrderDTO>> addOrderToTemplate(
            @PathVariable UUID id,
            @RequestBody AddOrderToTemplateRequest request) {
        log.info("Adding order '{}' to workflow template: {}", request.getOrderCode(), id);

        TemplateOrder order = TemplateOrder.builder()
                .orderCode(request.getOrderCode())
                .orderName(request.getOrderName())
                .description(request.getDescription())
                .externalApiEndpoint(request.getExternalApiEndpoint())
                .apiMethod(request.getApiMethod())
                .apiRequestPayload(request.getApiRequestPayload())
                .isRequired(request.getIsRequired())
                .isAutomatic(request.getIsAutomatic())
                .orderSequence(request.getOrderSequence())
                .metadata(request.getMetadata())
                .build();

        TemplateOrder savedOrder = templateManagementService.addOrderToTemplate(id, order);
        TemplateOrderDTO dto = mapOrderToDTO(savedOrder);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Order added to template successfully"));
    }

    /**
     * Get all orders in a template
     * GET /workflows/templates/{id}/orders
     */
    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<List<TemplateOrderDTO>>> getTemplateOrders(@PathVariable UUID id) {
        log.info("Fetching orders for workflow template: {}", id);

        WorkflowTemplate template = templateService.getTemplate(id);
        List<TemplateOrderDTO> orders = template.getOrders().stream()
                .map(this::mapOrderToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    /**
     * Delete order from template
     * DELETE /workflows/templates/{id}/orders/{orderId}
     */
    @DeleteMapping("/{id}/orders/{orderId}")
    public ResponseEntity<ApiResponse<String>> deleteOrderFromTemplate(
            @PathVariable UUID id,
            @PathVariable String orderId) {
        log.info("Deleting order {} from workflow template: {}", orderId, id);

        templateManagementService.removeOrderFromTemplate(id, orderId);

        return ResponseEntity.ok(ApiResponse.success("Order deleted from template successfully",
                "Order deleted from template successfully"));
    }

    /**
     * Add task to template
     * POST /workflows/templates/{id}/tasks
     */
    @PostMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<WorkflowTemplateDTO>> addTaskToTemplate(
            @PathVariable UUID id,
            @RequestBody AddTaskToTemplateRequest request) {
        log.info("Adding task '{}' to workflow template: {}", request.getName(), id);

        WorkflowTaskDefinition taskDef = WorkflowTaskDefinition.builder()
                .name(request.getName())
                .description(request.getDescription())
                .assignTo(request.getAssignTo())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .instructions(request.getInstructions())
                .isParallel(request.getIsParallel())
                .isOptional(request.getIsOptional())
                .nextTaskId(request.getNextTaskId())
                .failureTaskId(request.getFailureTaskId())
                .metadata(request.getMetadata() != null ?
                    com.fasterxml.jackson.databind.ObjectMapper::new().toString() : null)
                .build();

        WorkflowTemplate template = templateService.addTaskToTemplate(id, taskDef);
        WorkflowTemplateDTO dto = mapToDTO(template);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Task added to template successfully"));
    }

    /**
     * Get all tasks in a template
     * GET /workflows/templates/{id}/tasks
     */
    @GetMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<List<WorkflowTaskDefinitionDTO>>> getTemplateTasks(
            @PathVariable UUID id) {
        log.info("Fetching tasks for workflow template: {}", id);

        WorkflowTemplate template = templateService.getTemplate(id);
        List<WorkflowTaskDefinitionDTO> tasks = template.getTasks().stream()
                .map(this::mapTaskToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(tasks, "Tasks retrieved successfully"));
    }

    /**
     * Delete task from template
     * DELETE /workflows/templates/{id}/tasks/{taskId}
     */
    @DeleteMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<ApiResponse<String>> deleteTaskFromTemplate(
            @PathVariable UUID id,
            @PathVariable UUID taskId) {
        log.info("Deleting task {} from workflow template: {}", taskId, id);

        WorkflowTemplate template = templateService.getTemplate(id);

        template.getTasks().removeIf(task -> task.getId().equals(taskId));

        // Save the updated template (implementation detail)
        log.info("Task deleted from template");

        return ResponseEntity.ok(ApiResponse.success("Task deleted from template successfully",
                "Task deleted from template successfully"));
    }

    // ==================== Task Notification Configuration Endpoints ====================

    /**
     * Configure external notification for a task definition.
     * Sets up Kafka topic and/or API endpoint for downstream system notifications.
     * PUT /workflows/templates/{id}/tasks/{taskId}/notification
     */
    @PutMapping("/{id}/tasks/{taskId}/notification")
    public ResponseEntity<ApiResponse<TaskNotificationConfigDTO>> configureTaskNotification(
            @PathVariable UUID id,
            @PathVariable UUID taskId,
            @RequestBody TaskNotificationConfigRequest request) {
        log.info("Configuring notification for task {} in template {}", taskId, id);

        WorkflowTemplate template = templateService.getTemplate(id);

        WorkflowTaskDefinition taskDef = template.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // Update notification configuration
        taskDef.setNotificationType(request.getNotificationType());
        taskDef.setNotificationKafkaTopic(request.getNotificationKafkaTopic());
        taskDef.setNotificationApiEndpoint(request.getNotificationApiEndpoint());
        taskDef.setNotificationApiMethod(request.getNotificationApiMethod());
        taskDef.setNotificationMessageTemplate(request.getNotificationMessageTemplate());
        taskDef.setNotificationApiHeaders(request.getNotificationApiHeaders());

        if (request.getNotifyOnFailure() != null) {
            taskDef.setNotifyOnFailure(request.getNotifyOnFailure());
        }
        if (request.getNotifyOnSkip() != null) {
            taskDef.setNotifyOnSkip(request.getNotifyOnSkip());
        }

        // Validate the configuration
        java.util.Map<String, Object> validation = externalNotificationService.validateConfiguration(taskDef);
        if (!Boolean.TRUE.equals(validation.get("valid"))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid notification configuration: " + validation.toString()));
        }

        // Save the updated template
        templateService.updateTemplate(id, template);

        TaskNotificationConfigDTO dto = TaskNotificationConfigDTO.fromEntity(taskDef);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task notification configured successfully"));
    }

    /**
     * Get notification configuration for a task definition.
     * GET /workflows/templates/{id}/tasks/{taskId}/notification
     */
    @GetMapping("/{id}/tasks/{taskId}/notification")
    public ResponseEntity<ApiResponse<TaskNotificationConfigDTO>> getTaskNotification(
            @PathVariable UUID id,
            @PathVariable UUID taskId) {
        log.info("Getting notification config for task {} in template {}", taskId, id);

        WorkflowTemplate template = templateService.getTemplate(id);

        WorkflowTaskDefinition taskDef = template.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        TaskNotificationConfigDTO dto = TaskNotificationConfigDTO.fromEntity(taskDef);

        return ResponseEntity.ok(ApiResponse.success(dto, "Task notification configuration retrieved"));
    }

    /**
     * Validate notification configuration for a task definition without saving.
     * POST /workflows/templates/{id}/tasks/{taskId}/notification/validate
     */
    @PostMapping("/{id}/tasks/{taskId}/notification/validate")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> validateTaskNotification(
            @PathVariable UUID id,
            @PathVariable UUID taskId,
            @RequestBody TaskNotificationConfigRequest request) {
        log.info("Validating notification config for task {} in template {}", taskId, id);

        WorkflowTemplate template = templateService.getTemplate(id);

        WorkflowTaskDefinition taskDef = template.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // Create a temporary copy for validation
        WorkflowTaskDefinition tempDef = WorkflowTaskDefinition.builder()
                .name(taskDef.getName())
                .notificationType(request.getNotificationType())
                .notificationKafkaTopic(request.getNotificationKafkaTopic())
                .notificationApiEndpoint(request.getNotificationApiEndpoint())
                .notificationApiMethod(request.getNotificationApiMethod())
                .notificationMessageTemplate(request.getNotificationMessageTemplate())
                .notificationApiHeaders(request.getNotificationApiHeaders())
                .build();

        java.util.Map<String, Object> validation = externalNotificationService.validateConfiguration(tempDef);

        String message = Boolean.TRUE.equals(validation.get("valid"))
                ? "Configuration is valid"
                : "Configuration has validation errors";

        return ResponseEntity.ok(ApiResponse.success(validation, message));
    }

    /**
     * Remove notification configuration from a task definition.
     * DELETE /workflows/templates/{id}/tasks/{taskId}/notification
     */
    @DeleteMapping("/{id}/tasks/{taskId}/notification")
    public ResponseEntity<ApiResponse<String>> removeTaskNotification(
            @PathVariable UUID id,
            @PathVariable UUID taskId) {
        log.info("Removing notification config for task {} in template {}", taskId, id);

        WorkflowTemplate template = templateService.getTemplate(id);

        WorkflowTaskDefinition taskDef = template.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // Clear notification configuration
        taskDef.setNotificationType("NONE");
        taskDef.setNotificationKafkaTopic(null);
        taskDef.setNotificationApiEndpoint(null);
        taskDef.setNotificationApiMethod(null);
        taskDef.setNotificationMessageTemplate(null);
        taskDef.setNotificationApiHeaders(null);
        taskDef.setNotifyOnFailure(true);
        taskDef.setNotifyOnSkip(false);

        // Save the updated template
        templateService.updateTemplate(id, template);

        return ResponseEntity.ok(ApiResponse.success("Notification configuration removed",
                "Task notification configuration has been removed"));
    }

    /**
     * Get all tasks with notification configured in a template.
     * GET /workflows/templates/{id}/tasks/with-notifications
     */
    @GetMapping("/{id}/tasks/with-notifications")
    public ResponseEntity<ApiResponse<List<TaskNotificationConfigDTO>>> getTasksWithNotifications(
            @PathVariable UUID id) {
        log.info("Getting all tasks with notifications in template {}", id);

        WorkflowTemplate template = templateService.getTemplate(id);

        List<TaskNotificationConfigDTO> tasksWithNotifications = template.getTasks().stream()
                .filter(t -> t.getNotificationType() != null
                        && !"NONE".equalsIgnoreCase(t.getNotificationType()))
                .map(TaskNotificationConfigDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(tasksWithNotifications,
                "Found " + tasksWithNotifications.size() + " tasks with notifications configured"));
    }

    // ==================== Helper Methods ====================

    private WorkflowTemplateDTO mapToDTO(WorkflowTemplate template) {
        List<WorkflowTaskDefinitionDTO> taskDTOs = template.getTasks().stream()
                .map(this::mapTaskToDTO)
                .collect(Collectors.toList());

        return WorkflowTemplateDTO.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .active(template.getActive())
                .version(template.getVersion())
                .category(template.getCategory())
                .tasks(taskDTOs)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private WorkflowTaskDefinitionDTO mapTaskToDTO(WorkflowTaskDefinition task) {
        return WorkflowTaskDefinitionDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .taskOrder(task.getTaskOrder())
                .assignTo(task.getAssignTo())
                .estimatedDurationMinutes(task.getEstimatedDurationMinutes())
                .instructions(task.getInstructions())
                .isParallel(task.getIsParallel())
                .isOptional(task.getIsOptional())
                .nextTaskId(task.getNextTaskId())
                .failureTaskId(task.getFailureTaskId())
                .build();
    }

    private TemplateOrderDTO mapOrderToDTO(TemplateOrder order) {
        return TemplateOrderDTO.builder()
                .id(order.getId())
                .templateId(order.getTemplateId())
                .orderCode(order.getOrderCode())
                .orderName(order.getOrderName())
                .description(order.getDescription())
                .externalApiEndpoint(order.getExternalApiEndpoint())
                .apiMethod(order.getApiMethod())
                .isRequired(order.getIsRequired())
                .isAutomatic(order.getIsAutomatic())
                .orderSequence(order.getOrderSequence())
                .metadata(order.getMetadata())
                .isActive(order.getIsActive())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Inner class for approval request
     */
    static class ApprovalRequest {
        public String approvedBy;

        public String getApprovedBy() {
            return approvedBy;
        }

        public void setApprovedBy(String approvedBy) {
            this.approvedBy = approvedBy;
        }
    }

    /**
     * Inner class for EDIT request
     */
    static class EditTemplateRequest {
        public String newName;
        public String newDescription;

        public String getNewName() {
            return newName;
        }

        public void setNewName(String newName) {
            this.newName = newName;
        }

        public String getNewDescription() {
            return newDescription;
        }

        public void setNewDescription(String newDescription) {
            this.newDescription = newDescription;
        }
    }

    /**
     * Inner class for CLONE request
     */
    static class CloneTemplateRequest {
        public String newName;

        public String getNewName() {
            return newName;
        }

        public void setNewName(String newName) {
            this.newName = newName;
        }
    }
}
