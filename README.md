# HMIS Workflow Engine

A comprehensive Spring Boot 3.4 workflow orchestration platform for Healthcare Management Information Systems, built with JDK 21.

## Quick Start

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

- API Docs: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

## Features

✅ **Workflow Template Management**
- Versioned, governed templates with task definitions
- Gates and checklists for formal checkpoints
- Conditional routing with decision logic
- Order sets bundling orders/tasks/instructions

✅ **Workflow Execution Engine**
- Patient workflow instances with automatic task creation
- 8-state order lifecycle (Proposed → Closed/Cancelled)
- Task assignment, escalation, and SLA tracking
- Retry logic and failure compensation
- **Optional predecessor dependencies** (see Task Dependency Model below)

✅ **Event-Driven Architecture**
- Kafka-based event publishing for all state changes
- **Automatic task propagation** when tasks complete (no manual trigger needed)
- Task lifecycle events (created, started, completed, failed) with auto-retry on failure
- Order lifecycle events with automatic compensation action execution
- Workflow state change events with escalation management
- System events for operational monitoring and health checks
- 4 consumer services processing events in real-time

✅ **Observability & Compliance**
- Immutable audit log for all actions
- Correlation IDs for distributed tracing
- SLA breach detection and escalation
- Legal hold support for 7-10 year retention

## Project Structure

```
src/main/java/com/hmis/workflow/
├── config/              # Kafka and application configuration
├── domain/              # Domain models and enums
├── repository/          # Spring Data JPA repositories
├── service/             # Business logic services
└── kafka/              # Event producers
```

## Technology Stack

- Spring Boot 3.4.0
- JDK 21
- H2 Database
- Hibernate/JPA
- Apache Kafka
- Maven

## Task Dependency Model

The workflow engine supports **flexible task dependencies** with optional predecessors:

### Dependency Types

| Type | Description | predecessorTaskIds | Behavior |
|------|-------------|-------------------|----------|
| **Entry Task** | No predecessors | `null` or `[]` | Starts immediately when workflow begins |
| **Dependent Task** | Has predecessors | `["task-1", "task-2"]` | Waits until ALL predecessors complete |

### Task Status Flow

```
Entry Tasks:    PENDING → IN_PROGRESS → COMPLETED
                  ↓
Dependent Tasks: BLOCKED → PENDING → IN_PROGRESS → COMPLETED
                            (when predecessors complete)
```

### Example Workflow

```json
{
  "tasks": [
    {
      "name": "Initial Assessment",
      "predecessorTaskIds": null       // Entry task - starts immediately
    },
    {
      "name": "Lab Order",
      "predecessorTaskIds": null       // Entry task - runs in parallel
    },
    {
      "name": "Review Results",
      "predecessorTaskIds": ["task-1", "task-2"]  // Waits for both to complete
    },
    {
      "name": "Discharge Planning",
      "predecessorTaskIds": ["task-3"]  // Waits for Review Results
    }
  ]
}
```

### Key Features

- **Optional Predecessors**: Tasks without predecessors are entry points
- **Multiple Predecessors**: Supports AND-join (all predecessors must complete)
- **Automatic Unblocking**: When predecessors complete, dependent tasks become PENDING
- **SLA Calculation**: SLA timer starts when task becomes PENDING (not workflow start)
- **Backward Compatible**: Legacy `nextTaskId` still works alongside predecessors

## Implementation Status

### ✅ Completed
- Domain model design aligned with HMIS guidelines
- Repository layer for data access
- Service layer with core business logic
- Kafka configuration and event producers
- **REST API controllers for all major entities (44 endpoints)**
- **Kafka event consumers for automatic task propagation**
  - `TaskEventConsumer`: Auto-progresses tasks through workflow definitions
  - `OrderEventConsumer`: Processes order lifecycle and compensation actions
  - `WorkflowEventConsumer`: Manages workflow completion and escalation
  - `SystemEventConsumer`: Monitors system health and errors
- **Scheduled SLA monitoring service** (5-minute checks for deadline breaches)
- Comprehensive documentation

### 📋 Planned
- Module integration adapters (Lab, Imaging, Pharmacy, ADT)
- UI dashboard and designer
- Event sourcing and audit event replay
- Advanced routing and content-based filtering

## REST API Endpoints for UI Development

Complete REST API with 44 endpoints:

| Component | Count | Path |
|-----------|-------|------|
| Workflow Templates | 15 | `/workflows/templates/*` |
| Workflow Instances | 6 | `/workflows/instances/*` |
| Task Instances | 12 | `/workflows/tasks/*` |
| Orders | 11 | `/workflows/orders/*` |

**Key Capabilities:**
- Create, manage, and publish workflow templates
- Create patient workflow instances with automatic task generation
- Assign and execute individual tasks with SLA tracking
- Manage orders through 8-state lifecycle
- Handle task/workflow escalation and failures
- Auto-create compensation actions on order cancellation

**Access APIs:**
- Interactive Swagger UI: `http://localhost:8080/swagger-ui.html`
- REST Endpoints: `http://localhost:8080/api/v1`

## Event-Driven Workflow Execution

The workflow engine uses **Kafka event consumers** for automatic task progression:

1. **Task Completion Event** → `TaskEventConsumer` processes completion
2. **Automatic Next Task Trigger** → Finds next task from workflow definition
3. **Status Propagation** → Updates workflow status in real-time
4. **Failure Handling** → Routes to failure tasks or auto-retries
5. **Escalation** → Auto-escalates breached SLAs and stuck workflows

This enables workflows to **progress autonomously without manual intervention** while maintaining full audit trails.

**Example Flow:**
```
POST /workflows/tasks/123/complete
  ↓
TaskInstanceService completes task
  ↓
TaskEventProducer publishes TASK_COMPLETED to Kafka
  ↓
TaskEventConsumer receives event
  ↓
Finds next task from taskDef.nextTaskId
  ↓
Next task becomes available for assignment
  ↓
(If next task is automatic, repeats cycle)
```

## Documentation

- **[KAFKA_EVENT_DRIVEN_ARCHITECTURE.md](KAFKA_EVENT_DRIVEN_ARCHITECTURE.md)** - Complete event consumer architecture (2000+ lines with examples)
- **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - Complete REST API reference (44 endpoints, 50+ examples)
- **[README_IMPLEMENTATION.md](README_IMPLEMENTATION.md)** - Architecture and design documentation
- **[DATABASE_STRUCTURE.md](DATABASE_STRUCTURE.md)** - Comprehensive database schema guide
- **Swagger UI** - Interactive API explorer at `http://localhost:8080/swagger-ui.html`

## Guided by HMIS Workflow Guidelines

This implementation follows comprehensive guidelines covering:
- Order lifecycle and clinical workflows
- Task management with SLA tracking
- Exception handling and compensation
- Integration with hospital modules
- Audit and compliance requirements

Refer to https://github.com/sriniaravamuthan/workflowguidelinesdocs for detailed guidelines.
