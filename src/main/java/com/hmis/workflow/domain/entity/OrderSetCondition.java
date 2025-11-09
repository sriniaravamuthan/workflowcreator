package com.hmis.workflow.domain.entity;

import com.hmis.workflow.domain.enums.DecisionLogicOperator;
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

/**
 * OrderSetCondition entity representing conditional logic for OrderSet activation
 * Evaluates patient data and clinical guidelines to determine orderset applicability
 */
@Entity
@Table(name = "order_set_conditions")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "orderSet")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSetCondition extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String conditionName;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 100)
    private String dataPoint; // Patient data field to evaluate (e.g., "age", "diagnosis", "lab_result")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionLogicOperator operator; // Comparison operator

    @Column(nullable = false, columnDefinition = "TEXT")
    private String expectedValue; // Value to compare against

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean required = true; // If true, must be satisfied for orderset activation

    @Column(length = 50)
    private String logicalConnector; // AND, OR (for combining multiple conditions)

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne
    @JoinColumn(name = "order_set_id", nullable = false, foreignKey = @ForeignKey(name = "fk_condition_orderset"))
    @JsonIgnore
    private OrderSet orderSet;
}
