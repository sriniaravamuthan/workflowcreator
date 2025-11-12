# Kafka Event-Driven Architecture

## Overview

The HMIS Workflow Engine implements a complete event-driven architecture using Apache Kafka for asynchronous processing of workflow events. This enables automatic task propagation, state management, and workflow coordination without blocking operations.

## Architecture Components

### 1. Event Types

The system publishes and consumes four categories of events:

#### 1.1 Task Events (`workflow-task-events` topic)
Published when task instances change state throughout their lifecycle.

**Event Types:**
- `TASK_CREATED`: Task instance created from template definition
- `TASK_STARTED`: Task execution initiated by assignee
- `TASK_COMPLETED`: Task execution completed successfully
- `TASK_FAILED`: Task execution failed with error

**Producer:** `TaskEventProducer` (src/main/java/com/hmis/workflow/kafka/producer/TaskEventProducer.java)

**Consumer:** `TaskEventConsumer` (src/main/java/com/hmis/workflow/kafka/consumer/TaskEventConsumer.java)

**Key Fields:**
- `eventId`: Unique event identifier
- `taskInstanceId`: Task instance UUID
- `workflowInstanceId`: Parent workflow instance UUID
- `patientId`: Patient affected
- `status`: Current task status
- `taskName`: Task definition name
- `eventTime`: Event timestamp
- `errorMessage`: Failure details (for TASK_FAILED)
- `metadata`: Additional context

**Example Event Flow:**
```
1. API: POST /workflows/tasks/{id}/complete
2. Service: taskInstanceService.completeTask()
3. Producer: taskEventProducer.publishTaskCompleted()
4. Kafka: Publish to workflow-task-events
5. Consumer: TaskEventConsumer.handleTaskCompleted()
6. Logic: Trigger next task propagation
7. Database: Update task and workflow status
```

#### 1.2 Order Events (`workflow-order-events` topic)
Published when orders transition through their 8-state lifecycle.

**Event Types:**
- `ORDER_CREATED`: New order created
- `ORDER_AUTHORIZED`: Order approved by clinician
- `ORDER_ACTIVATED`: Order ready for fulfillment
- `ORDER_RESULTED`: Order results recorded
- `ORDER_CANCELLED`: Order cancelled with reason

**Producer:** `OrderEventProducer` (src/main/java/com/hmis/workflow/kafka/producer/OrderEventProducer.java)

**Consumer:** `OrderEventConsumer` (src/main/java/com/hmis/workflow/kafka/consumer/OrderEventConsumer.java)

**State Transitions:**
```
PROPOSED → AUTHORIZED → ACTIVATED → IN_PROGRESS → RESULTED/DISPENSED/COMPLETED → VERIFIED → CLOSED
                                                  ↘
                                                   CANCELLED (from any state)
```

**Key Fields:**
- `eventId`: Unique event identifier
- `orderId`: Order UUID
- `workflowInstanceId`: Associated workflow
- `orderType`: LAB_TEST, IMAGING, MEDICATION, SURGERY, etc.
- `orderStatus`: Current state
- `actor`: User performing action
- `eventTime`: Timestamp
- `metadata`: Details like cost, results, etc.

**Example Event Flow:**
```
1. API: POST /workflows/orders/{id}/cancel
2. Service: orderService.cancelOrder(orderId, reason)
3. Action: Create compensation actions (charge reversal, notifications)
4. Producer: orderEventProducer.publishOrderCancelled()
5. Kafka: Publish to workflow-order-events
6. Consumer: OrderEventConsumer.handleOrderCancelled()
7. Logic: Execute pending compensation actions
8. Database: Update order status, mark compensations complete
```

#### 1.3 Workflow Events (`workflow-state-events` topic)
Published for workflow instance state changes and lifecycle events.

**Event Types:**
- `WORKFLOW_STARTED`: Workflow instance execution begun
- `WORKFLOW_COMPLETED`: All tasks successfully completed
- `WORKFLOW_FAILED`: Workflow execution failed
- `WORKFLOW_ESCALATED`: Workflow escalated for manual intervention

**Producer:** `WorkflowEventProducer` (src/main/java/com/hmis/workflow/kafka/producer/WorkflowEventProducer.java)

**Consumer:** `WorkflowEventConsumer` (src/main/java/com/hmis/workflow/kafka/consumer/WorkflowEventConsumer.java)

**Key Fields:**
- `eventId`: Unique event identifier
- `workflowInstanceId`: Workflow instance UUID
- `patientId`: Patient undergoing workflow
- `workflowTemplateName`: Template name
- `status`: Current workflow status
- `progressPercentage`: Completion percentage (0-100)
- `eventTime`: Timestamp
- `escalationReason`: Reason if escalated

