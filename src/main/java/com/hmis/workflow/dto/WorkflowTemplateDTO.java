package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for WorkflowTemplate
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTemplateDTO {

    private UUID id;
    private String name;
    private String description;
    private Boolean active;
    private Integer version;
    private String category;
    private List<WorkflowTaskDefinitionDTO> tasks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
