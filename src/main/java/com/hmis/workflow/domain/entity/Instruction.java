package com.hmis.workflow.domain.entity;

import com.hmis.workflow.domain.enums.InstructionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Instruction entity representing directives or guidelines that influence workflows
 * Examples: NPO status, isolation precautions, discharge restrictions
 * Blocking flags can prevent downstream steps until acknowledged
 */
@Entity
@Table(name = "instructions")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "workflowInstance")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instruction extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String instructionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstructionType instructionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instructionText;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean blocking = false; // If true, prevents downstream steps until acknowledged

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean acknowledged = false;

    @Column
    private LocalDateTime acknowledgedAt;

    @Column(length = 100)
    private String acknowledgedByUser;

    @Column(columnDefinition = "TEXT")
    private String acknowledgedNotes;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne
    @JoinColumn(name = "workflow_instance_id", nullable = false, foreignKey = @ForeignKey(name = "fk_instruction_workflow"))
    @JsonIgnore
    private WorkflowInstance workflowInstance;
}
