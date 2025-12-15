# HMIS Workflow Engine - Database Structure Guide

Complete documentation of the database schema, tables, relationships, and usage patterns.

## Quick Reference

### Database Files Location
```
src/main/resources/db/
├── ddl/
│   └── 01-schema.sql          # Complete schema with 15 tables and 35+ indexes
├── data/
│   └── sample-data.sql        # Sample data for testing
├── init-db.sh                 # Database initialization script
└── README.md                  # Detailed database documentation
```

### Database Statistics
- **Total Tables**: 18
- **Total Indexes**: 55+
- **Audit Tables**: 4 (immutable, append-only for compliance)
  - audit_logs (general audit)
  - task_notes (task documentation)
  - task_results (task outcomes)
  - order_notes (order documentation)
- **Core Tables**: 14 (operational data)
- **Views**: 0 (can be added for reporting)
- **Sequences**: Optional (UUID-based auto-increment)

## Table Catalog

### 1. PATIENTS (Patient Management)
```
Columns: 15
Primary Key: id (UUID)
Unique Keys: patient_id, email
```
**Purpose**: Store patient demographic and medical information

**Key Columns**:
- `patient_id`: Unique patient identifier in system
- `first_name`, `last_name`, `middle_name`: Patient name
- `date_of_birth`: Birth date (required for age calculation)
- `medical_history`: CLOB field for detailed medical history
- `active`: Boolean flag for soft deletes

**Indexes**:
- `idx_patients_patient_id` - Quick lookup by patient ID
- `idx_patients_email` - Email-based search
- `idx_patients_active` - Filter active patients

**Relationships**:
- Workflow Instances (1:M) - One patient can have multiple active workflows
- Audit Logs (1:M) - All patient-related actions tracked

**Example Query**:
```sql
SELECT * FROM patients WHERE patient_id = 'PAT001' AND active = TRUE;
```

---

### 2. WORKFLOW_TEMPLATES (Template Management)
```
Columns: 12
Primary Key: id (UUID)
Unique Keys: name
```
**Purpose**: Store reusable workflow blueprints with versioning and governance

**Key Columns**:
- `name`: Unique template name (e.g., "Emergency Admission")
- `version`: Version number (1, 2, 3, ...)
- `review_status`: DRAFT, IN_REVIEW, APPROVED, PUBLISHED, DEPRECATED
- `approved_by_user`: User who approved template
- `category`: Template category (Emergency, Outpatient, Lab, etc.)

**Governance Workflow**:
```
DRAFT → IN_REVIEW → APPROVED → PUBLISHED
                                    ↓
                              (new version)
                                    ↓
                                 DRAFT
```

**Indexes**:
- `idx_workflow_templates_name` - Lookup by name
- `idx_workflow_templates_category` - Filter by category
- `idx_workflow_templates_review_status` - Find published templates
- `idx_workflow_templates_active` - Filter active templates

**Relationships**:
- Workflow Task Definitions (1:M) - Template contains multiple tasks
- Workflow Instances (1:M) - Instances created from templates
- Gates (1:M) - Template contains checkpoints
- Decision Logic (1:M) - Template contains routing rules

**Example Queries**:
```sql
-- Get all published templates
SELECT * FROM workflow_templates
WHERE active = TRUE AND review_status = 'PUBLISHED';

-- Get templates by category
SELECT * FROM workflow_templates
WHERE category = 'Emergency' AND active = TRUE;
```

---

### 3. WORKFLOW_TASK_DEFINITIONS (Task Definitions)
```
Columns: 19
Primary Key: id (UUID)
Foreign Key: template_id (references workflow_templates)
```
**Purpose**: Define individual tasks within workflow templates with scheduling constraints

**Key Columns**:
- `task_order`: Sequential position (1, 2, 3, ...)
- `assign_to`: Role or user assignment (e.g., "nurse", "physician")
- `estimated_duration_minutes`: SLA calculation basis
- `is_parallel`: TRUE for parallel execution
- `is_optional`: TRUE for optional tasks
- `next_task_id`: Task to activate on completion
- `failure_task_id`: Fallback task on failure

**Scheduling Constraint Columns**:
- `scheduled_start_time`: Time of day when task should ideally start (e.g., 08:00)
- `max_wait_time_minutes`: Max time in PENDING before escalation
- `allowed_start_time_of_day`: Earliest time task can execute (e.g., 06:00)
- `allowed_end_time_of_day`: Latest time task can execute (e.g., 18:00)
- `allowed_days_of_week`: Comma-separated days (e.g., "MON,TUE,WED,THU,FRI")

**Scheduling Examples**:
```
Fasting Lab Draw:
- allowed_start_time_of_day: 06:00
- allowed_end_time_of_day: 10:00
- allowed_days_of_week: NULL (any day)

Medication Administration:
- scheduled_start_time: 08:00 (first dose)
- max_wait_time_minutes: 30

Operating Room Procedure:
- allowed_start_time_of_day: 07:00
- allowed_end_time_of_day: 17:00
- allowed_days_of_week: "MON,TUE,WED,THU,FRI"
```

