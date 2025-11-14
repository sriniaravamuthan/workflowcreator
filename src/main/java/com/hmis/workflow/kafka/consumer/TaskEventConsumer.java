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

import java.util.UUID;

/**
 * Kafka event consumer for task lifecycle events.
 * Automatically processes task events and triggers next task progression.
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
     * Handles task completion event
     * Triggers propagation to the next task in the workflow
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

            // Propagate to next task if defined in task definition
            if (completedTask.getTaskDefinition() != null &&
                    completedTask.getTaskDefinition().getNextTaskId() != null) {

                UUID nextTaskId = completedTask.getTaskDefinition().getNextTaskId();
                TaskInstance nextTask = taskInstanceService.getTaskInstance(nextTaskId);

                if (nextTask != null) {
                    log.info("Triggering next task: {} after completion of task: {}",
                            nextTaskId, taskInstanceId);
                    // Next task is now ready to be started
                    // In a real system, this might trigger auto-assignment or notifications
                }
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
     * Sends task escalation notification to escalated user
     */
    private void notifyTaskEscalation(TaskInstance task, String escalatedToUser) {
        try {
            String subject = String.format("URGENT: Task '%s' Escalated - SLA Breach",
                    task.getTaskDefinition().getName());

            String message = String.format(
                    "Task '%s' has been escalated due to SLA breach.\n\n" +
                    "Patient: %s\n" +
                    "Due Date: %s\n" +
                    "Escalated By: System\n" +
                    "Priority: URGENT\n\n" +
                    "Please attend to this task immediately.",
                    task.getTaskDefinition().getName(),
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
     * Sends task assignment notification to assigned user
     */
    private void notifyTaskAssignment(TaskInstance task, String assignedToUser) {
        try {
            String subject = String.format("New Task Assigned: '%s'",
                    task.getTaskDefinition().getName());

            String message = String.format(
                    "You have been assigned a new task.\n\n" +
                    "Task: %s\n" +
                    "Patient: %s\n" +
                    "Due Date: %s\n" +
                    "Priority: %s\n\n" +
                    "Please log in to the workflow system to view details and start the task.",
                    task.getTaskDefinition().getName(),
                    task.getWorkflowInstance().getPatient().getId(),
                    task.getDueAt(),
                    task.getTaskDefinition().getPriority() != null ?
                            task.getTaskDefinition().getPriority() : "NORMAL"
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
     * Sends SLA breach notification
     */
    private void notifySLABreach(TaskInstance task, String escalatedToUser) {
        try {
            String subject = "ALERT: Task SLA Breach";

            String message = String.format(
                    "Critical Alert: Task SLA has been breached!\n\n" +
                    "Task: %s\n" +
                    "Patient: %s\n" +
                    "Due Date: %s\n" +
                    "Current Time: %s\n" +
                    "Assigned To: %s\n\n" +
                    "Immediate action required.",
                    task.getTaskDefinition().getName(),
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
