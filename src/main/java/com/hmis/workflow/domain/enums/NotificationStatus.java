package com.hmis.workflow.domain.enums;

/**
 * NotificationStatus tracks the lifecycle of a notification
 * from creation through delivery or failure
 */
public enum NotificationStatus {
    PENDING("pending", "Notification is queued for sending"),
    SENT("sent", "Notification has been sent to provider"),
    DELIVERED("delivered", "Notification was successfully delivered to recipient"),
    FAILED("failed", "Notification failed to send"),
    BOUNCED("bounced", "Notification was bounced by provider (invalid address)");

    private final String code;
    private final String description;

    NotificationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
