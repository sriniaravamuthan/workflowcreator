package com.hmis.workflow.domain.enums;

/**
 * Enum representing types of instructions
 */
public enum InstructionType {
    NPO,                    // Nothing by mouth
    ISOLATION,              // Isolation precautions
    DISCHARGE_RESTRICTION,  // Discharge restrictions
    DIET_RESTRICTION,       // Diet restrictions
    ACTIVITY_RESTRICTION,   // Activity restrictions
    MONITORING,             // Special monitoring requirements
    PRECAUTION,             // General precautions
    CUSTOM                  // Custom instruction
}
