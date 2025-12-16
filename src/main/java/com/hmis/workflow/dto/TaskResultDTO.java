package com.hmis.workflow.dto;

import com.hmis.workflow.domain.entity.TaskResult;
import com.hmis.workflow.domain.entity.TaskResult.ResultType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for TaskResult entity.
 * Used for API responses when returning task results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResultDTO {

    private UUID id;
    private UUID taskInstanceId;
    private String taskInstanceIdString;
    private ResultType resultType;
    private String resultName;
    private String resultCode;
    private String codeSystem;
    private String resultValue;
    private String unit;
    private String referenceRange;
    private String interpretationCode;
    private String recordedByUser;
    private String recordedByRole;
    private LocalDateTime recordedAt;
    private LocalDateTime observedAt;
    private String verifiedByUser;
    private LocalDateTime verifiedAt;
    private Boolean isVerified;
    private Boolean isCritical;
    private String correctsResultId;
    private String metadata;
    private String comments;
    private LocalDateTime createdAt;

    /**
     * Convert entity to DTO.
     */
    public static TaskResultDTO fromEntity(TaskResult entity) {
        if (entity == null) {
            return null;
        }
        return TaskResultDTO.builder()
                .id(entity.getId())
                .taskInstanceId(entity.getTaskInstanceId())
                .taskInstanceIdString(entity.getTaskInstanceIdString())
                .resultType(entity.getResultType())
                .resultName(entity.getResultName())
                .resultCode(entity.getResultCode())
                .codeSystem(entity.getCodeSystem())
                .resultValue(entity.getResultValue())
                .unit(entity.getUnit())
                .referenceRange(entity.getReferenceRange())
                .interpretationCode(entity.getInterpretationCode())
                .recordedByUser(entity.getRecordedByUser())
                .recordedByRole(entity.getRecordedByRole())
                .recordedAt(entity.getRecordedAt())
                .observedAt(entity.getObservedAt())
                .verifiedByUser(entity.getVerifiedByUser())
                .verifiedAt(entity.getVerifiedAt())
                .isVerified(entity.getIsVerified())
                .isCritical(entity.getIsCritical())
                .correctsResultId(entity.getCorrectsResultId())
                .metadata(entity.getMetadata())
                .comments(entity.getComments())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
