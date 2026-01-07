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

import java.time.LocalTime;
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

    // Scheduling Constraints
    /**
     * Optional scheduled start time (time of day).
     * When set, the task should ideally start at this time.
     * Example: 08:00 for morning medication rounds
     */
    @Column
    private LocalTime scheduledStartTime;

    /**
     * Maximum wait time in minutes before escalation.
     * If task stays in PENDING status longer than this, it should be escalated.
     * Different from SLA which tracks total duration from start to completion.
     */
    @Column
    private Integer maxWaitTimeMinutes;

    /**
     * Earliest time of day when this task can be executed.
     * Example: 06:00 for fasting lab draws
     */
    @Column
    private LocalTime allowedStartTimeOfDay;

    /**
     * Latest time of day when this task can be executed.
     * Example: 18:00 for non-emergency procedures
     */
    @Column
    private LocalTime allowedEndTimeOfDay;

    /**
     * Comma-separated list of allowed days of week.
     * Example: "MON,TUE,WED,THU,FRI" for weekday-only tasks
     * If null, task can be executed any day.
     */
    @Column(length = 50)
    private String allowedDaysOfWeek;

    // ========================================================================
    // EXTERNAL NOTIFICATION CONFIGURATION
    // ========================================================================

    /**
     * Type of external notification to send when task completes.
     * Values: NONE, KAFKA, API, BOTH
     * NONE = No external notification (default)
     * KAFKA = Publish to configured Kafka topic
     * API = Call configured HTTP API endpoint
     * BOTH = Both Kafka and API
     */
    @Column(length = 20)
    private String notificationType;

    /**
     * Kafka topic name for task completion events.
     * Only used when notificationType is KAFKA or BOTH.
     * Example: "lab-orders-completed", "pharmacy-dispense-requests"
     */
    @Column(length = 255)
    private String notificationKafkaTopic;

    /**
     * External API endpoint URL for task completion notification.
     * Only used when notificationType is API or BOTH.
     * Example: "https://lab-system.hospital.com/api/orders/complete"
     */
    @Column(length = 500)
    private String notificationApiEndpoint;

    /**
     * HTTP method for API notification.
     * Values: POST, PUT (default: POST)
     */
    @Column(length = 10)
    private String notificationApiMethod;

    /**
     * Message/payload template for notifications (JSON format).
     * Supports placeholders for dynamic values:
     * - ${taskInstanceId} - Task instance UUID
     * - ${taskName} - Name of the task
     * - ${taskResult} - Result string from task completion
     * - ${workflowInstanceId} - Workflow instance UUID
     * - ${patientId} - Patient UUID
     * - ${patientMrn} - Patient MRN
     * - ${completedAt} - Completion timestamp
     * - ${completedBy} - User who completed the task
     * - ${status} - Task status (COMPLETED, FAILED, SKIPPED)
     * - ${orderId} - Associated order ID (if any)
     * - ${orderCode} - Associated order code (if any)
     *
     * Example template:
     * {
     *   "eventType": "TASK_COMPLETED",
     *   "taskId": "${taskInstanceId}",
     *   "taskName": "${taskName}",
     *   "result": "${taskResult}",
     *   "patientMrn": "${patientMrn}",
     *   "completedAt": "${completedAt}",
     *   "completedBy": "${completedBy}"
     * }
     */
    @Column(columnDefinition = "TEXT")
    private String notificationMessageTemplate;

    /**
     * Additional HTTP headers for API notification (JSON format).
     * Example: {"X-API-Key": "secret123", "X-Source": "workflow-engine"}
     */
    @Column(columnDefinition = "TEXT")
    private String notificationApiHeaders;

    /**
     * Whether to send notification on task failure.
     * Default: true
     */
    @Column(columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean notifyOnFailure = true;

    /**
     * Whether to send notification on task skip.
     * Default: false
     */
    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean notifyOnSkip = false;

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

    /**
     * Get the list of allowed days of week.
     *
     * @return List of day names (e.g., "MON", "TUE"), or empty list if all days allowed
     */
    @Transient
    public List<String> getAllowedDaysOfWeekList() {
        if (allowedDaysOfWeek == null || allowedDaysOfWeek.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(allowedDaysOfWeek.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Check if this task has time-of-day constraints.
     *
     * @return true if the task has allowed time window restrictions
     */
    @Transient
    public boolean hasTimeOfDayConstraints() {
        return allowedStartTimeOfDay != null || allowedEndTimeOfDay != null;
    }

    /**
     * Check if a given time is within the allowed execution window.
     *
     * @param time The time to check
     * @return true if the time is within allowed window, or if no constraints exist
     */
    @Transient
    public boolean isTimeAllowed(LocalTime time) {
        if (time == null || !hasTimeOfDayConstraints()) {
            return true;
        }
        boolean afterStart = allowedStartTimeOfDay == null || !time.isBefore(allowedStartTimeOfDay);
        boolean beforeEnd = allowedEndTimeOfDay == null || !time.isAfter(allowedEndTimeOfDay);
        return afterStart && beforeEnd;
    }

    /**
     * Check if a given day of week is allowed for this task.
     *
     * @param dayOfWeek The day to check (e.g., "MON", "TUE")
     * @return true if the day is allowed, or if no day constraints exist
     */
    @Transient
    public boolean isDayAllowed(String dayOfWeek) {
        List<String> allowedDays = getAllowedDaysOfWeekList();
        if (allowedDays.isEmpty()) {
            return true; // No restrictions
        }
        return allowedDays.contains(dayOfWeek.toUpperCase());
    }
}
