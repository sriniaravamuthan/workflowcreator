package com.hmis.workflow.dto;

import com.hmis.workflow.domain.entity.WorkflowTaskDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for task notification configuration response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskNotificationConfigDTO {

    private UUID taskDefinitionId;
    private String taskName;
    private String notificationType;
    private String notificationKafkaTopic;
    private String notificationApiEndpoint;
    private String notificationApiMethod;
    private String notificationMessageTemplate;
    private String notificationApiHeaders;
    private Boolean notifyOnFailure;
    private Boolean notifyOnSkip;

    /**
     * Create DTO from entity.
     */
    public static TaskNotificationConfigDTO fromEntity(WorkflowTaskDefinition taskDef) {
        return TaskNotificationConfigDTO.builder()
                .taskDefinitionId(taskDef.getId())
                .taskName(taskDef.getName())
                .notificationType(taskDef.getNotificationType())
                .notificationKafkaTopic(taskDef.getNotificationKafkaTopic())
                .notificationApiEndpoint(taskDef.getNotificationApiEndpoint())
                .notificationApiMethod(taskDef.getNotificationApiMethod())
                .notificationMessageTemplate(taskDef.getNotificationMessageTemplate())
                .notificationApiHeaders(taskDef.getNotificationApiHeaders())
                .notifyOnFailure(taskDef.getNotifyOnFailure())
                .notifyOnSkip(taskDef.getNotifyOnSkip())
                .build();
    }
}
