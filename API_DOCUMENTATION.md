# HMIS Workflow Engine - API Documentation

Complete REST API documentation for the HMIS Workflow Engine. All endpoints return a consistent `ApiResponse` wrapper with success status, message, data, and error details.

## Base URL

```
http://localhost:8080/api/v1
```

## Response Format

All endpoints return responses in the following format:

### Success Response
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {},
  "timestamp": "2024-11-10T12:00:00"
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error message",
  "error": {
    "errorCode": "ERROR_CODE",
    "errorMessage": "Detailed error message",
    "details": "Additional details",
    "timestamp": "2024-11-10T12:00:00"
  },
  "timestamp": "2024-11-10T12:00:00"
}
```

---

## Workflow Template Management

Endpoints for creating, managing, and publishing workflow templates.

### Create Workflow Template
**POST** `/workflows/templates`

Create a new workflow template in DRAFT status.

**Request:**
```json
{
  "name": "Emergency Admission Workflow",
  "description": "Workflow for emergency patient admission process",
  "category": "Emergency"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Workflow template created successfully",
  "data": {
    "id": "uuid",
    "name": "Emergency Admission Workflow",
    "description": "Workflow for emergency patient admission process",
    "active": true,
    "version": 1,
    "category": "Emergency",
    "tasks": [],
    "createdAt": "2024-11-10T12:00:00",
    "updatedAt": "2024-11-10T12:00:00"
  }
}
```

---

### Get All Templates
**GET** `/workflows/templates`

Retrieve all active workflow templates.

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Templates retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "name": "Template Name",
      "description": "Description",
      "active": true,
      "version": 1,
      "category": "Category",
      "tasks": [],
      "createdAt": "2024-11-10T12:00:00",
      "updatedAt": "2024-11-10T12:00:00"
    }
  ]
}
```

---

### Get Published Templates
**GET** `/workflows/templates/published`

Retrieve all published and active templates. These are the templates available for creating workflow instances.

**Response:** `200 OK`

Same structure as "Get All Templates" but only includes published templates.

---

### Get Templates by Category
**GET** `/workflows/templates/category/{category}`

Retrieve templates filtered by category.

**Parameters:**
- `category` (path): Category name (e.g., "Emergency", "Outpatient", "Inpatient")

**Response:** `200 OK`

Same structure as "Get All Templates".

---

### Get Template Details
**GET** `/workflows/templates/{id}`

Retrieve detailed information about a specific template including all tasks.

**Parameters:**
- `id` (path): Template UUID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Template retrieved successfully",
  "data": {
    "id": "uuid",
    "name": "Emergency Admission Workflow",
    "description": "...",
    "active": true,
    "version": 1,
    "category": "Emergency",
    "tasks": [
      {
        "id": "uuid",
        "name": "Triage",
        "description": "Initial patient triage",
        "taskOrder": 1,
        "assignTo": "nurse",
        "estimatedDurationMinutes": 15,
        "instructions": "Perform ESI triage assessment",
        "isParallel": false,
        "isOptional": false,
        "nextTaskId": null,
        "failureTaskId": null
      }
    ],
    "createdAt": "2024-11-10T12:00:00",
    "updatedAt": "2024-11-10T12:00:00"
  }
}
```

---

### Update Template
**PUT** `/workflows/templates/{id}`

Update template details. Only templates in DRAFT status can be updated.

**Parameters:**
- `id` (path): Template UUID

**Request:**
```json
{
  "name": "Updated Name",
  "description": "Updated description",
  "category": "Updated Category",
  "notes": "Internal notes"
}
```

**Response:** `200 OK`

Same as Get Template Details response.

---

### Delete Template
**DELETE** `/workflows/templates/{id}`

Delete a template. Only DRAFT templates can be deleted.

**Parameters:**
- `id` (path): Template UUID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Template deleted successfully",
  "data": "Template deleted successfully"
}
```

---

### Submit Template for Review
**POST** `/workflows/templates/{id}/submit-review`

Submit a DRAFT template for review. Transitions status from DRAFT to IN_REVIEW.

