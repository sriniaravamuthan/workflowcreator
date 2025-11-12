# HMIS Workflow Engine - Database Documentation

This directory contains all database scripts (DDL and DML) for the HMIS Workflow Engine.

## Directory Structure

```
db/
├── ddl/
│   └── 01-schema.sql          # Complete database schema with all tables and indexes
├── data/
│   └── sample-data.sql        # Sample data for testing and demonstration
└── README.md                  # This file
```

## Database Overview

### Technology Stack
- **Database**: H2 (in-memory for development), PostgreSQL/MySQL (for production)
- **ORM**: Hibernate/JPA
- **Dialect**: H2Dialect (configured in application.yml)

### Table Count: 15 Core Tables

| Table | Purpose | Records |
|-------|---------|---------|
| `patients` | Patient profiles | Multiple per clinic |
| `workflow_templates` | Reusable workflow blueprints | ~50-100 per organization |
| `workflow_task_definitions` | Task definitions in templates | ~300-500 total |
| `workflow_instances` | Patient workflow executions | Hundreds/thousands daily |
| `task_instances` | Executed task instances | Thousands daily |
| `orders` | Clinical orders | Thousands daily |
| `order_sets` | Bundled orders for conditions | ~50-100 per organization |
| `order_set_items` | Items within order sets | ~200-300 total |
| `order_set_conditions` | Activation rules for order sets | ~100-150 total |
| `instructions` | Clinical directives | Thousands in active workflows |
| `gates` | Formal checkpoints | ~20-50 per organization |
| `checklist_items` | Items in gate checklists | ~100-200 total |
| `decision_logic` | Conditional routing | ~50-100 per organization |
| `compensation_actions` | Failure recovery actions | Hundreds daily |
| `audit_logs` | Immutable execution timeline | Millions over time |

## Schema Design Details

### Core Design Principles

1. **UUID Primary Keys**
   - All tables use VARCHAR(36) for UUID support
   - Database-independent approach
   - No sequential ID dependencies

2. **Audit Columns**
   - All tables have: `created_at`, `updated_at`, `created_by`, `updated_by`
   - Managed automatically by Hibernate
   - Essential for compliance and debugging

3. **Referential Integrity**
   - Foreign keys enforce data consistency
   - Cascade deletes for dependent records
   - Restrict deletes where data integrity is critical

4. **Indexing Strategy**
   - Indexes on all frequently queried columns
   - Foreign key columns indexed
   - Status columns indexed for filtering
   - Timestamps indexed for date range queries

### Relationship Diagram (Simplified)

```
PATIENTS (1)
    ├─→ (M) WORKFLOW_INSTANCES
    │        ├─→ (M) TASK_INSTANCES
    │        ├─→ (M) ORDERS
    │        │   └─→ (M) COMPENSATION_ACTIONS
    │        └─→ (M) INSTRUCTIONS
    │
WORKFLOW_TEMPLATES (1)
    ├─→ (M) WORKFLOW_TASK_DEFINITIONS
    ├─→ (M) GATES
    │   └─→ (M) CHECKLIST_ITEMS
    └─→ (M) DECISION_LOGIC

ORDER_SETS (1)
    ├─→ (M) ORDER_SET_ITEMS
    └─→ (M) ORDER_SET_CONDITIONS

AUDIT_LOGS (independent)
    └─ Records all entity changes
```

## SQL Scripts Guide

### 1. Schema Creation (`ddl/01-schema.sql`)

#### What It Creates
- 15 tables with complete column definitions
- 35+ indexes for performance
- Foreign key constraints
- Default values and constraints

#### How to Use

**H2 Database (Development):**
```sql
-- Automatically applied on startup via Hibernate
-- Or manually run:
@see src/main/resources/db/ddl/01-schema.sql
```

**PostgreSQL (Production):**
```bash
psql -U postgres -d workflow_db -f src/main/resources/db/ddl/01-schema.sql
```

**MySQL (Production):**
```bash
mysql -u root -p workflow_db < src/main/resources/db/ddl/01-schema.sql
```

#### Key Tables