**Indexes**:
- `idx_workflow_task_definitions_template_id` - Find template tasks
- `idx_workflow_task_definitions_task_order` - Order by sequence

**Relationships**:
- Workflow Templates (M:1) - Many tasks per template
- Task Instances (1:M) - Task definition creates instances

**Example Query**:
```sql
-- Get all tasks in a template, ordered
SELECT * FROM workflow_task_definitions
WHERE template_id = 'template-001'
ORDER BY task_order;

-- Get tasks with time restrictions
SELECT * FROM workflow_task_definitions
WHERE allowed_start_time_of_day IS NOT NULL
   OR allowed_end_time_of_day IS NOT NULL;
```

---

### 4. WORKFLOW_INSTANCES (Workflow Execution)
```
Columns: 17
Primary Key: id (UUID)
Unique Keys: workflow_instance_id
Foreign Keys: patient_id, template_id
```
**Purpose**: Store patient-specific workflow instances (running workflows)

**Key Columns**:
- `workflow_instance_id`: Unique instance identifier
- `patient_id`: Associated patient
- `template_id`: Template this instance was created from
- `encounter_id`: Clinical encounter ID (e.g., ER visit, admission) - for workflow isolation
- `visit_id`: ADT visit tracking ID - for integration with ADT systems
- `status`: ACTIVE, PAUSED, COMPLETED, FAILED, CANCELLED
- `started_at`: Workflow start time
- `completed_at`: Workflow completion time
- `is_escalated`: Boolean for escalation status
- `escalation_reason`: Reason for escalation
- `review_status`: PENDING_REVIEW, IN_REVIEW, APPROVED, REJECTED, EXECUTED
- `can_execute`: Boolean, only true after approval

**Clinical Context (Patient + Encounter + Visit)**:
```
Uniqueness Validation:
- Only ONE active workflow per patient + encounter + template combination
- Prevents duplicate protocol execution for the same clinical context
- Completed/cancelled/failed workflows don't block new creation

Use Cases:
- Patient admitted (ENC-001) → Start Admission Protocol ✓
- Same patient, different ER visit (ENC-002) → Can start another ✓
- Trying to start duplicate for ENC-001 → ERROR: "Active workflow exists"
```

**Status Transitions**:
```
        ┌─→ PAUSED ──┐
        │            │
ACTIVE ─┤    ↓       └─→ COMPLETED
        │    └────────┘
        └─→ FAILED
        └─→ CANCELLED
```

**Indexes**:
- `idx_workflow_instances_workflow_instance_id` - Lookup by instance ID
- `idx_workflow_instances_patient_id` - Find patient workflows
- `idx_workflow_instances_status` - Filter by status
- `idx_workflow_instances_encounter_id` - Find by encounter
- `idx_workflow_instances_visit_id` - Find by visit
- `idx_workflow_instances_is_escalated` - Find escalated workflows
- `idx_workflow_instances_created_at` - Time-based queries
- `idx_workflow_instances_patient_encounter_template` - Composite index for uniqueness check

**Relationships**:
- Patients (M:1) - Many instances per patient
- Workflow Templates (M:1) - Multiple instances per template
- Task Instances (1:M) - Instance contains tasks
- Orders (1:M) - Instance contains orders
- Instructions (1:M) - Instance contains instructions
- Audit Logs (1:M) - All changes tracked

**Example Queries**:
```sql
-- Get active workflows for a patient
SELECT * FROM workflow_instances
WHERE patient_id = 'patient-001' AND status = 'ACTIVE';

-- Get workflows for a specific encounter
SELECT * FROM workflow_instances
WHERE encounter_id = 'ENC-2024-001';

-- Check for duplicate before creating (service layer does this)
SELECT * FROM workflow_instances
WHERE patient_id = 'patient-001'
  AND encounter_id = 'ENC-001'
  AND template_id = 'template-001'
  AND status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED');

-- Get all escalated workflows
SELECT * FROM workflow_instances
WHERE is_escalated = TRUE;

-- Get today's completed workflows
SELECT * FROM workflow_instances
WHERE status = 'COMPLETED'
  AND DATE(completed_at) = CURRENT_DATE;
```

---

### 5. TASK_INSTANCES (Task Execution)
```
Columns: 35
Primary Key: id (UUID)
Unique Keys: task_instance_id
Foreign Keys: workflow_instance_id, task_definition_id (nullable for ad-hoc tasks)
```
**Purpose**: Store individual task executions within workflow instances, including ad-hoc tasks

