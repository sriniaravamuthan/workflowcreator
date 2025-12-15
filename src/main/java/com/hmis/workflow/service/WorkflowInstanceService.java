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
    private final NotificationService notificationService;

    /**
     * Create a new workflow instance for a patient (backward compatible).
     * Use createWorkflowInstance with encounter/visit IDs for full clinical context.
     */
    public WorkflowInstance createWorkflowInstance(UUID patientId, UUID templateId) {
        return createWorkflowInstance(patientId, templateId, null, null);
    }

    /**
     * Create a new workflow instance for a patient with full clinical context.
     *
     * Context Parameters:
     * - patientId: The patient this workflow is for (required)
     * - templateId: The workflow template to instantiate (required)
     * - encounterId: The clinical encounter (e.g., ER visit, admission) - optional but recommended
     * - visitId: The ADT visit tracking ID - optional
     *
     * Uniqueness Validation:
     * - Prevents duplicate active workflows for the same patient + encounter + template combination
     * - Completed/cancelled/failed workflows do not block new workflow creation
     *
     * Task Status Assignment:
     * - Entry tasks (no predecessors): Set to PENDING - can be started immediately
     * - Tasks with predecessors: Set to BLOCKED - will become PENDING when all predecessors complete
     *
     * SLA Calculation:
     * - Entry tasks: SLA starts from workflow creation time
     * - Blocked tasks: SLA will be calculated when they become PENDING (predecessors complete)
     *
     * Notifications:
     * - Entry task assignees are notified immediately upon workflow creation
     *
     * @param patientId The patient ID (required)
     * @param templateId The workflow template ID (required)
     * @param encounterId The encounter/admission ID (optional but recommended)
     * @param visitId The ADT visit ID (optional)
     * @return The created workflow instance
     * @throws IllegalArgumentException if patient or template not found
     * @throws IllegalStateException if template not published or duplicate workflow exists
     */
    public WorkflowInstance createWorkflowInstance(UUID patientId, UUID templateId,
                                                    String encounterId, String visitId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        WorkflowTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        if (!template.isPublished()) {
            throw new IllegalStateException("Cannot start workflow with non-published template");
        }

        // Check for duplicate active workflows for the same patient + encounter + template
        List<WorkflowInstance> existingWorkflows = workflowRepository
                .findActiveByPatientEncounterAndTemplate(patientId, encounterId, templateId);

        if (!existingWorkflows.isEmpty()) {
            WorkflowInstance existing = existingWorkflows.get(0);
            throw new IllegalStateException(String.format(
                    "An active workflow already exists for this patient/encounter/template combination. " +
                    "Existing workflow ID: %s, Status: %s. " +
                    "Complete or cancel the existing workflow before creating a new one.",
                    existing.getWorkflowInstanceId(),
                    existing.getStatus()));
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowInstanceId(UUID.randomUUID().toString());
        instance.setPatient(patient);
        instance.setTemplate(template);
        instance.setEncounterId(encounterId);
        instance.setVisitId(visitId);
        instance.setStatus(WorkflowStatus.ACTIVE);
        instance.setStartedAt(LocalDateTime.now());

        log.info("Created workflow instance {} for patient {} (encounter: {}, visit: {})",
                instance.getWorkflowInstanceId(), patientId, encounterId, visitId);

        // Create task instances from template tasks
        template.getTasks().forEach(taskDef -> {
            TaskInstance taskInstance = new TaskInstance();
            taskInstance.setTaskInstanceId(UUID.randomUUID().toString());
            taskInstance.setWorkflowInstance(instance);
            taskInstance.setTaskDefinition(taskDef);
            taskInstance.setAssignedTo(taskDef.getAssignTo());
            taskInstance.setRequiredRole(taskDef.getAssignTo());
            taskInstance.setMaxRetries(3);

            // Determine initial status based on predecessors
            if (taskDef.isEntryTask()) {
                // Entry task (no predecessors) - can start immediately
                taskInstance.setStatus(TaskStatus.PENDING);
                log.debug("Task {} is an entry task, setting to PENDING", taskDef.getName());

                // Set SLA for entry tasks from now
                if (taskDef.getEstimatedDurationMinutes() > 0) {
                    taskInstance.setSlaMinutes(taskDef.getEstimatedDurationMinutes());
                    taskInstance.setDueAt(LocalDateTime.now().plusMinutes(taskDef.getEstimatedDurationMinutes()));
                }
            } else {
                // Task has predecessors - must wait for them to complete
                taskInstance.setStatus(TaskStatus.BLOCKED);
                log.debug("Task {} has predecessors {}, setting to BLOCKED",
                        taskDef.getName(), taskDef.getPredecessorTaskIds());

                // SLA will be set when task becomes PENDING (predecessors complete)
                if (taskDef.getEstimatedDurationMinutes() > 0) {
                    taskInstance.setSlaMinutes(taskDef.getEstimatedDurationMinutes());
                    // dueAt will be calculated when task is unblocked
                }
            }

            instance.getTaskInstances().add(taskInstance);
        });

        WorkflowInstance savedInstance = workflowRepository.save(instance);

        // Notify assignees of entry tasks (tasks that can start immediately)
        notifyEntryTaskAssignees(savedInstance);

        return savedInstance;
    }

    /**
     * Notify assignees of all entry tasks in a newly created workflow.
     * Entry tasks are tasks with no predecessors that can start immediately.
     *
     * @param instance The newly created workflow instance
     */
    private void notifyEntryTaskAssignees(WorkflowInstance instance) {
        instance.getTaskInstances().stream()
                .filter(task -> task.getTaskDefinition().isEntryTask())
                .filter(task -> task.getAssignedTo() != null && !task.getAssignedTo().isEmpty())
                .forEach(task -> {
                    try {
                        notifyTaskAssignment(task, instance.getPatient());
                        log.info("Sent task assignment notification to {} for entry task {}",
                                task.getAssignedTo(), task.getTaskDefinition().getName());
                    } catch (Exception e) {
                        log.error("Failed to send notification for entry task {}: {}",
                                task.getTaskDefinition().getName(), e.getMessage());
                    }
                });
    }

    /**
     * Send task assignment notification to the assigned user.
     *
     * @param task The task instance being assigned
     * @param patient The patient associated with the workflow
     */
    private void notifyTaskAssignment(TaskInstance task, Patient patient) {
        String subject = String.format("New Task Assigned: %s", task.getTaskDefinition().getName());

        String message = String.format(
                "You have been assigned a new task.\n\n" +
                "Task: %s\n" +
                "Description: %s\n" +
                "Patient: %s %s\n" +
                "Due Date: %s\n" +
                "Priority: %s\n\n" +
                "Please log in to the workflow system to view details and start the task.",
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

        long skippedTasks = instance.getTaskInstances().stream()
                .filter(t -> t.getStatus() == TaskStatus.SKIPPED)
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

        // Check if all tasks are done (completed, failed, or skipped)
        long totalDone = completedTasks + failedTasks + skippedTasks;
        if (totalDone == instance.getTaskInstances().size()) {
            return completeWorkflow(instanceId);
        }

        return instance;
    }

    /**
     * Unblock tasks whose predecessors have all completed.
     * Called after a task is completed to check if any blocked tasks can now proceed.
     *
     * @param instanceId The workflow instance ID
     * @param completedTaskDefId The task definition ID of the just-completed task
     * @return List of task instances that were unblocked
     */
    public List<TaskInstance> unblockReadyTasks(UUID instanceId, String completedTaskDefId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);

        // Get all completed task definition IDs
        java.util.Set<String> completedTaskDefIds = instance.getTaskInstances().stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.SKIPPED)
                .map(t -> t.getTaskDefinition().getId().toString())
                .collect(java.util.stream.Collectors.toSet());

        List<TaskInstance> unblockedTasks = new java.util.ArrayList<>();

        // Check each blocked task to see if all its predecessors are completed
        instance.getTaskInstances().stream()
                .filter(t -> t.getStatus() == TaskStatus.BLOCKED)
                .forEach(blockedTask -> {
                    List<String> predecessors = blockedTask.getTaskDefinition().getPredecessorTaskIdList();

                    // Check if all predecessors are completed
                    boolean allPredecessorsComplete = predecessors.stream()
                            .allMatch(completedTaskDefIds::contains);

                    if (allPredecessorsComplete) {
                        // Unblock this task
                        blockedTask.setStatus(TaskStatus.PENDING);

                        // Set SLA now that task is unblocked
                        if (blockedTask.getSlaMinutes() != null && blockedTask.getSlaMinutes() > 0) {
                            blockedTask.setDueAt(LocalDateTime.now().plusMinutes(blockedTask.getSlaMinutes()));
                        }

                        taskRepository.save(blockedTask);
                        unblockedTasks.add(blockedTask);

                        log.info("Unblocked task {} (all predecessors completed)",
                                blockedTask.getTaskDefinition().getName());
                    }
                });

        return unblockedTasks;
    }

    /**
     * Check if a specific task can be unblocked (all predecessors completed).
     *
     * @param taskInstance The task instance to check
     * @return true if the task can be unblocked (all predecessors are completed)
     */
    public boolean canUnblockTask(TaskInstance taskInstance) {
        if (taskInstance.getStatus() != TaskStatus.BLOCKED) {
            return false;
        }

        WorkflowInstance instance = taskInstance.getWorkflowInstance();
        List<String> predecessors = taskInstance.getTaskDefinition().getPredecessorTaskIdList();

        if (predecessors.isEmpty()) {
            // No predecessors - should not be blocked, can unblock
            return true;
        }

        // Get all completed task definition IDs
        java.util.Set<String> completedTaskDefIds = instance.getTaskInstances().stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.SKIPPED)
                .map(t -> t.getTaskDefinition().getId().toString())
                .collect(java.util.stream.Collectors.toSet());

        // Check if all predecessors are completed
        return predecessors.stream().allMatch(completedTaskDefIds::contains);
    }

    /**
     * Get all entry tasks (tasks with no predecessors) for a workflow instance.
     *
     * @param instanceId The workflow instance ID
     * @return List of entry task instances
     */
    public List<TaskInstance> getEntryTasks(UUID instanceId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);

        return instance.getTaskInstances().stream()
                .filter(t -> t.getTaskDefinition().isEntryTask())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get all blocked tasks for a workflow instance.
     *
     * @param instanceId The workflow instance ID
     * @return List of blocked task instances
     */
    public List<TaskInstance> getBlockedTasks(UUID instanceId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);

        return instance.getTaskInstances().stream()
                .filter(t -> t.getStatus() == TaskStatus.BLOCKED)
                .collect(java.util.stream.Collectors.toList());
    }

    // ========================================================================
    // AD-HOC TASK MANAGEMENT
    // ========================================================================

    /**
     * Add an ad-hoc task to a workflow instance.
     *
     * Ad-hoc tasks are dynamically created at runtime by clinicians (e.g., doctor orders
     * "administer saline") that are not part of the original workflow template.
     *
     * Ad-hoc tasks:
     * - Are immediately set to PENDING status (can be started right away)
     * - Are marked as optional by default
     * - Do not have a task definition in the template
     * - Can have their own SLA/due date
     *
     * @param instanceId The workflow instance ID
     * @param taskName The name of the ad-hoc task
     * @param taskDescription Description of the task
     * @param assignTo User to assign the task to
     * @param createdByUser User creating the ad-hoc task
     * @param slaMinutes Optional SLA in minutes (0 or null for no SLA)
     * @return The created ad-hoc task instance
     */
    public TaskInstance addAdhocTask(UUID instanceId, String taskName, String taskDescription,
                                     String assignTo, String createdByUser, Integer slaMinutes) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);

        if (instance.getStatus() != WorkflowStatus.ACTIVE) {
            throw new IllegalStateException("Cannot add ad-hoc task to non-active workflow");
        }

        if (taskName == null || taskName.trim().isEmpty()) {
            throw new IllegalArgumentException("Ad-hoc task name is required");
        }

        // Create the ad-hoc task instance
        TaskInstance adhocTask = new TaskInstance();
        adhocTask.setTaskInstanceId(UUID.randomUUID().toString());
        adhocTask.setWorkflowInstance(instance);
        adhocTask.setTaskDefinition(null); // No template definition
        adhocTask.setIsAdhoc(true);
        adhocTask.setAdhocTaskName(taskName.trim());
        adhocTask.setAdhocTaskDescription(taskDescription);
        adhocTask.setAssignedTo(assignTo);
        adhocTask.setRequiredRole(assignTo);
        adhocTask.setCreatedByUser(createdByUser);
        adhocTask.setStatus(TaskStatus.PENDING); // Ready to start immediately
        adhocTask.setMaxRetries(3);
        adhocTask.setRetryCount(0);

        // Set SLA if provided
        if (slaMinutes != null && slaMinutes > 0) {
            adhocTask.setSlaMinutes(slaMinutes);
            adhocTask.setDueAt(LocalDateTime.now().plusMinutes(slaMinutes));
        }

        instance.getTaskInstances().add(adhocTask);
        TaskInstance savedTask = taskRepository.save(adhocTask);
        workflowRepository.save(instance);

        log.info("Added ad-hoc task '{}' to workflow {} by user {}",
                taskName, instanceId, createdByUser);

        // Notify the assigned user
        if (assignTo != null && !assignTo.isEmpty()) {
            notifyAdhocTaskAssignment(savedTask, instance.getPatient());
        }

        return savedTask;
    }

    /**
     * Send notification for ad-hoc task assignment.
     *
     * @param task The ad-hoc task instance
     * @param patient The patient associated with the workflow
     */
    private void notifyAdhocTaskAssignment(TaskInstance task, Patient patient) {
        try {
            String subject = String.format("New Ad-hoc Task Assigned: %s", task.getAdhocTaskName());

            String message = String.format(
                    "You have been assigned a new ad-hoc task.\n\n" +
                    "Task: %s\n" +
                    "Description: %s\n" +
                    "Patient: %s %s\n" +
                    "Due Date: %s\n" +
                    "Created By: %s\n\n" +
                    "This is a clinician-ordered task that requires immediate attention.\n" +
                    "Please log in to the workflow system to view details and start the task.",
                    task.getAdhocTaskName(),
                    task.getAdhocTaskDescription() != null ? task.getAdhocTaskDescription() : "N/A",
                    patient.getFirstName(),
                    patient.getLastName(),
                    task.getDueAt() != null ? task.getDueAt().toString() : "Not set",
                    task.getCreatedByUser() != null ? task.getCreatedByUser() : "Unknown"
            );

            NotificationRequest request = new NotificationRequest(
                    task.getAssignedTo(),
                    "ADHOC_TASK_ASSIGNMENT",
                    subject,
                    message
            );
            request.setTaskInstanceId(task.getId());
            request.setWorkflowInstanceId(task.getWorkflowInstance().getId());
            request.setPatientId(patient.getId());

            notificationService.notifyUser(request);
            log.info("Sent ad-hoc task assignment notification to {} for task {}",
                    task.getAssignedTo(), task.getAdhocTaskName());

        } catch (Exception e) {
            log.error("Failed to send ad-hoc task assignment notification for task {}: {}",
                    task.getId(), e.getMessage());
        }
    }

    /**
     * Get all ad-hoc tasks for a workflow instance.
     *
     * @param instanceId The workflow instance ID
     * @return List of ad-hoc task instances
     */
    public List<TaskInstance> getAdhocTasks(UUID instanceId) {
        WorkflowInstance instance = getWorkflowInstance(instanceId);

        return instance.getTaskInstances().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsAdhoc()))
                .collect(java.util.stream.Collectors.toList());
    }
}
