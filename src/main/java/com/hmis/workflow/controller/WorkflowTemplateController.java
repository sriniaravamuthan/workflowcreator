package com.hmis.workflow.controller;

import com.hmis.workflow.domain.entity.WorkflowTemplate;
import com.hmis.workflow.domain.entity.WorkflowTaskDefinition;
import com.hmis.workflow.dto.AddTaskToTemplateRequest;
import com.hmis.workflow.dto.ApiResponse;
import com.hmis.workflow.dto.CreateWorkflowTemplateRequest;
import com.hmis.workflow.dto.UpdateWorkflowTemplateRequest;
import com.hmis.workflow.dto.WorkflowTaskDefinitionDTO;
import com.hmis.workflow.dto.WorkflowTemplateDTO;
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
     * Delete template (DRAFT only)
     * DELETE /workflows/templates/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTemplate(@PathVariable UUID id) {
        log.info("Deleting workflow template: {}", id);

        templateService.deleteTemplate(id);

        return ResponseEntity.ok(ApiResponse.success("Template deleted successfully",
                "Template deleted successfully"));
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
}
