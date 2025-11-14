package com.hmis.workflow.domain.entity;

import com.hmis.workflow.domain.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * UserNotificationPreference stores user communication preferences
 * Defines which channels they want to receive notifications on
 */
@Entity
@Table(
    name = "user_notification_preferences",
    indexes = {
        @Index(name = "idx_user_notif_pref_user_id", columnList = "user_id"),
        @Index(name = "idx_user_notif_pref_active", columnList = "is_active")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationPreference {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "mobile_push_token")
    private String mobilePushToken;

    // Bitmask or list of preferred channels
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_notification_channels",
        joinColumns = @JoinColumn(name = "user_preference_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private Set<NotificationChannel> preferredChannels;

    @Column(name = "notify_task_assignment")
    @Builder.Default
    private Boolean notifyTaskAssignment = true;

    @Column(name = "notify_task_escalation")
    @Builder.Default
    private Boolean notifyTaskEscalation = true;

    @Column(name = "notify_sla_breach")
    @Builder.Default
    private Boolean notifySLABreach = true;

    @Column(name = "notify_order_created")
    @Builder.Default
    private Boolean notifyOrderCreated = false;

    @Column(name = "notify_workflow_completion")
    @Builder.Default
    private Boolean notifyWorkflowCompletion = true;

    @Column(name = "notify_workflow_failure")
    @Builder.Default
    private Boolean notifyWorkflowFailure = true;

    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private Boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    private String quietHoursStart; // Format: HH:mm (e.g., "22:00")

    @Column(name = "quiet_hours_end")
    private String quietHoursEnd; // Format: HH:mm (e.g., "08:00")

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.preferredChannels == null) {
            this.preferredChannels = new HashSet<>();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if user wants to be notified via a specific channel
     */
    public boolean isChannelEnabled(NotificationChannel channel) {
        return this.isActive && this.preferredChannels.contains(channel);
    }

    /**
     * Checks if user wants to be notified for a specific event type
     */
    public boolean shouldNotify(String eventType) {
        if (!this.isActive) {
            return false;
        }

        return switch (eventType) {
            case "TASK_ASSIGNMENT" -> this.notifyTaskAssignment;
            case "TASK_ESCALATION" -> this.notifyTaskEscalation;
            case "SLA_BREACH" -> this.notifySLABreach;
            case "ORDER_CREATED" -> this.notifyOrderCreated;
            case "WORKFLOW_COMPLETION" -> this.notifyWorkflowCompletion;
            case "WORKFLOW_FAILURE" -> this.notifyWorkflowFailure;
            default -> false;
        };
    }

    /**
     * Checks if current time is within quiet hours
     */
    public boolean isWithinQuietHours() {
        if (!this.quietHoursEnabled) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return isTimeWithinRange(now.getHour(), now.getMinute());
    }

    /**
     * Helper to check if time is within quiet hours range
     */
    private boolean isTimeWithinRange(int currentHour, int currentMinute) {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        String[] startParts = quietHoursStart.split(":");
        String[] endParts = quietHoursEnd.split(":");

        int startHour = Integer.parseInt(startParts[0]);
        int startMinute = Integer.parseInt(startParts[1]);
        int endHour = Integer.parseInt(endParts[0]);
        int endMinute = Integer.parseInt(endParts[1]);

        int currentTotalMinutes = currentHour * 60 + currentMinute;
        int startTotalMinutes = startHour * 60 + startMinute;
        int endTotalMinutes = endHour * 60 + endMinute;

        if (startTotalMinutes <= endTotalMinutes) {
            // Quiet hours don't cross midnight
            return currentTotalMinutes >= startTotalMinutes && currentTotalMinutes < endTotalMinutes;
        } else {
            // Quiet hours cross midnight
            return currentTotalMinutes >= startTotalMinutes || currentTotalMinutes < endTotalMinutes;
        }
    }
}
