# HMIS Workflow Engine - Implementation Guide

A comprehensive Spring Boot 3.4 workflow orchestration engine for Healthcare Management Information Systems (HMIS) built with JDK 21, supporting template management, task execution, order lifecycle management, and Kafka-based event propagation.

## Architecture Overview

The workflow engine implements a three-part architecture:

### 1. Workflow Template Management
- **Purpose**: Define reusable workflow blueprints for clinical processes
- **Components**:
  - `WorkflowTemplate`: Versioned template definitions with governance
  - `WorkflowTaskDefinition`: Individual task definitions with SLA and assignment
  - `Gate` & `ChecklistItem`: Formal checkpoints bundling requirements
  - `DecisionLogic`: IF-THEN conditional routing based on patient data
  - `OrderSet`: Bundled orders, tasks, instructions for specific conditions

### 2. Workflow Execution Engine
- **Purpose**: Create and manage patient-specific workflow instances
- **Components**:
  - `WorkflowInstance`: Patient workflow execution context
  - `TaskInstance`: Individual task instances with SLA tracking and escalation
  - `Order`: Clinical orders with 8-state lifecycle (Proposed → Closed/Cancelled)
  - `Instruction`: Directives and guidelines (NPO, isolation, etc.)
  - `CompensationAction`: Automatic recovery actions on failure

### 3. Event-Driven Task Propagation (Kafka)
- **Purpose**: Asynchronously propagate task completions to next tasks/orders
- **Topics**:
  - `workflow-task-events`: Task lifecycle events
  - `workflow-order-events`: Order lifecycle events
  - `workflow-state-events`: Workflow state changes
  - `system-events`: System-level events

## Domain Model

### Core Entities

#### WorkflowTemplate & Gates
```
WorkflowTemplate (versioned, governed)
  ├── WorkflowTaskDefinition (ordered, with SLA)
  ├── Gate (checkpoints bundling requirements)
  │   └── ChecklistItem (individual checkpoint items)
  └── DecisionLogic (conditional routing)
```

#### OrderSet (Clinical Bundles)
```
OrderSet (clinical condition specific)
  ├── OrderSetItem (orders, tasks, instructions)
  └── OrderSetCondition (activation rules)
```

#### WorkflowInstance & Execution
```
WorkflowInstance (patient workflow)
  ├── TaskInstance (with SLA, escalation, retry logic)
  ├── Order (8-state lifecycle)
  │   └── CompensationAction (failure recovery)
  └── Instruction (blocking directives)
```

### Order Lifecycle (8 States)
```
PROPOSED
  ↓
AUTHORIZED (clinician signs)
  ↓
ACTIVATED (transmitted to department)
  ↓
IN_PROGRESS (work underway)
  ↓
RESULTED/DISPENSED/COMPLETED
  ↓
VERIFIED (clinician reviews)
  ↓
CLOSED

Alternative: CANCELLED (from any state, triggers compensation)
```

### Task Lifecycle & SLA
```
PENDING
  ↓
IN_PROGRESS (with SLA deadline and escalation)
  ↓
COMPLETED (or FAILED)

Additional: SKIPPED (for optional tasks)
          BLOCKED (waiting for prerequisites)
          EXPIRED/ESCALATED (SLA breach)
```

## Services

### WorkflowTemplateService
- Template creation, versioning, and governance
- Review workflow: DRAFT → IN_REVIEW → APPROVED → PUBLISHED
- Template deprecation and versioning support

### WorkflowInstanceService
- Create workflow instances from published templates
- Automatic task instance creation with SLA calculation
- Workflow state management (ACTIVE, PAUSED, COMPLETED, FAILED, CANCELLED)
- Task dependency tracking and workflow progression

### TaskInstanceService
- Task assignment, start, completion, and failure
- SLA monitoring and breach detection
- Task escalation with time remaining calculation
- Retry logic for failed tasks (up to maxRetries)
- Skip optional tasks
- Next task activation on completion

### OrderService
- Full 8-state order lifecycle with validation
- Order authorization, activation, and result recording
- Automatic compensation action creation on cancellation
- Compensation action execution (charge reversal, notifications)
- Order status transitions with idempotent operations

### OrderSetService
- Order set creation and management
- Clinical condition-based activation rules
- Order set versioning with latest version tracking
- Item and condition management
- Access level control (PRIVATE, TEAM, DEPARTMENT, HOSPITAL_WIDE)

