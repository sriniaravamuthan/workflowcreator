package com.hmis.workflow.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * WorkflowTemplate entity representing a template for workflows
 * Defines structure, tasks, gates, decision logic, and conditions for clinical workflows
 * Supports versioning and governance controls
 */
@Entity
@Table(name = "workflow_templates")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"tasks", "gates", "decisionLogics", "orders"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer version = 1;

    @Column(length = 50)
    private String category;

    @Column(length = 100)
    private String reviewStatus; // DRAFT, IN_REVIEW, APPROVED, PUBLISHED, DEPRECATED

    @Column(length = 100)
    private String approvedByUser;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "template", orphanRemoval = true)
    @Builder.Default
    private Set<WorkflowTaskDefinition> tasks = new HashSet<>();

    @OneToMany(mappedBy = "template", orphanRemoval = true)
    @Builder.Default
    private Set<Gate> gates = new HashSet<>();

    @OneToMany(mappedBy = "template", orphanRemoval = true)
    @Builder.Default
    private Set<DecisionLogic> decisionLogics = new HashSet<>();

    /**
     * Orders that are part of this workflow template
     * Combination of internal tasks and external API orders
     */
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TemplateOrder> orders = new HashSet<>();

    /**
     * Soft delete flag - true if template is deleted
     */
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * References the template this was cloned from (if any)
     */
    @Column(name = "cloned_from_template_id")
    private UUID clonedFromTemplateId;

    /**
     * If this is an edited version, references the previous version
     */
    @Column(name = "parent_template_id")
    private UUID parentTemplateId;

    public Integer getNextTaskOrder() {
        return tasks.isEmpty() ? 1 : tasks.stream()
                .mapToInt(WorkflowTaskDefinition::getTaskOrder)
                .max()
                .orElse(0) + 1;
    }

    public boolean isPublished() {
        return "PUBLISHED".equals(reviewStatus);
    }

    public boolean isDeprecated() {
        return "DEPRECATED".equals(reviewStatus);
    }

    /**
     * Soft deletes this template (marks as deleted without removing from database)
     */
    public void softDelete() {
        this.isDeleted = true;
        this.active = false;
    }

    /**
     * Archives this template - used when creating a new edited version
     * Sets parent template ID and marks as archived
     */
    public void archiveForEdit(UUID newTemplateId) {
        this.isDeleted = false;  // Keep for history
        this.active = false;     // But mark as inactive
        this.reviewStatus = "ARCHIVED";
        this.parentTemplateId = newTemplateId;
    }

    /**
     * Checks if this template is soft deleted
     */
    public boolean isBeingDeleted() {
        return isDeleted != null && isDeleted;
    }

    /**
     * Checks if this is an archived version (was edited to create a new version)
     */
    public boolean isArchived() {
        return "ARCHIVED".equals(reviewStatus) && parentTemplateId != null;
    }
}
