package com.hmis.workflow.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TaskResult entity representing a structured result from a task instance.
 *
 * IMPORTANT: This entity is APPEND-ONLY for compliance with medical record-keeping
 * requirements. Results should NEVER be updated or deleted once created.
 * Corrections should be added as new results with resultType = CORRECTION.
 *
 * This supports:
 * - Structured clinical results (measurements, observations)
 * - Multiple results per task (e.g., multiple vital signs)
 * - Result verification workflow
 * - Audit trail for all recorded values
 */
@Entity
@Table(name = "task_results", indexes = {
    @Index(name = "idx_task_results_task_instance", columnList = "task_instance_id"),
    @Index(name = "idx_task_results_recorded_at", columnList = "recorded_at"),
    @Index(name = "idx_task_results_type", columnList = "result_type"),
    @Index(name = "idx_task_results_code", columnList = "result_code")
})
@Data
@EqualsAndHashCode(callSuper = true, exclude = "taskInstance")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResult extends BaseEntity {

    /**
     * Type of result for categorization.
     */
    public enum ResultType {
        /** Primary outcome of the task */
        OUTCOME,
        /** Numeric measurement (e.g., blood pressure, temperature) */
        MEASUREMENT,
        /** Clinical observation */
        OBSERVATION,
        /** Calculated or derived value */
        CALCULATED,
        /** Test result (lab, imaging, etc.) */
        TEST_RESULT,
        /** Assessment score (e.g., pain scale, ESI level) */
        ASSESSMENT,
        /** Procedure outcome */
        PROCEDURE_OUTCOME,
        /** Correction to a previous result */
        CORRECTION,
        /** Final summary result */
        SUMMARY
    }

    @ManyToOne
    @JoinColumn(name = "task_instance_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_result_task"))
    @JsonIgnore
    private TaskInstance taskInstance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultType resultType;

    /**
     * Name/label of the result (e.g., "Blood Pressure", "ESI Level").
     */
    @Column(nullable = false, length = 100)
    private String resultName;

    /**
     * Standard code for the result (e.g., LOINC code for lab results).
     */
    @Column(length = 50)
    private String resultCode;

    /**
     * Code system for the result code (e.g., "LOINC", "SNOMED").
     */
    @Column(length = 50)
    private String codeSystem;

    /**
     * The actual result value as text.
     * For structured data, use JSON format.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String resultValue;

    /**
     * Unit of measurement (e.g., "mmHg", "°F", "mg/dL").
     */
    @Column(length = 50)
    private String unit;

    /**
     * Reference range for the result (e.g., "120-140").
     */
    @Column(length = 100)
    private String referenceRange;

    /**
     * Interpretation flag (e.g., "N" = Normal, "H" = High, "L" = Low, "A" = Abnormal).
     */
    @Column(length = 10)
    private String interpretationCode;

    /**
     * User who recorded this result.
     */
    @Column(nullable = false, length = 255)
    private String recordedByUser;

    /**
     * Role of the user who recorded this result.
     */
    @Column(length = 100)
    private String recordedByRole;

    /**
     * Timestamp when the result was recorded.
     */
    @Column(nullable = false)
    private LocalDateTime recordedAt;

    /**
     * Timestamp when the result was actually observed/measured.
     * May differ from recordedAt for delayed documentation.
     */
    @Column
    private LocalDateTime observedAt;

    /**
     * User who verified/validated this result (if applicable).
     */
    @Column(length = 255)
    private String verifiedByUser;

    /**
     * Timestamp when the result was verified.
     */
    @Column
    private LocalDateTime verifiedAt;

    /**
     * Whether this result has been verified.
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isVerified = false;

    /**
     * Whether this result is flagged as critical/abnormal.
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isCritical = false;

    /**
     * Reference to a previous result this corrects (for CORRECTION type).
     */
    @Column(length = 100)
    private String correctsResultId;

    /**
     * Additional metadata in JSON format.
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Optional clinical comments about this result.
     */
    @Column(columnDefinition = "TEXT")
    private String comments;

    /**
     * Helper method to get the task instance ID for DTOs.
     */
    public java.util.UUID getTaskInstanceId() {
        return taskInstance != null ? taskInstance.getId() : null;
    }

    /**
     * Helper method to get the task instance identifier string.
     */
    public String getTaskInstanceIdString() {
        return taskInstance != null ? taskInstance.getTaskInstanceId() : null;
    }
}
