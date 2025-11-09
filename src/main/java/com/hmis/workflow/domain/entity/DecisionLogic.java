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
 * DecisionLogic entity representing conditional branching in workflows
 * Evaluates patient data and order/task outcomes to route execution
 * Supports IF-THEN routing with multiple branches
 */
@Entity
@Table(name = "decision_logic")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "template")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionLogic extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String decisionId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 100)
    private String dataPoint; // Patient/order data to evaluate

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionLogicOperator operator; // EQUALS, GREATER_THAN, CONTAINS, etc.

    @Column(nullable = false, columnDefinition = "TEXT")
    private String expectedValue; // Value to compare

    @Column(nullable = false, length = 100)
    private String truePathTaskId; // Task/step if condition is true

    @Column(nullable = false, length = 100)
    private String falsePathTaskId; // Task/step if condition is false

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false, foreignKey = @ForeignKey(name = "fk_decision_template"))
    @JsonIgnore
    private WorkflowTemplate template;

    public boolean evaluate(Object actualValue) {
        if (actualValue == null) {
            return operator == DecisionLogicOperator.IS_NULL;
        }

        return switch (operator) {
            case EQUALS -> actualValue.toString().equals(expectedValue);
            case NOT_EQUALS -> !actualValue.toString().equals(expectedValue);
            case GREATER_THAN -> Double.parseDouble(actualValue.toString()) > Double.parseDouble(expectedValue);
            case LESS_THAN -> Double.parseDouble(actualValue.toString()) < Double.parseDouble(expectedValue);
            case GREATER_THAN_OR_EQUAL -> Double.parseDouble(actualValue.toString()) >= Double.parseDouble(expectedValue);
            case LESS_THAN_OR_EQUAL -> Double.parseDouble(actualValue.toString()) <= Double.parseDouble(expectedValue);
            case CONTAINS -> actualValue.toString().contains(expectedValue);
            case NOT_CONTAINS -> !actualValue.toString().contains(expectedValue);
            case IN -> expectedValue.contains(actualValue.toString());
            case NOT_IN -> !expectedValue.contains(actualValue.toString());
            case IS_NULL -> false;
            case IS_NOT_NULL -> true;
            default -> false;
        };
    }
}