**Example Event Flow:**
```
1. All Tasks Complete
2. Service: workflowInstanceService.updateWorkflowStatus()
3. Logic: Check if all required tasks done
4. Action: Set status to COMPLETED
5. Producer: workflowEventProducer.publishWorkflowCompleted()
6. Kafka: Publish to workflow-state-events
7. Consumer: WorkflowEventConsumer.handleWorkflowCompleted()
8. Logic: Archive workflow, trigger post-completion actions
9. Database: Mark workflow completed with timestamp
```

#### 1.4 System Events (`system-events` topic)
Published for operational monitoring and diagnostics.

**Event Types:**
- `HEALTH_CHECK`: System component health status
- `PERFORMANCE_METRIC`: Performance measurement
- `ERROR_REPORT`: Error condition
- `AUDIT_EVENT`: Compliance audit trail

**Consumer:** `SystemEventConsumer` (src/main/java/com/hmis/workflow/kafka/consumer/SystemEventConsumer.java)

### 2. Consumer Processing

#### 2.1 TaskEventConsumer

**Location:** `src/main/java/com/hmis/workflow/kafka/consumer/TaskEventConsumer.java`

**Listener Configuration:**
- **Topic:** `workflow-task-events`
- **Consumer Group:** `workflow-engine-task-consumer`
- **Partition Assignment:** By task instance ID for ordering guarantee

**Processing Logic by Event Type:**

**TASK_COMPLETED Handler:**
```java
1. Validate task instance exists and is in correct state
2. Check for blocking instructions/gates
3. Identify next task from task definition (nextTaskId)
4. Mark next task as ready (creates task instance if needed)
5. Update workflow status (checks if all tasks complete)
6. Return to step 2 for next task propagation
```

**TASK_FAILED Handler:**
```java
1. Validate task instance exists
2. Check retry eligibility:
   - If retryable AND retries remaining: Trigger automatic retry
   - If not retryable: Check for failure task definition
3. If failure task defined:
   - Create failure task instance
   - Mark as ready for execution
4. Else:
   - Mark workflow as FAILED
   - Trigger escalation
```

**TASK_STARTED Handler:**
```java
1. Validate task exists and transitioned to IN_PROGRESS
2. Check if SLA already breached (task started late)
3. If breached: Log warning, may trigger escalation
4. Update task timestamps
```

**TASK_CREATED Handler:**
```java
1. Validate task instance created
2. Extract SLA information
3. Initialize task tracking
4. In production: Send notifications to assignees
```

**Example Consumer Processing:**
```
Receive: TaskEvent(eventType: "TASK_COMPLETED", taskInstanceId: "task-123")
↓
Load: TaskInstance from database
↓
Validate: Status is COMPLETED, workflow is ACTIVE
↓
Check: No blocking instructions
↓
Find: taskDef.nextTaskId = "task-456"
↓
Load: Next TaskInstance
↓
Action: Next task is now available for assignment
↓
Update: WorkflowInstance status based on task progression
↓
Continue: If next task is automatic, repeat cycle
```

#### 2.2 OrderEventConsumer

**Location:** `src/main/java/com/hmis/workflow/kafka/consumer/OrderEventConsumer.java`

**Listener Configuration:**
- **Topic:** `workflow-order-events`
- **Consumer Group:** `workflow-engine-order-consumer`
- **Partition Assignment:** By order ID for state consistency

**Processing Logic by Event Type:**

**ORDER_CREATED Handler:**
```java
1. Validate order exists and is in PROPOSED status
2. Extract order metadata (type, cost, department)
3. Initialize order tracking
4. Send notifications to clinical team
5. Update dashboards
```

**ORDER_AUTHORIZED Handler:**
```java
1. Validate order in AUTHORIZED status
2. Check authorization was by eligible clinician
3. Determine if order can be auto-activated:
   - LAB_TEST, IMAGING: Usually auto-activate
   - MEDICATION, SURGERY: Require manual activation
4. If auto-activate eligible: Trigger activation
5. Update order timeline
```

**ORDER_ACTIVATED Handler:**
```java
1. Validate order in ACTIVATED status
2. Send to fulfillment systems (Lab, Pharmacy, etc.)
3. Update department notifications
4. Start tracking for fulfillment deadline
```

