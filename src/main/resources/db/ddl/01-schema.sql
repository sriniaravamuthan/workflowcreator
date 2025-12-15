-- ============================================================================
-- HMIS Workflow Engine - Database Schema DDL
-- ============================================================================
-- Created for H2 Database
-- This script creates all tables for the workflow engine
-- ============================================================================

-- ============================================================================
-- PATIENTS TABLE
-- ============================================================================
CREATE TABLE patients (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    patient_id VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    phone_number VARCHAR(20),
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    medical_history CLOB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- ============================================================================
-- WORKFLOW_TEMPLATES TABLE
-- ============================================================================
CREATE TABLE workflow_templates (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 1,
    category VARCHAR(50),
    review_status VARCHAR(100),
    approved_by_user VARCHAR(100),
    notes CLOB,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    cloned_from_template_id VARCHAR(36),
    parent_template_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- ============================================================================
-- WORKFLOW_TASK_DEFINITIONS TABLE
-- ============================================================================
-- Task Dependency Model:
-- - predecessor_task_ids: Comma-separated list of task definition IDs that must
--   complete before this task can start. If NULL or empty, this is an "entry task"
--   that starts immediately when the workflow begins.
-- - next_task_id: Legacy successor-based chaining (optional).
-- - failure_task_id: Task to activate if this task fails.
-- ============================================================================
CREATE TABLE workflow_task_definitions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    template_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    task_order INT NOT NULL,
    assign_to VARCHAR(100),
    estimated_duration_minutes INT DEFAULT 0,
    instructions CLOB,
    is_parallel BOOLEAN NOT NULL DEFAULT FALSE,
    is_optional BOOLEAN NOT NULL DEFAULT FALSE,
    -- Predecessor task IDs (comma-separated). NULL or empty = entry task (starts immediately)
    predecessor_task_ids CLOB,
    -- Legacy: Next task to activate (consider using predecessor_task_ids instead)
    next_task_id VARCHAR(100),
    failure_task_id VARCHAR(100),
    metadata CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (template_id) REFERENCES workflow_templates(id) ON DELETE CASCADE
);

-- ============================================================================
-- TEMPLATE_ORDERS TABLE
-- ============================================================================
CREATE TABLE template_orders (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    template_id VARCHAR(36) NOT NULL,
    order_code VARCHAR(100) NOT NULL,
    order_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    external_api_endpoint VARCHAR(500),
    api_method VARCHAR(10) NOT NULL DEFAULT 'GET',
    api_request_payload CLOB,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    is_automatic BOOLEAN NOT NULL DEFAULT FALSE,
    order_sequence INT,
    metadata CLOB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (template_id) REFERENCES workflow_templates(id) ON DELETE CASCADE,
    UNIQUE (template_id, order_code)
);

-- ============================================================================
-- WORKFLOW_INSTANCES TABLE
-- ============================================================================
-- Workflow Context:
-- - patient_id: The patient this workflow is for (required)
-- - encounter_id: The clinical encounter (e.g., ER visit, admission)
-- - visit_id: The ADT visit tracking ID (for visit tracking)
--
-- Uniqueness: Only one active workflow per patient + encounter + template combination.
-- This prevents duplicate workflows for the same clinical context.
-- ============================================================================
CREATE TABLE workflow_instances (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow_instance_id VARCHAR(100) NOT NULL UNIQUE,
    patient_id VARCHAR(36) NOT NULL,
    template_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes VARCHAR(500),
    -- Clinical context identifiers
    encounter_id VARCHAR(100),  -- Clinical encounter (e.g., ER visit, admission)
    visit_id VARCHAR(100),      -- ADT visit tracking ID
    -- Escalation tracking
    is_escalated BOOLEAN NOT NULL DEFAULT FALSE,
    escalation_reason VARCHAR(500),
    -- Review/approval workflow
    review_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by_user VARCHAR(100),
    reviewed_at TIMESTAMP,
    review_notes CLOB,
    can_execute BOOLEAN NOT NULL DEFAULT FALSE,
    -- Timestamps
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (template_id) REFERENCES workflow_templates(id) ON DELETE RESTRICT
);

-- ============================================================================
-- TASK_INSTANCES TABLE
-- ============================================================================
-- Supports both template-based tasks and ad-hoc tasks.
-- Ad-hoc tasks are created dynamically at runtime by clinicians (e.g., doctor
-- orders "administer saline") without being part of the original workflow template.
-- ============================================================================
CREATE TABLE task_instances (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow_instance_id VARCHAR(36) NOT NULL,
    -- task_definition_id is nullable for ad-hoc tasks
    task_definition_id VARCHAR(36),
    task_instance_id VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    assigned_to VARCHAR(255),
    required_role VARCHAR(100),
    task_input CLOB,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    comments CLOB,
    result CLOB,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    error_message VARCHAR(500),
    due_at TIMESTAMP,
    escalated_at TIMESTAMP,
    is_escalated BOOLEAN NOT NULL DEFAULT FALSE,
    escalated_to_user VARCHAR(100),
    sla_minutes INT DEFAULT 0,
    sla_breached BOOLEAN NOT NULL DEFAULT FALSE,
    -- Ad-hoc task support
    is_adhoc BOOLEAN NOT NULL DEFAULT FALSE,
    adhoc_task_name VARCHAR(100),
    adhoc_task_description VARCHAR(500),
    created_by_user VARCHAR(255),
    -- Skip support
    skip_reason CLOB,
    skipped_by_user VARCHAR(255),
    -- Audit fields
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE,
    FOREIGN KEY (task_definition_id) REFERENCES workflow_task_definitions(id) ON DELETE RESTRICT
);

-- ============================================================================
-- ORDERS TABLE
-- ============================================================================
CREATE TABLE orders (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow_instance_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    order_type VARCHAR(50) NOT NULL,
    order_description VARCHAR(255) NOT NULL,
    order_code VARCHAR(100),
    department_target VARCHAR(100),
    ordered_by_user VARCHAR(100),
    authorized_by_user VARCHAR(100),
    authorized_at TIMESTAMP,
    activated_at TIMESTAMP,
    resulted_at TIMESTAMP,
    verified_at TIMESTAMP,
    closed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason CLOB,
    result CLOB,
    estimated_cost DECIMAL(10, 2),
    actual_cost DECIMAL(10, 2),
    priority INT DEFAULT 0,
    metadata CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE
);

-- ============================================================================
-- ORDER_ADJUSTMENTS TABLE
-- ============================================================================
CREATE TABLE order_adjustments (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    adjustment_code VARCHAR(100) NOT NULL UNIQUE,
    order_id VARCHAR(36) NOT NULL,
    workflow_instance_id VARCHAR(36) NOT NULL,
    parameter_name VARCHAR(255) NOT NULL,
    original_value CLOB NOT NULL,
    adjusted_value CLOB NOT NULL,
    adjustment_reason CLOB,
    adjustment_type VARCHAR(50) NOT NULL DEFAULT 'PARAMETER',
    is_applied BOOLEAN NOT NULL DEFAULT FALSE,
    adjusted_by_user VARCHAR(100),
    adjusted_at TIMESTAMP,
    metadata CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE
);

-- ============================================================================
-- ORDER_SETS TABLE
-- ============================================================================
CREATE TABLE order_sets (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    order_set_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    clinical_condition VARCHAR(100),
    category VARCHAR(100),
    version INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_parallel BOOLEAN NOT NULL DEFAULT FALSE,
    instructions CLOB,
    created_by_user VARCHAR(100),
    approved_by_user VARCHAR(100),
    access_level VARCHAR(50),
    metadata CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(255)
);

-- ============================================================================
-- ORDER_SET_ITEMS TABLE
-- ============================================================================
CREATE TABLE order_set_items (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    order_set_id VARCHAR(36) NOT NULL,
    sequence_number INT NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    order_type VARCHAR(50),
    item_name VARCHAR(255) NOT NULL,
    item_description VARCHAR(500),
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    is_parallel BOOLEAN NOT NULL DEFAULT FALSE,
    depends_on_item_id VARCHAR(100),
    conditional_logic_id VARCHAR(100),
    default_parameters CLOB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (order_set_id) REFERENCES order_sets(id) ON DELETE CASCADE
);

-- ============================================================================
-- ORDER_SET_CONDITIONS TABLE
-- ============================================================================
CREATE TABLE order_set_conditions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    order_set_id VARCHAR(36) NOT NULL,
    condition_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    data_point VARCHAR(100) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    expected_value CLOB NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    logical_connector VARCHAR(50),
    metadata CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (order_set_id) REFERENCES order_sets(id) ON DELETE CASCADE
);

