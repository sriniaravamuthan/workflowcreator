package com.hmis.workflow.kafka.consumer;

import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.enums.WorkflowStatus;
import com.hmis.workflow.domain.event.WorkflowEvent;
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
 * Kafka event consumer for workflow lifecycle events.
 * Automatically processes workflow state changes and manages overall workflow execution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventConsumer {

    private final WorkflowInstanceService workflowInstanceService;

    /**
     * Listens for workflow events and processes them
     * Manages workflow lifecycle and coordinates task transitions
     */
    @KafkaListener(topics = "workflow-state-events", groupId = "workflow-engine-state-consumer")
    public void handleWorkflowEvent(
            @Payload WorkflowEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received workflow event: {} for workflow: {} [topic: {}, partition: {}, offset: {}]",
                event.getEventType(), event.getWorkflowInstanceId(), topic, partition, offset);

        try {
            switch (event.getEventType()) {
                case "WORKFLOW_STARTED":
                    handleWorkflowStarted(event);
                    break;
                case "WORKFLOW_COMPLETED":
                    handleWorkflowCompleted(event);
                    break;
                case "WORKFLOW_FAILED":
                    handleWorkflowFailed(event);
                    break;
                case "WORKFLOW_ESCALATED":
                    handleWorkflowEscalated(event);
                    break;
                default:
                    log.warn("Unknown workflow event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing workflow event: {}", event.getEventId(), e);
            // In production, send to dead-letter queue for retry
        }
    }

    /**
     * Handles workflow start event
     * Initializes workflow tracking and starts first task(s)
     */
    private void handleWorkflowStarted(WorkflowEvent event) {
        log.info("Processing WORKFLOW_STARTED event for workflow: {} for patient: {}",
                event.getWorkflowInstanceId(), event.getPatientId());

        try {
            UUID workflowInstanceId = UUID.fromString(event.getWorkflowInstanceId());
            WorkflowInstance workflow = workflowInstanceService.getWorkflowInstance(workflowInstanceId);

            if (workflow == null) {
                log.warn("Workflow instance not found: {}", workflowInstanceId);
                return;
            }

            if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
                log.warn("Workflow {} is in unexpected state: {} for start event",
                        workflowInstanceId, workflow.getStatus());
                return;
            }

            log.info("Workflow {} started successfully for patient: {}. Progress: {}%",
                    workflowInstanceId, event.getPatientId(), event.getProgressPercentage());

            // In production: Send notifications to care team, update dashboards, initiate monitoring

        } catch (Exception e) {
            log.error("Error handling workflow started event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handles workflow completion event
     * Finalizes workflow and triggers post-completion actions
     */
    private void handleWorkflowCompleted(WorkflowEvent event) {
        log.info("Processing WORKFLOW_COMPLETED event for workflow: {}", event.getWorkflowInstanceId());

        try {
            UUID workflowInstanceId = UUID.fromString(event.getWorkflowInstanceId());
            WorkflowInstance workflow = workflowInstanceService.getWorkflowInstance(workflowInstanceId);

            if (workflow == null) {
                log.warn("Workflow instance not found: {}", workflowInstanceId);
                return;
            }

            if (workflow.getStatus() != WorkflowStatus.COMPLETED) {
                log.warn("Workflow {} is in unexpected state: {} for completion event",
                        workflowInstanceId, workflow.getStatus());
                return;
            }

            log.info("Workflow {} completed successfully for patient: {}",
                    workflowInstanceId, workflow.getPatient().getId());

            // Calculate total duration
            long durationSeconds = java.time.Duration.between(
                    workflow.getStartedAt(),
                    workflow.getCompletedAt()
            ).getSeconds();

            log.info("Workflow {} execution time: {}m {}s",
                    workflowInstanceId,
                    durationSeconds / 60,
                    durationSeconds % 60);

            // Archive workflow if configured
            archiveCompletedWorkflow(workflowInstanceId);

            // Trigger post-completion actions
            handlePostCompletionActions(workflow);

            // In production:
            // - Send completion notifications to patient/care team
            // - Archive related orders and documents
            // - Trigger post-care follow-ups if needed
            // - Update patient status records

        } catch (Exception e) {
            log.error("Error handling workflow completed event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handles workflow failure event
     * Initiates failure recovery and escalation procedures
     */
    private void handleWorkflowFailed(WorkflowEvent event) {
        log.info("Processing WORKFLOW_FAILED event for workflow: {}", event.getWorkflowInstanceId());

        try {
            UUID workflowInstanceId = UUID.fromString(event.getWorkflowInstanceId());
            WorkflowInstance workflow = workflowInstanceService.getWorkflowInstance(workflowInstanceId);

            if (workflow == null) {
                log.warn("Workflow instance not found: {}", workflowInstanceId);
                return;
            }

            if (workflow.getStatus() != WorkflowStatus.FAILED) {
                log.warn("Workflow {} is in unexpected state: {} for failure event",
                        workflowInstanceId, workflow.getStatus());
                return;
            }

            log.error("Workflow {} failed for patient: {}. Progress at failure: {}%",
                    workflowInstanceId, workflow.getPatient().getId(), event.getProgressPercentage());

            // Automatic escalation on failure
            escalateFailedWorkflow(workflowInstanceId, "Workflow execution failed - escalating for review");

            // In production:
            // - Send critical alerts to care team
            // - Trigger incident management procedures
            // - Create support tickets for manual intervention
            // - Document failure reason for audit trail
            // - Potentially trigger alternative care pathways

        } catch (Exception e) {
            log.error("Error handling workflow failed event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handles workflow escalation event
     * Manages escalation workflows and notifications
     */
    private void handleWorkflowEscalated(WorkflowEvent event) {
        log.info("Processing WORKFLOW_ESCALATED event for workflow: {}", event.getWorkflowInstanceId());

        try {
            UUID workflowInstanceId = UUID.fromString(event.getWorkflowInstanceId());
            WorkflowInstance workflow = workflowInstanceService.getWorkflowInstance(workflowInstanceId);

            if (workflow == null) {
                log.warn("Workflow instance not found: {}", workflowInstanceId);
                return;
            }

            if (!workflow.isEscalated()) {
                log.warn("Workflow {} is marked as not escalated in escalation event", workflowInstanceId);
                return;
            }

            log.warn("Workflow {} escalated for patient: {}. Escalation reason: {}",
                    workflowInstanceId, workflow.getPatient().getId(), workflow.getEscalationReason());

            // Log escalation details
            logEscalationDetails(workflow);

            // In production:
            // - Notify senior clinicians/managers
            // - Create escalation task assignments
            // - Update on-call support systems
            // - Send urgent notifications

        } catch (Exception e) {
            log.error("Error handling workflow escalated event: {}", event.getEventId(), e);
        }
    }

    /**
     * Escalates a failed workflow automatically
     */
    private void escalateFailedWorkflow(UUID workflowInstanceId, String reason) {
        try {
            log.info("Escalating failed workflow: {} - Reason: {}", workflowInstanceId, reason);
            workflowInstanceService.escalateWorkflow(workflowInstanceId, reason);
            log.info("Workflow {} successfully escalated", workflowInstanceId);
        } catch (Exception e) {
            log.error("Failed to escalate workflow {}: {}", workflowInstanceId, e.getMessage());
        }
    }

    /**
     * Archives a completed workflow for long-term retention
     */
    private void archiveCompletedWorkflow(UUID workflowInstanceId) {
        try {
            // In production: Move workflow to archive storage/database
            // This is typically done after retention period or for compliance
            log.info("Archiving completed workflow: {}", workflowInstanceId);
            // Archive logic would go here
        } catch (Exception e) {
            log.error("Failed to archive workflow {}: {}", workflowInstanceId, e.getMessage());
        }
    }

    /**
     * Handles post-completion actions
     */
    private void handlePostCompletionActions(WorkflowInstance workflow) {
        try {
            log.info("Executing post-completion actions for workflow: {}", workflow.getId());

            // Check if follow-up workflows are needed
            boolean needsFollowUp = checkNeedsFollowUp(workflow);

            if (needsFollowUp) {
                log.info("Patient {} needs follow-up workflow", workflow.getPatient().getId());
                // In production: Create follow-up workflow instance
            }

            // Generate workflow completion report
            generateCompletionReport(workflow);

        } catch (Exception e) {
            log.error("Error executing post-completion actions for workflow {}: {}",
                    workflow.getId(), e.getMessage());
        }
    }

    /**
     * Checks if patient needs follow-up care after workflow completion
     */
    private boolean checkNeedsFollowUp(WorkflowInstance workflow) {
        // This would check workflow template configuration or patient-specific rules
        // For now, return false as default
        return false;
    }

    /**
     * Generates workflow completion report
     */
    private void generateCompletionReport(WorkflowInstance workflow) {
        try {
            log.info("Generating completion report for workflow: {}", workflow.getId());

            // Build report details
            StringBuilder report = new StringBuilder();
            report.append("Workflow Completion Report\n");
            report.append("===========================\n");
            report.append("Workflow ID: ").append(workflow.getWorkflowInstanceId()).append("\n");
            report.append("Patient: ").append(workflow.getPatient().getId()).append("\n");
            report.append("Template: ").append(workflow.getWorkflowTemplate().getName()).append("\n");
            report.append("Started: ").append(workflow.getStartedAt()).append("\n");
            report.append("Completed: ").append(workflow.getCompletedAt()).append("\n");

            log.info("Workflow completion report:\n{}", report.toString());

            // In production: Save report to document storage, email to clinicians

        } catch (Exception e) {
            log.error("Error generating completion report for workflow {}: {}",
                    workflow.getId(), e.getMessage());
        }
    }

    /**
     * Logs detailed escalation information for audit trail
     */
    private void logEscalationDetails(WorkflowInstance workflow) {
        try {
            log.info("ESCALATION DETAILS");
            log.info("==================");
            log.info("Workflow ID: {}", workflow.getId());
            log.info("Patient ID: {}", workflow.getPatient().getId());
            log.info("Template: {}", workflow.getWorkflowTemplate().getName());
            log.info("Status: {}", workflow.getStatus());
            log.info("Is Escalated: {}", workflow.isEscalated());
            log.info("Escalation Reason: {}", workflow.getEscalationReason());
            log.info("Escalated At: {}", workflow.getEscalatedAt());

        } catch (Exception e) {
            log.error("Error logging escalation details: {}", e.getMessage());
        }
    }
}
