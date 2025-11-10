package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new workflow template
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkflowTemplateRequest {
    private String name;
    private String description;
    private String category;
}
