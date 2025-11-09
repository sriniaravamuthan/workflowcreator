package com.hmis.workflow.domain.enums;

/**
 * Enum representing types of compensation actions triggered when orders/tasks fail
 */
public enum CompensationActionType {
    CANCEL_ORDER,           // Cancel the order
    REVERSE_CHARGE,         // Reverse billing charges
    NOTIFY_LAB,             // Notify lab to discard specimens
    RELEASE_RESOURCE,       // Release allocated resources
    CANCEL_APPOINTMENT,     // Cancel scheduled appointment
    ESCALATE_MANAGER,       // Escalate to manager
    SEND_NOTIFICATION,      // Send notification to stakeholders
    REQUEUE_TASK,           // Requeue task for retry
    CUSTOM                  // Custom compensation action
}
