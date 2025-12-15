package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adjusting order parameters during workflow review
 * Used by doctors to modify dosages, timings, frequencies, etc. before signing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAdjustmentRequest {

    private String orderId;

    private String parameterName;

    private String originalValue;

    private String adjustedValue;

    private String adjustmentReason;

    private String adjustmentType; // PARAMETER, DOSAGE, FREQUENCY, TIMING, QUANTITY

    private String adjustedByUser;

    private String metadata;
}