**Patients**
```sql
-- Store patient demographic and medical information
-- Primary key: id (UUID)
-- Unique constraint: patient_id (unique identifier within system)
-- Indexed: patient_id, email, active
```

**Workflow Templates & Tasks**
```sql
-- Templates: Reusable workflow blueprints
-- TaskDefinitions: Individual tasks within templates
-- Supports versioning and governance (DRAFT → PUBLISHED)
-- Indexed by: name, category, status, template relationship
```

**Workflow Instances & Tasks**
```sql
-- Instances: Patient-specific workflow executions
-- TaskInstances: Individual task executions
-- Tracks status, assignment, SLA, escalation
-- Indexed by: status, assignee, due date, escalation
```

**Orders & Compensation**
```sql
-- Orders: 8-state clinical order lifecycle
-- CompensationActions: Automatic failure recovery
-- Tracks authorization, activation, results, verification
-- Indexed by: status, type, workflow relationship
```

**Gates & Checklists**
```sql
-- Gates: Formal checkpoints with requirements
-- ChecklistItems: Individual items in checklists
-- Tracks completion and responsibility
-- Used for safety and compliance verification
```

**Audit Logs**
```sql
-- Immutable timeline of all entity changes
-- Retention: 7-10 years
-- Supports legal hold for compliance
-- Indexed by: entity_id, action_timestamp, correlation_id
```

### 2. Sample Data (`data/sample-data.sql`)

#### What It Inserts
- 5 sample patients
- 4 workflow templates
- 10 task definitions
- 4 workflow instances
- 8 task instances
- 4 orders
- 3 order sets
- 2 gates with 4 checklist items
- 2 compensation actions
- 4 audit log entries

#### How to Use

```bash
# After creating schema, insert sample data:

# H2 (via h2-console or programmatically)
@see src/main/resources/db/data/sample-data.sql

# PostgreSQL
psql -U postgres -d workflow_db -f src/main/resources/db/data/sample-data.sql

# MySQL
mysql -u root -p workflow_db < src/main/resources/db/data/sample-data.sql
```

#### Sample Data Scenarios

**Active Emergency Admission Workflow**
- Patient: John Doe (PAT001)
- Template: Emergency Admission
- Status: ACTIVE
- Progress: 1 completed task, 1 in-progress, others pending
- Orders: 2 (CBC lab order, Chest X-ray imaging)

**Completed Clinic Visit**
- Patient: Mary Williams (PAT004)
- Template: Emergency Admission
- Status: COMPLETED
- All tasks completed

**Active Lab Processing**
- Patient: Robert Johnson (PAT003)
- Template: Lab Test Processing
- Status: ACTIVE
- Lab order in processing

## Database Initialization Options

### Option 1: Automatic (Recommended for Development)

Hibernate will automatically create/update schema based on entities:

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # create, create-drop, update, validate
```

### Option 2: Manual (Recommended for Production)

1. Create schema using DDL script
2. Insert sample data (optional)
3. Run application with `ddl-auto: validate`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Only validates, doesn't modify
```

### Option 3: Flyway (Enterprise)

For production environments with version control:

```yaml
spring:
  flyway:
    locations: classpath:db/migration
    baselineOnMigrate: true
```

## Performance Considerations

### Indexing Strategy

**High-Priority Indexes** (created automatically):
- `idx_workflow_instances_patient_id` - Patient workflow lookup
- `idx_workflow_instances_status` - Workflow filtering
- `idx_task_instances_assigned_to` - User task assignment
- `idx_task_instances_due_at` - SLA deadline queries
- `idx_orders_workflow_instance_id` - Order lookup
- `idx_audit_logs_entity_id` - Audit trail retrieval

### Query Optimization Tips

1. **Always use indexed columns in WHERE clauses**
   - `status`, `patient_id`, `assigned_to`, `due_at`

2. **Use composite indexes for common filters**
   - `(patient_id, status)` for active patient workflows
   - `(assigned_to, status)` for user task queues

3. **Archive old audit logs** (older than 10 years)
   - Create archive table
   - Move logs with `is_legal_hold = FALSE`

4. **Partition large tables** (if database supports)
   - Partition `audit_logs` by month
   - Partition `task_instances` by status