**ORDER_RESULTED Handler:**
```java
1. Validate order in RESULTED/DISPENSED status
2. Record result/medication dispensed information
3. Queue for clinician verification
4. Update patient record
5. Check for critical results requiring immediate action
```

**ORDER_CANCELLED Handler:**
```java
1. Validate order in CANCELLED status
2. Load compensation actions for order
3. Execute compensations:
   - CANCEL_ORDER: Notify fulfillment system
   - REVERSE_CHARGE: Update billing system
   - SEND_NOTIFICATION: Notify relevant parties
   - RELEASE_RESOURCE: Free allocated resources
4. Mark compensations complete
5. Archive order if needed
```

#### 2.3 WorkflowEventConsumer

**Location:** `src/main/java/com/hmis/workflow/kafka/consumer/WorkflowEventConsumer.java`

**Listener Configuration:**
- **Topic:** `workflow-state-events`
- **Consumer Group:** `workflow-engine-state-consumer`
- **Partition Assignment:** By workflow instance ID

**Processing Logic by Event Type:**

**WORKFLOW_STARTED Handler:**
```java
1. Validate workflow in ACTIVE status
2. Log workflow initialization
3. Send start notifications to care team
4. Initialize monitoring/dashboards
5. Start SLA tracking
```

**WORKFLOW_COMPLETED Handler:**
```java
1. Validate workflow in COMPLETED status
2. Calculate total duration
3. Archive workflow for retention
4. Generate completion report
5. Check for required follow-up workflows
6. Send completion notifications
7. Update patient status records
```

**WORKFLOW_FAILED Handler:**
```java
1. Validate workflow in FAILED status
2. Log failure details
3. Auto-escalate to supervisors
4. Trigger incident management
5. Create support tickets
6. Document failure reason
7. Send critical alerts
```

**WORKFLOW_ESCALATED Handler:**
```java
1. Validate workflow is_escalated flag
2. Log escalation details with reason
3. Notify senior clinicians/managers
4. Create escalation task assignments
5. Update on-call systems
6. Send urgent notifications
```

#### 2.4 SystemEventConsumer

**Location:** `src/main/java/com/hmis/workflow/kafka/consumer/SystemEventConsumer.java`

**Listener Configuration:**
- **Topic:** `system-events`
- **Consumer Group:** `workflow-engine-system-consumer`

**Processing Logic:**

**HEALTH_CHECK Handler:**
```java
1. Extract component and status
2. Log health status
3. If UNHEALTHY: Trigger alerting
4. Update system dashboard
```

**PERFORMANCE_METRIC Handler:**
```java
1. Extract metric name, value, unit
2. Compare against thresholds
3. If threshold exceeded: Log warning
4. Update monitoring systems
```

**ERROR_REPORT Handler:**
```java
1. Extract error details
2. Route by severity:
   - CRITICAL: Immediate escalation
   - HIGH: Support team alert
   - MEDIUM: Log and monitor
   - LOW: Diagnostic logging
```

**AUDIT_EVENT Handler:**
```java
1. Extract audit details
2. Store in immutable audit log
3. Check for compliance requirements
4. Update audit dashboard
```

### 3. SLA Monitoring Service

**Location:** `src/main/java/com/hmis/workflow/service/SLAMonitoringService.java`

**Scheduled Tasks:**

**monitorSLABreaches() - Every 5 Minutes**
```java
1. Query tasks with potential SLA breaches
2. For each breached task:
   a. Validate actual breach
   b. Mark task as slaBreached = true
   c. Auto-escalate to manager
   d. Send breach alert
   e. Log to audit trail
```

**monitorStaleTasks() - Every 10 Minutes**
```java
1. Find tasks pending > 4 hours without update
2. Identify stuck workflows
3. Notify workflow coordinators
4. Consider automatic escalation
```

**monitorEscalatedWorkflows() - Every 15 Minutes**
```java
1. Review all escalated workflows
2. Check if escalations are being addressed
3. Trigger further escalation if needed
4. Update escalation status
```

**healthCheckSLAMonitoring() - Every 1 Hour**
```java
1. Validate monitoring is functioning
2. Count tasks processed
3. Verify alerts being sent
4. Check for stuck escalations
```

### 4. Event Publishing

#### 4.1 TaskEventProducer

**Configuration:**
- Idempotent publishing (prevents duplicate events)
- Transactional behavior (atomic with database)
- Async callbacks for confirmation

**Publishing Methods:**
```java
publishTaskCreated(taskInstance) → TaskEvent
publishTaskStarted(taskInstance) → TaskEvent
publishTaskCompleted(taskInstance, result) → TaskEvent
publishTaskFailed(taskInstance, errorMessage) → TaskEvent
```

