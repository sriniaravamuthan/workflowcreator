package com.hmis.workflow.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OrderNote entity representing an immutable note attached to an order.
 *
 * IMPORTANT: This entity is APPEND-ONLY for compliance with medical record-keeping
 * requirements. Notes should NEVER be updated or deleted once created.
 *
 * This supports:
 * - Clinical notes during order lifecycle
 * - Authorization justifications
 * - Cancellation reasons
 * - Result interpretations
 * - Audit trail for compliance
 */
@Entity
@Table(name = "order_notes", indexes = {
    @Index(name = "idx_order_notes_order", columnList = "order_id"),
    @Index(name = "idx_order_notes_noted_at", columnList = "noted_at"),
    @Index(name = "idx_order_notes_author", columnList = "author_user"),
    @Index(name = "idx_order_notes_type", columnList = "note_type")
})
@Data
@EqualsAndHashCode(callSuper = true, exclude = "order")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderNote extends BaseEntity {

    /**
     * Type of note for categorization and filtering.
     */
    public enum NoteType {
        /** Note added when order is created/proposed */
        CREATION,
        /** Clinical indication/justification for the order */
        INDICATION,
        /** Note added during authorization */
        AUTHORIZATION,
        /** Note added when order is activated */
        ACTIVATION,
        /** Progress update during order execution */
        PROGRESS,
        /** Note about order result */
        RESULT,
        /** Clinical interpretation of results */
        INTERPRETATION,
        /** Note added during verification */
        VERIFICATION,
        /** Note added when order is cancelled */
        CANCELLATION,
        /** Note added when order is closed */
        CLOSURE,
        /** General comment or annotation */
        COMMENT,
        /** Priority change justification */
        PRIORITY_CHANGE,
        /** Modification note */
        MODIFICATION,
        /** Alert or warning note */
        ALERT,
        /** Correction or clarification of previous note */
        ADDENDUM
    }

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_note_order"))
    @JsonIgnore
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoteType noteType;

    /**
     * The actual note content. Cannot be modified after creation.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * User who authored this note.
     */
    @Column(nullable = false, length = 255)
    private String authorUser;

    /**
     * Role of the author at time of note creation (for context).
     */
    @Column(length = 100)
    private String authorRole;

    /**
     * Timestamp when the note was recorded.
     */
    @Column(nullable = false)
    private LocalDateTime notedAt;

    /**
     * Optional reference to a previous note this is an addendum to.
     */
    @Column(length = 100)
    private String addendumToNoteId;

    /**
     * Optional priority/severity for alerts.
     * 0 = Normal, 1 = Important, 2 = Critical
     */
    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer priority = 0;

    /**
     * Whether this note is flagged for attention.
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isFlagged = false;

    /**
     * Optional metadata in JSON format.
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Helper method to get the order ID for DTOs.
     */
    public java.util.UUID getOrderId() {
        return order != null ? order.getId() : null;
    }

    /**
     * Helper method to get the order identifier string.
     */
    public String getOrderIdString() {
        return order != null ? order.getOrderId() : null;
    }
}
