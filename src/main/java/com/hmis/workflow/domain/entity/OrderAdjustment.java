package com.hmis.workflow.domain.entity;

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

/**
 * OrderAdjustment entity tracking modifications made by doctors to orders/templates
 * before signing and workflow execution. Enables parameter adjustment for dosages,
 * timings, frequencies, etc. without modifying the template itself.
 */
@Entity
@Table(name = "order_adjustments")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"order", "workflowInstance"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAdjustment extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String adjustmentCode;

    @Column(nullable = false, length = 255)
    private String parameterName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalValue;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String adjustedValue;

    @Column(columnDefinition = "TEXT")
    private String adjustmentReason;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String adjustmentType = "PARAMETER"; // PARAMETER, DOSAGE, FREQUENCY, TIMING, QUANTITY

    @Column(nullable = false)
    @Builder.Default
    private Boolean isApplied = false;

    @Column(length = 100)
    private String adjustedByUser;

    @Column
    private java.time.LocalDateTime adjustedAt;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adjustment_order"))
    private Order order;

    @ManyToOne
    @JoinColumn(name = "workflow_instance_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adjustment_workflow"))
    private WorkflowInstance workflowInstance;

    @Column(columnDefinition = "TEXT")
    private String metadata;
}
