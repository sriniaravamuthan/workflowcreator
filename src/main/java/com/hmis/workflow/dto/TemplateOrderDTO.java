package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for TemplateOrder
 * Used to expose order information in APIs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateOrderDTO {

    private String id;

    private String templateId;

    private String orderCode;

    private String orderName;

    private String description;

    private String externalApiEndpoint;

    private String apiMethod;

    private Boolean isRequired;

    private Boolean isAutomatic;

    private Integer orderSequence;

    private String metadata;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
