package com.hmis.workflow.dto;

import com.hmis.workflow.domain.entity.TaskResult.ResultType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a task result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResultRequest {

    @NotNull(message = "Result type is required")
    private ResultType resultType;

    @NotBlank(message = "Result name is required")
    private String resultName;

    /**
     * Standard code for the result (e.g., LOINC code).
     */
    private String resultCode;

    /**
     * Code system for the result code (e.g., "LOINC", "SNOMED").
     */
    private String codeSystem;

    @NotBlank(message = "Result value is required")
    private String resultValue;

    /**
     * Unit of measurement (e.g., "mmHg", "°F", "mg/dL").
     */
    private String unit;

    /**
     * Reference range for the result (e.g., "120-140").
     */
    private String referenceRange;

    /**
     * Interpretation flag: "N" = Normal, "H" = High, "L" = Low, "A" = Abnormal.
     */
    private String interpretationCode;

    @NotBlank(message = "Recorded by user is required")
    private String recordedByUser;

    private String recordedByRole;

    /**
     * Timestamp when the result was actually observed/measured.
     * If not provided, defaults to current time.
     */
    private LocalDateTime observedAt;

    /**
     * Whether this result is flagged as critical/abnormal.
     */
    private Boolean isCritical;

    /**
     * Reference to a previous result this corrects (for CORRECTION type).
     */
    private String correctsResultId;

    /**
     * Optional metadata in JSON format.
     */
    private String metadata;

    /**
     * Optional clinical comments about this result.
     */
    private String comments;
}
