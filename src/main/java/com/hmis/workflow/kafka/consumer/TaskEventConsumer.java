package com.hmis.workflow.kafka.consumer;

import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.enums.TaskStatus;
import com.hmis.workflow.domain.event.TaskEvent;
import com.hmis.workflow.service.NotificationRequest;
import com.hmis.workflow.service.NotificationService;
import com.hmis.workflow.service.TaskInstanceService;
import com.hmis.workflow.service.WorkflowInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Kafka event consumer for task lifecycle events.
 * Automatically processes task events and triggers next task progression.
 *
 * Task Propagation Modes:
 * 1. Predecessor-based: Tasks with predecessorTaskIds are unblocked when all predecessors complete
 * 2. Successor-based: Tasks with nextTaskId are explicitly activated (legacy support)
 *
 * Entry tasks (no predecessors) start immediately when workflow begins.
 * Tasks with predecessors remain BLOCKED until all predecessors complete.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskEventConsumer {

    private final TaskInstanceService taskInstanceService;
    private final WorkflowInstanceService workflowInstanceService;
    private final NotificationService notificationService;

    /**
     * Listens for task events and processes them
     * Automatically triggers next task in workflow when current task completes
     */
    @KafkaListener(topics = "workflow-task-events", groupId = "workflow-engine-task-consumer")
    public void handleTaskEvent(
            @Payload TaskEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received task event: {} for task: {} in workflow: {} [topic: {}, partition: {}, offset: {}]",
                event.getEventType(), event.getTaskInstanceId(), event.getWorkflowInstanceId(),
                topic, partition, offset);

        try {
            switch (event.getEventType()) {
                case "TASK_COMPLETED":
                    handleTaskCompleted(event);
                    break;
                case "TASK_FAILED":
                    handleTaskFailed(event);
                    break;
                case "TASK_STARTED":
                    handleTaskStarted(event);
                    break;
                case "TASK_CREATED":
                    handleTaskCreated(event);
                    break;
                default:
                    log.warn("Unknown task event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing task event: {}", event.getEventId(), e);
            // In production, this should be sent to a dead-letter queue
        }
    }

    /**
     * Handles task completion event.
     *
     * Task Propagation Logic:
     * 1. Predecessor-based (primary): Unblock any tasks that have this task as a predecessor
     * 2. Successor-based (legacy): Activate nextTaskId if defined
     *
     * This dual approach ensures backward compatibility while supporting the new
     * predecessor-based dependency model.
     *
     * Note: Ad-hoc tasks (taskDefinition = null) are supported but don't trigger
     * predecessor-based propagation since they have no task definition ID.
     */
    private void handleTaskCompleted(TaskEvent event) {
        log.info("Processing TASK_COMPLETED event for task: {}", event.getTaskInstanceId());

        try {
            UUID taskInstanceId = UUID.fromString(event.getTaskInstanceId());
            UUID workflowInstanceId = UUID.fromString(event.getWorkflowInstanceId());

            // Get the task instance
            TaskInstance completedTask = taskInstanceService.getTaskInstance(taskInstanceId);

            if (completedTask == null) {
                log.warn("Task instance not found: {}", taskInstanceId);
                return;
            }

            // Get workflow instance
            WorkflowInstance workflow = workflowInstanceService.getWorkflowInstance(workflowInstanceId);

            if (workflow == null) {
                log.warn("Workflow instance not found: {}", workflowInstanceId);
                return;
            }

            // Check if there are any pending gates/instructions blocking the next task
            boolean isBlocked = checkForBlockingInstructions(workflowInstanceId);

            if (isBlocked) {
                log.info("Task completion blocked by unacknowledged instructions for workflow: {}",
                        workflowInstanceId);
                return;
            }

            // Only perform predecessor-based propagation for template-based tasks
            // Ad-hoc tasks (taskDefinition = null) don't have a definition ID to reference
            if (completedTask.getTaskDefinition() != null) {
                String completedTaskDefId = completedTask.getTaskDefinition().getId().toString();

                // PRIMARY: Predecessor-based unblocking
                // Unblock any tasks that have this completed task as a predecessor
                List<TaskInstance> unblockedTasks = workflowInstanceService.unblockReadyTasks(
                        workflowInstanceId, completedTaskDefId);

                if (!unblockedTasks.isEmpty()) {
                    log.info("Unblocked {} tasks after completion of task: {}",
                            unblockedTasks.size(), completedTask.getTaskName());

                    // Notify assignees of newly available tasks
                    for (TaskInstance unblocked : unblockedTasks) {
                        if (unblocked.getAssignedTo() != null) {
                            notifyTaskAssignment(unblocked, unblocked.getAssignedTo());
                        }
                    }
                }

                // LEGACY: Successor-based propagation (for backward compatibility)
                // Propagate to next task if defined in task definition
                if (completedTask.getTaskDefinition().getNextTaskId() != null) {
                    String nextTaskDefId = completedTask.getTaskDefinition().getNextTaskId();
                    log.info("Legacy nextTaskId defined: {} after completion of task: {}",
                            nextTaskDefId, taskInstanceId);

                    // Find the task instance for this definition (only template-based tasks)
                    workflow.getTaskInstances().stream()
                            .filter(t -> t.getTaskDefinition() != null)
                            .filter(t -> t.getTaskDefinition().getId().toString().equals(nextTaskDefId))
                            .filter(t -> t.getStatus() == TaskStatus.BLOCKED)
                            .findFirst()
                            .ifPresent(nextTask -> {
                                // Check if all predecessors are complete before unblocking
                                if (workflowInstanceService.canUnblockTask(nextTask)) {
                                    nextTask.setStatus(TaskStatus.PENDING);
                                    if (nextTask.getSlaMinutes() != null && nextTask.getSlaMinutes() > 0) {
                                        nextTask.setDueAt(java.time.LocalDateTime.now()
                                                .plusMinutes(nextTask.getSlaMinutes()));
                                    }
                                    log.info("Activated next task via nextTaskId: {}",
                                            nextTask.getTaskName());
                                }
                            });
                }
            } else {
                log.info("Ad-hoc task completed - no predecessor-based propagation: {}",
                        completedTask.getTaskName());
            }

            // Update workflow status - check if all tasks are done
            workflowInstanceService.updateWorkflowStatus(workflowInstanceId);

            log.info("Successfully processed TASK_COMPLETED event for task: {}", event.getTaskInstanceId());
        } catch (Exception e) {
            log.error("Error handling task completion event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handles task failure event
     * Evaluates failure task definition and triggers recovery actions
     */
    private void handleTaskFailed(TaskEvent event) {
        log.info("Processing TASK_FAILED event for task: {} - Error: {}",
                event.getTaskInstanceId(), event.getErrorMessage());

        try {
            UUID taskInstanceId = UUID.fromString(event.getTaskInstanceId());

            TaskInstance failedTask = taskInstanceService.getTaskInstance(taskInstanceId);

            if (failedTask == null) {
                log.warn("Task instance not found: {}", taskInstanceId);
                return;
            }

            // Check if task can be retried
            if (failedTask.isRetryable()) {
                log.info("Task {} is retryable (attempt {}/{})",
                        taskInstanceId, failedTask.getRetryCount(), failedTask.getMaxRetries());

                // Trigger automatic retry based on business rules
                // In production, may want to apply backoff strategies
                if (failedTask.getRetryCount() < failedTask.getMaxRetries()) {
                    log.info("Automatically retrying task: {}", taskInstanceId);
                    taskInstanceService.retryTask(taskInstanceId);
                }
            } else {
                // Task cannot be retried, handle failure task if defined
                if (failedTask.getTaskDefinition() != null &&
                        failedTask.getTaskDefinition().getFailureTaskId() != null) {

                    UUID failureTaskId = failedTask.getTaskDefinition().getFailureTaskId();
                    log.info("Triggering failure task: {} after failure of task: {}",
                            failureTaskId, taskInstanceId);

                    TaskInstance failureTask = taskInstanceService.getTaskInstance(failureTaskId);
                    if (failureTask != null) {
                        // Mark failure task as ready
                        log.info("Failure task {} is now available for execution", failureTaskId);
                    }
                } else {
                    // No recovery task, mark workflow as failed
                    UUID workflowInstanceId = UUID.fromString(event.getWorkflowInstanceId());
                    log.warn("No failure task defined, marking workflow {} as failed", workflowInstanceId);
                    workflowInstanceService.updateWorkflowStatus(workflowInstanceId);
                }
            }

            log.info("Successfully processed TASK_FAILED event for task: {}", event.getTaskInstanceId());
        } catch (Exception e) {
            log.error("Error handling task failure event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handles task started event
     * Logs task start and updates SLA tracking
     */
    private void handleTaskStarted(TaskEvent event) {
        log.info("Processing TASK_STARTED event for task: {}", event.getTaskInstanceId());

        try {
            UUID taskInstanceId = UUID.fromString(event.getTaskInstanceId());
            TaskInstance task = taskInstanceService.getTaskInstance(taskInstanceId);

            if (task != null) {
                // Check SLA status
                if (task.isSLABreached()) {
                    log.warn("Task {} started but SLA already breached (due: {})",
                            taskInstanceId, task.getDueAt());
                }

                log.info("Task {} has been started by user: {}", taskInstanceId, task.getAssignedTo());
            }
        } catch (Exception e) {
            log.error("Error handling task started event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handles task created event
     * Initializes task tracking and notifications
     */
    private void handleTaskCreated(TaskEvent event) {
        log.info("Processing TASK_CREATED event for task: {} in workflow: {}",
                event.getTaskInstanceId(), event.getWorkflowInstanceId());

        try {
            UUID taskInstanceId = UUID.fromString(event.getTaskInstanceId());
            TaskInstance task = taskInstanceService.getTaskInstance(taskInstanceId);

            if (task != null) {
                log.info("Task created: {} with due date: {}", taskInstanceId, task.getDueAt());
                // In production: Send notifications to task owners, update dashboards, etc.
            }
        } catch (Exception e) {
            log.error("Error handling task created event: {}", event.getEventId(), e);
        }
    }

    /**
     * Checks if there are any blocking instructions for a workflow instance
     * Returns true if there are unacknowledged blocking instructions
     */
    private boolean checkForBlockingInstructions(UUID workflowInstanceId) {
        try {
            // This is a placeholder for actual business logic
            // In production, query instructions table for blocking unacknowledged instructions
            // SELECT COUNT(*) FROM instructions WHERE workflow_instance_id = ? AND is_blocking = true AND acknowledged = false
            return false; // For now, assume no blocking
        } catch (Exception e) {
            log.error("Error checking blocking instructions for workflow: {}", workflowInstanceId, e);
            return true; // Default to blocking on error for safety
        }
    }

    /**
     * Sends task escalation notification to escalated user.
     * Uses helper methods to support both template-based and ad-hoc tasks.
     */
    private void notifyTaskEscalation(TaskInstance task, String escalatedToUser) {
        try {
            // Use helper methods to handle both template-based and ad-hoc tasks
            String taskName = task.getTaskName();

            String subject = String.format("URGENT: Task '%s' Escalated - SLA Breach", taskName);

            String message = String.format(
                    "Task '%s' has been escalated due to SLA breach.\n\n" +
                    "Patient: %s\n" +
                    "Due Date: %s\n" +
                    "Escalated By: System\n" +
                    "Priority: URGENT\n\n" +
                    "Please attend to this task immediately.",
                    taskName,
                    task.getWorkflowInstance().getPatient().getId(),
                    task.getDueAt()
            );

            NotificationRequest request = new NotificationRequest(
                    escalatedToUser,
                    "TASK_ESCALATION",
                    subject,
                    message
            );
            request.setTaskInstanceId(task.getId());
            request.setWorkflowInstanceId(task.getWorkflowInstance().getId());
            request.setPatientId(task.getWorkflowInstance().getPatient().getId());

            notificationService.notifyUser(request);
            log.info("Task escalation notification sent to: {}", escalatedToUser);

        } catch (Exception e) {
            log.error("Error sending task escalation notification", e);
        }
    }

    /**
     * Sends task assignment notification to assigned user.
     * Uses helper methods to support both template-based and ad-hoc tasks.
     */
    private void notifyTaskAssignment(TaskInstance task, String assignedToUser) {
        try {
            // Use helper methods to handle both template-based and ad-hoc tasks
            String taskName = task.getTaskName();
            Boolean isOptional = task.isOptional();

            String subject = String.format("New Task Assigned: '%s'", taskName);

            String message = String.format(
                    "You have been assigned a new task.\n\n" +
                    "Task: %s\n" +
                    "Patient: %s\n" +
                    "Due Date: %s\n" +
                    "Priority: %s\n\n" +
                    "Please log in to the workflow system to view details and start the task.",
                    taskName,
                    task.getWorkflowInstance().getPatient().getId(),
                    task.getDueAt(),
                    Boolean.TRUE.equals(isOptional) ? "Optional" : "Required"
            );

            NotificationRequest request = new NotificationRequest(
                    assignedToUser,
                    "TASK_ASSIGNMENT",
                    subject,
                    message
            );
            request.setTaskInstanceId(task.getId());
            request.setWorkflowInstanceId(task.getWorkflowInstance().getId());
            request.setPatientId(task.getWorkflowInstance().getPatient().getId());

            notificationService.notifyUser(request);
            log.info("Task assignment notification sent to: {}", assignedToUser);

        } catch (Exception e) {
            log.error("Error sending task assignment notification", e);
        }
    }

    /**
     * Sends SLA breach notification.
     * Uses helper methods to support both template-based and ad-hoc tasks.
     */
    private void notifySLABreach(TaskInstance task, String escalatedToUser) {
        try {
            // Use helper methods to handle both template-based and ad-hoc tasks
            String taskName = task.getTaskName();

            String subject = "ALERT: Task SLA Breach";

            String message = String.format(
                    "Critical Alert: Task SLA has been breached!\n\n" +
                    "Task: %s\n" +
                    "Patient: %s\n" +
                    "Due Date: %s\n" +
                    "Current Time: %s\n" +
                    "Assigned To: %s\n\n" +
                    "Immediate action required.",
                    taskName,
                    task.getWorkflowInstance().getPatient().getId(),
                    task.getDueAt(),
                    java.time.LocalDateTime.now(),
                    task.getAssignedTo()
            );

            NotificationRequest request = new NotificationRequest(
                    escalatedToUser,
                    "SLA_BREACH",
                    subject,
                    message
            );
            request.setTaskInstanceId(task.getId());
            request.setWorkflowInstanceId(task.getWorkflowInstance().getId());
            request.setPatientId(task.getWorkflowInstance().getPatient().getId());

            notificationService.notifyUser(request);
            log.warn("SLA breach notification sent to: {}", escalatedToUser);

        } catch (Exception e) {
            log.error("Error sending SLA breach notification", e);
        }
    }
}