## Kafka Event Architecture

### Event Publishing
```
Service Layer
    ↓
Kafka Producers (TaskEventProducer, OrderEventProducer, WorkflowEventProducer)
    ↓
Kafka Topics
    ├── workflow-task-events
    ├── workflow-order-events
    ├── workflow-state-events
    └── system-events
```

### Event Types

#### TaskEvent
```json
{
  "eventId": "uuid",
  "taskInstanceId": "task-123",
  "workflowInstanceId": "workflow-456",
  "status": "IN_PROGRESS",
  "eventType": "TASK_STARTED",
  "eventTime": "2024-11-09T12:00:00",
  "correlationId": "uuid"
}
```

#### OrderEvent
```json
{
  "eventId": "uuid",
  "orderId": "order-789",
  "workflowInstanceId": "workflow-456",
  "status": "AUTHORIZED",
  "eventType": "ORDER_AUTHORIZED",
  "actor": "dr.smith",
  "correlationId": "uuid"
}
```

#### WorkflowEvent
```json
{
  "eventId": "uuid",
  "workflowInstanceId": "workflow-456",
  "status": "ACTIVE",
  "eventType": "WORKFLOW_STARTED",
  "progressPercentage": 0,
  "correlationId": "uuid"
}
```

## Technology Stack

- **Framework**: Spring Boot 3.4.0
- **Java Version**: JDK 21
- **Database**: H2 (in-memory for demo)
- **ORM**: Hibernate/JPA
- **Message Broker**: Apache Kafka
- **Logging**: SLF4J + Logback
- **API Documentation**: OpenAPI/Swagger
- **Build**: Maven

## Project Structure

```
src/main/java/com/hmis/workflow/
├── HmisWorkflowEngineApplication.java     # Spring Boot entry point
├── config/
│   └── KafkaConfig.java                   # Kafka topic & producer configs
├── domain/
│   ├── entity/
│   │   ├── BaseEntity.java               # Audit fields (createdAt, updatedAt)
│   │   ├── Patient.java
│   │   ├── WorkflowTemplate.java
│   │   ├── WorkflowInstance.java
│   │   ├── TaskInstance.java
│   │   ├── Order.java
│   │   ├── OrderSet.java
│   │   ├── Instruction.java
│   │   ├── Gate.java
│   │   ├── DecisionLogic.java
│   │   ├── CompensationAction.java
│   │   └── AuditLog.java                 # Immutable timeline
│   ├── enums/
│   │   ├── WorkflowStatus.java
│   │   ├── TaskStatus.java
│   │   ├── OrderStatus.java
│   │   ├── OrderType.java
│   │   ├── InstructionType.java
│   │   ├── CompensationActionType.java
│   │   └── DecisionLogicOperator.java
│   └── event/
│       ├── TaskEvent.java
│       ├── OrderEvent.java
│       └── WorkflowEvent.java
├── repository/
│   ├── PatientRepository.java
│   ├── WorkflowTemplateRepository.java
│   ├── WorkflowInstanceRepository.java
│   ├── TaskInstanceRepository.java
│   ├── OrderRepository.java
│   ├── OrderSetRepository.java
│   ├── AuditLogRepository.java
│   ├── GateRepository.java
│   ├── InstructionRepository.java
│   └── CompensationActionRepository.java
├── service/
│   ├── WorkflowTemplateService.java
│   ├── WorkflowInstanceService.java
│   ├── TaskInstanceService.java
│   ├── OrderService.java
│   └── OrderSetService.java
└── kafka/
    └── producer/
        ├── TaskEventProducer.java
        ├── OrderEventProducer.java
        └── WorkflowEventProducer.java

src/main/resources/
└── application.yml                        # Configuration
```

## Key Features

### 1. Workflow Template Management
- ✅ Versioned templates with governance workflow
- ✅ Task definitions with SLA and assignment rules
- ✅ Gates and checklists for formal checkpoints
- ✅ Conditional routing with decision logic
- ✅ Order sets bundling orders/tasks/instructions
- ⏳ Drag-and-drop UI designer (next phase)
- ⏳ Natural language authoring (next phase)