#### 4.2 OrderEventProducer

**Configuration:**
- Ordered partition key (by order ID)
- Cost tracking embedded in events
- Compensation action references

**Publishing Methods:**
```java
publishOrderCreated(order) → OrderEvent
publishOrderAuthorized(order, actor) → OrderEvent
publishOrderActivated(order) → OrderEvent
publishOrderResulted(order, result) → OrderEvent
publishOrderCancelled(order, reason) → OrderEvent
```

#### 4.3 WorkflowEventProducer

**Configuration:**
- Progress tracking with percentage
- Patient-centric partitioning
- Escalation reason capture

**Publishing Methods:**
```java
publishWorkflowStarted(workflow) → WorkflowEvent
publishWorkflowCompleted(workflow) → WorkflowEvent
publishWorkflowFailed(workflow) → WorkflowEvent
publishWorkflowEscalated(workflow, reason) → WorkflowEvent
```

## Event Flow Examples

### Example 1: Task Completion with Automatic Next Task Propagation

```
User Action: POST /workflows/tasks/task-123/complete
    ↓
TaskInstanceController.completeTask()
    ↓
TaskInstanceService.completeTask()
    ├─ Validate task in IN_PROGRESS status
    ├─ Update task to COMPLETED
    ├─ Find next task from taskDef.nextTaskId
    ├─ Publish event to Kafka
    └─ Return response to user
    ↓
TaskEventProducer.publishTaskCompleted()
    ├─ Create TaskEvent(eventType: "TASK_COMPLETED", ...)
    ├─ Set partition key = taskInstanceId
    └─ Send to workflow-task-events topic
    ↓
Kafka Broker
    ├─ Persist event
    └─ Distribute to consumers
    ↓
TaskEventConsumer.handleTaskEvent()
    ├─ Receive TASK_COMPLETED event
    ├─ Load task instance
    ├─ Check for blocking instructions
    ├─ Find next task from taskDef.nextTaskId
    ├─ Next task is now IN_PENDING status (ready)
    ├─ Update workflow status
    └─ If next task is automatic: Recursively process
    ↓
Database Update
    ├─ task_instances: update status, completedAt
    ├─ workflow_instances: update status if needed
    └─ task_instances (next): available for assignment
    ↓
Result: Workflow automatically progresses to next task
```

### Example 2: Order Cancellation with Compensation Actions

```
User Action: POST /workflows/orders/order-456/cancel
    ↓
OrderController.cancelOrder()
    ↓
OrderService.cancelOrder()
    ├─ Validate order can transition to CANCELLED
    ├─ Set status = CANCELLED
    ├─ Create CompensationAction records:
    │  ├─ REVERSE_CHARGE
    │  ├─ SEND_NOTIFICATION
    │  └─ CANCEL_APPOINTMENT
    ├─ Save to database
    └─ Return response
    ↓
OrderController.executePendingCompensations()
    ├─ Call orderService.executePendingCompensations(orderId)
    ├─ For each compensation action:
    │  ├─ Execute action
    │  ├─ Mark complete
    │  └─ Publish order event
    └─ Publish OrderEvent(eventType: "ORDER_CANCELLED")
    ↓
OrderEventProducer.publishOrderCancelled()
    ├─ Create OrderEvent with compensation details
    ├─ Set partition key = orderId
    └─ Send to workflow-order-events topic
    ↓
TaskEventConsumer.handleOrderEvent()
    ├─ Receive ORDER_CANCELLED event
    ├─ Load order with compensation actions
    ├─ Execute each action:
    │  ├─ REVERSE_CHARGE: Update billing system
    │  ├─ SEND_NOTIFICATION: Email/SMS to staff
    │  └─ CANCEL_APPOINTMENT: Notify scheduling system
    ├─ Mark compensations complete
    └─ Update order status
    ↓
Database Update
    ├─ orders: status = CANCELLED, cancelledAt = NOW
    ├─ compensation_actions: executed = true, completedAt = NOW
    └─ billing: credit reversal processed
    ↓
Result: Order cancelled with all compensation actions executed
```

### Example 3: Task SLA Breach Detection and Escalation

