package com.hmis.workflow.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WorkflowTaskDefinition entity representing a task definition in a workflow template.
 *
 * Task Dependency Model:
 * - predecessorTaskIds: Comma-separated list of task definition IDs that must complete
 *   before this task can start. If null or empty, this is an "entry task" that can
 *   start immediately when the workflow begins.
 * - nextTaskId: The task to activate after this task completes (successor pattern).
 * - failureTaskId: The task to activate if this task fails.
 *
 * Entry tasks (no predecessors) are automatically set to READY status when workflow starts.
 * Tasks with predecessors remain BLOCKED until all predecessors complete.
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

    /**
     * Comma-separated list of task definition IDs that must complete before this task can start.
     * If null or empty, this task is an "entry task" and can start immediately.
     *
     * Example: "task-def-1,task-def-2" means both task-def-1 AND task-def-2 must complete.
     *
     * This field is OPTIONAL - tasks without predecessors are entry points in the workflow.
     */
    @Column(columnDefinition = "TEXT")
    private String predecessorTaskIds;

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

    /**
     * Check if this task is an entry task (has no predecessors).
     * Entry tasks can start immediately when the workflow begins.
     *
     * @return true if this task has no predecessor dependencies
     */
    @Transient
    public boolean isEntryTask() {
        return predecessorTaskIds == null || predecessorTaskIds.trim().isEmpty();
    }

    /**
     * Get the list of predecessor task definition IDs.
     *
     * @return List of predecessor task IDs, or empty list if this is an entry task
     */
    @Transient
    public List<String> getPredecessorTaskIdList() {
        if (predecessorTaskIds == null || predecessorTaskIds.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(predecessorTaskIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Set predecessors from a list of task definition IDs.
     *
     * @param predecessorIds List of predecessor task definition IDs
     */
    public void setPredecessorTaskIdList(List<String> predecessorIds) {
        if (predecessorIds == null || predecessorIds.isEmpty()) {
            this.predecessorTaskIds = null;
        } else {
            this.predecessorTaskIds = String.join(",", predecessorIds);
        }
    }

    /**
     * Add a predecessor task ID to the existing list.
     *
     * @param predecessorId Task definition ID to add as predecessor
     */
    public void addPredecessor(String predecessorId) {
        if (predecessorId == null || predecessorId.trim().isEmpty()) {
            return;
        }
        List<String> current = new java.util.ArrayList<>(getPredecessorTaskIdList());
        if (!current.contains(predecessorId.trim())) {
            current.add(predecessorId.trim());
            setPredecessorTaskIdList(current);
        }
    }

    /**
     * Remove a predecessor task ID from the list.
     *
     * @param predecessorId Task definition ID to remove
     */
    public void removePredecessor(String predecessorId) {
        if (predecessorId == null || predecessorId.trim().isEmpty()) {
            return;
        }
        List<String> current = new java.util.ArrayList<>(getPredecessorTaskIdList());
        current.remove(predecessorId.trim());
        setPredecessorTaskIdList(current);
    }
}
