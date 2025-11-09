package com.hmis.workflow.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AuditLog entity for maintaining immutable timeline of all workflow actions
 * Records all state changes, user actions, and outcomes for compliance and observability
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String entityType; // WORKFLOW, TASK, ORDER, etc.

    @Column(nullable = false, columnDefinition = "VARCHAR(36)")
    private String entityId;

    @Column(nullable = false, length = 100)
    private String action; // CREATED, UPDATED, TRANSITIONED, COMPLETED, FAILED, etc.

    @Column(length = 100)
    private String actor; // User who performed action

    @Column(nullable = false)
    private LocalDateTime actionTimestamp;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details; // JSON-serialized details of change

    @Column(length = 100)
    private String previousValue;

    @Column(length = 100)
    private String newValue;

    @Column(columnDefinition = "VARCHAR(36)")
    private String correlationId; // Trace ID for cross-step correlation

    @Column(columnDefinition = "VARCHAR(36)")
    private String patientId;

    @Column(columnDefinition = "VARCHAR(36)")
    private String workflowInstanceId;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isLegalHold = false; // If true, cannot be deleted per legal requirement
}
