package com.hmis.workflow.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Gate entity representing formal checkpoints in workflows
 * Bundles multiple requirements (consents, assessments, checklists) that must be satisfied
 * Example: Surgical Safety Gate requires WHO safety checklist completion
 */
@Entity
@Table(name = "gates")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"template", "checklist"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String gateId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 100)
    private String gateType; // SAFETY, CONSENT, ASSESSMENT, CLEARANCE, CUSTOM

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean required = false; // If true, must be completed before proceeding

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isOpen = false; // If false, gate is closed; must be opened

    @Column(columnDefinition = "TEXT")
    private String instructions; // Instructions for gate completion

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false, foreignKey = @ForeignKey(name = "fk_gate_template"))
    @JsonIgnore
    private WorkflowTemplate template;

    @OneToMany(mappedBy = "gate", orphanRemoval = true)
    @Builder.Default
    private Set<ChecklistItem> checklist = new HashSet<>();

    public Integer getCompletionPercentage() {
        if (checklist.isEmpty()) {
            return 0;
        }
        long completedItems = checklist.stream()
                .filter(ChecklistItem::getCompleted)
                .count();
        return (int) ((completedItems * 100) / checklist.size());
    }

    public boolean allItemsCompleted() {
        return checklist.stream().allMatch(ChecklistItem::getCompleted);
    }
}
