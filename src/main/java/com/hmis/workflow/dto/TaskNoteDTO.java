package com.hmis.workflow.dto;

import com.hmis.workflow.domain.entity.TaskNote;
import com.hmis.workflow.domain.entity.TaskNote.NoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for TaskNote entity.
 * Used for API responses when returning task notes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskNoteDTO {

    private UUID id;
    private UUID taskInstanceId;
    private String taskInstanceIdString;
    private NoteType noteType;
    private String content;
    private String authorUser;
    private String authorRole;
    private LocalDateTime notedAt;
    private String addendumToNoteId;
    private Integer priority;
    private Boolean isFlagged;
    private String metadata;
    private LocalDateTime createdAt;

    /**
     * Convert entity to DTO.
     */
    public static TaskNoteDTO fromEntity(TaskNote entity) {
        if (entity == null) {
            return null;
        }
        return TaskNoteDTO.builder()
                .id(entity.getId())
                .taskInstanceId(entity.getTaskInstanceId())
                .taskInstanceIdString(entity.getTaskInstanceIdString())
                .noteType(entity.getNoteType())
                .content(entity.getContent())
                .authorUser(entity.getAuthorUser())
                .authorRole(entity.getAuthorRole())
                .notedAt(entity.getNotedAt())
                .addendumToNoteId(entity.getAddendumToNoteId())
                .priority(entity.getPriority())
                .isFlagged(entity.getIsFlagged())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