-- ============================================================================
-- CLINICAL_DATA_POINTS TABLE
-- ============================================================================
CREATE TABLE clinical_data_points (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    data_point_code VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    value CLOB,
    unit VARCHAR(20),
    numeric_value DECIMAL(10, 2),
    measurement_time TIMESTAMP NOT NULL,
    measured_by_user VARCHAR(100),
    measurement_method VARCHAR(100),
    data_source VARCHAR(100),
    is_normal BOOLEAN NOT NULL DEFAULT FALSE,
    reference_range CLOB,
    interpretation CLOB,
    metadata CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

-- ============================================================================
-- INSTRUCTIONS TABLE
-- ============================================================================
CREATE TABLE instructions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow_instance_id VARCHAR(36) NOT NULL,
    instruction_id VARCHAR(100) NOT NULL UNIQUE,
    instruction_type VARCHAR(50) NOT NULL,
    instruction_text CLOB NOT NULL,
    blocking BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMP,
    acknowledged_by_user VARCHAR(100),
    acknowledged_notes CLOB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE
);

-- ============================================================================
-- GATES TABLE
-- ============================================================================
CREATE TABLE gates (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    template_id VARCHAR(36) NOT NULL,
    gate_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    gate_type VARCHAR(100) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    is_open BOOLEAN NOT NULL DEFAULT FALSE,
    instructions CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (template_id) REFERENCES workflow_templates(id) ON DELETE CASCADE
);