### Data Retention Policies

| Table | Retention | Archive | Notes |
|-------|-----------|---------|-------|
| audit_logs | 7-10 years | Yes | Must be immutable |
| workflow_instances | 3 years | Yes | Historical tracking |
| task_instances | 3 years | Yes | Task history |
| orders | 7 years | Yes | Financial/compliance |
| other | 2 years | Yes | System operation |

## Database Backup & Recovery

### Backup Strategy

```bash
# PostgreSQL
pg_dump -U postgres workflow_db > backup_$(date +%Y%m%d_%H%M%S).sql

# MySQL
mysqldump -u root -p workflow_db > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Recovery

```bash
# PostgreSQL
psql -U postgres workflow_db < backup_20240101_120000.sql

# MySQL
mysql -u root -p workflow_db < backup_20240101_120000.sql
```

### High Availability

1. **Master-Replica Setup**
   - Primary: Handles writes
   - Replica: Read-only for reporting

2. **Automated Backups**
   - Daily full backups
   - Hourly incremental backups
   - Test recovery monthly

3. **Disaster Recovery**
   - RTO: 1 hour
   - RPO: 15 minutes

## Compliance & Legal Requirements

### HIPAA Compliance
- Audit log retention: 10 years
- All user actions logged with timestamps
- Access controls implemented at application layer
- Data encryption at rest and in transit (via Spring Security)

### GDPR Compliance
- Patient right to erasure (logical delete)
- Data portability (export functionality)
- Privacy impact assessments documented
- Data processing agreements in place

### Consent Management
- Store consent status in workflow instances
- Track consent changes in audit logs
- Enforce consent-based task visibility

## Troubleshooting

### Common Issues

**Issue: Foreign Key Constraint Violation**
```sql
-- Check constraint definition
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.CONSTRAINTS
WHERE TABLE_NAME = 'workflow_instances';

-- Temporarily disable (use with caution):
-- MySQL: SET FOREIGN_KEY_CHECKS=0;
-- PostgreSQL: ALTER TABLE ... DISABLE TRIGGER ALL;
```

**Issue: Slow Queries**
```sql
-- Analyze query execution plan
EXPLAIN ANALYZE
SELECT * FROM workflow_instances
WHERE patient_id = 'patient-001'
  AND status = 'ACTIVE';

-- Verify indexes exist
SHOW INDEX FROM workflow_instances;
```

**Issue: Deadlocks**
```sql
-- Review active transactions
-- Reduce transaction duration
-- Use appropriate isolation levels
-- Implement retry logic in application
```

## Development vs Production

### Development (H2 In-Memory)
- Auto-create schema on startup
- Sample data loaded automatically
- Fast iteration
- No persistence between runs

### Production (PostgreSQL/MySQL)
- Manual schema creation
- Controlled data loading
- Backup/recovery procedures
- Performance optimization
- Monitoring and alerting

## Migration Guide

### From Development to Production

1. **Prepare Production Database**
   ```bash
   # Create database
   createdb workflow_db  # PostgreSQL
   CREATE DATABASE workflow_db;  # MySQL
   ```

2. **Apply Schema**
   ```bash
   # Run DDL script
   psql -U postgres -d workflow_db -f ddl/01-schema.sql
   ```

3. **Configure Application**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://prod-db:5432/workflow_db
       username: workflow_user
       password: ${DB_PASSWORD}
     jpa:
       hibernate:
         ddl-auto: validate
   ```

4. **Verify Schema**
   ```sql
   -- Check table count
   SELECT COUNT(*) FROM information_schema.tables
   WHERE table_schema = 'public';
   ```

5. **Test Connectivity**
   ```bash
   # Run application health check
   curl http://localhost:8080/actuator/health
   ```

## Support & Documentation

- **Schema Questions**: See README_IMPLEMENTATION.md
- **API Usage**: See API_DOCUMENTATION.md
- **Entity Details**: See inline Javadoc in entity classes

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-11-10 | Initial schema with 15 tables |

---

**Last Updated**: 2024-11-10
**Author**: HMIS Development Team
**Database Version**: 1.0
