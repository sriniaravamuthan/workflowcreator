package com.hmis.workflow.domain.entity;

import com.hmis.workflow.domain.enums.TaskStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TaskInstance entity representing an instance of a task in a workflow
 * Supports SLA tracking, escalation, role-based assignment, and task inputs/outputs
 */
@Entity
@Table(name = "task_instances")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"workflowInstance", "taskDefinition"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskInstance extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String taskInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(length = 255)
    private String assignedTo;

    @Column(length = 100)
    private String requiredRole; // Role required to complete task

    @Column(columnDefinition = "TEXT")
    private String taskInput; // JSON-serialized input data for task

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(columnDefinition = "TEXT")
    private String result; // Task output/result

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer retryCount = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 3")
    private Integer maxRetries = 3;

    @Column(length = 500)
    private String errorMessage;

    // SLA Tracking
    @Column
    private LocalDateTime dueAt; // SLA deadline

    @Column
    private LocalDateTime escalatedAt; // When task was escalated

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isEscalated = false;

    @Column(length = 100)
    private String escalatedToUser; // User escalated to

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer slaMinutes = 0; // Task SLA in minutes

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean slaBreached = false;

    @ManyToOne
    @JoinColumn(name = "workflow_instance_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_workflow"))
    @JsonIgnore
    private WorkflowInstance workflowInstance;

    @ManyToOne
    @JoinColumn(name = "task_definition_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_definition"))
    @JsonIgnore
    private WorkflowTaskDefinition taskDefinition;

    public Boolean isRetryable() {
        return retryCount < maxRetries && TaskStatus.FAILED.equals(status);
    }

    public Boolean isSLABreached() {
        if (dueAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(dueAt);
    }

    public Integer getTimeRemainingMinutes() {
        if (dueAt == null) {
            return null;
        }
        long minutesRemaining = java.time.temporal.ChronoUnit.MINUTES.between(LocalDateTime.now(), dueAt);
        return (int) minutesRemaining;
    }
}
