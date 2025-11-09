package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.WorkflowTemplate;
import com.hmis.workflow.domain.entity.WorkflowTaskDefinition;
import com.hmis.workflow.repository.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing workflow templates
 * Handles template creation, versioning, publishing, and governance
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowTemplateService {

    private final WorkflowTemplateRepository templateRepository;

    /**
     * Create a new workflow template
     */
    public WorkflowTemplate createTemplate(WorkflowTemplate template) {
        log.info("Creating new workflow template: {}", template.getName());
        template.setVersion(1);
        template.setReviewStatus("DRAFT");
        return templateRepository.save(template);
    }

    /**
     * Get template by ID
     */
    public WorkflowTemplate getTemplate(UUID templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
    }

    /**
     * Get template by name
     */
    public WorkflowTemplate getTemplateByName(String name) {
        return templateRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + name));
    }

    /**
     * Get all active templates
     */
    public List<WorkflowTemplate> getActiveTemplates() {
        return templateRepository.findByActiveTrue();
    }

    /**
     * Get all published templates
     */
    public List<WorkflowTemplate> getPublishedTemplates() {
        return templateRepository.findAllPublished();
    }

    /**
     * Get templates by category
     */
    public List<WorkflowTemplate> getTemplatesByCategory(String category) {
        return templateRepository.findByCategory(category);
    }

    /**
     * Update template (only if in DRAFT status)
     */
    public WorkflowTemplate updateTemplate(UUID templateId, WorkflowTemplate updates) {
        WorkflowTemplate template = getTemplate(templateId);

        if (!"DRAFT".equals(template.getReviewStatus())) {
            throw new IllegalStateException("Cannot update template not in DRAFT status");
        }

        template.setName(updates.getName());
        template.setDescription(updates.getDescription());
        template.setCategory(updates.getCategory());
        template.setNotes(updates.getNotes());

        log.info("Updated template: {}", templateId);
        return templateRepository.save(template);
    }

    /**
     * Submit template for review
     */
    public WorkflowTemplate submitForReview(UUID templateId) {
        WorkflowTemplate template = getTemplate(templateId);

        if (!"DRAFT".equals(template.getReviewStatus())) {
            throw new IllegalStateException("Only DRAFT templates can be submitted for review");
        }

        if (template.getTasks().isEmpty()) {
            throw new IllegalStateException("Cannot submit template without tasks");
        }

        template.setReviewStatus("IN_REVIEW");
        log.info("Submitted template for review: {}", templateId);
        return templateRepository.save(template);
    }

    /**
     * Approve template
     */
    public WorkflowTemplate approveTemplate(UUID templateId, String approvedByUser) {
        WorkflowTemplate template = getTemplate(templateId);

        if (!"IN_REVIEW".equals(template.getReviewStatus())) {
            throw new IllegalStateException("Only IN_REVIEW templates can be approved");
        }

        template.setReviewStatus("APPROVED");
        template.setApprovedByUser(approvedByUser);
        log.info("Approved template: {} by {}", templateId, approvedByUser);
        return templateRepository.save(template);
    }

    /**
     * Publish template
     */
    public WorkflowTemplate publishTemplate(UUID templateId) {
        WorkflowTemplate template = getTemplate(templateId);

        if (!"APPROVED".equals(template.getReviewStatus())) {
            throw new IllegalStateException("Only APPROVED templates can be published");
        }

        template.setReviewStatus("PUBLISHED");
        template.setActive(true);
        log.info("Published template: {}", templateId);
        return templateRepository.save(template);
    }

    /**
     * Create a new version of a published template
     */
    public WorkflowTemplate createNewVersion(UUID templateId) {
        WorkflowTemplate original = getTemplate(templateId);

        if (!"PUBLISHED".equals(original.getReviewStatus())) {
            throw new IllegalStateException("Cannot version a non-published template");
        }

        WorkflowTemplate newVersion = new WorkflowTemplate();
        newVersion.setName(original.getName());
        newVersion.setDescription(original.getDescription());
        newVersion.setCategory(original.getCategory());
        newVersion.setVersion(original.getVersion() + 1);
        newVersion.setReviewStatus("DRAFT");

        // Copy tasks from original
        for (WorkflowTaskDefinition task : original.getTasks()) {
            WorkflowTaskDefinition newTask = new WorkflowTaskDefinition();
            newTask.setName(task.getName());
            newTask.setDescription(task.getDescription());
            newTask.setTaskOrder(task.getTaskOrder());
            newTask.setAssignTo(task.getAssignTo());
            newTask.setEstimatedDurationMinutes(task.getEstimatedDurationMinutes());
            newTask.setInstructions(task.getInstructions());
            newTask.setIsParallel(task.getIsParallel());
            newTask.setIsOptional(task.getIsOptional());
            newTask.setTemplate(newVersion);
            newVersion.getTasks().add(newTask);
        }

        log.info("Created new version {} of template: {}", newVersion.getVersion(), templateId);
        return templateRepository.save(newVersion);
    }

    /**
     * Deprecate a template
     */
    public WorkflowTemplate deprecateTemplate(UUID templateId) {
        WorkflowTemplate template = getTemplate(templateId);
        template.setReviewStatus("DEPRECATED");
        template.setActive(false);
        log.info("Deprecated template: {}", templateId);
        return templateRepository.save(template);
    }

    /**
     * Delete template (only if DRAFT)
     */
    public void deleteTemplate(UUID templateId) {
        WorkflowTemplate template = getTemplate(templateId);

        if (!"DRAFT".equals(template.getReviewStatus())) {
            throw new IllegalStateException("Cannot delete non-draft templates");
        }

        templateRepository.deleteById(templateId);
        log.info("Deleted template: {}", templateId);
    }

    /**
     * Add task to template
     */
    public WorkflowTemplate addTaskToTemplate(UUID templateId, WorkflowTaskDefinition task) {
        WorkflowTemplate template = getTemplate(templateId);

        if (!"DRAFT".equals(template.getReviewStatus())) {
            throw new IllegalStateException("Cannot add tasks to non-draft templates");
        }

        task.setTemplate(template);
        task.setTaskOrder(template.getNextTaskOrder());
        template.getTasks().add(task);

        log.info("Added task to template {}: {}", templateId, task.getName());
        return templateRepository.save(template);
    }
}
