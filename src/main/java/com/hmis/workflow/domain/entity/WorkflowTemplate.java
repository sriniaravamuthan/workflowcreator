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
 * Used to define the structure and tasks of a workflow
 */
@Entity
@Table(name = "workflow_templates")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "tasks")
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

    @OneToMany(mappedBy = "template", orphanRemoval = true)
    @Builder.Default
    private Set<WorkflowTaskDefinition> tasks = new HashSet<>();

    public Integer getNextTaskOrder() {
        return tasks.isEmpty() ? 1 : tasks.stream()
                .mapToInt(WorkflowTaskDefinition::getTaskOrder)
                .max()
                .orElse(0) + 1;
    }
}
