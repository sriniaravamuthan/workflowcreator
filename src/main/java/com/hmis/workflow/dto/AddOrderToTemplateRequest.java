package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to add an order to a workflow template
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddOrderToTemplateRequest {

    private String orderCode;

    private String orderName;

    private String description;

    private String externalApiEndpoint;

    private String apiMethod;

    private String apiRequestPayload;

    private Boolean isRequired;

    private Boolean isAutomatic;

    private Integer orderSequence;

    private String metadata;
}
