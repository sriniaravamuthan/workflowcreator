package com.hmis.workflow.domain.enums;

/**
 * Enum representing types of orders in the clinical workflow
 */
public enum OrderType {
    LAB_TEST,           // Laboratory test
    IMAGING,            // Imaging study
    PROCEDURE,          // Clinical procedure
    MEDICATION,         // Prescription or medication
    SURGERY,            // Surgical procedure
    CONSULTATION,       // Consultation with specialist
    APPOINTMENT,        // Scheduled appointment
    SUPPLY_REQUEST,     // Medical supply request
    EQUIPMENT,          // Equipment request
    CUSTOM              // Custom order type
}
