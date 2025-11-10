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

✅ **Event-Driven Architecture**
- Kafka-based event publishing for all state changes
- Task lifecycle events (created, started, completed, failed)
- Order lifecycle events
- Workflow state change events

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

## Implementation Status

### ✅ Completed
- Domain model design aligned with HMIS guidelines
- Repository layer for data access
- Service layer with core business logic
- Kafka configuration and event producers
- **REST API controllers for all major entities (44 endpoints)**
- Comprehensive documentation

### 📋 Planned
- Kafka event consumers
- Scheduled SLA monitoring
- Module integration adapters (Lab, Imaging, Pharmacy, ADT)
- UI dashboard and designer

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

## Documentation

- **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - Complete REST API reference (44 endpoints, 50+ examples)
- **[README_IMPLEMENTATION.md](README_IMPLEMENTATION.md)** - Architecture and design documentation
- **Swagger UI** - Interactive API explorer at `http://localhost:8080/swagger-ui.html`

## Guided by HMIS Workflow Guidelines

This implementation follows comprehensive guidelines covering:
- Order lifecycle and clinical workflows
- Task management with SLA tracking
- Exception handling and compensation
- Integration with hospital modules
- Audit and compliance requirements

Refer to https://github.com/sriniaravamuthan/workflowguidelinesdocs for detailed guidelines.
