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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorkflowInstanceService ad-hoc task functionality.
 *
 * Tests cover:
 * - Creating ad-hoc tasks with various parameters
 * - Validation of required fields
 * - Workflow state validation
 * - SLA assignment
 * - Notification triggering
 * - Retrieving ad-hoc tasks
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowInstanceService - Ad-hoc Task Tests")
class WorkflowInstanceServiceAdhocTaskTest {

    @Mock
    private WorkflowInstanceRepository workflowRepository;

    @Mock
    private WorkflowTemplateRepository templateRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private TaskInstanceRepository taskRepository;

    @Mock
    private TaskInstanceService taskService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WorkflowInstanceService workflowInstanceService;

    @Captor
    private ArgumentCaptor<TaskInstance> taskInstanceCaptor;

    @Captor
    private ArgumentCaptor<NotificationRequest> notificationCaptor;

    private WorkflowInstance activeWorkflow;
    private Patient patient;
    private UUID workflowId;

    @BeforeEach
    void setUp() {
        workflowId = UUID.randomUUID();

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setPatientId("P-12345");

        WorkflowTemplate template = new WorkflowTemplate();
        template.setId(UUID.randomUUID());
        template.setName("Test Template");

        activeWorkflow = new WorkflowInstance();
        activeWorkflow.setId(workflowId);
        activeWorkflow.setWorkflowInstanceId("WF-001");
        activeWorkflow.setStatus(WorkflowStatus.ACTIVE);
        activeWorkflow.setPatient(patient);
        activeWorkflow.setTemplate(template);
        activeWorkflow.setTaskInstances(new ArrayList<>());
    }

    @Nested
    @DisplayName("addAdhocTask - Success Scenarios")
    class AddAdhocTaskSuccessTests {

        @Test
        @DisplayName("Should create ad-hoc task with all parameters")
        void shouldCreateAdhocTaskWithAllParameters() {
            // Given
            String taskName = "Administer Saline";
            String taskDescription = "IV saline solution 500ml over 2 hours";
            String assignTo = "nurse-001";
            String createdByUser = "doctor-smith";
            Integer slaMinutes = 60;

            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            // When
            TaskInstance result = workflowInstanceService.addAdhocTask(
                    workflowId, taskName, taskDescription, assignTo, createdByUser, slaMinutes);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsAdhoc()).isTrue();
            assertThat(result.getAdhocTaskName()).isEqualTo(taskName);
            assertThat(result.getAdhocTaskDescription()).isEqualTo(taskDescription);
            assertThat(result.getAssignedTo()).isEqualTo(assignTo);
            assertThat(result.getCreatedByUser()).isEqualTo(createdByUser);
            assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(result.getSlaMinutes()).isEqualTo(slaMinutes);
            assertThat(result.getDueAt()).isNotNull();
            assertThat(result.getTaskDefinition()).isNull();

            verify(taskRepository).save(any(TaskInstance.class));
            verify(workflowRepository).save(activeWorkflow);
        }

        @Test
        @DisplayName("Should create ad-hoc task without SLA")
        void shouldCreateAdhocTaskWithoutSla() {
            // Given
            String taskName = "Check Vitals";
            String assignTo = "nurse-002";
            String createdByUser = "doctor-jones";

            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            // When
            TaskInstance result = workflowInstanceService.addAdhocTask(
                    workflowId, taskName, null, assignTo, createdByUser, null);

            // Then
            assertThat(result.getSlaMinutes()).isNull();
            assertThat(result.getDueAt()).isNull();
        }

        @Test
        @DisplayName("Should create ad-hoc task with zero SLA (no deadline)")
        void shouldCreateAdhocTaskWithZeroSla() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            // When
            TaskInstance result = workflowInstanceService.addAdhocTask(
                    workflowId, "Task Name", null, "user", "creator", 0);

