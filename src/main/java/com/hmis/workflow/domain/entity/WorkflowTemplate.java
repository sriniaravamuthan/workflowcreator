package com.hmis.workflow.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * WorkflowTemplate entity representing a template for workflows
 * Defines structure, tasks, gates, decision logic, and conditions for clinical workflows
 * Supports versioning and governance controls
 */
@Entity
@Table(name = "workflow_templates")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"tasks", "gates", "decisionLogics"})
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
}
