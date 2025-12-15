package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.Patient;
import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.entity.WorkflowTaskDefinition;
import com.hmis.workflow.domain.enums.TaskStatus;
import com.hmis.workflow.repository.TaskInstanceRepository;
import com.hmis.workflow.repository.WorkflowInstanceRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskInstanceService skip functionality.
 *
 * Tests cover:
 * - Skipping optional tasks without reason
 * - Skipping required tasks with forceSkip and reason
 * - Validation of skip conditions
 * - Skip reason and user tracking
 * - Audit comments for required task skips
 * - Retrieving skipped tasks
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskInstanceService - Skip Task Tests")
class TaskInstanceServiceSkipTest {

    @Mock
    private TaskInstanceRepository taskRepository;

    @Mock
    private WorkflowInstanceRepository workflowRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskInstanceService taskInstanceService;

    @Captor
    private ArgumentCaptor<TaskInstance> taskCaptor;

    private TaskInstance optionalTask;
    private TaskInstance requiredTask;
    private TaskInstance adhocTask;
    private WorkflowInstance workflow;
    private Patient patient;
    private WorkflowTaskDefinition optionalTaskDef;
    private WorkflowTaskDefinition requiredTaskDef;
    private UUID optionalTaskId;
    private UUID requiredTaskId;
    private UUID adhocTaskId;

    @BeforeEach
    void setUp() {
        optionalTaskId = UUID.randomUUID();
        requiredTaskId = UUID.randomUUID();
        adhocTaskId = UUID.randomUUID();

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("John");
        patient.setLastName("Doe");

        workflow = new WorkflowInstance();
        workflow.setId(UUID.randomUUID());
        workflow.setPatient(patient);

        // Optional task definition
        optionalTaskDef = new WorkflowTaskDefinition();
        optionalTaskDef.setId(UUID.randomUUID());
        optionalTaskDef.setName("Optional Lab Test");
        optionalTaskDef.setIsOptional(true);

        // Required task definition
        requiredTaskDef = new WorkflowTaskDefinition();
        requiredTaskDef.setId(UUID.randomUUID());
        requiredTaskDef.setName("Blood Test");
        requiredTaskDef.setIsOptional(false);

        // Optional task instance
        optionalTask = new TaskInstance();
        optionalTask.setId(optionalTaskId);
        optionalTask.setTaskInstanceId("TASK-OPT-001");
        optionalTask.setTaskDefinition(optionalTaskDef);
        optionalTask.setWorkflowInstance(workflow);
        optionalTask.setStatus(TaskStatus.PENDING);
        optionalTask.setIsAdhoc(false);

        // Required task instance
        requiredTask = new TaskInstance();
        requiredTask.setId(requiredTaskId);
        requiredTask.setTaskInstanceId("TASK-REQ-001");
        requiredTask.setTaskDefinition(requiredTaskDef);
        requiredTask.setWorkflowInstance(workflow);
        requiredTask.setStatus(TaskStatus.PENDING);
        requiredTask.setIsAdhoc(false);

        // Ad-hoc task instance (optional by default)
        adhocTask = new TaskInstance();
        adhocTask.setId(adhocTaskId);
        adhocTask.setTaskInstanceId("TASK-ADHOC-001");
        adhocTask.setTaskDefinition(null);
        adhocTask.setWorkflowInstance(workflow);
        adhocTask.setStatus(TaskStatus.PENDING);
        adhocTask.setIsAdhoc(true);
        adhocTask.setAdhocTaskName("Administer Saline");
    }

    @Nested
    @DisplayName("skipTask - Legacy Method (Optional Tasks)")
    class SkipTaskLegacyTests {

        @Test
        @DisplayName("Should skip optional task without reason")
        void shouldSkipOptionalTaskWithoutReason() {
            // Given
            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTask(optionalTaskId);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when skipping required task without force")
        void shouldThrowExceptionWhenSkippingRequiredTaskWithoutForce() {
            // Given
            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));