**Key Columns**:
- `task_instance_id`: Unique task execution identifier
- `task_definition_id`: Task definition ID (NULL for ad-hoc tasks)
- `status`: PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED, BLOCKED
- `assigned_to`: User assigned to task
- `required_role`: Role required to complete
- `due_at`: SLA deadline (calculated from task_order × duration)
- `started_at`: Task start time
- `completed_at`: Task completion time
- `result`: Task output/result data
- `retry_count`: Number of retry attempts
- `max_retries`: Maximum retry limit (default: 3)
- `sla_minutes`: Task SLA duration
- `sla_breached`: Boolean flag for SLA violation
- `is_escalated`: Boolean for escalation
- `escalated_to_user`: User escalated to

**Ad-hoc Task Columns** (for dynamically created tasks):
- `is_adhoc`: Boolean flag for ad-hoc tasks
- `adhoc_task_name`: Task name (when no task definition)
- `adhoc_task_description`: Task description (when no task definition)
- `created_by_user`: User who created the ad-hoc task

**Skip Tracking Columns**:
- `skip_reason`: Reason for skipping the task
- `skipped_by_user`: User who skipped the task

**Scheduling Constraint Columns** (instance-level):
- `scheduled_start_time`: When this specific task instance should start
- `max_wait_deadline`: If task is still PENDING after this time, escalate
- `allowed_start_time_of_day`: Earliest time task can execute
- `allowed_end_time_of_day`: Latest time task can execute
- `allowed_days_of_week`: Comma-separated days

**Ad-hoc Tasks**:
```
Ad-hoc tasks are dynamically created during workflow execution:
- Doctor orders "Administer Saline" mid-workflow
- No task definition in template (task_definition_id = NULL)
- Start in PENDING status immediately
- Treated as optional by default
- Full audit tracking: who created, when, why
```

**Skip Functionality**:
```
Optional Tasks: Can be skipped without reason
Required Tasks: Can be skipped with forceSkip=true AND reason

Skip Types:
- Optional task skip: No reason required
- Required task skip: Reason + audit comment mandatory

Use Cases:
- Blood test already performed elsewhere
- Patient refused procedure
- Clinical judgment override
- Task no longer applicable
```

**Status Lifecycle**:
```
PENDING → (assign) → IN_PROGRESS → (complete) → COMPLETED
  ↑                      ↓
  └─ (skip if optional) SKIPPED
  └─ (skip with force)  SKIPPED + audit comment
  └─ (fail) → FAILED → (retry) → PENDING
  └─────────────── BLOCKED (waiting for predecessors)
```

**Indexes**:
- `idx_task_instances_task_instance_id` - Lookup by task ID
- `idx_task_instances_workflow_instance_id` - Find workflow tasks
- `idx_task_instances_assigned_to` - User task queue
- `idx_task_instances_status` - Status filtering
- `idx_task_instances_due_at` - SLA deadline queries
- `idx_task_instances_sla_breached` - Find breached SLAs
- `idx_task_instances_is_escalated` - Find escalated tasks
- `idx_task_instances_is_adhoc` - Filter ad-hoc tasks

**Relationships**:
- Workflow Instances (M:1) - Many tasks per instance
- Workflow Task Definitions (M:1) - Definition for this instance (NULL for ad-hoc)

**Example Queries**:
```sql
-- Get pending tasks for a user
SELECT * FROM task_instances
WHERE assigned_to = 'nurse.smith'
  AND status IN ('PENDING', 'IN_PROGRESS');

-- Get ad-hoc tasks for a workflow
SELECT * FROM task_instances
WHERE workflow_instance_id = 'wf-001'
  AND is_adhoc = TRUE;

-- Get skipped tasks with reasons
SELECT task_instance_id, adhoc_task_name, skip_reason, skipped_by_user
FROM task_instances
WHERE status = 'SKIPPED'
  AND skip_reason IS NOT NULL;

-- Get SLA-breached tasks
SELECT * FROM task_instances
WHERE sla_breached = TRUE AND status != 'COMPLETED';

-- Get tasks due in next 30 minutes
SELECT * FROM task_instances
WHERE due_at BETWEEN CURRENT_TIMESTAMP AND DATEADD(MINUTE, 30, CURRENT_TIMESTAMP)
  AND status IN ('PENDING', 'IN_PROGRESS');
```

---

### 6. ORDERS (Clinical Orders)
```
Columns: 24
Primary Key: id (UUID)
Unique Keys: order_id
Foreign Key: workflow_instance_id
```
**Purpose**: Store clinical orders (lab tests, imaging, medications, procedures)

**Key Columns**:
- `order_id`: Unique order identifier
- `status`: 8-state lifecycle (see below)
- `order_type`: LAB_TEST, IMAGING, PROCEDURE, MEDICATION, SURGERY, etc.
- `order_description`: What is being ordered
- `department_target`: Where order goes (LABORATORY, IMAGING, PHARMACY, etc.)
- `ordered_by_user`: Clinician who ordered
- `authorized_by_user`: Clinician who authorized
- `result`: Order result (for tests/imaging)
- `estimated_cost`, `actual_cost`: Cost tracking
- `priority`: 0 (normal), 1 (high), 2 (critical)

