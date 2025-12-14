package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.Patient;
import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.enums.TaskStatus;
import com.hmis.workflow.repository.TaskInstanceRepository;
import com.hmis.workflow.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing task instances
 * Handles task assignment, execution, escalation, and SLA monitoring
 *
 * Notifications are sent for:
 * - Task assignment (when task is explicitly assigned to a user)
 * - Task escalation (when task is escalated to another user)
 * - Task reassignment (when task is reassigned from one user to another)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskInstanceService {

    private final TaskInstanceRepository taskRepository;
    private final WorkflowInstanceRepository workflowRepository;
    private final NotificationService notificationService;

    /**
     * Get task instance by ID
     */
    public TaskInstance getTaskInstance(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task instance not found: " + taskId));
    }

    /**
     * Get task instance by instance ID
     */
    public TaskInstance getTaskByInstanceId(String instanceId) {
        return taskRepository.findByTaskInstanceId(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Task instance not found: " + instanceId));
    }

    /**
     * Get all tasks for a workflow
     */
    public List<TaskInstance> getTasksByWorkflow(UUID workflowInstanceId) {
        return taskRepository.findByWorkflowInstanceId(workflowInstanceId);
    }

    /**
     * Get all pending and in-progress tasks for an assignee
     */
    public List<TaskInstance> getAssignedTasks(String assignedTo) {
        return taskRepository.findPendingAndInProgressByAssignee(assignedTo);
    }

    /**
     * Assign task to user.
     * Sends notification to the assigned user.
     *
     * @param taskId The task instance ID
     * @param assignedTo The user ID to assign the task to
     * @return The updated task instance
     */
    public TaskInstance assignTask(UUID taskId, String assignedTo) {
        TaskInstance task = getTaskInstance(taskId);

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("Cannot assign task not in PENDING status");
        }

        String previousAssignee = task.getAssignedTo();
        task.setAssignedTo(assignedTo);
        log.info("Assigned task {} to {}", taskId, assignedTo);
        TaskInstance savedTask = taskRepository.save(task);

        // Notify the newly assigned user
        if (assignedTo != null && !assignedTo.isEmpty()) {
            boolean isReassignment = previousAssignee != null && !previousAssignee.isEmpty()
                    && !previousAssignee.equals(assignedTo);
            notifyTaskAssignment(savedTask, isReassignment);
        }

        return savedTask;
    }

    /**
     * Start task
     */
    public TaskInstance startTask(UUID taskId, String startedByUser) {
        TaskInstance task = getTaskInstance(taskId);

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("Cannot start task not in PENDING status");
        }

        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());
        log.info("Started task {} by {}", taskId, startedByUser);
        return taskRepository.save(task);
    }

    /**
     * Complete task with result
     */
    public TaskInstance completeTask(UUID taskId, String result, String completedByUser) {
        TaskInstance task = getTaskInstance(taskId);

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete task not in IN_PROGRESS status");
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setResult(result);
        task.setCompletedAt(LocalDateTime.now());
        task.setRetryCount(0);

        log.info("Completed task {} by {}", taskId, completedByUser);
        TaskInstance savedTask = taskRepository.save(task);

        // Trigger next tasks/orders in workflow
        WorkflowInstance workflow = task.getWorkflowInstance();
        if (task.getTaskDefinition().getNextTaskId() != null) {
            // Find and activate next task
            workflow.getTaskInstances().stream()
                    .filter(t -> task.getTaskDefinition().getNextTaskId().equals(t.getTaskDefinition().getId().toString()))
                    .forEach(t -> {
                        t.setStatus(TaskStatus.PENDING);
                        taskRepository.save(t);
                    });
        }

        return savedTask;
    }

    /**
     * Fail task with error message
     */
    public TaskInstance failTask(UUID taskId, String errorMessage, String failedByUser) {
        TaskInstance task = getTaskInstance(taskId);

        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cannot fail a completed or cancelled task");
        }

        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setCompletedAt(LocalDateTime.now());

        log.warn("Failed task {} - Error: {}", taskId, errorMessage);
        return taskRepository.save(task);
    }

    /**
     * Retry failed task
     */
    public TaskInstance retryTask(UUID taskId) {
        TaskInstance task = getTaskInstance(taskId);

        if (!task.isRetryable()) {
            throw new IllegalStateException("Task cannot be retried (max retries reached or not failed)");
        }

        task.setStatus(TaskStatus.PENDING);
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(null);
        task.setStartedAt(null);
        task.setCompletedAt(null);

        log.info("Retrying task {} - Attempt {}", taskId, task.getRetryCount());
        return taskRepository.save(task);
    }

    /**
     * Escalate task to another user.
     * Sends notification to the escalated user.
     *
     * @param taskId The task instance ID
     * @param escalatedToUser The user ID to escalate the task to
     * @param reason The reason for escalation
     * @return The updated task instance
     */
    public TaskInstance escalateTask(UUID taskId, String escalatedToUser, String reason) {
        TaskInstance task = getTaskInstance(taskId);

        task.setIsEscalated(true);
        task.setEscalatedAt(LocalDateTime.now());
        task.setEscalatedToUser(escalatedToUser);
        task.setComments((task.getComments() != null ? task.getComments() + "; " : "") + "Escalated: " + reason);

        log.info("Escalated task {} to {} - Reason: {}", taskId, escalatedToUser, reason);
        TaskInstance savedTask = taskRepository.save(task);

        // Notify the escalated user
        if (escalatedToUser != null && !escalatedToUser.isEmpty()) {
            notifyTaskEscalation(savedTask, reason);
        }

        return savedTask;
    }

    /**
     * Skip optional task (legacy method for backward compatibility).
     * Use skipTaskWithReason for better tracking.
     */
    public TaskInstance skipTask(UUID taskId) {
        return skipTaskWithReason(taskId, null, null, false);
    }

    /**
     * Skip a task with a reason and user tracking.
     *
     * Supports skipping both optional and required tasks:
     * - Optional tasks can be skipped without requiring a reason
     * - Required tasks can only be skipped with forceSkip=true and require a reason
     *
     * Use cases for skipping required tasks:
     * - Blood test already performed elsewhere
     * - Patient refused the procedure
     * - Clinical judgment overrides standard protocol
     * - Task no longer applicable due to condition change
     *
     * @param taskId The task instance ID
     * @param reason The reason for skipping (required for non-optional tasks)
     * @param skippedByUser The user who is skipping the task
     * @param forceSkip If true, allows skipping non-optional tasks (requires reason)
     * @return The updated task instance
     */
    public TaskInstance skipTaskWithReason(UUID taskId, String reason, String skippedByUser, boolean forceSkip) {
        TaskInstance task = getTaskInstance(taskId);

        // Check if task can be skipped
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Cannot skip an already completed task");
        }

        if (task.getStatus() == TaskStatus.SKIPPED) {
            throw new IllegalStateException("Task is already skipped");
        }

        // Determine if task is optional
        boolean isOptional = task.isOptional();

        if (!isOptional && !forceSkip) {
            throw new IllegalStateException(
                    "Cannot skip required task. Use forceSkip=true with a reason to override.");
        }

        if (!isOptional && (reason == null || reason.trim().isEmpty())) {
            throw new IllegalArgumentException(
                    "A reason is required when skipping a required task");
        }

        // Skip the task
        task.setStatus(TaskStatus.SKIPPED);
        task.setCompletedAt(LocalDateTime.now());
        task.setSkipReason(reason);
        task.setSkippedByUser(skippedByUser);

        if (!isOptional) {
            // Add audit comment for non-optional task skip
            String auditComment = String.format("REQUIRED TASK SKIPPED by %s. Reason: %s",
                    skippedByUser != null ? skippedByUser : "Unknown",
                    reason);
            task.setComments((task.getComments() != null ? task.getComments() + "; " : "") + auditComment);
        }

        log.info("Skipped {} task {} by {} - Reason: {}",
                isOptional ? "optional" : "REQUIRED",
                taskId,
                skippedByUser,
                reason);

        return taskRepository.save(task);
    }

    /**
     * Get skipped tasks for a workflow
     */
    public List<TaskInstance> getSkippedTasks(UUID workflowInstanceId) {
        return taskRepository.findByWorkflowInstanceId(workflowInstanceId).stream()
                .filter(t -> t.getStatus() == TaskStatus.SKIPPED)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get SLA-breached tasks
     */
    public List<TaskInstance> getSLABreachedTasks() {
        return taskRepository.findSLABreachedTasks(LocalDateTime.now());
    }

    /**
     * Check and update SLA status for task
     */
    public TaskInstance checkAndUpdateSLA(UUID taskId) {
        TaskInstance task = getTaskInstance(taskId);

        if (task.isSLABreached()) {
            task.setSlaBreached(true);
            log.warn("SLA breached for task {}", taskId);
            return taskRepository.save(task);
        }

        return task;
    }

    /**
     * Get retryable tasks
     */
    public List<TaskInstance> getRetryableTasks() {
        return taskRepository.findRetryableTasks();
    }

    /**
     * Update task comments
     */
    public TaskInstance updateTaskComments(UUID taskId, String comments) {
        TaskInstance task = getTaskInstance(taskId);
        task.setComments(comments);
        return taskRepository.save(task);
    }

    // ========================================================================
    // NOTIFICATION HELPER METHODS
    // ========================================================================

    /**
     * Send task assignment notification to the assigned user.
     *
     * @param task The task instance being assigned
     * @param isReassignment True if this is a reassignment from another user
     */
    private void notifyTaskAssignment(TaskInstance task, boolean isReassignment) {
        try {
            Patient patient = task.getWorkflowInstance().getPatient();
            String actionType = isReassignment ? "reassigned" : "assigned";

            String subject = String.format("Task %s: %s",
                    isReassignment ? "Reassigned" : "Assigned",
                    task.getTaskDefinition().getName());

            String message = String.format(
                    "You have been %s a task.\n\n" +
                    "Task: %s\n" +
                    "Description: %s\n" +
                    "Patient: %s %s\n" +
                    "Due Date: %s\n" +
                    "Priority: %s\n\n" +
                    "Please log in to the workflow system to view details and start the task.",
                    actionType,
                    task.getTaskDefinition().getName(),
                    task.getTaskDefinition().getDescription() != null ?
                            task.getTaskDefinition().getDescription() : "N/A",
                    patient.getFirstName(),
                    patient.getLastName(),
                    task.getDueAt() != null ? task.getDueAt().toString() : "Not set",
                    task.getTaskDefinition().getIsOptional() ? "Optional" : "Required"
            );

            NotificationRequest request = new NotificationRequest(
                    task.getAssignedTo(),
                    "TASK_ASSIGNMENT",
                    subject,
                    message
            );
            request.setTaskInstanceId(task.getId());
            request.setWorkflowInstanceId(task.getWorkflowInstance().getId());
            request.setPatientId(patient.getId());

            notificationService.notifyUser(request);
            log.info("Sent task {} notification to {} for task {}",
                    actionType, task.getAssignedTo(), task.getTaskDefinition().getName());

        } catch (Exception e) {
            log.error("Failed to send task assignment notification for task {}: {}",
                    task.getId(), e.getMessage());
        }
    }

    /**
     * Send task escalation notification to the escalated user.
     *
     * @param task The task instance being escalated
     * @param reason The reason for escalation
     */
    private void notifyTaskEscalation(TaskInstance task, String reason) {
        try {
            Patient patient = task.getWorkflowInstance().getPatient();

            String subject = String.format("URGENT: Task Escalated - %s",
                    task.getTaskDefinition().getName());

            String message = String.format(
                    "A task has been escalated to you and requires immediate attention.\n\n" +
                    "Task: %s\n" +
                    "Description: %s\n" +
                    "Patient: %s %s\n" +
                    "Due Date: %s\n" +
                    "Escalation Reason: %s\n" +
                    "Original Assignee: %s\n\n" +
                    "Please attend to this task immediately.",
                    task.getTaskDefinition().getName(),
                    task.getTaskDefinition().getDescription() != null ?
                            task.getTaskDefinition().getDescription() : "N/A",
                    patient.getFirstName(),
                    patient.getLastName(),
                    task.getDueAt() != null ? task.getDueAt().toString() : "Not set",
                    reason,
                    task.getAssignedTo() != null ? task.getAssignedTo() : "Not assigned"
            );

            NotificationRequest request = new NotificationRequest(
                    task.getEscalatedToUser(),
                    "TASK_ESCALATION",
                    subject,
                    message
            );
            request.setTaskInstanceId(task.getId());
            request.setWorkflowInstanceId(task.getWorkflowInstance().getId());
            request.setPatientId(patient.getId());

            notificationService.notifyUser(request);
            log.info("Sent task escalation notification to {} for task {}",
                    task.getEscalatedToUser(), task.getTaskDefinition().getName());

        } catch (Exception e) {
            log.error("Failed to send task escalation notification for task {}: {}",
                    task.getId(), e.getMessage());
        }
    }
}