            // When/Then
            assertThatThrownBy(() -> taskInstanceService.skipTask(requiredTaskId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("forceSkip=true");
        }
    }

    @Nested
    @DisplayName("skipTaskWithReason - Optional Task Tests")
    class SkipOptionalTaskTests {

        @Test
        @DisplayName("Should skip optional task with reason")
        void shouldSkipOptionalTaskWithReason() {
            // Given
            String reason = "Patient declined optional test";
            String skippedByUser = "nurse-001";

            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    optionalTaskId, reason, skippedByUser, false);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
            assertThat(result.getSkipReason()).isEqualTo(reason);
            assertThat(result.getSkippedByUser()).isEqualTo(skippedByUser);
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should skip optional task without reason")
        void shouldSkipOptionalTaskWithoutReason() {
            // Given
            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    optionalTaskId, null, "user", false);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
            assertThat(result.getSkipReason()).isNull();
        }

        @Test
        @DisplayName("Should not add audit comment for optional task skip")
        void shouldNotAddAuditCommentForOptionalTaskSkip() {
            // Given
            optionalTask.setComments("Existing comment");
            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    optionalTaskId, "Some reason", "user", false);

            // Then
            assertThat(result.getComments()).isEqualTo("Existing comment");
            assertThat(result.getComments()).doesNotContain("REQUIRED TASK SKIPPED");
        }
    }

    @Nested
    @DisplayName("skipTaskWithReason - Required Task Tests")
    class SkipRequiredTaskTests {

        @Test
        @DisplayName("Should skip required task with forceSkip and reason")
        void shouldSkipRequiredTaskWithForceAndReason() {
            // Given
            String reason = "Blood test already performed at external lab";
            String skippedByUser = "doctor-jones";

            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    requiredTaskId, reason, skippedByUser, true);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
            assertThat(result.getSkipReason()).isEqualTo(reason);
            assertThat(result.getSkippedByUser()).isEqualTo(skippedByUser);
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when forceSkip is false for required task")
        void shouldThrowExceptionWhenForceSkipIsFalse() {
            // Given
            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));

            // When/Then
            assertThatThrownBy(() -> taskInstanceService.skipTaskWithReason(
                    requiredTaskId, "Reason", "user", false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot skip required task")
                    .hasMessageContaining("forceSkip=true");
        }

        @Test
        @DisplayName("Should throw exception when reason is null for required task")
        void shouldThrowExceptionWhenReasonIsNull() {
            // Given
            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));

            // When/Then
            assertThatThrownBy(() -> taskInstanceService.skipTaskWithReason(
                    requiredTaskId, null, "user", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required");
        }

        @Test
        @DisplayName("Should throw exception when reason is empty for required task")
        void shouldThrowExceptionWhenReasonIsEmpty() {
            // Given
            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));

            // When/Then
            assertThatThrownBy(() -> taskInstanceService.skipTaskWithReason(
                    requiredTaskId, "", "user", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required");
        }

        @Test
        @DisplayName("Should throw exception when reason is only whitespace for required task")
        void shouldThrowExceptionWhenReasonIsWhitespace() {
            // Given
            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));

            // When/Then
            assertThatThrownBy(() -> taskInstanceService.skipTaskWithReason(
                    requiredTaskId, "   ", "user", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required");
        }

        @Test
        @DisplayName("Should add audit comment for required task skip")
        void shouldAddAuditCommentForRequiredTaskSkip() {
            // Given
            String reason = "Patient refused procedure";
            String skippedByUser = "doctor-smith";

            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    requiredTaskId, reason, skippedByUser, true);

            // Then
            assertThat(result.getComments()).contains("REQUIRED TASK SKIPPED");
            assertThat(result.getComments()).contains(skippedByUser);
            assertThat(result.getComments()).contains(reason);
        }

        @Test
        @DisplayName("Should append audit comment to existing comments")
        void shouldAppendAuditCommentToExistingComments() {
            // Given
            requiredTask.setComments("Previous comment");
            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    requiredTaskId, "Reason", "user", true);

            // Then
            assertThat(result.getComments()).startsWith("Previous comment; ");
            assertThat(result.getComments()).contains("REQUIRED TASK SKIPPED");
        }

        @Test
        @DisplayName("Should handle null skippedByUser in audit comment")
        void shouldHandleNullSkippedByUserInAuditComment() {
            // Given
            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    requiredTaskId, "Reason", null, true);

            // Then
            assertThat(result.getComments()).contains("REQUIRED TASK SKIPPED by Unknown");
        }
    }

    @Nested
    @DisplayName("skipTaskWithReason - Ad-hoc Task Tests")
    class SkipAdhocTaskTests {

        @Test
        @DisplayName("Should skip ad-hoc task without forceSkip (treated as optional)")
        void shouldSkipAdhocTaskWithoutForceSkip() {
            // Given
            when(taskRepository.findById(adhocTaskId)).thenReturn(Optional.of(adhocTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    adhocTaskId, "No longer needed", "nurse", false);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
        }

        @Test
        @DisplayName("Should skip ad-hoc task without reason (optional by default)")
        void shouldSkipAdhocTaskWithoutReason() {
            // Given
            when(taskRepository.findById(adhocTaskId)).thenReturn(Optional.of(adhocTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    adhocTaskId, null, "user", false);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
            assertThat(result.getSkipReason()).isNull();
        }
    }

    @Nested
    @DisplayName("skipTaskWithReason - Status Validation Tests")
    class SkipStatusValidationTests {

        @Test
        @DisplayName("Should throw exception when task is already completed")
        void shouldThrowExceptionWhenTaskIsCompleted() {
            // Given
            optionalTask.setStatus(TaskStatus.COMPLETED);
            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));

            // When/Then
            assertThatThrownBy(() -> taskInstanceService.skipTaskWithReason(
                    optionalTaskId, "Reason", "user", false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already completed");
        }

        @Test
        @DisplayName("Should throw exception when task is already skipped")
        void shouldThrowExceptionWhenTaskIsAlreadySkipped() {
            // Given
            optionalTask.setStatus(TaskStatus.SKIPPED);
            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));

            // When/Then
            assertThatThrownBy(() -> taskInstanceService.skipTaskWithReason(
                    optionalTaskId, "Reason", "user", false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already skipped");
        }

        @Test
        @DisplayName("Should allow skipping task in PENDING status")
        void shouldAllowSkippingPendingTask() {
            // Given
            optionalTask.setStatus(TaskStatus.PENDING);
            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    optionalTaskId, null, "user", false);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
        }

        @Test
        @DisplayName("Should allow skipping task in BLOCKED status")
        void shouldAllowSkippingBlockedTask() {
            // Given
            optionalTask.setStatus(TaskStatus.BLOCKED);
            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    optionalTaskId, null, "user", false);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
        }

        @Test
        @DisplayName("Should allow skipping task in IN_PROGRESS status")
        void shouldAllowSkippingInProgressTask() {
            // Given
            optionalTask.setStatus(TaskStatus.IN_PROGRESS);
            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    optionalTaskId, null, "user", false);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
        }

        @Test
        @DisplayName("Should allow skipping task in FAILED status")
        void shouldAllowSkippingFailedTask() {
            // Given
            optionalTask.setStatus(TaskStatus.FAILED);
            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    optionalTaskId, null, "user", false);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> taskInstanceService.skipTaskWithReason(
                    nonExistentId, "Reason", "user", false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("getSkippedTasks - Retrieval Tests")
    class GetSkippedTasksTests {

        @Test
        @DisplayName("Should return only skipped tasks")
        void shouldReturnOnlySkippedTasks() {
            // Given
            UUID workflowId = workflow.getId();

            TaskInstance skippedTask1 = new TaskInstance();
            skippedTask1.setStatus(TaskStatus.SKIPPED);

            TaskInstance skippedTask2 = new TaskInstance();
            skippedTask2.setStatus(TaskStatus.SKIPPED);

            TaskInstance pendingTask = new TaskInstance();
            pendingTask.setStatus(TaskStatus.PENDING);

            TaskInstance completedTask = new TaskInstance();
            completedTask.setStatus(TaskStatus.COMPLETED);

            when(taskRepository.findByWorkflowInstanceId(workflowId))
                    .thenReturn(List.of(skippedTask1, pendingTask, skippedTask2, completedTask));

            // When
            var result = taskInstanceService.getSkippedTasks(workflowId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(t -> t.getStatus() == TaskStatus.SKIPPED);
        }

        @Test
        @DisplayName("Should return empty list when no skipped tasks")
        void shouldReturnEmptyListWhenNoSkippedTasks() {
            // Given
            UUID workflowId = workflow.getId();

            TaskInstance pendingTask = new TaskInstance();
            pendingTask.setStatus(TaskStatus.PENDING);

            when(taskRepository.findByWorkflowInstanceId(workflowId))
                    .thenReturn(List.of(pendingTask));

            // When
            var result = taskInstanceService.getSkippedTasks(workflowId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when no tasks exist")
        void shouldReturnEmptyListWhenNoTasks() {
            // Given
            UUID workflowId = workflow.getId();
            when(taskRepository.findByWorkflowInstanceId(workflowId)).thenReturn(List.of());

            // When
            var result = taskInstanceService.getSkippedTasks(workflowId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Skip Task - Timestamp Tests")
    class SkipTimestampTests {

        @Test
        @DisplayName("Should set completedAt timestamp when skipping")
        void shouldSetCompletedAtTimestamp() {
            // Given
            LocalDateTime beforeSkip = LocalDateTime.now().minusSeconds(1);
            when(taskRepository.findById(optionalTaskId)).thenReturn(Optional.of(optionalTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    optionalTaskId, null, "user", false);

            // Then
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getCompletedAt()).isAfter(beforeSkip);
        }
    }

    @Nested
    @DisplayName("Skip Task - Use Case Tests")
    class SkipUseCaseTests {

        @Test
        @DisplayName("Use Case: Blood test already performed elsewhere")
        void useCaseBloodTestPerformedElsewhere() {
            // Given
            String reason = "Blood test already performed at external lab - results attached in patient file";
            String skippedByUser = "doctor-jones";

            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    requiredTaskId, reason, skippedByUser, true);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
            assertThat(result.getSkipReason()).isEqualTo(reason);
            assertThat(result.getComments()).contains("REQUIRED TASK SKIPPED");
        }

        @Test
        @DisplayName("Use Case: Patient refused procedure")
        void useCasePatientRefused() {
            // Given
            String reason = "Patient refused the procedure after informed consent discussion";
            String skippedByUser = "doctor-smith";

            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    requiredTaskId, reason, skippedByUser, true);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
            assertThat(result.getSkipReason()).contains("Patient refused");
        }

        @Test
        @DisplayName("Use Case: Clinical judgment overrides protocol")
        void useCaseClinicalJudgment() {
            // Given
            String reason = "Clinical judgment - test contraindicated due to patient's current medication";
            String skippedByUser = "attending-physician";

            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    requiredTaskId, reason, skippedByUser, true);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
            assertThat(result.getSkipReason()).contains("Clinical judgment");
        }

        @Test
        @DisplayName("Use Case: Task no longer applicable")
        void useCaseTaskNoLongerApplicable() {
            // Given
            String reason = "Task no longer applicable - patient condition changed to outpatient status";
            String skippedByUser = "care-coordinator";

            when(taskRepository.findById(requiredTaskId)).thenReturn(Optional.of(requiredTask));
            when(taskRepository.save(any(TaskInstance.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TaskInstance result = taskInstanceService.skipTaskWithReason(
                    requiredTaskId, reason, skippedByUser, true);

            // Then
            assertThat(result.getStatus()).isEqualTo(TaskStatus.SKIPPED);
            assertThat(result.getSkipReason()).contains("no longer applicable");
        }
    }
}
