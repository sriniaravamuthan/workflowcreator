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
 * TaskNote entity representing an immutable note attached to a task instance.
 *
 * IMPORTANT: This entity is APPEND-ONLY for compliance with medical record-keeping
 * requirements. Notes should NEVER be updated or deleted once created.
 *
 * This supports:
 * - Clinical documentation during task execution
 * - Audit trail for compliance (HIPAA, legal discovery)
 * - Notes at workflow creation time (initial observations)
 * - Notes at task completion (outcomes, findings)
 * - Progress notes during task execution
 */
@Entity
@Table(name = "task_notes", indexes = {
    @Index(name = "idx_task_notes_task_instance", columnList = "task_instance_id"),
    @Index(name = "idx_task_notes_noted_at", columnList = "noted_at"),
    @Index(name = "idx_task_notes_author", columnList = "author_user"),
    @Index(name = "idx_task_notes_type", columnList = "note_type")
})
@Data
@EqualsAndHashCode(callSuper = true, exclude = "taskInstance")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskNote extends BaseEntity {

    /**
     * Type of note for categorization and filtering.
     */
    public enum NoteType {
        /** Note added when task/workflow is created */
        CREATION,
        /** Note added during task assignment */
        ASSIGNMENT,
        /** Note added when task is started */
        START,
        /** Progress update during task execution */
        PROGRESS,
        /** Clinical observation or finding */
        OBSERVATION,
        /** Note added when task is completed */
        COMPLETION,
        /** Note added when task is skipped */
        SKIP,
        /** Note added during escalation */
        ESCALATION,
        /** Note added when task fails */
        FAILURE,
        /** General comment or annotation */
        COMMENT,
        /** Handoff note for shift changes */
        HANDOFF,
        /** Alert or warning note */
        ALERT,
        /** Correction or clarification of previous note */
        ADDENDUM
    }

    @ManyToOne
    @JoinColumn(name = "task_instance_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_note_task"))
    @JsonIgnore
    private TaskInstance taskInstance;

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
     * This is separate from createdAt to allow for backdated entries
     * (e.g., documenting something that happened earlier).
     */
    @Column(nullable = false)
    private LocalDateTime notedAt;

    /**
     * Optional reference to a previous note this is an addendum to.
     * Used for corrections or clarifications.
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
     * Optional metadata in JSON format (e.g., vital signs, measurements).
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Helper method to get the task instance ID for DTOs.
     */
    public java.util.UUID getTaskInstanceId() {
        return taskInstance != null ? taskInstance.getId() : null;
    }

    /**
     * Helper method to get the task instance identifier string.
     */
    public String getTaskInstanceIdString() {
        return taskInstance != null ? taskInstance.getTaskInstanceId() : null;
    }
}