**8-State Lifecycle**:
```
PROPOSED
  ↓ (authorize)
AUTHORIZED
  ↓ (activate/transmit)
ACTIVATED
  ↓ (start work)
IN_PROGRESS
  ↓ (record result)
RESULTED/DISPENSED/COMPLETED
  ↓ (verify)
VERIFIED
  ↓ (close)
CLOSED

Alternative: CANCELLED (from any state, triggers compensation)
```

**State Transitions Enforced**:
```sql
-- Not all transitions allowed - must follow workflow
PROPOSED    → AUTHORIZED, CANCELLED
AUTHORIZED  → ACTIVATED, CANCELLED
ACTIVATED   → IN_PROGRESS, CANCELLED
IN_PROGRESS → RESULTED/DISPENSED/COMPLETED, CANCELLED
RESULTED    → VERIFIED, CANCELLED
VERIFIED    → CLOSED
CLOSED      → (terminal state)
CANCELLED   → (terminal state, triggers compensation actions)
```

**Indexes**:
- `idx_orders_order_id` - Lookup by order ID
- `idx_orders_workflow_instance_id` - Find workflow orders
- `idx_orders_status` - Status filtering
- `idx_orders_order_type` - Filter by type
- `idx_orders_department_target` - Route to department

**Relationships**:
- Workflow Instances (M:1) - Many orders per instance
- Compensation Actions (1:M) - Compensation for cancelled orders

**Example Queries**:
```sql
-- Get open orders (not closed/cancelled)
SELECT * FROM orders
WHERE status NOT IN ('CLOSED', 'CANCELLED');

-- Get orders ready for verification
SELECT * FROM orders
WHERE status IN ('RESULTED', 'DISPENSED', 'COMPLETED');

-- Get orders by department
SELECT * FROM orders
WHERE department_target = 'LABORATORY'
  AND status IN ('ACTIVATED', 'IN_PROGRESS');
```

---

### 7. ORDER_SETS (Order Bundles)
```
Columns: 14
Primary Key: id (UUID)
Unique Keys: order_set_id
```
**Purpose**: Bundle orders/tasks/instructions for specific clinical conditions

**Key Columns**:
- `order_set_id`: Unique identifier
- `name`: OrderSet name (e.g., "Diabetes Management Bundle")
- `clinical_condition`: Condition this applies to
- `category`: Clinical category
- `version`: Version number
- `access_level`: PRIVATE, TEAM, DEPARTMENT, HOSPITAL_WIDE
- `is_parallel`: Execute items in parallel or sequentially

**Indexes**:
- `idx_order_sets_order_set_id` - Lookup by ID
- `idx_order_sets_name` - Name search
- `idx_order_sets_clinical_condition` - Find by condition
- `idx_order_sets_category` - Filter by category
- `idx_order_sets_access_level` - Permission filtering

**Relationships**:
- Order Set Items (1:M) - Contains multiple items
- Order Set Conditions (1:M) - Activation conditions

**Example Queries**:
```sql
-- Get all ordersets for a condition
SELECT * FROM order_sets
WHERE clinical_condition = 'Diabetes Type 2' AND active = TRUE;

-- Get hospital-wide ordersets
SELECT * FROM order_sets
WHERE access_level = 'HOSPITAL_WIDE' AND active = TRUE;
```

---

### 8. ORDER_SET_ITEMS (OrderSet Items)
```
Columns: 14
Primary Key: id (UUID)
Foreign Key: order_set_id
```
**Purpose**: Individual items (orders/tasks/instructions) within order sets

**Key Columns**:
- `sequence_number`: Order of execution
- `item_type`: ORDER, TASK, or INSTRUCTION
- `order_type`: If type is ORDER
- `item_name`: Name of item
- `mandatory`: Required or optional
- `is_parallel`: Execute in parallel with others
- `depends_on_item_id`: Dependency on another item

---

### 9. ORDER_SET_CONDITIONS (OrderSet Activation Rules)
```
Columns: 13
Primary Key: id (UUID)
Foreign Key: order_set_id
```
**Purpose**: Define conditions for automatic OrderSet activation

**Key Columns**:
- `condition_name`: Name of condition
- `data_point`: What to evaluate (e.g., "age", "diagnosis")
- `operator`: EQUALS, GREATER_THAN, CONTAINS, IN, etc.
- `expected_value`: Value to match
- `required`: Condition must be satisfied

**Example**:
```sql
-- OrderSet activates if patient age > 65 AND diagnosis = 'Diabetes'
INSERT INTO order_set_conditions VALUES (
    'uuid', 'orderset-001', 'Age Check',
    'patient_age', 'GREATER_THAN', '65', TRUE, 'AND'
);
```