**Parameters:**
- `id` (path): Template UUID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Template submitted for review",
  "data": {
    "id": "uuid",
    "name": "Emergency Admission Workflow",
    "description": "...",
    "active": true,
    "version": 1,
    "category": "Emergency",
    "tasks": [],
    "createdAt": "2024-11-10T12:00:00",
    "updatedAt": "2024-11-10T12:00:00"
  }
}
```

---

### Approve Template
**POST** `/workflows/templates/{id}/approve`

Approve an IN_REVIEW template. Transitions status from IN_REVIEW to APPROVED.

**Parameters:**
- `id` (path): Template UUID

**Request (Optional):**
```json
{
  "approvedBy": "admin.user"
}
```

**Response:** `200 OK`

---

### Publish Template
**POST** `/workflows/templates/{id}/publish`

Publish an APPROVED template, making it available for workflow creation. Transitions status from APPROVED to PUBLISHED.

**Parameters:**
- `id` (path): Template UUID

**Response:** `200 OK`

---

### Create New Template Version
**POST** `/workflows/templates/{id}/version`

Create a new version of a published template. The original template remains PUBLISHED, and a new DRAFT version is created with all tasks copied over.

**Parameters:**
- `id` (path): Template UUID

**Response:** `201 Created`

Returns the new template version with version number incremented.

---

### Deprecate Template
**POST** `/workflows/templates/{id}/deprecate`

Mark a template as deprecated. Transitions status to DEPRECATED and sets active to false.

**Parameters:**
- `id` (path): Template UUID

**Response:** `200 OK`

---

### Add Task to Template
**POST** `/workflows/templates/{id}/tasks`

Add a task definition to a template. Only DRAFT templates can have tasks added/modified.

**Parameters:**
- `id` (path): Template UUID

**Request:**
```json
{
  "name": "Physician Consultation",
  "description": "Doctor consultation with patient",
  "assignTo": "physician",
  "estimatedDurationMinutes": 30,
  "instructions": "Conduct thorough medical examination and diagnosis",
  "isParallel": false,
  "isOptional": false,
  "nextTaskId": null,
  "failureTaskId": null,
  "metadata": {
    "priority": "high",
    "requiresApproval": true
  }
}
```

**Response:** `201 Created`

Returns the updated template with the new task included.

---

### Get Template Tasks
**GET** `/workflows/templates/{id}/tasks`

Retrieve all tasks defined in a template.

**Parameters:**
- `id` (path): Template UUID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Tasks retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "name": "Triage",
      "description": "Initial patient triage",
      "taskOrder": 1,
      "assignTo": "nurse",
      "estimatedDurationMinutes": 15,
      "instructions": "Perform ESI triage assessment",
      "isParallel": false,
      "isOptional": false,
      "nextTaskId": null,
      "failureTaskId": null
    }
  ]
}
```

---

### Delete Task from Template
**DELETE** `/workflows/templates/{id}/tasks/{taskId}`

Remove a task from a template. Only DRAFT templates can have tasks removed.

**Parameters:**
- `id` (path): Template UUID
- `taskId` (path): Task Definition UUID

**Response:** `200 OK`

---

## Workflow Instance Management

Endpoints for creating and managing patient-specific workflow instances.

### Create Workflow Instance
**POST** `/workflows/instances`

Create a new workflow instance for a patient using a published template. Automatically creates task instances for all template tasks with SLA calculations.

**Request:**
```json
{
  "patientId": "uuid",
  "templateId": "uuid"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Workflow instance created successfully",
  "data": {
    "id": "uuid",
    "workflowInstanceId": "workflow-123-abc",
    "status": "ACTIVE",
    "notes": null,
    "startedAt": "2024-11-10T12:00:00",
    "completedAt": null,
    "patientId": "uuid",
    "patientName": "John Doe",
    "templateId": "uuid",
    "templateName": "Emergency Admission Workflow",
    "taskInstances": [],
    "progressPercentage": 0,
    "createdAt": "2024-11-10T12:00:00",
    "updatedAt": "2024-11-10T12:00:00"
  }
}
```

---

### Get Workflow Instance
**GET** `/workflows/instances/{id}`

Retrieve details of a specific workflow instance.

**Parameters:**
- `id` (path): Workflow Instance UUID

**Response:** `200 OK`

Same structure as Create Workflow Instance response.

---

### Get Patient Workflows
**GET** `/workflows/instances/patient/{patientId}`

Retrieve all workflow instances for a patient.

