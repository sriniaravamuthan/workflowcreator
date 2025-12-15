package com.hmis.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for workflow approval/rejection during review phase
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowApprovalRequest {

    private String reviewedByUser;

    private String reviewNotes;
}