---

### 10. INSTRUCTIONS (Clinical Directives)
```
Columns: 15
Primary Key: id (UUID)
Unique Keys: instruction_id
Foreign Key: workflow_instance_id
```
**Purpose**: Store clinical directives and guidelines (NPO, isolation, restrictions)

**Key Columns**:
- `instruction_type`: NPO, ISOLATION, DISCHARGE_RESTRICTION, etc.
- `instruction_text`: Full instruction text
- `blocking`: TRUE if blocks downstream steps until acknowledged
- `acknowledged`: TRUE if instruction has been acknowledged
- `acknowledged_by_user`: User who acknowledged
- `acknowledged_at`: Acknowledgment timestamp

**Indexes**:
- `idx_instructions_instruction_id` - Lookup by ID
- `idx_instructions_workflow_instance_id` - Find workflow instructions
- `idx_instructions_instruction_type` - Filter by type
- `idx_instructions_blocking` - Find blocking instructions

---

### 11. GATES (Formal Checkpoints)
```
Columns: 13
Primary Key: id (UUID)
Unique Keys: gate_id
Foreign Key: template_id
```
**Purpose**: Define formal checkpoints bundling requirements (safety, consents, assessments)

**Key Columns**:
- `gate_id`: Unique gate identifier
- `gate_type`: SAFETY, CONSENT, ASSESSMENT, CLEARANCE, CUSTOM
- `required`: TRUE if must be completed
- `is_open`: TRUE if gate is open/satisfied
- `instructions`: What must be verified

**Example Gates**:
- Surgical Safety Gate → WHO safety checklist
- Consent Gate → Patient consent verification
- Assessment Gate → Clinical assessment requirements

---

### 12. CHECKLIST_ITEMS (Gate Checklist Items)
```
Columns: 15
Primary Key: id (UUID)
Foreign Key: gate_id
```
**Purpose**: Individual items within gate checklists

**Key Columns**:
- `sequence_number`: Order in checklist
- `item_text`: What must be verified
- `completed`: Boolean completion status
- `completed_by_user`: User who completed
- `mandatory`: Must be completed to satisfy gate

---

### 13. DECISION_LOGIC (Conditional Routing)
```
Columns: 15
Primary Key: id (UUID)
Foreign Key: template_id
```
**Purpose**: Define IF-THEN routing based on patient data

**Key Columns**:
- `decision_id`: Unique decision identifier
- `data_point`: What to evaluate
- `operator`: EQUALS, GREATER_THAN, CONTAINS, etc.
- `expected_value`: Value to compare
- `true_path_task_id`: Task if condition true
- `false_path_task_id`: Task if condition false

**Example**:
```sql
-- Route based on age
IF patient_age > 65 THEN task-elderly ELSE task-standard
```

---

### 14. COMPENSATION_ACTIONS (Failure Recovery)
```
Columns: 15
Primary Key: id (UUID)
Foreign Key: order_id
```
**Purpose**: Automatic recovery actions when orders fail/cancel

**Key Columns**:
- `action_type`: CANCEL_ORDER, REVERSE_CHARGE, NOTIFY_LAB, etc.
- `action_description`: What action will do
- `executed`: TRUE if action completed
- `executed_at`: When executed
- `retry_count`: Retry attempt count
- `max_retries`: Maximum retry limit

**Automatic Compensation Creation**:
```sql
-- When order is cancelled, create compensation actions:
INSERT INTO compensation_actions (action_type, description)
VALUES ('REVERSE_CHARGE', 'Reverse $150 charge'),
       ('SEND_NOTIFICATION', 'Notify lab to discard specimen'),
       ('RELEASE_RESOURCE', 'Release reserved bed');
```

**Indexes**:
- `idx_compensation_actions_order_id` - Find order compensation
- `idx_compensation_actions_executed` - Find pending actions

---

### 15. AUDIT_LOGS (Immutable Timeline)
```
Columns: 14
Primary Key: id (UUID)
No Foreign Keys (intentional - immutable)
```
**Purpose**: Immutable audit trail of all entity changes (7-10 year retention)

**Key Columns**:
- `entity_type`: PATIENT, WORKFLOW, TASK, ORDER, etc.
- `entity_id`: What entity changed
- `action`: CREATED, UPDATED, TRANSITIONED, COMPLETED, etc.
- `actor`: Who made the change
- `action_timestamp`: When change occurred
- `details`: JSON-serialized change details
- `previous_value`, `new_value`: Before/after values
- `correlation_id`: Trace ID for distributed tracing
- `is_legal_hold`: TRUE if must retain for legal reasons

**Indexes**:
- `idx_audit_logs_entity_id` - Find entity changes
- `idx_audit_logs_action_timestamp` - Time-based queries
- `idx_audit_logs_correlation_id` - Distributed tracing
- `idx_audit_logs_patient_id` - Patient audit trail

