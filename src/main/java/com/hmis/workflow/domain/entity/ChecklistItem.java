package com.hmis.workflow.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ChecklistItem entity representing individual items in a gate's checklist
 * Example: WHO Surgical Safety Checklist includes items like "Confirm patient identity"
 */
@Entity
@Table(name = "checklist_items")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "gate")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItem extends BaseEntity {

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false, length = 255)
    private String itemText;

    @Column(length = 500)
    private String itemDescription;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean completed = false;

    @Column(length = 100)
    private String completedByUser;

    @Column
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String completionNotes;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean mandatory = false; // If true, must be completed before gate is satisfied

    @Column(columnDefinition = "TEXT")
    private String instructions; // Specific instructions for this item

    @ManyToOne
    @JoinColumn(name = "gate_id", nullable = false, foreignKey = @ForeignKey(name = "fk_checklist_gate"))
    @JsonIgnore
    private Gate gate;
}
