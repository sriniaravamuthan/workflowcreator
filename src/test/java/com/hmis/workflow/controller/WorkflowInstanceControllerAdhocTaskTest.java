package com.hmis.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmis.workflow.domain.entity.Patient;
import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.entity.WorkflowTemplate;
import com.hmis.workflow.domain.enums.TaskStatus;
import com.hmis.workflow.domain.enums.WorkflowStatus;
import com.hmis.workflow.service.WorkflowInstanceService;
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
 * Integration tests for WorkflowInstanceController ad-hoc task endpoints.
 *
 * Tests cover:
 * - POST /workflows/instances/{id}/adhoc-task
 * - GET /workflows/instances/{id}/adhoc-tasks
 * - Request validation
 * - Response structure
 * - Error handling
 */
@WebMvcTest(WorkflowInstanceController.class)
@DisplayName("WorkflowInstanceController - Ad-hoc Task API Tests")
class WorkflowInstanceControllerAdhocTaskTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkflowInstanceService workflowInstanceService;

    private UUID workflowId;
    private WorkflowInstance workflow;
    private Patient patient;
    private WorkflowTemplate template;

    @BeforeEach
    void setUp() {
        workflowId = UUID.randomUUID();

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setPatientId("P-12345");

        template = new WorkflowTemplate();
        template.setId(UUID.randomUUID());
        template.setName("Test Template");

        workflow = new WorkflowInstance();
        workflow.setId(workflowId);
        workflow.setWorkflowInstanceId("WF-001");
        workflow.setStatus(WorkflowStatus.ACTIVE);
        workflow.setPatient(patient);
        workflow.setTemplate(template);
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("POST /workflows/instances/{id}/adhoc-task")
    class AddAdhocTaskEndpointTests {

        @Test
        @DisplayName("Should create ad-hoc task with all parameters")
        void shouldCreateAdhocTaskWithAllParameters() throws Exception {
            // Given
            TaskInstance createdTask = createAdhocTask(
                    "Administer Saline",
                    "IV saline solution 500ml over 2 hours",
                    "nurse-001",
                    "doctor-smith",
                    60
            );

            when(workflowInstanceService.addAdhocTask(
                    eq(workflowId),
                    eq("Administer Saline"),
                    eq("IV saline solution 500ml over 2 hours"),
                    eq("nurse-001"),
                    eq("doctor-smith"),
                    eq(60)
            )).thenReturn(createdTask);

            String requestBody = """
                {
                    "taskName": "Administer Saline",
                    "taskDescription": "IV saline solution 500ml over 2 hours",
                    "assignTo": "nurse-001",
                    "createdByUser": "doctor-smith",
                    "slaMinutes": 60
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/instances/{id}/adhoc-task", workflowId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Ad-hoc task created successfully"))
                    .andExpect(jsonPath("$.data.taskName").value("Administer Saline"))
                    .andExpect(jsonPath("$.data.taskDescription").value("IV saline solution 500ml over 2 hours"))
                    .andExpect(jsonPath("$.data.assignedTo").value("nurse-001"))
                    .andExpect(jsonPath("$.data.isAdhoc").value(true))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should create ad-hoc task with minimal parameters")
        void shouldCreateAdhocTaskWithMinimalParameters() throws Exception {
            // Given
            TaskInstance createdTask = createAdhocTask(
                    "Check Vitals", null, null, "nurse-001", null
            );

            when(workflowInstanceService.addAdhocTask(
                    eq(workflowId),
                    eq("Check Vitals"),
                    isNull(),
                    isNull(),
                    eq("nurse-001"),
                    isNull()
            )).thenReturn(createdTask);

            String requestBody = """
                {
                    "taskName": "Check Vitals",
                    "createdByUser": "nurse-001"
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/instances/{id}/adhoc-task", workflowId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.taskName").value("Check Vitals"));
        }

        @Test
        @DisplayName("Should return 400 when workflow not found")
        void shouldReturn400WhenWorkflowNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(workflowInstanceService.addAdhocTask(
                    eq(nonExistentId), any(), any(), any(), any(), any()
            )).thenThrow(new IllegalArgumentException("Workflow instance not found: " + nonExistentId));

            String requestBody = """
                {
                    "taskName": "Task",
                    "createdByUser": "user"
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/instances/{id}/adhoc-task", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when workflow is not active")
        void shouldReturn400WhenWorkflowNotActive() throws Exception {
            // Given
            when(workflowInstanceService.addAdhocTask(
                    eq(workflowId), any(), any(), any(), any(), any()
            )).thenThrow(new IllegalStateException("Cannot add ad-hoc task to non-active workflow"));

            String requestBody = """
                {
                    "taskName": "Task",
                    "createdByUser": "user"
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/instances/{id}/adhoc-task", workflowId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when task name is missing")
        void shouldReturn400WhenTaskNameMissing() throws Exception {
            // Given
            when(workflowInstanceService.addAdhocTask(
                    eq(workflowId), isNull(), any(), any(), any(), any()
            )).thenThrow(new IllegalArgumentException("Ad-hoc task name is required"));

            String requestBody = """
                {
                    "taskDescription": "Description only",
                    "createdByUser": "user"
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/instances/{id}/adhoc-task", workflowId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle invalid UUID gracefully")
        void shouldHandleInvalidUuidGracefully() throws Exception {
            // Given
            String requestBody = """
                {
                    "taskName": "Task",
                    "createdByUser": "user"
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/instances/invalid-uuid/adhoc-task")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /workflows/instances/{id}/adhoc-tasks")
    class GetAdhocTasksEndpointTests {

        @Test
        @DisplayName("Should return list of ad-hoc tasks")
        void shouldReturnListOfAdhocTasks() throws Exception {
            // Given
            TaskInstance task1 = createAdhocTask("Task 1", "Desc 1", "user1", "creator1", 30);
            TaskInstance task2 = createAdhocTask("Task 2", "Desc 2", "user2", "creator2", 60);

            when(workflowInstanceService.getAdhocTasks(workflowId))
                    .thenReturn(List.of(task1, task2));

            // When/Then
            mockMvc.perform(get("/workflows/instances/{id}/adhoc-tasks", workflowId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].taskName").value("Task 1"))
                    .andExpect(jsonPath("$.data[0].isAdhoc").value(true))
                    .andExpect(jsonPath("$.data[1].taskName").value("Task 2"))
                    .andExpect(jsonPath("$.data[1].isAdhoc").value(true));
        }

        @Test
        @DisplayName("Should return empty list when no ad-hoc tasks")
        void shouldReturnEmptyListWhenNoAdhocTasks() throws Exception {
            // Given
            when(workflowInstanceService.getAdhocTasks(workflowId))
                    .thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/workflows/instances/{id}/adhoc-tasks", workflowId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 400 when workflow not found")
        void shouldReturn400WhenWorkflowNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(workflowInstanceService.getAdhocTasks(nonExistentId))
                    .thenThrow(new IllegalArgumentException("Workflow instance not found"));

            // When/Then
            mockMvc.perform(get("/workflows/instances/{id}/adhoc-tasks", nonExistentId))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Response DTO Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should include all ad-hoc task fields in response")
        void shouldIncludeAllAdhocTaskFieldsInResponse() throws Exception {
            // Given
            TaskInstance task = createAdhocTask(
                    "Administer Medication",
                    "Give 500mg Tylenol",
                    "nurse-001",
                    "doctor-smith",
                    45
            );
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());

            when(workflowInstanceService.addAdhocTask(any(), any(), any(), any(), any(), any()))
                    .thenReturn(task);

            String requestBody = """
                {
                    "taskName": "Administer Medication",
                    "taskDescription": "Give 500mg Tylenol",
                    "assignTo": "nurse-001",
                    "createdByUser": "doctor-smith",
                    "slaMinutes": 45
                }
                """;

            // When/Then
            mockMvc.perform(post("/workflows/instances/{id}/adhoc-task", workflowId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.taskInstanceId").exists())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.taskName").value("Administer Medication"))
                    .andExpect(jsonPath("$.data.taskDescription").value("Give 500mg Tylenol"))
                    .andExpect(jsonPath("$.data.assignedTo").value("nurse-001"))
                    .andExpect(jsonPath("$.data.isAdhoc").value(true))
                    .andExpect(jsonPath("$.data.workflowInstanceId").exists())
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.updatedAt").exists());
        }
    }

    // Helper method to create ad-hoc task instances
    private TaskInstance createAdhocTask(String name, String description, String assignTo,
                                          String createdByUser, Integer slaMinutes) {
        TaskInstance task = new TaskInstance();
        task.setId(UUID.randomUUID());
        task.setTaskInstanceId(UUID.randomUUID().toString());
        task.setIsAdhoc(true);
        task.setAdhocTaskName(name);
        task.setAdhocTaskDescription(description);
        task.setAssignedTo(assignTo);
        task.setCreatedByUser(createdByUser);
        task.setStatus(TaskStatus.PENDING);
        task.setWorkflowInstance(workflow);
        task.setMaxRetries(3);
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        if (slaMinutes != null && slaMinutes > 0) {
            task.setSlaMinutes(slaMinutes);
            task.setDueAt(LocalDateTime.now().plusMinutes(slaMinutes));
        }

        return task;
    }
}