**Parameters:**
- `patientId` (path): Patient UUID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Patient workflows retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "workflowInstanceId": "workflow-123-abc",
      "status": "ACTIVE",
      "notes": null,
      "startedAt": "2024-11-10T12:00:00",
      "completedAt": null,
      "patientId": "uuid",
      "patientName": "John Doe",
      "templateId": "uuid",
      "templateName": "Emergency Admission Workflow",
      "taskInstances": [],
      "progressPercentage": 45,
      "createdAt": "2024-11-10T12:00:00",
      "updatedAt": "2024-11-10T12:00:00"
    }
  ]
}
```

---

### Get Active Patient Workflows
**GET** `/workflows/instances/patient/{patientId}/active`

Retrieve only active workflow instances for a patient.

**Parameters:**
- `patientId` (path): Patient UUID

**Response:** `200 OK`

Same structure as Get Patient Workflows.

---

### Get Escalated Workflows
**GET** `/workflows/instances/escalated`

Retrieve all escalated workflow instances across all patients.

**Response:** `200 OK`

---

### Pause Workflow
**POST** `/workflows/instances/{id}/pause`

Pause an active workflow instance. Transitions status from ACTIVE to PAUSED.

**Parameters:**
- `id` (path): Workflow Instance UUID

**Response:** `200 OK`

---

### Resume Workflow
**POST** `/workflows/instances/{id}/resume`

Resume a paused workflow instance. Transitions status from PAUSED to ACTIVE.

**Parameters:**
- `id` (path): Workflow Instance UUID

**Response:** `200 OK`

---

### Cancel Workflow
**POST** `/workflows/instances/{id}/cancel`

Cancel a workflow instance and all associated tasks.

**Parameters:**
- `id` (path): Workflow Instance UUID

**Request:**
```json
{
  "reason": "Patient refused treatment"
}
```

**Response:** `200 OK`

---

### Complete Workflow
**POST** `/workflows/instances/{id}/complete`

Complete a workflow instance. All required tasks must be completed.

**Parameters:**
- `id` (path): Workflow Instance UUID

**Response:** `200 OK`

---

### Escalate Workflow
**POST** `/workflows/instances/{id}/escalate`

Escalate a workflow instance for urgent attention.

**Parameters:**
- `id` (path): Workflow Instance UUID

**Request:**
```json
{
  "reason": "Patient condition deteriorated"
}
```

**Response:** `200 OK`

---

## Task Instance Management

Endpoints for managing individual task instances within workflows.

### Get Task Instance
**GET** `/workflows/tasks/{id}`

Retrieve details of a specific task instance.

**Parameters:**
- `id` (path): Task Instance UUID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Task instance retrieved successfully",
  "data": {
    "id": "uuid",
    "taskInstanceId": "task-123-abc",
    "status": "PENDING",
    "assignedTo": "nurse.smith",
    "startedAt": null,
    "completedAt": null,
    "comments": null,
    "result": null,
    "retryCount": 0,
    "maxRetries": 3,
    "errorMessage": null,
    "workflowInstanceId": "uuid",
    "taskName": "Triage",
    "taskDescription": "Initial patient triage",
    "createdAt": "2024-11-10T12:00:00",
    "updatedAt": "2024-11-10T12:00:00"
  }
}
```

---

### Get Workflow Tasks
**GET** `/workflows/tasks/workflow/{workflowInstanceId}`

Retrieve all tasks in a workflow instance.

