package com.hmis.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmis.workflow.config.AuditContext;
import com.hmis.workflow.domain.entity.*;
import com.hmis.workflow.domain.enums.TaskStatus;
import com.hmis.workflow.domain.enums.WorkflowStatus;
import com.hmis.workflow.repository.AuditLogRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService.
 *
 * Tests cover:
 * - Logging task status changes
 * - Logging task completions, failures, and skips
 * - Logging task assignments
 * - Legal hold operations
 * - Query methods
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditService auditService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private TaskInstance task;
    private WorkflowInstance workflow;
    private Patient patient;
    private UUID taskId;
    private UUID workflowId;
    private UUID patientId;

    @BeforeEach
    void setUp() throws Exception {
        taskId = UUID.randomUUID();
        workflowId = UUID.randomUUID();
        patientId = UUID.randomUUID();

        patient = new Patient();
        patient.setId(patientId);
        patient.setFirstName("John");
        patient.setLastName("Doe");

        workflow = new WorkflowInstance();
        workflow.setId(workflowId);
        workflow.setStatus(WorkflowStatus.ACTIVE);
        workflow.setPatient(patient);

        WorkflowTaskDefinition taskDef = new WorkflowTaskDefinition();
        taskDef.setId(UUID.randomUUID());
        taskDef.setName("Blood Test");

        task = new TaskInstance();
        task.setId(taskId);
        task.setTaskDefinition(taskDef);
        task.setWorkflowInstance(workflow);
        task.setStatus(TaskStatus.PENDING);
        task.setIsAdhoc(false);

        // Mock ObjectMapper to return valid JSON
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    }

    @AfterEach
    void tearDown() {
        AuditContext.clear();
    }

    @Nested
    @DisplayName("Task Status Change Logging")
    class TaskStatusChangeTests {

        @Test
        @DisplayName("Should log task status change with all details")
        void shouldLogTaskStatusChange() {
            // Given
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
                AuditLog log = i.getArgument(0);
                log.setId(UUID.randomUUID());
                return log;
            });

            // When
            AuditLog result = auditService.logTaskStatusChange(task, "PENDING", "IN_PROGRESS", "nurse-001");

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog captured = auditLogCaptor.getValue();

            assertThat(captured.getEntityType()).isEqualTo("TASK_INSTANCE");
            assertThat(captured.getEntityId()).isEqualTo(taskId.toString());
            assertThat(captured.getAction()).isEqualTo("STATUS_CHANGED");
            assertThat(captured.getActor()).isEqualTo("nurse-001");
            assertThat(captured.getPreviousValue()).isEqualTo("PENDING");
            assertThat(captured.getNewValue()).isEqualTo("IN_PROGRESS");
            assertThat(captured.getPatientId()).isEqualTo(patientId.toString());
            assertThat(captured.getWorkflowInstanceId()).isEqualTo(workflowId.toString());
            assertThat(captured.getActionTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should use SYSTEM as actor when not provided")
        void shouldUseSystemActorWhenNotProvided() {
            // Given
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
                AuditLog log = i.getArgument(0);
                log.setId(UUID.randomUUID());
                return log;
            });

            // When
            auditService.logTaskStatusChange(task, "PENDING", "IN_PROGRESS", null);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            assertThat(auditLogCaptor.getValue().getActor()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("Should use actor from AuditContext when not explicitly provided")
        void shouldUseActorFromContext() {
            // Given
            AuditContext.setCurrentUser("context-user");
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
                AuditLog log = i.getArgument(0);
                log.setId(UUID.randomUUID());
                return log;
            });

            // When
            auditService.logTaskStatusChange(task, "PENDING", "IN_PROGRESS", null);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            assertThat(auditLogCaptor.getValue().getActor()).isEqualTo("context-user");
        }
    }

    @Nested
    @DisplayName("Task Completion Logging")
    class TaskCompletionTests {

        @Test
        @DisplayName("Should log task completion with result")
        void shouldLogTaskCompletion() {
            // Given
            task.setCompletedAt(LocalDateTime.now());
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
                AuditLog log = i.getArgument(0);
                log.setId(UUID.randomUUID());
                return log;
            });

            // When
            auditService.logTaskCompleted(task, "Blood pressure: 120/80", "nurse-001");

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog captured = auditLogCaptor.getValue();

            assertThat(captured.getAction()).isEqualTo("COMPLETED");
            assertThat(captured.getNewValue()).isEqualTo("COMPLETED");
        }
    }

    @Nested
    @DisplayName("Task Failure Logging")
    class TaskFailureTests {

        @Test
        @DisplayName("Should log task failure with error message")
        void shouldLogTaskFailure() {
            // Given
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
                AuditLog log = i.getArgument(0);
                log.setId(UUID.randomUUID());
                return log;
            });

            // When
            auditService.logTaskFailed(task, "Equipment malfunction", "technician-001");

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog captured = auditLogCaptor.getValue();

            assertThat(captured.getAction()).isEqualTo("FAILED");
            assertThat(captured.getNewValue()).isEqualTo("FAILED");
            assertThat(captured.getActor()).isEqualTo("technician-001");
        }
    }

    @Nested
    @DisplayName("Task Skip Logging")
    class TaskSkipTests {

        @Test
        @DisplayName("Should log task skip with reason")
        void shouldLogTaskSkip() {
            // Given
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
                AuditLog log = i.getArgument(0);
                log.setId(UUID.randomUUID());
                return log;
            });

            // When
            auditService.logTaskSkipped(task, "Patient refused procedure", "doctor-smith");

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog captured = auditLogCaptor.getValue();

            assertThat(captured.getAction()).isEqualTo("SKIPPED");
            assertThat(captured.getNewValue()).isEqualTo("SKIPPED");
        }
    }

    @Nested
    @DisplayName("Task Assignment Logging")
    class TaskAssignmentTests {

        @Test
        @DisplayName("Should log task assignment")
        void shouldLogTaskAssignment() {
            // Given
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
                AuditLog log = i.getArgument(0);
                log.setId(UUID.randomUUID());
                return log;
            });

            // When
            auditService.logTaskAssigned(task, "nurse-001", "nurse-002", "charge-nurse");

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog captured = auditLogCaptor.getValue();

            assertThat(captured.getAction()).isEqualTo("ASSIGNED");
            assertThat(captured.getPreviousValue()).isEqualTo("nurse-001");
            assertThat(captured.getNewValue()).isEqualTo("nurse-002");
        }

        @Test
        @DisplayName("Should handle null previous assignee")
        void shouldHandleNullPreviousAssignee() {
            // Given
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
                AuditLog log = i.getArgument(0);
                log.setId(UUID.randomUUID());
                return log;
            });

            // When
            auditService.logTaskAssigned(task, null, "nurse-001", "charge-nurse");

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            assertThat(auditLogCaptor.getValue().getPreviousValue()).isNull();
        }
    }

    @Nested
    @DisplayName("Legal Hold Operations")
    class LegalHoldTests {

        @Test
        @DisplayName("Should set legal hold for patient")
        void shouldSetLegalHoldForPatient() {
            // Given
            AuditLog log1 = new AuditLog();
            log1.setId(UUID.randomUUID());
            log1.setIsLegalHold(false);

            AuditLog log2 = new AuditLog();
            log2.setId(UUID.randomUUID());
            log2.setIsLegalHold(false);

            when(auditLogRepository.findByPatientIdOrderByActionTimestampDesc(patientId))
                    .thenReturn(List.of(log1, log2));
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

            // When
            int count = auditService.setLegalHoldForPatient(patientId.toString(), "legal-admin", "Litigation hold");

            // Then
            assertThat(count).isEqualTo(2);
            verify(auditLogRepository, atLeast(2)).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Should skip already held records when setting legal hold")
        void shouldSkipAlreadyHeldRecords() {
            // Given
            AuditLog alreadyHeld = new AuditLog();
            alreadyHeld.setId(UUID.randomUUID());
            alreadyHeld.setIsLegalHold(true);

            AuditLog notHeld = new AuditLog();
            notHeld.setId(UUID.randomUUID());
            notHeld.setIsLegalHold(false);

            when(auditLogRepository.findByPatientIdOrderByActionTimestampDesc(patientId))
                    .thenReturn(List.of(alreadyHeld, notHeld));
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

            // When
            int count = auditService.setLegalHoldForPatient(patientId.toString(), "admin", "Reason");

            // Then
            assertThat(count).isEqualTo(1); // Only one record should be updated
        }

        @Test
        @DisplayName("Should release legal hold for patient")
        void shouldReleaseLegalHoldForPatient() {
            // Given
            AuditLog held = new AuditLog();
            held.setId(UUID.randomUUID());
            held.setIsLegalHold(true);

            when(auditLogRepository.findByPatientIdOrderByActionTimestampDesc(patientId))
                    .thenReturn(List.of(held));
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

            // When
            int count = auditService.releaseLegalHoldForPatient(patientId.toString(), "legal-admin", "Case closed");

            // Then
            assertThat(count).isEqualTo(1);
            assertThat(held.getIsLegalHold()).isFalse();
        }

        @Test
        @DisplayName("Should set legal hold for workflow")
        void shouldSetLegalHoldForWorkflow() {
            // Given
            AuditLog log = new AuditLog();
            log.setId(UUID.randomUUID());
            log.setIsLegalHold(false);

            when(auditLogRepository.findByWorkflowInstanceIdOrderByActionTimestampDesc(workflowId))
                    .thenReturn(List.of(log));
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

            // When
            int count = auditService.setLegalHoldForWorkflow(workflowId.toString(), "admin", "Investigation");

            // Then
            assertThat(count).isEqualTo(1);
            assertThat(log.getIsLegalHold()).isTrue();
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethodTests {

        @Test
        @DisplayName("Should get entity history")
        void shouldGetEntityHistory() {
            // Given
            AuditLog log = new AuditLog();
            when(auditLogRepository.findByEntityIdOrderByActionTimestampDesc(taskId.toString()))
                    .thenReturn(List.of(log));

            // When
            List<AuditLog> result = auditService.getEntityHistory(taskId.toString());

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get workflow history")
        void shouldGetWorkflowHistory() {
            // Given
            AuditLog log = new AuditLog();
            when(auditLogRepository.findByWorkflowInstanceIdOrderByActionTimestampDesc(workflowId))
                    .thenReturn(List.of(log));

            // When
            List<AuditLog> result = auditService.getWorkflowHistory(workflowId);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get patient history")
        void shouldGetPatientHistory() {
            // Given
            AuditLog log = new AuditLog();
            when(auditLogRepository.findByPatientIdOrderByActionTimestampDesc(patientId))
                    .thenReturn(List.of(log));

            // When
            List<AuditLog> result = auditService.getPatientHistory(patientId);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get actor history")
        void shouldGetActorHistory() {
            // Given
            AuditLog log = new AuditLog();
            when(auditLogRepository.findByActor("nurse-001")).thenReturn(List.of(log));

            // When
            List<AuditLog> result = auditService.getActorHistory("nurse-001");

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get history by date range")
        void shouldGetHistoryByDateRange() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusDays(7);
            LocalDateTime end = LocalDateTime.now();
            AuditLog log = new AuditLog();
            when(auditLogRepository.findByDateRange(start, end)).thenReturn(List.of(log));

            // When
            List<AuditLog> result = auditService.getHistoryByDateRange(start, end);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get legal hold logs")
        void shouldGetLegalHoldLogs() {
            // Given
            AuditLog log = new AuditLog();
            log.setIsLegalHold(true);
            when(auditLogRepository.findLegalHoldLogs()).thenReturn(List.of(log));

            // When
            List<AuditLog> result = auditService.getLegalHoldLogs();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsLegalHold()).isTrue();
        }
    }

    @Nested
    @DisplayName("Value Truncation")
    class ValueTruncationTests {

        @Test
        @DisplayName("Should truncate long values to 100 characters")
        void shouldTruncateLongValues() {
            // Given
            String longValue = "A".repeat(150);
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
                AuditLog log = i.getArgument(0);
                log.setId(UUID.randomUUID());
                return log;
            });

            // When
            auditService.logTaskStatusChange(task, longValue, "NEW", "user");

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            String previousValue = auditLogCaptor.getValue().getPreviousValue();
            assertThat(previousValue).hasSize(100);
            assertThat(previousValue).endsWith("...");
        }
    }

    @Nested
    @DisplayName("Correlation ID")
    class CorrelationIdTests {

        @Test
        @DisplayName("Should include correlation ID from context")
        void shouldIncludeCorrelationId() {
            // Given
            String correlationId = UUID.randomUUID().toString();
            AuditContext.setCorrelationId(correlationId);
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
                AuditLog log = i.getArgument(0);
                log.setId(UUID.randomUUID());
                return log;
            });

            // When
            auditService.logTaskStatusChange(task, "OLD", "NEW", "user");

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            assertThat(auditLogCaptor.getValue().getCorrelationId()).isEqualTo(correlationId);
        }
    }
}