```
SLAMonitoringService.monitorSLABreaches() [Every 5 minutes]
    ↓
Query: SELECT task_instances WHERE due_at < NOW AND slaBreached = false AND status IN ('PENDING', 'IN_PROGRESS')
    ↓
For each breached task:
    ├─ Load task with assignment info
    ├─ Calculate hours overdue
    ├─ Update task: slaBreached = true
    ├─ Determine escalation user (manager/supervisor)
    ├─ Call taskInstanceService.escalateTask()
    │  ├─ Set isEscalated = true
    │  ├─ Set escalatedToUser = determined_user
    │  ├─ Append reason to comments
    │  ├─ Save task
    │  └─ Publish TaskEvent implicitly
    ├─ Send SLA breach alert
    │  ├─ Email to escalatedToUser
    │  ├─ SMS to manager
    │  └─ Dashboard notification
    └─ Log escalation details for audit
    ↓
Database Update
    ├─ task_instances: slaBreached = true, isEscalated = true, escalatedToUser = 'MANAGER'
    ├─ task_comments: append escalation reason
    └─ audit_logs: record SLA breach event
    ↓
Result: Task escalated and notifications sent
```

## Configuration

### Kafka Configuration

**File:** `src/main/java/com/hmis/workflow/config/KafkaConfig.java`

**Topics Created:**
```
workflow-task-events (partitions: 10, replication-factor: 3)
workflow-order-events (partitions: 10, replication-factor: 3)
workflow-state-events (partitions: 5, replication-factor: 3)
system-events (partitions: 5, replication-factor: 3)
```

**Consumer Configuration:**
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=workflow-engine-consumer
spring.kafka.consumer.key-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```

## Error Handling

### Dead Letter Queue Strategy

Failed events are handled according to severity:

**Transient Errors (retriable):**
- Network timeouts
- Temporary database unavailability
- Lock contention

**Recovery:** Kafka consumer group will auto-retry with exponential backoff

**Permanent Errors (non-retriable):**
- Invalid event format
- Unknown entity reference
- Invalid state transition

**Recovery:** Log to error topic for manual review

### Exception Handling in Consumers

```java
try {
    handleEvent(event);
} catch (Exception e) {
    log.error("Error processing event", e);
    // Event will be retried by Kafka consumer group
}
```

## Monitoring and Observability

### Metrics

- `kafka.consumer.lag`: Consumer offset lag per topic partition
- `task.events.processed`: Count of task events processed
- `order.events.processed`: Count of order events processed
- `workflow.events.processed`: Count of workflow events processed
- `sla.breaches.detected`: Count of SLA breaches found
- `event.processing.time.ms`: Time to process event
- `event.publish.failures`: Count of publish failures

### Logs

All consumers log at INFO level for important events and DEBUG for detailed tracing:

```
INFO TaskEventConsumer - Received task event: TASK_COMPLETED for task: task-123 in workflow: wf-456
DEBUG TaskEventConsumer - Task completion processing: Checking for blocking instructions
INFO TaskEventConsumer - Triggering next task: task-789 after completion of task: task-123
INFO TaskEventConsumer - Successfully processed TASK_COMPLETED event for task: task-123
```

### Health Checks

System Event Consumer monitors:
- Kafka broker connectivity
- Consumer lag
- Processing delays
- Error rates

## Best Practices

### 1. Ordering Guarantees
- Use consistent partition keys for related events
- Task events partitioned by taskInstanceId
- Order events partitioned by orderId
- Workflow events partitioned by workflowInstanceId

### 2. Idempotency
- All consumers handle duplicate events gracefully
- State updates are idempotent (same result on replay)
- Skip events if entity already in expected state

### 3. Timeout Handling
- Consumer timeout: 30 seconds (task processing)
- Event TTL: 7 days (retained for replay)
- Dead letter retention: 30 days

### 4. Scaling
- Consumer groups scale horizontally
- Each partition assigned to one consumer instance
- Add consumers for higher throughput

### 5. Monitoring
- Alert on consumer lag > 1000 messages
- Alert on event processing time > 5 seconds
- Daily health checks of all topics

## Future Enhancements

1. **Event Sourcing:** Maintain complete event log as single source of truth
2. **CQRS:** Separate read and write models for scalability
3. **Sagas Pattern:** Distributed transactions across services
4. **Stream Processing:** Real-time analytics on event streams
5. **Event Replay:** Rebuild state from events for disaster recovery
6. **Advanced Routing:** Content-based routing based on event payload
7. **Message Encryption:** Encrypt sensitive health data in transit
8. **Schema Registry:** Central management of event schemas

## Conclusion

The Kafka event-driven architecture provides:
- ✅ Asynchronous task propagation
- ✅ Automatic workflow progression
- ✅ Loose coupling between components
- ✅ High scalability and throughput
- ✅ Built-in ordering and replay capabilities
- ✅ Observable and auditable workflow execution