**Parameters:**
- `workflowInstanceId` (path): Workflow Instance UUID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Workflow tasks retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "taskInstanceId": "task-123-abc",
      "status": "PENDING",
      "assignedTo": "nurse.smith",
      "startedAt": null,
      "completedAt": null,
      "comments": null,
      "result": null,
      "retryCount": 0,
      "maxRetries": 3,
      "errorMessage": null,
      "workflowInstanceId": "uuid",
      "taskName": "Triage",
      "taskDescription": "Initial patient triage",
      "createdAt": "2024-11-10T12:00:00",
      "updatedAt": "2024-11-10T12:00:00"
    }
  ]
}
```

---

### Get Assigned Tasks
**GET** `/workflows/tasks/assigned-to/{assignedTo}`

Retrieve all tasks assigned to a specific user (pending and in-progress).

**Parameters:**
- `assignedTo` (path): Username or user ID

**Response:** `200 OK`

---

### Assign Task
**POST** `/workflows/tasks/{id}/assign`

Assign a task to a user. Task must be in PENDING status.

**Parameters:**
- `id` (path): Task Instance UUID

**Request:**
```json
{
  "assignedTo": "nurse.smith"
}
```

**Response:** `200 OK`

---

### Start Task
**POST** `/workflows/tasks/{id}/start`

Start a task (transition from PENDING to IN_PROGRESS).

**Parameters:**
- `id` (path): Task Instance UUID

**Request (Optional):**
```json
{
  "startedByUser": "nurse.smith"
}
```

**Response:** `200 OK`

---

### Complete Task
**POST** `/workflows/tasks/{id}/complete`

Complete a task with result data. Activates next tasks in workflow.

**Parameters:**
- `id` (path): Task Instance UUID

**Request:**
```json
{
  "result": "Triage completed - ESI Level 3",
  "completedByUser": "nurse.smith"
}
```

**Response:** `200 OK`

---

### Fail Task
**POST** `/workflows/tasks/{id}/fail`

Mark a task as failed with error message.

**Parameters:**
- `id` (path): Task Instance UUID

**Request:**
```json
{
  "errorMessage": "Equipment malfunction during test",
  "failedByUser": "tech.john"
}
```

**Response:** `200 OK`

---

### Retry Task
**POST** `/workflows/tasks/{id}/retry`

Retry a failed task (if retries remaining). Resets task to PENDING status.

**Parameters:**
- `id` (path): Task Instance UUID

**Response:** `200 OK`

---

### Escalate Task
**POST** `/workflows/tasks/{id}/escalate`

Escalate a task to another user for urgent attention.

**Parameters:**
- `id` (path): Task Instance UUID

**Request:**
```json
{
  "escalatedToUser": "supervisor.jane",
  "reason": "Patient critical condition"
}
```

**Response:** `200 OK`

---

### Skip Optional Task
**POST** `/workflows/tasks/{id}/skip`

Skip an optional task. Only works for tasks marked as optional.

**Parameters:**
- `id` (path): Task Instance UUID

**Response:** `200 OK`

---

### Get SLA-Breached Tasks
**GET** `/workflows/tasks/sla/breached`

Retrieve all tasks where SLA deadline has been exceeded.

**Response:** `200 OK`

---

### Get Retryable Tasks
**GET** `/workflows/tasks/retryable`

Retrieve all failed tasks that can still be retried.

**Response:** `200 OK`

---

### Update Task Comments
**PUT** `/workflows/tasks/{id}/comments`

Add or update comments on a task instance.

**Parameters:**
- `id` (path): Task Instance UUID

**Request:**
```json
{
  "comments": "Patient responsive, vital signs stable"
}
```

**Response:** `200 OK`

---

## Order Management

Endpoints for managing orders within workflow instances.

### Create Order
**POST** `/workflows/orders`

Create a new order in a workflow instance.

**Request:**
```json
{
  "orderType": "LAB_TEST",
  "orderDescription": "Complete Blood Count (CBC)",
  "orderCode": "CBC001",
  "departmentTarget": "LABORATORY",
  "workflowInstanceId": "uuid",
  "estimatedCost": 150.00,
  "priority": 0
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Order created successfully",
  "data": {
    "id": "uuid",
    "orderId": "order-123-abc",
    "status": "PROPOSED",
    "orderType": "LAB_TEST",
    "orderDescription": "Complete Blood Count (CBC)",
    "orderCode": "CBC001",
    "departmentTarget": "LABORATORY",
    "orderedByUser": null,
    "authorizedByUser": null,
    "authorizedAt": null,
    "activatedAt": null,
    "resultedAt": null,
    "verifiedAt": null,
    "closedAt": null,
    "cancelledAt": null,
    "result": null,
    "estimatedCost": 150.00,
    "actualCost": null,
    "priority": 0,
    "createdAt": "2024-11-10T12:00:00",
    "updatedAt": "2024-11-10T12:00:00"
  }
}
```

---

### Get Order
**GET** `/workflows/orders/{id}`

Retrieve order details.

**Parameters:**
- `id` (path): Order UUID

**Response:** `200 OK`

---

### Get Workflow Orders
**GET** `/workflows/orders/workflow/{workflowInstanceId}`

Retrieve all orders in a workflow instance.

**Parameters:**
- `workflowInstanceId` (path): Workflow Instance UUID

**Response:** `200 OK`

---

### Get Open Orders
**GET** `/workflows/orders/status/open`

Retrieve all open (non-closed, non-cancelled) orders.

**Response:** `200 OK`

---

### Authorize Order
**POST** `/workflows/orders/{id}/authorize`

Authorize an order (PROPOSED → AUTHORIZED). Typically done by clinician.

**Parameters:**
- `id` (path): Order UUID

**Request:**
```json
{
  "authorizedByUser": "dr.smith"
}
```

**Response:** `200 OK`

---

### Activate Order
**POST** `/workflows/orders/{id}/activate`

Activate an order (AUTHORIZED → ACTIVATED). Transmit to department.

**Parameters:**
- `id` (path): Order UUID

**Response:** `200 OK`

---

### Start Order
**POST** `/workflows/orders/{id}/start`

Mark order as in progress (ACTIVATED → IN_PROGRESS).

**Parameters:**
- `id` (path): Order UUID

**Response:** `200 OK`

---

### Record Order Result
**POST** `/workflows/orders/{id}/result`

Record result/completion of order.

**Parameters:**
- `id` (path): Order UUID

**Request:**
```json
{
  "result": "WBC: 7.5, RBC: 4.8, HGB: 14.2"
}
```

**Response:** `200 OK`

---

### Verify Order
**POST** `/workflows/orders/{id}/verify`

Verify order result (clinician review and acceptance).

**Parameters:**
- `id` (path): Order UUID

**Request:**
```json
{
  "verifiedByUser": "dr.smith"
}
```

**Response:** `200 OK`

---

### Close Order
**POST** `/workflows/orders/{id}/close`

Close completed and verified order.

**Parameters:**
- `id` (path): Order UUID

**Response:** `200 OK`

---

### Cancel Order
**POST** `/workflows/orders/{id}/cancel`

Cancel an order. Automatically creates compensation actions (charge reversal, notifications).

**Parameters:**
- `id` (path): Order UUID

**Request:**
```json
{
  "reason": "Patient refused test"
}
```

**Response:** `200 OK`

---

### Get Orders with Results
**GET** `/workflows/orders/status/resulted`

Retrieve all orders with recorded results.

**Response:** `200 OK`

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| INVALID_ARGUMENT | 400 | Invalid request parameter |
| INVALID_STATE | 409 | Invalid state transition |
| RESOURCE_NOT_FOUND | 404 | Resource not found |
| VALIDATION_ERROR | 400 | Validation failed |
| INTERNAL_SERVER_ERROR | 500 | Unexpected server error |

---

## Swagger/OpenAPI Documentation

Access the interactive API documentation at:

```
http://localhost:8080/swagger-ui.html
```

This provides an interactive interface to test all endpoints directly from the browser.

---

## Example Workflow: Creating and Executing a Template

### Step 1: Create Template
```bash
POST /api/v1/workflows/templates
{
  "name": "Emergency Admission",
  "description": "Emergency patient admission workflow",
  "category": "Emergency"
}
```

### Step 2: Add Tasks to Template
```bash
POST /api/v1/workflows/templates/{templateId}/tasks
{
  "name": "Triage",
  "description": "Initial triage",
  "assignTo": "nurse",
  "estimatedDurationMinutes": 15,
  "isParallel": false,
  "isOptional": false
}
```

### Step 3: Submit for Review
```bash
POST /api/v1/workflows/templates/{templateId}/submit-review
```

### Step 4: Approve
```bash
POST /api/v1/workflows/templates/{templateId}/approve
```

### Step 5: Publish
```bash
POST /api/v1/workflows/templates/{templateId}/publish
```

### Step 6: Create Workflow Instance
```bash
POST /api/v1/workflows/instances
{
  "patientId": "{patientId}",
  "templateId": "{templateId}"
}
```

### Step 7: Execute Tasks
```bash
POST /api/v1/workflows/tasks/{taskId}/assign
{
  "assignedTo": "nurse.smith"
}

POST /api/v1/workflows/tasks/{taskId}/start
{
  "startedByUser": "nurse.smith"
}

POST /api/v1/workflows/tasks/{taskId}/complete
{
  "result": "Triage completed - ESI Level 3",
  "completedByUser": "nurse.smith"
}
```

---

## Performance Considerations

- Templates should be published (not modified in production)
- Workflow instances are immutable once created
- SLA calculations are done at task creation time
- Kafka events are published asynchronously
- Large workflows (100+ tasks) should be split into sub-workflows
- Use pagination for list endpoints returning large datasets

---

## Rate Limiting

Currently no rate limiting is enforced. For production, implement:
- Per-user rate limits (e.g., 100 requests/minute)
- Per-IP rate limits
- Graduated backoff for retries

---

## Authentication & Authorization

Currently no authentication is implemented. For production, add:
- JWT token-based authentication
- Role-based access control (RBAC)
- Audit logging for all operations