**Immutability Strategy**:
```sql
-- No UPDATE or DELETE allowed
-- Only INSERT
-- Retention: 7-10 years
-- Archive after 3 years for performance
SELECT COUNT(*) FROM audit_logs
WHERE is_legal_hold = FALSE
  AND action_timestamp < DATE_SUB(CURRENT_DATE, INTERVAL 10 YEAR);
-- Move to archive table
```

**Example Queries**:
```sql
-- Patient audit trail
SELECT * FROM audit_logs
WHERE patient_id = 'patient-001'
ORDER BY action_timestamp DESC;

-- Trace a workflow change
SELECT * FROM audit_logs
WHERE correlation_id = 'corr-001'
ORDER BY action_timestamp ASC;

-- Compliance audit
SELECT * FROM audit_logs
WHERE entity_type = 'ORDER'
  AND action = 'CANCELLED'
  AND DATE(action_timestamp) = CURRENT_DATE;
```

---

### 16. TASK_NOTES (Append-Only Clinical Documentation)
```
Columns: 14
Primary Key: id (UUID)
Foreign Key: task_instance_id
```
**Purpose**: Immutable notes for task instances - clinical documentation, audit trail, compliance

**CRITICAL**: This table is **APPEND-ONLY**. Notes should NEVER be updated or deleted.
Corrections must be added as ADDENDUM type notes referencing the original.

**Key Columns**:
- `task_instance_id`: Task this note belongs to
- `note_type`: CREATION, ASSIGNMENT, START, PROGRESS, OBSERVATION, COMPLETION, SKIP, ESCALATION, FAILURE, COMMENT, HANDOFF, ALERT, ADDENDUM
- `content`: Note text (immutable once created)
- `author_user`: User who wrote the note
- `author_role`: Role at time of note creation
- `noted_at`: Timestamp of note creation
- `addendum_to_note_id`: Reference to note being corrected (for ADDENDUM type)
- `priority`: 0=Normal, 1=Important, 2=Critical
- `is_flagged`: Boolean for flagged/urgent notes

**Note Types**:
```
CREATION    - Note added when task/workflow created
PROGRESS    - Update during task execution
OBSERVATION - Clinical observation or finding
COMPLETION  - Note when task completed
SKIP        - Reason for skipping task
ESCALATION  - Escalation reason/documentation
HANDOFF     - Shift change handoff note
ADDENDUM    - Correction to previous note
```

**Indexes**:
- `idx_task_notes_task_instance` - Find task notes
- `idx_task_notes_noted_at` - Time-based queries
- `idx_task_notes_author` - Find notes by author
- `idx_task_notes_type` - Filter by note type
- `idx_task_notes_is_flagged` - Find urgent notes

**Example Queries**:
```sql
-- Get all notes for a task (chronological)
SELECT * FROM task_notes
WHERE task_instance_id = 'task-001'
ORDER BY noted_at ASC;

-- Get flagged notes requiring attention
SELECT * FROM task_notes
WHERE is_flagged = TRUE
ORDER BY noted_at DESC;

-- Find addenda to a note
SELECT * FROM task_notes
WHERE addendum_to_note_id = 'note-001'
ORDER BY noted_at ASC;
```

---

### 17. TASK_RESULTS (Append-Only Structured Results)
```
Columns: 21
Primary Key: id (UUID)
Foreign Key: task_instance_id
```
**Purpose**: Structured clinical results - measurements, observations, outcomes

**CRITICAL**: This table is **APPEND-ONLY**. Results should NEVER be updated.
Use CORRECTION type to correct a previous result.

**Key Columns**:
- `task_instance_id`: Task this result belongs to
- `result_type`: OUTCOME, MEASUREMENT, OBSERVATION, CALCULATED, TEST_RESULT, ASSESSMENT, PROCEDURE_OUTCOME, CORRECTION, SUMMARY
- `result_name`: Name of result (e.g., "Blood Pressure")
- `result_code`: Standard code (e.g., LOINC)
- `code_system`: Code system (e.g., "LOINC", "SNOMED")
- `result_value`: The actual value
- `unit`: Unit of measurement (e.g., "mmHg")
- `reference_range`: Normal range (e.g., "120-140")
- `interpretation_code`: N=Normal, H=High, L=Low, A=Abnormal
- `recorded_by_user`: User who recorded
- `recorded_at`: When recorded
- `observed_at`: When actually observed (may differ from recorded)
- `verified_by_user`: User who verified
- `verified_at`: Verification timestamp
- `is_verified`: Boolean verification status
- `is_critical`: Boolean for critical values
- `corrects_result_id`: For CORRECTION type

**Result Types**:
```
OUTCOME          - Primary task outcome
MEASUREMENT      - Numeric measurement (vital signs)
OBSERVATION      - Clinical observation
TEST_RESULT      - Lab/imaging result
ASSESSMENT       - Score (pain scale, ESI level)
CORRECTION       - Correction to previous result
```

