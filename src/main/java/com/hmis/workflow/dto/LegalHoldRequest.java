package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for legal hold operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalHoldRequest {

    /**
     * Reason for setting or releasing legal hold.
     * Required for compliance documentation.
     */
    private String reason;

    /**
     * User performing the legal hold action.
     */
    private String actor;
}