### 2. Workflow Execution
- ✅ Patient workflow instances from templates
- ✅ Automatic task instance creation
- ✅ Task lifecycle management (assign, start, complete, fail)
- ✅ Task escalation and retry logic
- ✅ SLA tracking with deadline calculation
- ✅ Workflow state transitions (ACTIVE, PAUSED, COMPLETED, FAILED, CANCELLED)
- ✅ Next task activation on completion

### 3. Order Management
- ✅ 8-state order lifecycle with transition validation
- ✅ Order authorization, activation, and result recording
- ✅ Order verification and closure
- ✅ Automatic compensation on cancellation
- ✅ Charge reversal and notification actions
- ⏳ Integration with billing system (next phase)

### 4. Event-Driven Architecture
- ✅ Kafka-based event publishing
- ✅ Task lifecycle events (created, started, completed, failed)
- ✅ Order lifecycle events
- ✅ Workflow state change events
- ✅ Async processing with idempotency
- ⏳ Event consumers for next task activation (next phase)
- ⏳ Module integration events (Lab, Imaging, Pharmacy) (next phase)

### 5. Observability & Compliance
- ✅ Immutable audit log for all actions
- ✅ Correlation IDs for distributed tracing
- ✅ Comprehensive logging
- ✅ SLA breach detection and escalation
- ✅ Legal hold support for audit retention
- ⏳ Real-time dashboards (next phase)
- ⏳ Analytics and reporting (next phase)

## Running the Application

### Prerequisites
```bash
Java 21+
Maven 3.8+
Kafka 3.0+ (for event publishing)
```

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`
- API Documentation: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console`

### Kafka Setup (Local Development)
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka
bin/kafka-server-start.sh config/server.properties

# Create topics
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic workflow-task-events --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic workflow-order-events --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic workflow-state-events --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic system-events --partitions 3 --replication-factor 1
```

## Next Phase Implementation

### REST Controllers
- `WorkflowTemplateController`: Create/manage templates
- `WorkflowInstanceController`: Create/manage workflow instances
- `TaskInstanceController`: Assign, start, complete tasks
- `OrderController`: Create/manage orders
- `OrderSetController`: Manage order sets

### Kafka Consumers
- `TaskEventConsumer`: Handle task events and activate next tasks
- `OrderEventConsumer`: Handle order results and propagate to tasks
- `WorkflowEventConsumer`: Handle workflow state changes

### Scheduled Tasks
- SLA monitoring (detect breaches, escalate)
- Task aging (find old pending tasks)
- Compensation action execution
- Audit log archival

### Module Integration Adapters
- Lab module integration (result posting)
- Imaging module integration (result posting)
- Pharmacy module integration (dispensing)
- ADT module integration (admission/discharge events)
- Billing module integration (charge creation/reversal)

### UI Components
- Workflow template designer (drag-and-drop)
- Workflow instance tracking dashboard
- Task management interface
- Order management interface
- Audit trail viewer
- SLA monitoring dashboard

## Design Decisions

### 1. Order 8-State Lifecycle
Based on HMIS best practices and clinical workflows, ensures proper authorization, tracking, and result verification.

### 2. SLA-Driven Task Management
Tasks include SLA deadlines calculated from template definitions, enabling proactive escalation and SLA breach detection.

### 3. Compensation Actions
Automatic creation of compensating transactions (charge reversal, notifications) on order cancellation ensures data integrity.

### 4. Kafka-Based Event Propagation
Async event publishing enables loose coupling between services, supports horizontal scaling, and provides audit trail.

### 5. Immutable Audit Log
All actions recorded with timestamps, actors, and changes for 7-10 year retention and legal compliance.

## Compliance & Standards

- **Order Lifecycle**: Aligned with ANSI HL7 standards
- **SLA Tracking**: Support for queue metrics, turnaround time, and escalation
- **Audit Trail**: 7-10 year retention with legal hold support
- **Data Quality**: Deduplication and mapping to LOINC, SNOMED, ICD standards
- **Security**: Role-based task visibility and elevated action justification

## Contributing

Follow these guidelines when extending the system:

1. Use the existing service layer pattern
2. Always use transactions for multi-entity operations
3. Publish events for all state changes
4. Log all significant operations
5. Create audit log entries for compliance
6. Support idempotent operations for failure recovery

## License

Proprietary - Healthcare Management Information Systems