**Indexes**:
- `idx_task_results_task_instance` - Find task results
- `idx_task_results_recorded_at` - Time-based queries
- `idx_task_results_type` - Filter by result type
- `idx_task_results_code` - Find by LOINC/SNOMED code
- `idx_task_results_is_verified` - Find unverified results
- `idx_task_results_is_critical` - Find critical values

**Example Queries**:
```sql
-- Get all results for a task
SELECT * FROM task_results
WHERE task_instance_id = 'task-001'
ORDER BY recorded_at ASC;

-- Find critical unverified results
SELECT * FROM task_results
WHERE is_critical = TRUE AND is_verified = FALSE;

-- Get results by LOINC code
SELECT * FROM task_results
WHERE result_code = '8480-6' -- Systolic BP
ORDER BY recorded_at DESC;
```

---

### 18. ORDER_NOTES (Append-Only Order Documentation)
```
Columns: 14
Primary Key: id (UUID)
Foreign Key: order_id
```
**Purpose**: Immutable notes for orders - indications, interpretations, audit trail

**CRITICAL**: This table is **APPEND-ONLY**. Notes should NEVER be updated or deleted.

**Key Columns**:
- `order_id`: Order this note belongs to
- `note_type`: CREATION, INDICATION, AUTHORIZATION, ACTIVATION, PROGRESS, RESULT, INTERPRETATION, VERIFICATION, CANCELLATION, CLOSURE, COMMENT, PRIORITY_CHANGE, MODIFICATION, ALERT, ADDENDUM
- `content`: Note text (immutable)
- `author_user`: User who wrote the note
- `author_role`: Role at time of note creation
- `noted_at`: Timestamp of note creation
- `addendum_to_note_id`: Reference to note being corrected
- `priority`: 0=Normal, 1=Important, 2=Critical
- `is_flagged`: Boolean for flagged notes

**Note Types**:
```
INDICATION      - Clinical reason for order
AUTHORIZATION   - Justification for signing
INTERPRETATION  - Clinical interpretation of results
CANCELLATION    - Reason for cancellation
ADDENDUM        - Correction to previous note
```

**Indexes**:
- `idx_order_notes_order` - Find order notes
- `idx_order_notes_noted_at` - Time-based queries
- `idx_order_notes_author` - Find notes by author
- `idx_order_notes_type` - Filter by note type
- `idx_order_notes_is_flagged` - Find urgent notes

**Example Queries**:
```sql
-- Get all notes for an order (chronological)
SELECT * FROM order_notes
WHERE order_id = 'order-001'
ORDER BY noted_at ASC;

-- Get clinical indication for an order
SELECT content FROM order_notes
WHERE order_id = 'order-001' AND note_type = 'INDICATION';

-- Get cancellation reasons for today
SELECT o.order_id, n.content, n.author_user
FROM orders o
JOIN order_notes n ON o.id = n.order_id
WHERE o.status = 'CANCELLED'
  AND n.note_type = 'CANCELLATION'
  AND DATE(o.cancelled_at) = CURRENT_DATE;
```

---

## Entity Relationship Diagram (Simplified)

```
                        PATIENTS (1)
                           |
                           | 1:M
                           v
                    WORKFLOW_INSTANCES
                      /    |    \
                     /     |     \
                1:M /   1:M|   1:M \
                   /       |        \
                  v        v         v
            TASK_INSTANCES ORDERS  INSTRUCTIONS
                 /   \      |   \
              1:M     1:M   |    1:M
               v       v    |     v
         TASK_NOTES  TASK_  |   ORDER_NOTES
                     RESULTS|
                            |
                         1:M|
                            v
                   COMPENSATION_ACTIONS


         WORKFLOW_TEMPLATES (1)
              /      |      \
             /    1:M|    1:M \
         1:M /       |         \
           /         v          v
          v   TASK_DEFINITIONS  GATES
     WORKFLOW_           |       |
     INSTANCES      1:M|  1:M|
                        v       v
                   (already    CHECKLIST_
                    linked)     ITEMS


          ORDER_SETS (1)
            /        \
        1:M /      1:M \
           /           \
          v             v
    ORDER_SET_ITEMS   ORDER_SET_
                      CONDITIONS


      DECISION_LOGIC ← 1:M ← WORKFLOW_TEMPLATES
```

---

## Performance Optimization Guide

### Query Patterns by Use Case

**1. Find User's Task Queue**
```sql
-- Optimized with composite index (assigned_to, status)
SELECT id, task_instance_id, task_definition_id, due_at
FROM task_instances
WHERE assigned_to = 'nurse.smith'
  AND status IN ('PENDING', 'IN_PROGRESS')
ORDER BY due_at ASC;
```

