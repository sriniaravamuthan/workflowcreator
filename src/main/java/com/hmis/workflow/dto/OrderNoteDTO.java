package com.hmis.workflow.dto;

import com.hmis.workflow.domain.entity.OrderNote;
import com.hmis.workflow.domain.entity.OrderNote.NoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for OrderNote entity.
 * Used for API responses when returning order notes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderNoteDTO {

    private UUID id;
    private UUID orderId;
    private String orderIdString;
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
    public static OrderNoteDTO fromEntity(OrderNote entity) {
        if (entity == null) {
            return null;
        }
        return OrderNoteDTO.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .orderIdString(entity.getOrderIdString())
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
