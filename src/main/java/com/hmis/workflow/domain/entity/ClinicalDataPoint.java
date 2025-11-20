package com.hmis.workflow.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ClinicalDataPoint entity representing patient vital signs and clinical measurements
 * Used for clinical decision logic evaluation during workflow execution
 * Examples: O2 saturation, Heart rate, Blood pressure, Temperature, etc.
 */
@Entity
@Table(name = "clinical_data_points")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"patient"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalDataPoint extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String dataPointCode; // O2_SAT, HR, BP_SYS, BP_DIA, TEMP, RR, etc.

    @Column(nullable = false, length = 255)
    private String displayName; // "Oxygen Saturation", "Heart Rate", etc.

    @Column(columnDefinition = "TEXT")
    private String value; // Stored as string for flexibility (can be numeric or complex like "120/80")

    @Column(length = 20)
    private String unit; // %, bpm, mmHg, °C, breaths/min

    @Column(precision = 10, scale = 2)
    private BigDecimal numericValue; // For numeric comparisons in decision logic

    @Column(nullable = false)
    private LocalDateTime measurementTime; // When the measurement was taken

    @Column(length = 100)
    private String measuredByUser; // Who recorded this measurement

    @Column(length = 100)
    private String measurementMethod; // Manual, Automated, Device name, etc.

    @Column(length = 100)
    private String dataSource; // EHR, Monitoring device, Lab system, etc.

    @Column(nullable = false)
    @Builder.Default
    private Boolean isNormal = false; // Flag indicating if value is within normal range

    @Column(columnDefinition = "TEXT")
    private String referenceRange; // e.g., "95-100%" for O2 saturation

    @Column(columnDefinition = "TEXT")
    private String interpretation; // Clinical interpretation of the measurement

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false, foreignKey = @ForeignKey(name = "fk_clinical_data_patient"))
    private Patient patient;

    /**
     * Check if value meets a given threshold
     * @param operator Comparison operator (>, <, >=, <=, ==, !=)
     * @param threshold Threshold value to compare against
     * @return true if condition is met
     */
    public boolean evaluateCondition(String operator, BigDecimal threshold) {
        if (numericValue == null || threshold == null) {
            return false;
        }

        return switch (operator) {
            case ">" -> numericValue.compareTo(threshold) > 0;
            case "<" -> numericValue.compareTo(threshold) < 0;
            case ">=" -> numericValue.compareTo(threshold) >= 0;
            case "<=" -> numericValue.compareTo(threshold) <= 0;
            case "==" -> numericValue.compareTo(threshold) == 0;
            case "!=" -> numericValue.compareTo(threshold) != 0;
            default -> false;
        };
    }
}