-- ============================================================================
-- CHECKLIST_ITEMS TABLE
-- ============================================================================
CREATE TABLE checklist_items (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    gate_id VARCHAR(36) NOT NULL,
    sequence_number INT NOT NULL,
    item_text VARCHAR(255) NOT NULL,
    item_description VARCHAR(500),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_by_user VARCHAR(100),
    completed_at TIMESTAMP,
    completion_notes CLOB,
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    instructions CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (gate_id) REFERENCES gates(id) ON DELETE CASCADE
);

-- ============================================================================
-- DECISION_LOGIC TABLE
-- ============================================================================
CREATE TABLE decision_logic (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    template_id VARCHAR(36) NOT NULL,
    decision_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    data_point VARCHAR(100) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    expected_value CLOB NOT NULL,
    true_path_task_id VARCHAR(100) NOT NULL,
    false_path_task_id VARCHAR(100) NOT NULL,
    metadata CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (template_id) REFERENCES workflow_templates(id) ON DELETE CASCADE
);

-- ============================================================================
-- COMPENSATION_ACTIONS TABLE
-- ============================================================================
CREATE TABLE compensation_actions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_description VARCHAR(255) NOT NULL,
    triggering_event VARCHAR(100),
    executed BOOLEAN NOT NULL DEFAULT FALSE,
    executed_at TIMESTAMP,
    execution_result VARCHAR(500),
    error_message VARCHAR(500),
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    metadata CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- ============================================================================
-- AUDIT_LOGS TABLE
-- ============================================================================
CREATE TABLE audit_logs (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(36) NOT NULL,
    action VARCHAR(100) NOT NULL,
    actor VARCHAR(100),
    action_timestamp TIMESTAMP NOT NULL,
    details CLOB NOT NULL,
    previous_value VARCHAR(100),
    new_value VARCHAR(100),
    correlation_id VARCHAR(36),
    patient_id VARCHAR(36),
    workflow_instance_id VARCHAR(36),
    is_legal_hold BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- Patient indexes
CREATE INDEX idx_patients_patient_id ON patients(patient_id);
CREATE INDEX idx_patients_email ON patients(email);
CREATE INDEX idx_patients_active ON patients(active);

-- Workflow Template indexes
CREATE INDEX idx_workflow_templates_name ON workflow_templates(name);
CREATE INDEX idx_workflow_templates_category ON workflow_templates(category);
CREATE INDEX idx_workflow_templates_review_status ON workflow_templates(review_status);
CREATE INDEX idx_workflow_templates_active ON workflow_templates(active);
CREATE INDEX idx_workflow_templates_is_deleted ON workflow_templates(is_deleted);
CREATE INDEX idx_workflow_templates_cloned_from ON workflow_templates(cloned_from_template_id);
CREATE INDEX idx_workflow_templates_parent ON workflow_templates(parent_template_id);

-- Workflow Task Definition indexes
CREATE INDEX idx_workflow_task_definitions_template_id ON workflow_task_definitions(template_id);
CREATE INDEX idx_workflow_task_definitions_task_order ON workflow_task_definitions(task_order);

-- Template Order indexes
CREATE INDEX idx_template_orders_template_id ON template_orders(template_id);
CREATE INDEX idx_template_orders_order_code ON template_orders(order_code);
CREATE INDEX idx_template_orders_is_active ON template_orders(is_active);
CREATE INDEX idx_template_orders_is_automatic ON template_orders(is_automatic);
CREATE INDEX idx_template_orders_order_sequence ON template_orders(order_sequence);

-- Workflow Instance indexes
CREATE INDEX idx_workflow_instances_workflow_instance_id ON workflow_instances(workflow_instance_id);
CREATE INDEX idx_workflow_instances_patient_id ON workflow_instances(patient_id);
CREATE INDEX idx_workflow_instances_template_id ON workflow_instances(template_id);
CREATE INDEX idx_workflow_instances_status ON workflow_instances(status);
CREATE INDEX idx_workflow_instances_encounter_id ON workflow_instances(encounter_id);
CREATE INDEX idx_workflow_instances_visit_id ON workflow_instances(visit_id);
CREATE INDEX idx_workflow_instances_is_escalated ON workflow_instances(is_escalated);
CREATE INDEX idx_workflow_instances_created_at ON workflow_instances(created_at);
CREATE INDEX idx_workflow_instances_review_status ON workflow_instances(review_status);
CREATE INDEX idx_workflow_instances_can_execute ON workflow_instances(can_execute);
-- Composite index for uniqueness check: patient + encounter + template
CREATE INDEX idx_workflow_instances_patient_encounter_template ON workflow_instances(patient_id, encounter_id, template_id);

-- Task Instance indexes
CREATE INDEX idx_task_instances_task_instance_id ON task_instances(task_instance_id);
CREATE INDEX idx_task_instances_workflow_instance_id ON task_instances(workflow_instance_id);
CREATE INDEX idx_task_instances_task_definition_id ON task_instances(task_definition_id);
CREATE INDEX idx_task_instances_status ON task_instances(status);
CREATE INDEX idx_task_instances_assigned_to ON task_instances(assigned_to);
CREATE INDEX idx_task_instances_due_at ON task_instances(due_at);
CREATE INDEX idx_task_instances_sla_breached ON task_instances(sla_breached);
CREATE INDEX idx_task_instances_is_escalated ON task_instances(is_escalated);
CREATE INDEX idx_task_instances_is_adhoc ON task_instances(is_adhoc);

-- Order indexes
CREATE INDEX idx_orders_order_id ON orders(order_id);
CREATE INDEX idx_orders_workflow_instance_id ON orders(workflow_instance_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_type ON orders(order_type);
CREATE INDEX idx_orders_department_target ON orders(department_target);

-- Order Adjustment indexes
CREATE INDEX idx_order_adjustments_order_id ON order_adjustments(order_id);
CREATE INDEX idx_order_adjustments_workflow_instance_id ON order_adjustments(workflow_instance_id);
CREATE INDEX idx_order_adjustments_is_applied ON order_adjustments(is_applied);
CREATE INDEX idx_order_adjustments_adjustment_code ON order_adjustments(adjustment_code);
CREATE INDEX idx_order_adjustments_adjustment_type ON order_adjustments(adjustment_type);

-- Clinical Data Point indexes
CREATE INDEX idx_clinical_data_points_patient_id ON clinical_data_points(patient_id);
CREATE INDEX idx_clinical_data_points_data_point_code ON clinical_data_points(data_point_code);
CREATE INDEX idx_clinical_data_points_measurement_time ON clinical_data_points(measurement_time);
CREATE INDEX idx_clinical_data_points_is_normal ON clinical_data_points(is_normal);
CREATE INDEX idx_clinical_data_points_patient_code ON clinical_data_points(patient_id, data_point_code);

-- Order Set indexes
CREATE INDEX idx_order_sets_order_set_id ON order_sets(order_set_id);
CREATE INDEX idx_order_sets_name ON order_sets(name);
CREATE INDEX idx_order_sets_clinical_condition ON order_sets(clinical_condition);
CREATE INDEX idx_order_sets_category ON order_sets(category);
CREATE INDEX idx_order_sets_access_level ON order_sets(access_level);
CREATE INDEX idx_order_sets_active ON order_sets(active);

-- Instruction indexes
CREATE INDEX idx_instructions_instruction_id ON instructions(instruction_id);
CREATE INDEX idx_instructions_workflow_instance_id ON instructions(workflow_instance_id);
CREATE INDEX idx_instructions_instruction_type ON instructions(instruction_type);
CREATE INDEX idx_instructions_blocking ON instructions(blocking);

-- Gate indexes
CREATE INDEX idx_gates_gate_id ON gates(gate_id);
CREATE INDEX idx_gates_template_id ON gates(template_id);
CREATE INDEX idx_gates_gate_type ON gates(gate_type);

-- Decision Logic indexes
CREATE INDEX idx_decision_logic_decision_id ON decision_logic(decision_id);
CREATE INDEX idx_decision_logic_template_id ON decision_logic(template_id);

-- Compensation Action indexes
CREATE INDEX idx_compensation_actions_order_id ON compensation_actions(order_id);
CREATE INDEX idx_compensation_actions_action_type ON compensation_actions(action_type);
CREATE INDEX idx_compensation_actions_executed ON compensation_actions(executed);

-- Audit Log indexes
CREATE INDEX idx_audit_logs_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_action_timestamp ON audit_logs(action_timestamp);
CREATE INDEX idx_audit_logs_correlation_id ON audit_logs(correlation_id);
CREATE INDEX idx_audit_logs_patient_id ON audit_logs(patient_id);
CREATE INDEX idx_audit_logs_workflow_instance_id ON audit_logs(workflow_instance_id);
CREATE INDEX idx_audit_logs_is_legal_hold ON audit_logs(is_legal_hold);

-- ============================================================================
-- SEQUENCES (if using database that requires them)
-- ============================================================================
-- For H2, sequences are optional as we use UUID generation
-- Uncomment if needed for other databases

-- CREATE SEQUENCE seq_workflow_tasks START WITH 1 INCREMENT BY 1;
-- CREATE SEQUENCE seq_task_instances START WITH 1 INCREMENT BY 1;
-- CREATE SEQUENCE seq_orders START WITH 1000 INCREMENT BY 1;

-- ============================================================================
-- SCHEMA NOTES
-- ============================================================================
-- 1. All ID columns use VARCHAR(36) for UUID support
-- 2. Timestamps use TIMESTAMP for both created_at and updated_at
-- 3. Large text fields use CLOB for database independence
-- 4. Foreign keys use CASCADE delete for parent-child relationships
-- 5. RESTRICT delete is used where data integrity is critical
-- 6. Indexes are created for all frequently queried columns
-- 7. Audit tables have minimal constraints to allow long-term retention
-- 8. All timestamps are required and managed by Hibernate
-- ============================================================================
