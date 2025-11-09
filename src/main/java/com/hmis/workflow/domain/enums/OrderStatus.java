package com.hmis.workflow.domain.enums;

/**
 * Enum representing the 8 states of an order
 * Follows HMIS order lifecycle: Proposed → Authorized → Activated → In Progress → Resulted → Verified → Closed/Cancelled
 */
public enum OrderStatus {
    PROPOSED,      // Draft stage, editable before authorization
    AUTHORIZED,    // Clinician signs; becomes official request
    ACTIVATED,     // Transmitted to appropriate department
    IN_PROGRESS,   // Work underway; staff performing service
    RESULTED,      // Service finished; results posted (for tests/imaging)
    DISPENSED,     // For medications and items
    COMPLETED,     // Service completed
    VERIFIED,      // Clinician reviews and accepts outcome
    CLOSED,        // Fully complete
    CANCELLED      // Voided with compensation actions triggered
}