**2. Patient Workflow Summary**
```sql
-- Uses indexes on patient_id and workflow_instance_id
SELECT w.workflow_instance_id, w.status, COUNT(t.id) as task_count
FROM workflow_instances w
LEFT JOIN task_instances t ON w.id = t.workflow_instance_id
WHERE w.patient_id = 'patient-001'
GROUP BY w.id;
```

**3. SLA Monitoring**
```sql
-- Uses indexes on due_at and sla_breached
SELECT id, task_instance_id, assigned_to, due_at,
       DATEDIFF(MINUTE, CURRENT_TIMESTAMP, due_at) as minutes_remaining
FROM task_instances
WHERE due_at < CURRENT_TIMESTAMP
  AND status IN ('PENDING', 'IN_PROGRESS')
  AND sla_breached = FALSE
ORDER BY due_at ASC;
```

**4. Compliance Audit**
```sql
-- Uses indexes on entity_type and action_timestamp
SELECT entity_id, action, actor, action_timestamp
FROM audit_logs
WHERE entity_type IN ('ORDER', 'TASK', 'WORKFLOW')
  AND DATE(action_timestamp) BETWEEN ? AND ?
ORDER BY action_timestamp ASC;
```

---

## Data Retention & Archival

### Retention Policy

| Table | Retention | Archive | Legal Hold |
|-------|-----------|---------|-----------|
| audit_logs | 10 years | Yes (after 3 yrs) | Always |
| orders | 7 years | Yes (after 2 yrs) | If flagged |
| workflow_instances | 3 years | Yes | If flagged |
| task_instances | 3 years | Yes | If flagged |
| others | 2 years | Optional | If flagged |

### Archive Strategy

```sql
-- Move old data to archive table (monthly)
INSERT INTO audit_logs_archive
SELECT * FROM audit_logs
WHERE action_timestamp < DATE_SUB(CURRENT_DATE, INTERVAL 3 YEAR)
  AND is_legal_hold = FALSE;

DELETE FROM audit_logs
WHERE id IN (SELECT id FROM audit_logs_archive);
```

---

## Monitoring & Health Checks

### Database Health Queries

```sql
-- Table sizes
SELECT table_name, ROUND(SUM(data_length + index_length)/1024/1024, 2) as size_mb
FROM information_schema.TABLES
GROUP BY table_name
ORDER BY size_mb DESC;

-- Index effectiveness
SELECT * FROM information_schema.STATISTICS
WHERE table_schema = DATABASE()
ORDER BY table_name, seq_in_index;

-- Lock monitoring (MySQL)
SHOW OPEN TABLES WHERE in_use > 0;

-- Query performance (PostgreSQL)
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY total_time DESC LIMIT 10;
```

---

## Migration Guide

### Adding New Table

1. Create table definition in `ddl/01-schema.sql`
2. Add indexes for frequently queried columns
3. Create corresponding JPA entity
4. Create repository interface
5. Add service methods
6. Add REST controller endpoints
7. Add to audit logging
8. Document relationships

### Schema Change Process

1. **Development**: Modify DDL, let Hibernate auto-update
2. **Staging**: Apply DDL manually, test thoroughly
3. **Production**:
   - Create backup
   - Apply DDL during maintenance window
   - Verify with SELECT queries
   - Monitor application logs

---

## Troubleshooting

### Common Issues

**Issue: Slow Queries**
```sql
-- Analyze execution plan
EXPLAIN ANALYZE SELECT ...;

-- Check missing indexes
SELECT * FROM information_schema.STATISTICS
WHERE table_name = 'workflow_instances' AND seq_in_index = 1;
```

**Issue: Foreign Key Constraint**
```sql
-- Check constraint
SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINTS
WHERE TABLE_NAME = 'task_instances';

-- Investigate orphaned records
SELECT * FROM task_instances
WHERE workflow_instance_id NOT IN (SELECT id FROM workflow_instances);
```

**Issue: Lock Contention**
```sql
-- Find blocking queries (PostgreSQL)
SELECT blocked_locks.pid, blocked_locks.relation,
       blocking_locks.pid, blocking_locks.relation
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
  AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
  AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
  AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
  AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
  AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
  AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
  AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
  AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
  AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
  AND blocking_locks.granted AND NOT blocked_locks.granted;
```

---

## Files Summary

| File | Purpose | Size | Records |
|------|---------|------|---------|
| `01-schema.sql` | Complete DDL | ~750 lines | 18 tables, 55+ indexes |
| `sample-data.sql` | Test data | ~800 lines | 40+ records |
| `init-db.sh` | Setup script | ~250 lines | Supports 3 DBs |
| `README.md` | DB docs | ~800 lines | Complete reference |

---

**Last Updated**: 2024-12-15
**Database Version**: 1.1
**Status**: Production Ready

### Version History
- **1.1** (2024-12-15): Added task/order notes, task results, scheduling constraints
- **1.0** (2024-11-10): Initial schema with 15 tables
