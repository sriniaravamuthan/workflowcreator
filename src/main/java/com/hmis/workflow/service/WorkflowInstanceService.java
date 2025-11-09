package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.Patient;
import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.entity.WorkflowTemplate;
import com.hmis.workflow.domain.enums.TaskStatus;
import com.hmis.workflow.domain.enums.WorkflowStatus;
import com.hmis.workflow.repository.PatientRepository;
import com.hmis.workflow.repository.TaskInstanceRepository;
import com.hmis.workflow.repository.WorkflowInstanceRepository;
import com.hmis.workflow.repository.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing workflow instances
 * Handles creation, execution, and monitoring of patient workflows
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowInstanceService {

    private final WorkflowInstanceRepository workflowRepository;
    private final WorkflowTemplateRepository templateRepository;
    private final PatientRepository patientRepository;
    private final TaskInstanceRepository taskRepository;
    private final TaskInstanceService taskService;

    /**
     * Create a new workflow instance for a patient
     */
    public WorkflowInstance createWorkflowInstance(UUID patientId, UUID templateId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        WorkflowTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        if (!template.isPublished()) {
            throw new IllegalStateException("Cannot start workflow with non-published template");
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowInstanceId(UUID.randomUUID().toString());
        instance.setPatient(patient);
        instance.setTemplate(template);
        instance.setStatus(WorkflowStatus.ACTIVE);
        instance.setStartedAt(LocalDateTime.now());

        log.info("Created workflow instance {} for patient {}", instance.getWorkflowInstanceId(), patientId);

        // Create task instances from template tasks
        template.getTasks().forEach(taskDef -> {
            TaskInstance taskInstance = new TaskInstance();
            taskInstance.setTaskInstanceId(UUID.randomUUID().toString());
            taskInstance.setWorkflowInstance(instance);
            taskInstance.setTaskDefinition(taskDef);
            taskInstance.setStatus(TaskStatus.PENDING);
            taskInstance.setAssignedTo(taskDef.getAssignTo());
            taskInstance.setRequiredRole(taskDef.getAssignTo());
            taskInstance.setMaxRetries(3);

            // Set SLA
            if (taskDef.getEstimatedDurationMinutes() > 0) {
                taskInstance.setSlaMinutes(taskDef.getEstimatedDurationMinutes());
                taskInstance.setDueAt(LocalDateTime.now().plusMinutes(taskDef.getEstimatedDurationMinutes()));
            }

            instance.getTaskInstances().add(taskInstance);
        });

        return workflowRepository.save(instance);
    }

    /**
     * Get workflow instance by ID
     */
    public WorkflowInstance getWorkflowInstance(UUID instanceId) {
        return workflowRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + instanceId));
    }

    /**
     * Get workflow instance by instance ID
     */
    public WorkflowInstance getWorkflowByInstanceId(String instanceId) {
        return workflowRepository.findByWorkflowInstanceId(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + instanceId));
    }

    /**
     * Get all active workflows for a patient
     */
    public List<WorkflowInstance> getActiveWorkflowsForPatient(UUID patientId) {
        return workflowRepository.findActiveByPatientId(patientId);
    }

    /**
     * Get all workflows for a patient
     */
    public List<WorkflowInstance> getWorkflowsByPatient(UUID patientId) {
        return workflowRepository.findByPatientId(patientId);
    }

    /**
     * Get all escalated workflows
     */
    public List<WorkflowInstance> getEscalatedWorkflows() {
        return workflowRepository.findEscalatedWorkflows();
    }

    /**
     * Pause workflow
     */
    public WorkflowInstance pauseWorkflow(UUID instanceId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);

        if (instance.getStatus() != WorkflowStatus.ACTIVE) {
            throw new IllegalStateException("Cannot pause non-active workflow");
        }

        instance.setStatus(WorkflowStatus.PAUSED);
        log.info("Paused workflow: {}", instanceId);
        return workflowRepository.save(instance);
    }

    /**
     * Resume workflow
     */
    public WorkflowInstance resumeWorkflow(UUID instanceId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);

        if (instance.getStatus() != WorkflowStatus.PAUSED) {
            throw new IllegalStateException("Cannot resume non-paused workflow");
        }

        instance.setStatus(WorkflowStatus.ACTIVE);
        log.info("Resumed workflow: {}", instanceId);
        return workflowRepository.save(instance);
    }

    /**
     * Cancel workflow
     */
    public WorkflowInstance cancelWorkflow(UUID instanceId, String reason) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        instance.setStatus(WorkflowStatus.CANCELLED);
        instance.setNotes(reason);
        instance.setCompletedAt(LocalDateTime.now());

        // Cancel all pending/in-progress tasks
        instance.getTaskInstances().stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.FAILED)
                .forEach(t -> {
                    t.setStatus(TaskStatus.FAILED);
                    taskRepository.save(t);
                });

        log.info("Cancelled workflow: {} - Reason: {}", instanceId, reason);
        return workflowRepository.save(instance);
    }

    /**
     * Complete workflow
     */
    public WorkflowInstance completeWorkflow(UUID instanceId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);

        // Check if all required tasks are completed
        boolean allRequired = instance.getTaskInstances().stream()
                .filter(t -> !t.getTaskDefinition().getIsOptional())
                .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

        if (!allRequired) {
            throw new IllegalStateException("Cannot complete workflow with pending required tasks");
        }

        instance.setStatus(WorkflowStatus.COMPLETED);
        instance.setCompletedAt(LocalDateTime.now());
        log.info("Completed workflow: {}", instanceId);
        return workflowRepository.save(instance);
    }

    /**
     * Escalate workflow
     */
    public WorkflowInstance escalateWorkflow(UUID instanceId, String reason) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);
        instance.setIsEscalated(true);
        instance.setEscalationReason(reason);
        log.info("Escalated workflow: {} - Reason: {}", instanceId, reason);
        return workflowRepository.save(instance);
    }

    /**
     * Check and update workflow status based on task statuses
     */
    public WorkflowInstance updateWorkflowStatus(UUID instanceId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);

        long completedTasks = instance.getTaskInstances().stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();

        long failedTasks = instance.getTaskInstances().stream()
                .filter(t -> t.getStatus() == TaskStatus.FAILED)
                .count();

        // If any required task failed, mark workflow as failed
        boolean requiredTaskFailed = instance.getTaskInstances().stream()
                .filter(t -> !t.getTaskDefinition().getIsOptional())
                .anyMatch(t -> t.getStatus() == TaskStatus.FAILED);

        if (requiredTaskFailed) {
            instance.setStatus(WorkflowStatus.FAILED);
            log.warn("Workflow marked as FAILED due to required task failure: {}", instanceId);
            return workflowRepository.save(instance);
        }

        // Check if all tasks are done
        if (completedTasks + failedTasks == instance.getTaskInstances().size()) {
            return completeWorkflow(instanceId);
        }

        return instance;
    }
}
