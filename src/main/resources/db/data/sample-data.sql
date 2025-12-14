-- ============================================================================
-- HMIS Workflow Engine - Sample Data
-- ============================================================================
-- This script inserts sample data for testing and demonstration
-- ============================================================================

-- ============================================================================
-- INSERT SAMPLE PATIENTS
-- ============================================================================
INSERT INTO patients (id, patient_id, first_name, last_name, middle_name, email, phone_number, date_of_birth, gender, medical_history, active, created_at, updated_at, created_by, updated_by)
VALUES
    ('patient-001', 'PAT001', 'John', 'Doe', 'Michael', 'john.doe@example.com', '555-0101', '1980-05-15', 'M', 'Hypertension, Diabetes Type 2', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('patient-002', 'PAT002', 'Jane', 'Smith', 'Elizabeth', 'jane.smith@example.com', '555-0102', '1985-08-22', 'F', 'Asthma, Allergies', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('patient-003', 'PAT003', 'Robert', 'Johnson', 'James', 'robert.johnson@example.com', '555-0103', '1975-12-10', 'M', 'Cardiac History', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('patient-004', 'PAT004', 'Mary', 'Williams', NULL, 'mary.williams@example.com', '555-0104', '1990-03-05', 'F', 'None reported', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('patient-005', 'PAT005', 'William', 'Brown', 'Joseph', 'william.brown@example.com', '555-0105', '1988-07-18', 'M', 'Chronic pain management', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

-- ============================================================================
-- INSERT SAMPLE WORKFLOW TEMPLATES
-- ============================================================================
INSERT INTO workflow_templates (id, name, description, active, version, category, review_status, approved_by_user, notes, created_at, updated_at, created_by, updated_by)
VALUES
    ('template-001', 'Emergency Admission', 'Workflow for emergency patient admission and triage', TRUE, 1, 'Emergency', 'PUBLISHED', 'admin.user', 'Standard emergency admission process', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('template-002', 'Outpatient Visit', 'Routine outpatient clinic visit workflow', TRUE, 1, 'Outpatient', 'PUBLISHED', 'admin.user', 'Standard outpatient visit process', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('template-003', 'Lab Test Processing', 'Standard laboratory test workflow', TRUE, 1, 'Laboratory', 'PUBLISHED', 'admin.user', 'Lab processing workflow', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('template-004', 'Post-Op Recovery', 'Post-operative recovery workflow', TRUE, 1, 'Surgery', 'DRAFT', NULL, 'Work in progress', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

-- ============================================================================
-- INSERT SAMPLE WORKFLOW TASK DEFINITIONS
-- ============================================================================
INSERT INTO workflow_task_definitions (id, template_id, name, description, task_order, assign_to, estimated_duration_minutes, instructions, is_parallel, is_optional, next_task_id, failure_task_id, metadata, created_at, updated_at, created_by, updated_by)
VALUES
    ('task-def-001', 'template-001', 'Patient Registration', 'Register patient in system', 1, 'registration', 15, 'Verify patient identity and create/update registration', FALSE, FALSE, NULL, NULL, '{"priority":"high"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-def-002', 'template-001', 'Vital Signs', 'Record patient vital signs', 2, 'nurse', 10, 'Record BP, HR, Temperature, Respiratory Rate', FALSE, FALSE, NULL, NULL, '{"priority":"high"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-def-003', 'template-001', 'Triage Assessment', 'Perform ESI triage', 3, 'nurse', 15, 'Perform ESI triage assessment', FALSE, FALSE, NULL, NULL, '{"priority":"high"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-def-004', 'template-001', 'Physician Consultation', 'Doctor consultation', 4, 'physician', 30, 'Conduct medical examination and evaluation', FALSE, FALSE, NULL, NULL, '{"priority":"high"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-def-005', 'template-002', 'Check-in', 'Patient check-in at clinic', 1, 'reception', 10, 'Verify appointment and check patient in', FALSE, FALSE, NULL, NULL, '{"priority":"medium"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-def-006', 'template-002', 'Vitals', 'Collect vital signs', 2, 'nurse', 5, 'BP, HR, Temp, RR, Weight, Height', FALSE, FALSE, NULL, NULL, '{"priority":"medium"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-def-007', 'template-002', 'Doctor Visit', 'Patient sees physician', 3, 'physician', 20, 'Clinical consultation', FALSE, FALSE, NULL, NULL, '{"priority":"high"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-def-008', 'template-003', 'Sample Collection', 'Collect lab sample', 1, 'lab_tech', 10, 'Collect appropriate sample for test', FALSE, FALSE, NULL, NULL, '{"priority":"high"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-def-009', 'template-003', 'Sample Processing', 'Process sample in lab', 2, 'lab_tech', 30, 'Process sample and run test', FALSE, FALSE, NULL, NULL, '{"priority":"high"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-def-010', 'template-003', 'Quality Check', 'QA check on results', 3, 'lab_supervisor', 10, 'Verify test results quality', FALSE, FALSE, NULL, NULL, '{"priority":"medium"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

-- ============================================================================
-- INSERT SAMPLE WORKFLOW INSTANCES
-- ============================================================================
INSERT INTO workflow_instances (id, workflow_instance_id, patient_id, template_id, status, notes, encounter_id, is_escalated, escalation_reason, started_at, completed_at, created_at, updated_at, created_by, updated_by)
VALUES
    ('workflow-inst-001', 'WF-2024-001', 'patient-001', 'template-001', 'ACTIVE', 'Emergency admission in progress', 'ENC-001', FALSE, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('workflow-inst-002', 'WF-2024-002', 'patient-002', 'template-002', 'ACTIVE', 'Routine clinic visit', 'ENC-002', FALSE, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('workflow-inst-003', 'WF-2024-003', 'patient-003', 'template-003', 'ACTIVE', 'Lab test processing', 'ENC-003', FALSE, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('workflow-inst-004', 'WF-2024-004', 'patient-004', 'template-001', 'COMPLETED', 'Emergency admission completed', 'ENC-004', FALSE, NULL, DATEADD(HOUR, -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, DATEADD(HOUR, -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

-- ============================================================================
-- INSERT SAMPLE TASK INSTANCES
-- ============================================================================
INSERT INTO task_instances (id, workflow_instance_id, task_definition_id, task_instance_id, status, assigned_to, required_role, task_input, started_at, completed_at, comments, result, retry_count, max_retries, error_message, due_at, escalated_at, is_escalated, escalated_to_user, sla_minutes, sla_breached, created_at, updated_at, created_by, updated_by)
VALUES
    ('task-inst-001', 'workflow-inst-001', 'task-def-001', 'TASK-2024-001', 'COMPLETED', 'registration', 'registration', NULL, DATEADD(MINUTE, -15, CURRENT_TIMESTAMP), DATEADD(MINUTE, -10, CURRENT_TIMESTAMP), 'Patient registered successfully', 'Patient ID verified', 0, 3, NULL, DATEADD(MINUTE, 15, CURRENT_TIMESTAMP), NULL, FALSE, NULL, 15, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-inst-002', 'workflow-inst-001', 'task-def-002', 'TASK-2024-002', 'IN_PROGRESS', 'nurse.smith', 'nurse', NULL, DATEADD(MINUTE, -5, CURRENT_TIMESTAMP), NULL, 'Recording vital signs', NULL, 0, 3, NULL, DATEADD(MINUTE, 5, CURRENT_TIMESTAMP), NULL, FALSE, NULL, 10, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-inst-003', 'workflow-inst-001', 'task-def-003', 'TASK-2024-003', 'PENDING', NULL, 'nurse', NULL, NULL, NULL, NULL, NULL, 0, 3, NULL, DATEADD(MINUTE, 20, CURRENT_TIMESTAMP), NULL, FALSE, NULL, 15, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-inst-004', 'workflow-inst-002', 'task-def-005', 'TASK-2024-004', 'COMPLETED', 'reception.jones', 'reception', NULL, DATEADD(MINUTE, -20, CURRENT_TIMESTAMP), DATEADD(MINUTE, -18, CURRENT_TIMESTAMP), 'Checked in successfully', 'Confirmed appointment', 0, 3, NULL, DATEADD(MINUTE, 10, CURRENT_TIMESTAMP), NULL, FALSE, NULL, 10, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-inst-005', 'workflow-inst-002', 'task-def-006', 'TASK-2024-005', 'COMPLETED', 'nurse.patel', 'nurse', NULL, DATEADD(MINUTE, -15, CURRENT_TIMESTAMP), DATEADD(MINUTE, -12, CURRENT_TIMESTAMP), 'Vitals recorded', 'BP: 120/80, HR: 72, Temp: 98.6F', 0, 3, NULL, DATEADD(MINUTE, 5, CURRENT_TIMESTAMP), NULL, FALSE, NULL, 5, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-inst-006', 'workflow-inst-002', 'task-def-007', 'TASK-2024-006', 'IN_PROGRESS', 'dr.wilson', 'physician', NULL, DATEADD(MINUTE, -8, CURRENT_TIMESTAMP), NULL, 'Doctor consultation in progress', NULL, 0, 3, NULL, DATEADD(MINUTE, 12, CURRENT_TIMESTAMP), NULL, FALSE, NULL, 20, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-inst-007', 'workflow-inst-003', 'task-def-008', 'TASK-2024-007', 'COMPLETED', 'lab.tech.001', 'lab_tech', NULL, DATEADD(MINUTE, -25, CURRENT_TIMESTAMP), DATEADD(MINUTE, -20, CURRENT_TIMESTAMP), 'Sample collected', 'Blood sample', 0, 3, NULL, DATEADD(MINUTE, 10, CURRENT_TIMESTAMP), NULL, FALSE, NULL, 10, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('task-inst-008', 'workflow-inst-003', 'task-def-009', 'TASK-2024-008', 'IN_PROGRESS', 'lab.tech.002', 'lab_tech', NULL, DATEADD(MINUTE, -15, CURRENT_TIMESTAMP), NULL, 'Sample processing', NULL, 0, 3, NULL, DATEADD(MINUTE, 15, CURRENT_TIMESTAMP), NULL, FALSE, NULL, 30, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

-- ============================================================================
-- INSERT SAMPLE ORDERS
-- ============================================================================
INSERT INTO orders (id, workflow_instance_id, order_id, status, order_type, order_description, order_code, department_target, ordered_by_user, authorized_by_user, authorized_at, activated_at, resulted_at, verified_at, closed_at, cancelled_at, cancellation_reason, result, estimated_cost, actual_cost, priority, metadata, created_at, updated_at, created_by, updated_by)
VALUES
    ('order-001', 'workflow-inst-001', 'ORD-2024-001', 'ACTIVATED', 'LAB_TEST', 'Complete Blood Count (CBC)', 'CBC001', 'LABORATORY', 'dr.smith', 'dr.smith', DATEADD(MINUTE, -30, CURRENT_TIMESTAMP), DATEADD(MINUTE, -25, CURRENT_TIMESTAMP), NULL, NULL, NULL, NULL, NULL, NULL, 150.00, NULL, 1, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('order-002', 'workflow-inst-001', 'ORD-2024-002', 'IN_PROGRESS', 'IMAGING', 'Chest X-Ray', 'CXR001', 'IMAGING', 'dr.smith', 'dr.smith', DATEADD(MINUTE, -25, CURRENT_TIMESTAMP), DATEADD(MINUTE, -20, CURRENT_TIMESTAMP), NULL, NULL, NULL, NULL, NULL, NULL, 200.00, NULL, 1, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('order-003', 'workflow-inst-002', 'ORD-2024-003', 'RESULTED', 'LAB_TEST', 'Lipid Panel', 'LP001', 'LABORATORY', 'dr.wilson', 'dr.wilson', DATEADD(MINUTE, -45, CURRENT_TIMESTAMP), DATEADD(MINUTE, -40, CURRENT_TIMESTAMP), DATEADD(MINUTE, -5, CURRENT_TIMESTAMP), NULL, NULL, NULL, NULL, 'Total Cholesterol: 185, LDL: 110, HDL: 55, Triglycerides: 100', 120.00, 120.00, 0, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('order-004', 'workflow-inst-003', 'ORD-2024-004', 'COMPLETED', 'LAB_TEST', 'Blood Glucose', 'BG001', 'LABORATORY', 'dr.johnson', 'dr.johnson', DATEADD(MINUTE, -60, CURRENT_TIMESTAMP), DATEADD(MINUTE, -55, CURRENT_TIMESTAMP), DATEADD(MINUTE, -20, CURRENT_TIMESTAMP), DATEADD(MINUTE, -15, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, NULL, NULL, 'Glucose Level: 105 mg/dL', 50.00, 50.00, 0, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

-- ============================================================================
-- INSERT SAMPLE ORDER SETS
-- ============================================================================
INSERT INTO order_sets (id, order_set_id, name, description, clinical_condition, category, version, active, is_parallel, instructions, created_by_user, approved_by_user, access_level, metadata, created_at, updated_at, updated_by)
VALUES
    ('orderset-001', 'ORDS-001', 'Diabetes Management Bundle', 'Complete diabetes management order set', 'Diabetes Type 2', 'Endocrinology', 1, TRUE, FALSE, 'Apply for diabetes patients', 'dr.endocrinologist', 'chief.medical', 'HOSPITAL_WIDE', '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('orderset-002', 'ORDS-002', 'Cardiac Assessment Bundle', 'Comprehensive cardiac evaluation', 'Cardiac Condition', 'Cardiology', 1, TRUE, FALSE, 'Apply for cardiac patients', 'dr.cardiologist', 'chief.medical', 'HOSPITAL_WIDE', '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('orderset-003', 'ORDS-003', 'Post-Op Recovery Bundle', 'Post-operative recovery orders', 'Post-Operative', 'Surgery', 1, TRUE, TRUE, 'Apply after surgery', 'dr.surgeon', 'chief.medical', 'HOSPITAL_WIDE', '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

-- ============================================================================
-- INSERT SAMPLE GATES
-- ============================================================================
INSERT INTO gates (id, template_id, gate_id, name, description, gate_type, required, is_open, instructions, created_at, updated_at, created_by, updated_by)
VALUES
    ('gate-001', 'template-001', 'GATE-001', 'Safety Checklist', 'Pre-treatment safety verification', 'SAFETY', TRUE, FALSE, 'Complete WHO safety checklist before treatment', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('gate-002', 'template-001', 'GATE-002', 'Consent Verification', 'Verify informed consent', 'CONSENT', TRUE, FALSE, 'Ensure patient has signed consent forms', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

-- ============================================================================
-- INSERT SAMPLE CHECKLIST ITEMS
-- ============================================================================
INSERT INTO checklist_items (id, gate_id, sequence_number, item_text, item_description, completed, completed_by_user, completed_at, completion_notes, mandatory, instructions, created_at, updated_at, created_by, updated_by)
VALUES
    ('checklist-001', 'gate-001', 1, 'Confirm patient identity', 'Verify patient name and ID', FALSE, NULL, NULL, NULL, TRUE, 'Check patient ID band', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('checklist-002', 'gate-001', 2, 'Check allergies', 'Review allergy history', FALSE, NULL, NULL, NULL, TRUE, 'Verify allergy alert status', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('checklist-003', 'gate-001', 3, 'Verify site marking', 'Confirm surgical site marked', FALSE, NULL, NULL, NULL, TRUE, 'Physical inspection of site marking', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('checklist-004', 'gate-002', 1, 'Consent form signed', 'Patient signed consent', FALSE, NULL, NULL, NULL, TRUE, 'Patient and witness signatures present', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

-- ============================================================================
-- INSERT SAMPLE COMPENSATION ACTIONS
-- ============================================================================
INSERT INTO compensation_actions (id, order_id, action_type, action_description, triggering_event, executed, executed_at, execution_result, error_message, retry_count, max_retries, metadata, created_at, updated_at, created_by, updated_by)
VALUES
    ('comp-action-001', 'order-001', 'REVERSE_CHARGE', 'Reverse charge for cancelled test', 'ORDER_CANCELLED', FALSE, NULL, NULL, NULL, 0, 3, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'),
    ('comp-action-002', 'order-002', 'SEND_NOTIFICATION', 'Notify lab of cancellation', 'ORDER_CANCELLED', FALSE, NULL, NULL, NULL, 0, 3, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

-- ============================================================================
-- INSERT SAMPLE AUDIT LOGS
-- ============================================================================
INSERT INTO audit_logs (id, entity_type, entity_id, action, actor, action_timestamp, details, previous_value, new_value, correlation_id, patient_id, workflow_instance_id, is_legal_hold)
VALUES
    ('audit-001', 'WORKFLOW_INSTANCE', 'workflow-inst-001', 'CREATED', 'SYSTEM', DATEADD(MINUTE, -30, CURRENT_TIMESTAMP), 'Workflow instance created for emergency admission', NULL, 'ACTIVE', 'corr-001', 'patient-001', 'workflow-inst-001', FALSE),
    ('audit-002', 'TASK_INSTANCE', 'task-inst-001', 'COMPLETED', 'registration', DATEADD(MINUTE, -10, CURRENT_TIMESTAMP), 'Patient registration task completed', 'PENDING', 'COMPLETED', 'corr-001', 'patient-001', 'workflow-inst-001', FALSE),
    ('audit-003', 'ORDER', 'order-001', 'CREATED', 'SYSTEM', DATEADD(MINUTE, -30, CURRENT_TIMESTAMP), 'Lab order created', NULL, 'ACTIVATED', 'corr-001', 'patient-001', 'workflow-inst-001', FALSE),
    ('audit-004', 'WORKFLOW_INSTANCE', 'workflow-inst-004', 'COMPLETED', 'SYSTEM', CURRENT_TIMESTAMP, 'Emergency admission workflow completed', 'ACTIVE', 'COMPLETED', 'corr-002', 'patient-004', 'workflow-inst-004', FALSE);

-- ============================================================================
-- STATISTICS
-- ============================================================================
-- Sample Data Summary:
-- - Patients: 5
-- - Workflow Templates: 4 (3 published, 1 draft)
-- - Workflow Task Definitions: 10
-- - Workflow Instances: 4 (3 active, 1 completed)
-- - Task Instances: 8 (2 completed, 2 in-progress, 1 pending, 3 pending)
-- - Orders: 4 (1 activated, 1 in-progress, 1 resulted, 1 completed)
-- - Order Sets: 3
-- - Gates: 2
-- - Checklist Items: 4
-- - Compensation Actions: 2
-- - Audit Logs: 4
-- ============================================================================
