package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for OrderAdjustment entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAdjustmentDTO {

    private UUID id;

    private String adjustmentCode;

    private String parameterName;

    private String originalValue;

    private String adjustedValue;

    private String adjustmentReason;

    private String adjustmentType;

    private Boolean isApplied;

    private String adjustedByUser;

    private LocalDateTime adjustedAt;

    private UUID orderId;

    private UUID workflowInstanceId;

    private String metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
