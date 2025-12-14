package com.hmis.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmis.workflow.domain.entity.Patient;
import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.entity.WorkflowTaskDefinition;
import com.hmis.workflow.domain.enums.TaskStatus;
import com.hmis.workflow.domain.enums.WorkflowStatus;
import com.hmis.workflow.service.TaskInstanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TaskInstanceController skip task endpoints.
 *
 * Tests cover:
 * - POST /workflows/tasks/{id}/skip (legacy)
 * - POST /workflows/tasks/{id}/skip-with-reason
 * - GET /workflows/tasks/workflow/{id}/skipped
 * - Request validation
 * - Response structure
 * - Error handling
 */
@WebMvcTest(TaskInstanceController.class)
@DisplayName("TaskInstanceController - Skip Task API Tests")
class TaskInstanceControllerSkipTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskInstanceService taskInstanceService;

    private UUID taskId;
    private UUID workflowId;
    private TaskInstance optionalTask;
    private TaskInstance requiredTask;
    private WorkflowInstance workflow;
    private Patient patient;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        workflowId = UUID.randomUUID();

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("John");
        patient.setLastName("Doe");

        workflow = new WorkflowInstance();
        workflow.setId(workflowId);
        workflow.setStatus(WorkflowStatus.ACTIVE);
        workflow.setPatient(patient);

        WorkflowTaskDefinition optionalTaskDef = new WorkflowTaskDefinition();
        optionalTaskDef.setId(UUID.randomUUID());
        optionalTaskDef.setName("Optional Lab Test");
        optionalTaskDef.setDescription("Optional blood panel");
        optionalTaskDef.setIsOptional(true);

        WorkflowTaskDefinition requiredTaskDef = new WorkflowTaskDefinition();
        requiredTaskDef.setId(UUID.randomUUID());
        requiredTaskDef.setName("Blood Test");
        requiredTaskDef.setDescription("Required blood panel");
        requiredTaskDef.setIsOptional(false);

        optionalTask = createTask(optionalTaskDef, TaskStatus.PENDING);
        requiredTask = createTask(requiredTaskDef, TaskStatus.PENDING);
    }

    @Nested
    @DisplayName("POST /workflows/tasks/{id}/skip (Legacy)")
    class SkipTaskLegacyEndpointTests {

        @Test
        @DisplayName("Should skip optional task")
        void shouldSkipOptionalTask() throws Exception {
            // Given
            optionalTask.setStatus(TaskStatus.SKIPPED);
            optionalTask.setCompletedAt(LocalDateTime.now());

            when(taskInstanceService.skipTask(taskId)).thenReturn(optionalTask);

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip", taskId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task skipped successfully"))
                    .andExpect(jsonPath("$.data.status").value("SKIPPED"));
        }

        @Test
        @DisplayName("Should return 400 when trying to skip required task")
        void shouldReturn400WhenSkippingRequiredTask() throws Exception {
            // Given
            when(taskInstanceService.skipTask(taskId))
                    .thenThrow(new IllegalStateException("Cannot skip required task"));

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip", taskId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when task not found")
        void shouldReturn400WhenTaskNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(taskInstanceService.skipTask(nonExistentId))
                    .thenThrow(new IllegalArgumentException("Task instance not found"));

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip", nonExistentId))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /workflows/tasks/{id}/skip-with-reason")
    class SkipWithReasonEndpointTests {

        @Test
        @DisplayName("Should skip optional task with reason")
        void shouldSkipOptionalTaskWithReason() throws Exception {
            // Given
            optionalTask.setStatus(TaskStatus.SKIPPED);
            optionalTask.setSkipReason("Patient declined");
            optionalTask.setSkippedByUser("nurse-001");
            optionalTask.setCompletedAt(LocalDateTime.now());

            when(taskInstanceService.skipTaskWithReason(
                    eq(taskId), eq("Patient declined"), eq("nurse-001"), eq(false)
            )).thenReturn(optionalTask);

            String requestBody = """
                {
                    "reason": "Patient declined",
                    "skippedByUser": "nurse-001",
                    "forceSkip": false
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("SKIPPED"))
                    .andExpect(jsonPath("$.data.skipReason").value("Patient declined"))
                    .andExpect(jsonPath("$.data.skippedByUser").value("nurse-001"));
        }

        @Test
        @DisplayName("Should skip required task with forceSkip and reason")
        void shouldSkipRequiredTaskWithForceAndReason() throws Exception {
            // Given
            requiredTask.setStatus(TaskStatus.SKIPPED);
            requiredTask.setSkipReason("Blood test already performed at external lab");
            requiredTask.setSkippedByUser("doctor-jones");
            requiredTask.setCompletedAt(LocalDateTime.now());
            requiredTask.setComments("REQUIRED TASK SKIPPED by doctor-jones. Reason: Blood test already performed");

            when(taskInstanceService.skipTaskWithReason(
                    eq(taskId),
                    eq("Blood test already performed at external lab"),
                    eq("doctor-jones"),
                    eq(true)
            )).thenReturn(requiredTask);

            String requestBody = """
                {
                    "reason": "Blood test already performed at external lab",
                    "skippedByUser": "doctor-jones",
                    "forceSkip": true
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("SKIPPED"))
                    .andExpect(jsonPath("$.data.skipReason").value("Blood test already performed at external lab"))
                    .andExpect(jsonPath("$.data.skippedByUser").value("doctor-jones"));
        }

        @Test
        @DisplayName("Should return 400 when forceSkip is false for required task")
        void shouldReturn400WhenForceSkipFalseForRequiredTask() throws Exception {
            // Given
            when(taskInstanceService.skipTaskWithReason(
                    eq(taskId), any(), any(), eq(false)
            )).thenThrow(new IllegalStateException("Cannot skip required task. Use forceSkip=true"));

            String requestBody = """
                {
                    "reason": "Some reason",
                    "skippedByUser": "user",
                    "forceSkip": false
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when reason is missing for required task")
        void shouldReturn400WhenReasonMissingForRequiredTask() throws Exception {
            // Given
            when(taskInstanceService.skipTaskWithReason(
                    eq(taskId), isNull(), any(), eq(true)
            )).thenThrow(new IllegalArgumentException("A reason is required when skipping a required task"));

            String requestBody = """
                {
                    "skippedByUser": "user",
                    "forceSkip": true
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when task is already completed")
        void shouldReturn400WhenTaskAlreadyCompleted() throws Exception {
            // Given
            when(taskInstanceService.skipTaskWithReason(any(), any(), any(), anyBoolean()))
                    .thenThrow(new IllegalStateException("Cannot skip an already completed task"));

            String requestBody = """
                {
                    "reason": "Some reason",
                    "skippedByUser": "user",
                    "forceSkip": false
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when task is already skipped")
        void shouldReturn400WhenTaskAlreadySkipped() throws Exception {
            // Given
            when(taskInstanceService.skipTaskWithReason(any(), any(), any(), anyBoolean()))
                    .thenThrow(new IllegalStateException("Task is already skipped"));

            String requestBody = """
                {
                    "reason": "Some reason",
                    "skippedByUser": "user",
                    "forceSkip": false
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should skip task without skippedByUser")
        void shouldSkipTaskWithoutSkippedByUser() throws Exception {
            // Given
            optionalTask.setStatus(TaskStatus.SKIPPED);
            optionalTask.setSkipReason("Patient declined");
            optionalTask.setCompletedAt(LocalDateTime.now());

            when(taskInstanceService.skipTaskWithReason(
                    eq(taskId), eq("Patient declined"), isNull(), eq(false)
            )).thenReturn(optionalTask);

            String requestBody = """
                {
                    "reason": "Patient declined",
                    "forceSkip": false
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SKIPPED"));
        }

        @Test
        @DisplayName("Should default forceSkip to false")
        void shouldDefaultForceSkipToFalse() throws Exception {
            // Given
            optionalTask.setStatus(TaskStatus.SKIPPED);
            optionalTask.setCompletedAt(LocalDateTime.now());

            when(taskInstanceService.skipTaskWithReason(
                    eq(taskId), eq("Reason"), eq("user"), eq(false)
            )).thenReturn(optionalTask);

            String requestBody = """
                {
                    "reason": "Reason",
                    "skippedByUser": "user"
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            verify(taskInstanceService).skipTaskWithReason(taskId, "Reason", "user", false);
        }
    }

    @Nested
    @DisplayName("GET /workflows/tasks/workflow/{workflowInstanceId}/skipped")
    class GetSkippedTasksEndpointTests {

        @Test
        @DisplayName("Should return list of skipped tasks")
        void shouldReturnListOfSkippedTasks() throws Exception {
            // Given
            TaskInstance skipped1 = createSkippedTask("Task 1", "Reason 1", "user1");
            TaskInstance skipped2 = createSkippedTask("Task 2", "Reason 2", "user2");

            when(taskInstanceService.getSkippedTasks(workflowId))
                    .thenReturn(List.of(skipped1, skipped2));

            // When/Then
            mockMvc.perform(get("/workflows/tasks/workflow/{id}/skipped", workflowId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Skipped tasks retrieved successfully"))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].status").value("SKIPPED"))
                    .andExpect(jsonPath("$.data[0].skipReason").value("Reason 1"))
                    .andExpect(jsonPath("$.data[0].skippedByUser").value("user1"))
                    .andExpect(jsonPath("$.data[1].status").value("SKIPPED"))
                    .andExpect(jsonPath("$.data[1].skipReason").value("Reason 2"))
                    .andExpect(jsonPath("$.data[1].skippedByUser").value("user2"));
        }

        @Test
        @DisplayName("Should return empty list when no skipped tasks")
        void shouldReturnEmptyListWhenNoSkippedTasks() throws Exception {
            // Given
            when(taskInstanceService.getSkippedTasks(workflowId)).thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/workflows/tasks/workflow/{id}/skipped", workflowId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Use Case Tests")
    class UseCaseTests {

        @Test
        @DisplayName("Use Case: Blood test already performed elsewhere")
        void useCaseBloodTestPerformedElsewhere() throws Exception {
            // Given
            requiredTask.setStatus(TaskStatus.SKIPPED);
            requiredTask.setSkipReason("Blood test already performed at LabCorp - results in patient file");
            requiredTask.setSkippedByUser("dr-johnson");
            requiredTask.setCompletedAt(LocalDateTime.now());
            requiredTask.setComments("REQUIRED TASK SKIPPED by dr-johnson. Reason: Blood test already performed");

            when(taskInstanceService.skipTaskWithReason(any(), any(), any(), eq(true)))
                    .thenReturn(requiredTask);

            String requestBody = """
                {
                    "reason": "Blood test already performed at LabCorp - results in patient file",
                    "skippedByUser": "dr-johnson",
                    "forceSkip": true
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SKIPPED"))
                    .andExpect(jsonPath("$.data.skipReason").value(containsString("LabCorp")));
        }

        @Test
        @DisplayName("Use Case: Patient refused procedure")
        void useCasePatientRefused() throws Exception {
            // Given
            requiredTask.setStatus(TaskStatus.SKIPPED);
            requiredTask.setSkipReason("Patient refused after informed consent discussion - documented in chart");
            requiredTask.setSkippedByUser("dr-smith");
            requiredTask.setCompletedAt(LocalDateTime.now());

            when(taskInstanceService.skipTaskWithReason(any(), any(), any(), eq(true)))
                    .thenReturn(requiredTask);

            String requestBody = """
                {
                    "reason": "Patient refused after informed consent discussion - documented in chart",
                    "skippedByUser": "dr-smith",
                    "forceSkip": true
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.skipReason").value(containsString("Patient refused")));
        }

        @Test
        @DisplayName("Use Case: Clinical judgment override")
        void useCaseClinicalJudgmentOverride() throws Exception {
            // Given
            requiredTask.setStatus(TaskStatus.SKIPPED);
            requiredTask.setSkipReason("Clinical judgment - contraindicated due to patient's renal function");
            requiredTask.setSkippedByUser("attending-physician");
            requiredTask.setCompletedAt(LocalDateTime.now());

            when(taskInstanceService.skipTaskWithReason(any(), any(), any(), eq(true)))
                    .thenReturn(requiredTask);

            String requestBody = """
                {
                    "reason": "Clinical judgment - contraindicated due to patient's renal function",
                    "skippedByUser": "attending-physician",
                    "forceSkip": true
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.skipReason").value(containsString("Clinical judgment")));
        }
    }

    @Nested
    @DisplayName("Response DTO Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should include all skip-related fields in response")
        void shouldIncludeAllSkipFieldsInResponse() throws Exception {
            // Given
            optionalTask.setStatus(TaskStatus.SKIPPED);
            optionalTask.setSkipReason("Detailed reason for skipping");
            optionalTask.setSkippedByUser("user-who-skipped");
            optionalTask.setCompletedAt(LocalDateTime.now());
            optionalTask.setCreatedAt(LocalDateTime.now().minusHours(1));
            optionalTask.setUpdatedAt(LocalDateTime.now());

            when(taskInstanceService.skipTaskWithReason(any(), any(), any(), anyBoolean()))
                    .thenReturn(optionalTask);

            String requestBody = """
                {
                    "reason": "Detailed reason for skipping",
                    "skippedByUser": "user-who-skipped",
                    "forceSkip": false
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/tasks/{id}/skip-with-reason", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.taskInstanceId").exists())
                    .andExpect(jsonPath("$.data.status").value("SKIPPED"))
                    .andExpect(jsonPath("$.data.skipReason").value("Detailed reason for skipping"))
                    .andExpect(jsonPath("$.data.skippedByUser").value("user-who-skipped"))
                    .andExpect(jsonPath("$.data.completedAt").exists())
                    .andExpect(jsonPath("$.data.workflowInstanceId").exists())
                    .andExpect(jsonPath("$.data.taskName").exists())
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.updatedAt").exists());
        }
    }

    // Helper methods
    private TaskInstance createTask(WorkflowTaskDefinition taskDef, TaskStatus status) {
        TaskInstance task = new TaskInstance();
        task.setId(taskId);
        task.setTaskInstanceId(UUID.randomUUID().toString());
        task.setTaskDefinition(taskDef);
        task.setWorkflowInstance(workflow);
        task.setStatus(status);
        task.setIsAdhoc(false);
        task.setMaxRetries(3);
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private TaskInstance createSkippedTask(String name, String reason, String skippedBy) {
        WorkflowTaskDefinition taskDef = new WorkflowTaskDefinition();
        taskDef.setId(UUID.randomUUID());
        taskDef.setName(name);
        taskDef.setIsOptional(true);

        TaskInstance task = createTask(taskDef, TaskStatus.SKIPPED);
        task.setSkipReason(reason);
        task.setSkippedByUser(skippedBy);
        task.setCompletedAt(LocalDateTime.now());
        return task;
    }
}
