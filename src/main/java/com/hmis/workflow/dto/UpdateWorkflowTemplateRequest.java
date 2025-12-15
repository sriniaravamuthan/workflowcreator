package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a workflow template
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWorkflowTemplateRequest {
    private String name;
    private String description;
    private String category;
    private String notes;
}
