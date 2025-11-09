package com.hmis.workflow.service;

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
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskInstanceService {

    private final TaskInstanceRepository taskRepository;
    private final WorkflowInstanceRepository workflowRepository;

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
     * Assign task to user
     */
    public TaskInstance assignTask(UUID taskId, String assignedTo) {
        TaskInstance task = getTaskInstance(taskId);

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("Cannot assign task not in PENDING status");
        }

        task.setAssignedTo(assignedTo);
        log.info("Assigned task {} to {}", taskId, assignedTo);
        return taskRepository.save(task);
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
     * Escalate task
     */
    public TaskInstance escalateTask(UUID taskId, String escalatedToUser, String reason) {
        TaskInstance task = getTaskInstance(taskId);

        task.setIsEscalated(true);
        task.setEscalatedAt(LocalDateTime.now());
        task.setEscalatedToUser(escalatedToUser);
        task.setComments((task.getComments() != null ? task.getComments() + "; " : "") + "Escalated: " + reason);

        log.info("Escalated task {} to {} - Reason: {}", taskId, escalatedToUser, reason);
        return taskRepository.save(task);
    }

    /**
     * Skip optional task
     */
    public TaskInstance skipTask(UUID taskId) {
        TaskInstance task = getTaskInstance(taskId);

        if (!task.getTaskDefinition().getIsOptional()) {
            throw new IllegalStateException("Cannot skip required task");
        }

        task.setStatus(TaskStatus.SKIPPED);
        task.setCompletedAt(LocalDateTime.now());

        log.info("Skipped optional task {}", taskId);
        return taskRepository.save(task);
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
}
