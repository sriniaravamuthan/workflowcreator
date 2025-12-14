package com.hmis.workflow.domain.enums;

/**
 * NotificationChannel defines the available communication channels
 * for sending notifications to users
 */
public enum NotificationChannel {
    EMAIL("email", "Email"),
    SMS("sms", "SMS Text Message"),
    WHATSAPP("whatsapp", "WhatsApp"),
    PUSH_NOTIFICATION("push", "Mobile Push Notification");

    private final String code;
    private final String displayName;

    NotificationChannel(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
