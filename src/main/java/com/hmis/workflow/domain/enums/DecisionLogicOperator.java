package com.hmis.workflow.domain.enums;

/**
 * Enum representing operators for decision logic in workflows
 */
public enum DecisionLogicOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL,
    CONTAINS,
    NOT_CONTAINS,
    IN,
    NOT_IN,
    IS_NULL,
    IS_NOT_NULL,
    AND,
    OR
}
