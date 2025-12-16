package com.hmis.workflow.dto;

import com.hmis.workflow.domain.entity.OrderNote.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an order note.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderNoteRequest {

    @NotNull(message = "Note type is required")
    private NoteType noteType;

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Author user is required")
    private String authorUser;

    private String authorRole;

    /**
     * Optional: Reference to a previous note this is an addendum to.
     */
    private String addendumToNoteId;

    /**
     * Priority level: 0 = Normal, 1 = Important, 2 = Critical.
     */
    private Integer priority;

    /**
     * Whether to flag this note for attention.
     */
    private Boolean isFlagged;

    /**
     * Optional metadata in JSON format.
     */
    private String metadata;
}
