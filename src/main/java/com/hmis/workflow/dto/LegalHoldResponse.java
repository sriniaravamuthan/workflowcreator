package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for legal hold operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalHoldResponse {

    private String entityId;
    private String entityType;
    private String action;  // SET or RELEASED
    private int affectedRecords;
    private String reason;
    private String actor;
    private String message;
}
