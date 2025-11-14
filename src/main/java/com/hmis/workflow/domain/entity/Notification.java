package com.hmis.workflow.domain.entity;

import com.hmis.workflow.domain.enums.NotificationChannel;
import com.hmis.workflow.domain.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification entity tracks all notifications sent to users for workflow events
 * Maintains audit trail of communications for compliance and troubleshooting
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_user_id", columnList = "recipient_user_id"),
        @Index(name = "idx_notifications_status", columnList = "status"),
        @Index(name = "idx_notifications_channel", columnList = "channel"),
        @Index(name = "idx_notifications_workflow_id", columnList = "workflow_instance_id"),
        @Index(name = "idx_notifications_task_id", columnList = "task_instance_id"),
        @Index(name = "idx_notifications_created_at", columnList = "created_at"),
        @Index(name = "idx_notifications_sent_at", columnList = "sent_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    @Column(name = "recipient_user_id", nullable = false)
    private String recipientUserId;

    @Column(name = "recipient_phone_number")
    private String recipientPhoneNumber;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "recipient_whatsapp_number")
    private String recipientWhatsappNumber;

    @Column(name = "recipient_push_token")
    private String recipientPushToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel; // EMAIL, SMS, WHATSAPP, PUSH_NOTIFICATION

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status; // PENDING, SENT, DELIVERED, FAILED, BOUNCED

    @Column(name = "notification_type", nullable = false)
    private String notificationType; // TASK_ASSIGNED, TASK_ESCALATED, SLA_BREACH, ORDER_CREATED, etc.

    @Column(name = "subject")
    private String subject;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "workflow_instance_id")
    private String workflowInstanceId;

    @Column(name = "task_instance_id")
    private String taskInstanceId;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "correlation_id")
    private String correlationId; // For tracing related events

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.maxRetries = 3;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks notification as sent
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * Marks notification as delivered
     */
    public void markAsDelivered() {
        this.status = NotificationStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    /**
     * Marks notification as failed with reason
     */
    public void markAsFailed(String failureReason) {
        this.failureReason = failureReason;
        this.status = NotificationStatus.FAILED;
        this.retryCount++;
    }

    /**
     * Check if notification can be retried
     */
    public boolean isRetryable() {
        return this.retryCount < this.maxRetries &&
               (this.status == NotificationStatus.FAILED || this.status == NotificationStatus.PENDING);
    }
}
