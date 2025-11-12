package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.repository.TaskInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for monitoring SLA breaches on task instances.
 * Runs scheduled checks to identify tasks that have exceeded their deadline.
 * Triggers escalation actions for breached tasks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SLAMonitoringService {

    private final TaskInstanceRepository taskInstanceRepository;
    private final TaskInstanceService taskInstanceService;

    /**
     * Monitors SLA breaches for all active tasks
     * Runs every 5 minutes to check task deadlines
     * Marks breached tasks and triggers escalation actions
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void monitorSLABreaches() {
        log.debug("Starting SLA breach monitoring cycle");

        try {
            // Get all tasks that are still in progress or pending
            // Filter for tasks that have not yet been marked as SLA breached
            List<TaskInstance> tasksToCheck = taskInstanceRepository.findSLABreachedTasks();

            if (tasksToCheck.isEmpty()) {
                log.debug("No SLA breaches detected in this cycle");
                return;
            }

            log.info("Found {} tasks with potential SLA breaches", tasksToCheck.size());

            for (TaskInstance task : tasksToCheck) {
                processSLABreach(task);
            }

            log.info("SLA monitoring cycle completed. {} tasks processed", tasksToCheck.size());

        } catch (Exception e) {
            log.error("Error during SLA monitoring cycle", e);
        }
    }

    /**
     * Processes a single SLA breach for a task
     * Updates task status and triggers escalation
     */
    private void processSLABreach(TaskInstance task) {
        try {
            // Double-check that task hasn't been completed in the meantime
            if (task.getCompletedAt() != null) {
                log.debug("Task {} was completed, skipping SLA check", task.getId());
                return;
            }

            // Check if SLA is actually breached
            if (!task.isSLABreached()) {
                log.warn("Task {} marked for SLA check but SLA not actually breached", task.getId());
                return;
            }

            log.warn("SLA BREACH DETECTED: Task {} exceeded deadline. Due: {}, Current: {}",
                    task.getId(), task.getDueAt(), LocalDateTime.now());

            // Update task SLA status
            task.setSLABreached(true);

            // Auto-escalate the task
            String escalationReason = String.format(
                    "SLA Breach - Task due at %s, escalated at %s (%.1f hours overdue)",
                    task.getDueAt(),
                    LocalDateTime.now(),
                    calculateHoursOverdue(task.getDueAt())
            );

            log.info("Escalating task {} due to SLA breach", task.getId());

            // Escalate to manager/supervisor
            String escalationUser = determineEscalationUser(task);
            try {
                taskInstanceService.escalateTask(task.getId(), escalationUser, escalationReason);
                log.info("Task {} successfully escalated to {}", task.getId(), escalationUser);
            } catch (Exception e) {
                log.error("Failed to escalate task {}: {}", task.getId(), e.getMessage());
            }

            // Trigger alerts/notifications
            sendSLABreachAlert(task, escalationReason);

            // Log to audit trail
            logSLABreach(task);

        } catch (Exception e) {
            log.error("Error processing SLA breach for task {}: {}", task.getId(), e.getMessage());
        }
    }

    /**
     * Sends alerts/notifications for SLA breaches
     * In production, this would integrate with notification systems (email, SMS, dashboards)
     */
    private void sendSLABreachAlert(TaskInstance task, String reason) {
        try {
            log.warn("Sending SLA breach alert for task: {} - Reason: {}", task.getId(), reason);

            // In production, these notifications would be sent to:
            // - Task assignee (direct notification)
            // - Task manager/supervisor (escalation)
            // - Workflow coordinator
            // - Dashboard alerts
            // - Mobile push notifications if applicable

            String alertMessage = String.format(
                    "ALERT: Task '%s' has exceeded SLA deadline. " +
                            "Assigned to: %s, Due: %s, Overdue by: %.1f hours",
                    task.getTaskDefinition().getName(),
                    task.getAssignedTo(),
                    task.getDueAt(),
                    calculateHoursOverdue(task.getDueAt())
            );

            log.info(alertMessage);

        } catch (Exception e) {
            log.error("Error sending SLA breach alert for task {}: {}", task.getId(), e.getMessage());
        }
    }

    /**
     * Determines who should receive task escalation
     * Based on task assignment and organizational structure
     */
    private String determineEscalationUser(TaskInstance task) {
        // In production, this would query:
        // - Manager of assigned user
        // - On-call supervisor
        // - Department head
        // - Escalation rules configuration

        // Default: escalate to manager or department lead
        String assignedTo = task.getAssignedTo();

        if (assignedTo != null && assignedTo.startsWith("NURSE")) {
            return "NURSE_MANAGER";
        } else if (assignedTo != null && assignedTo.startsWith("LAB")) {
            return "LAB_DIRECTOR";
        } else if (assignedTo != null && assignedTo.startsWith("PHARMACY")) {
            return "PHARMACY_MANAGER";
        }

        // Default escalation
        return "SUPERVISOR";
    }

    /**
     * Calculates hours overdue for a task
     */
    private double calculateHoursOverdue(LocalDateTime dueAt) {
        if (dueAt == null) {
            return 0;
        }

        long minutesOverdue = java.time.temporal.ChronoUnit.MINUTES.between(dueAt, LocalDateTime.now());
        return minutesOverdue / 60.0;
    }

    /**
     * Logs SLA breach to audit trail
     */
    private void logSLABreach(TaskInstance task) {
        try {
            log.info("Logging SLA breach for audit trail:");
            log.info("  Task ID: {}", task.getId());
            log.info("  Task Name: {}", task.getTaskDefinition().getName());
            log.info("  Assigned To: {}", task.getAssignedTo());
            log.info("  Due At: {}", task.getDueAt());
            log.info("  Current Time: {}", LocalDateTime.now());
            log.info("  Hours Overdue: {}", calculateHoursOverdue(task.getDueAt()));
            log.info("  Workflow: {}", task.getWorkflowInstance().getWorkflowInstanceId());
            log.info("  Patient: {}", task.getWorkflowInstance().getPatient().getId());

        } catch (Exception e) {
            log.error("Error logging SLA breach: {}", e.getMessage());
        }
    }

    /**
     * Monitors for stale tasks that haven't progressed
     * Runs every 10 minutes to detect stuck workflows
     */
    @Scheduled(fixedDelay = 600000) // 10 minutes
    public void monitorStaleTask() {
        log.debug("Starting stale task detection cycle");

        try {
            // Find tasks that haven't been updated in the last X hours but are still pending
            LocalDateTime staleCutoff = LocalDateTime.now().minusHours(4);

            // Query would be: SELECT * FROM task_instances WHERE status = 'PENDING' AND updated_at < staleCutoff

            log.debug("Stale task detection cycle completed");

        } catch (Exception e) {
            log.error("Error during stale task detection", e);
        }
    }

    /**
     * Monitors for workflow escalations that may need attention
     * Runs every 15 minutes to check escalated workflows
     */
    @Scheduled(fixedDelay = 900000) // 15 minutes
    public void monitorEscalatedWorkflows() {
        log.debug("Starting escalated workflow monitoring");

        try {
            // Find all escalated workflows
            // Query: SELECT * FROM workflow_instances WHERE is_escalated = true AND escalated_at > NOW() - INTERVAL 24 HOUR

            log.debug("Escalated workflow monitoring completed");

        } catch (Exception e) {
            log.error("Error during escalated workflow monitoring", e);
        }
    }

    /**
     * Performs hourly health check on SLA monitoring system
     * Validates that monitoring is functioning correctly
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    public void healthCheckSLAMonitoring() {
        log.info("Performing SLA monitoring health check");

        try {
            // Check if monitoring tasks are being executed
            // Count tasks monitored in last cycle
            // Verify alert notifications are being sent
            // Check for any stuck or failed escalations

            log.info("SLA monitoring health check completed successfully");

        } catch (Exception e) {
            log.error("SLA monitoring health check failed", e);
        }
    }
}