            // Then
            assertThat(result.getDueAt()).isNull();
        }

        @Test
        @DisplayName("Should create ad-hoc task without assignee")
        void shouldCreateAdhocTaskWithoutAssignee() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            // When
            TaskInstance result = workflowInstanceService.addAdhocTask(
                    workflowId, "Unassigned Task", "Description", null, "creator", 30);

            // Then
            assertThat(result.getAssignedTo()).isNull();
            // No notification should be sent when there's no assignee
            verify(notificationService, never()).notifyUser(any());
        }

        @Test
        @DisplayName("Should trim whitespace from task name")
        void shouldTrimWhitespaceFromTaskName() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            // When
            TaskInstance result = workflowInstanceService.addAdhocTask(
                    workflowId, "  Task With Spaces  ", null, "user", "creator", null);

            // Then
            assertThat(result.getAdhocTaskName()).isEqualTo("Task With Spaces");
        }

        @Test
        @DisplayName("Should add task to workflow's task list")
        void shouldAddTaskToWorkflowTaskList() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            assertThat(activeWorkflow.getTaskInstances()).isEmpty();

            // When
            workflowInstanceService.addAdhocTask(
                    workflowId, "New Task", null, "user", "creator", null);

            // Then
            assertThat(activeWorkflow.getTaskInstances()).hasSize(1);
        }

        @Test
        @DisplayName("Should generate unique task instance ID")
        void shouldGenerateUniqueTaskInstanceId() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            // When
            TaskInstance result = workflowInstanceService.addAdhocTask(
                    workflowId, "Task", null, "user", "creator", null);

            // Then
            assertThat(result.getTaskInstanceId()).isNotNull();
            assertThat(result.getTaskInstanceId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("Should set default retry configuration")
        void shouldSetDefaultRetryConfiguration() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            // When
            TaskInstance result = workflowInstanceService.addAdhocTask(
                    workflowId, "Task", null, "user", "creator", null);

            // Then
            assertThat(result.getMaxRetries()).isEqualTo(3);
            assertThat(result.getRetryCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("addAdhocTask - Notification Tests")
    class AddAdhocTaskNotificationTests {

        @Test
        @DisplayName("Should send notification when assignee is provided")
        void shouldSendNotificationWhenAssigneeProvided() {
            // Given
            String assignTo = "nurse-001";
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            // When
            workflowInstanceService.addAdhocTask(
                    workflowId, "Administer Saline", "Description", assignTo, "doctor", 60);

            // Then
            verify(notificationService).notifyUser(notificationCaptor.capture());
            NotificationRequest notification = notificationCaptor.getValue();
            assertThat(notification.getUserId()).isEqualTo(assignTo);
            assertThat(notification.getEventType()).isEqualTo("ADHOC_TASK_ASSIGNMENT");
        }

        @Test
        @DisplayName("Should not send notification when assignee is null")
        void shouldNotSendNotificationWhenAssigneeIsNull() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            // When
            workflowInstanceService.addAdhocTask(
                    workflowId, "Task", null, null, "creator", null);

            // Then
            verify(notificationService, never()).notifyUser(any());
        }

        @Test
        @DisplayName("Should not send notification when assignee is empty string")
        void shouldNotSendNotificationWhenAssigneeIsEmpty() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);

            // When
            workflowInstanceService.addAdhocTask(
                    workflowId, "Task", null, "", "creator", null);

            // Then
            verify(notificationService, never()).notifyUser(any());
        }

        @Test
        @DisplayName("Should continue even if notification fails")
        void shouldContinueEvenIfNotificationFails() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(invocation -> {
                TaskInstance task = invocation.getArgument(0);
                task.setId(UUID.randomUUID());
                return task;
            });
            when(workflowRepository.save(any(WorkflowInstance.class))).thenReturn(activeWorkflow);
            doThrow(new RuntimeException("Notification failed")).when(notificationService).notifyUser(any());

            // When - should not throw exception
            TaskInstance result = workflowInstanceService.addAdhocTask(
                    workflowId, "Task", null, "user", "creator", null);

            // Then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("addAdhocTask - Validation Tests")
    class AddAdhocTaskValidationTests {

        @Test
        @DisplayName("Should throw exception when workflow not found")
        void shouldThrowExceptionWhenWorkflowNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(workflowRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> workflowInstanceService.addAdhocTask(
                    nonExistentId, "Task", null, "user", "creator", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should throw exception when workflow is not active")
        void shouldThrowExceptionWhenWorkflowNotActive() {
            // Given
            activeWorkflow.setStatus(WorkflowStatus.COMPLETED);
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));

            // When/Then
            assertThatThrownBy(() -> workflowInstanceService.addAdhocTask(
                    workflowId, "Task", null, "user", "creator", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("non-active workflow");
        }

        @Test
        @DisplayName("Should throw exception when workflow is paused")
        void shouldThrowExceptionWhenWorkflowPaused() {
            // Given
            activeWorkflow.setStatus(WorkflowStatus.PAUSED);
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));

            // When/Then
            assertThatThrownBy(() -> workflowInstanceService.addAdhocTask(
                    workflowId, "Task", null, "user", "creator", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("non-active workflow");
        }

        @Test
        @DisplayName("Should throw exception when workflow is cancelled")
        void shouldThrowExceptionWhenWorkflowCancelled() {
            // Given
            activeWorkflow.setStatus(WorkflowStatus.CANCELLED);
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));

            // When/Then
            assertThatThrownBy(() -> workflowInstanceService.addAdhocTask(
                    workflowId, "Task", null, "user", "creator", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("non-active workflow");
        }

        @Test
        @DisplayName("Should throw exception when task name is null")
        void shouldThrowExceptionWhenTaskNameIsNull() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));

            // When/Then
            assertThatThrownBy(() -> workflowInstanceService.addAdhocTask(
                    workflowId, null, null, "user", "creator", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("task name is required");
        }

        @Test
        @DisplayName("Should throw exception when task name is empty")
        void shouldThrowExceptionWhenTaskNameIsEmpty() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));

            // When/Then
            assertThatThrownBy(() -> workflowInstanceService.addAdhocTask(
                    workflowId, "", null, "user", "creator", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("task name is required");
        }

        @Test
        @DisplayName("Should throw exception when task name is only whitespace")
        void shouldThrowExceptionWhenTaskNameIsOnlyWhitespace() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));

            // When/Then
            assertThatThrownBy(() -> workflowInstanceService.addAdhocTask(
                    workflowId, "   ", null, "user", "creator", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("task name is required");
        }
    }

    @Nested
    @DisplayName("getAdhocTasks - Retrieval Tests")
    class GetAdhocTasksTests {

        @Test
        @DisplayName("Should return only ad-hoc tasks")
        void shouldReturnOnlyAdhocTasks() {
            // Given
            TaskInstance adhocTask1 = new TaskInstance();
            adhocTask1.setIsAdhoc(true);
            adhocTask1.setAdhocTaskName("Adhoc Task 1");

            TaskInstance adhocTask2 = new TaskInstance();
            adhocTask2.setIsAdhoc(true);
            adhocTask2.setAdhocTaskName("Adhoc Task 2");

            TaskInstance regularTask = new TaskInstance();
            regularTask.setIsAdhoc(false);

            activeWorkflow.getTaskInstances().add(adhocTask1);
            activeWorkflow.getTaskInstances().add(regularTask);
            activeWorkflow.getTaskInstances().add(adhocTask2);

            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));

            // When
            var result = workflowInstanceService.getAdhocTasks(workflowId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(t -> Boolean.TRUE.equals(t.getIsAdhoc()));
        }

        @Test
        @DisplayName("Should return empty list when no ad-hoc tasks exist")
        void shouldReturnEmptyListWhenNoAdhocTasks() {
            // Given
            TaskInstance regularTask = new TaskInstance();
            regularTask.setIsAdhoc(false);
            activeWorkflow.getTaskInstances().add(regularTask);

            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));

            // When
            var result = workflowInstanceService.getAdhocTasks(workflowId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when workflow has no tasks")
        void shouldReturnEmptyListWhenNoTasks() {
            // Given
            when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(activeWorkflow));

            // When
            var result = workflowInstanceService.getAdhocTasks(workflowId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when workflow not found")
        void shouldThrowExceptionWhenWorkflowNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(workflowRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> workflowInstanceService.getAdhocTasks(nonExistentId))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Ad-hoc Task - Helper Method Tests")
    class AdhocTaskHelperMethodTests {

        @Test
        @DisplayName("TaskInstance.getTaskName() should return adhocTaskName for ad-hoc tasks")
        void getTaskNameShouldReturnAdhocTaskName() {
            // Given
            TaskInstance adhocTask = new TaskInstance();
            adhocTask.setIsAdhoc(true);
            adhocTask.setAdhocTaskName("Administer Saline");

            // When/Then
            assertThat(adhocTask.getTaskName()).isEqualTo("Administer Saline");
        }

        @Test
        @DisplayName("TaskInstance.getTaskDescription() should return adhocTaskDescription for ad-hoc tasks")
        void getTaskDescriptionShouldReturnAdhocDescription() {
            // Given
            TaskInstance adhocTask = new TaskInstance();
            adhocTask.setIsAdhoc(true);
            adhocTask.setAdhocTaskDescription("IV solution 500ml");

            // When/Then
            assertThat(adhocTask.getTaskDescription()).isEqualTo("IV solution 500ml");
        }

        @Test
        @DisplayName("TaskInstance.isOptional() should return true for ad-hoc tasks")
        void isOptionalShouldReturnTrueForAdhocTasks() {
            // Given
            TaskInstance adhocTask = new TaskInstance();
            adhocTask.setIsAdhoc(true);

            // When/Then
            assertThat(adhocTask.isOptional()).isTrue();
        }
    }
}
