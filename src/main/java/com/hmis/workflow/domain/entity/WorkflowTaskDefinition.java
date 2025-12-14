package com.hmis.workflow.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * WorkflowTaskDefinition entity representing a task definition in a workflow template
 */
@Entity
@Table(name = "workflow_task_definitions")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "template")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTaskDefinition extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer taskOrder;

    @Column(length = 100)
    private String assignTo;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer estimatedDurationMinutes = 0;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isParallel = false;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isOptional = false;

    @Column(length = 100)
    private String nextTaskId;

    @Column(length = 100)
    private String failureTaskId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_template"))
    @JsonIgnore
    private WorkflowTemplate template;
}
