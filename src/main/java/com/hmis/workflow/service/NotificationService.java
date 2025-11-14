package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.Notification;
import com.hmis.workflow.domain.entity.UserNotificationPreference;
import com.hmis.workflow.domain.enums.NotificationChannel;
import com.hmis.workflow.domain.enums.NotificationStatus;
import com.hmis.workflow.repository.NotificationRepository;
import com.hmis.workflow.repository.UserNotificationPreferenceRepository;
import com.hmis.workflow.service.notification.EmailNotificationProvider;
import com.hmis.workflow.service.notification.PushNotificationProvider;
import com.hmis.workflow.service.notification.SMSNotificationProvider;
import com.hmis.workflow.service.notification.WhatsAppNotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * NotificationService manages multi-channel notifications
 * Handles routing to appropriate channels (Email, SMS, WhatsApp, Push)
 * Persists notifications for audit trail and retry logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationPreferenceRepository userPreferenceRepository;
    private final EmailNotificationProvider emailProvider;
    private final SMSNotificationProvider smsProvider;
    private final WhatsAppNotificationProvider whatsAppProvider;
    private final PushNotificationProvider pushProvider;

    @Value("${notification.max-retries:3}")
    private int maxRetries;

    @Value("${notification.enable-email:true}")
    private boolean enableEmailNotifications;

    @Value("${notification.enable-sms:true}")
    private boolean enableSmsNotifications;

    @Value("${notification.enable-whatsapp:true}")
    private boolean enableWhatsAppNotifications;

    @Value("${notification.enable-push:true}")
    private boolean enablePushNotifications;

    /**
     * Sends notifications to a user via their preferred channels
     * Handles multi-channel delivery asynchronously
     */
    @Async
    public void notifyUser(NotificationRequest request) {
        try {
            log.info("Processing notification request for user: {} - Type: {}",
                    request.getRecipientUserId(), request.getNotificationType());

            // Get user preferences
            Optional<UserNotificationPreference> preference = userPreferenceRepository
                    .findByUserId(request.getRecipientUserId());

            if (preference.isEmpty()) {
                log.warn("No notification preferences found for user: {}", request.getRecipientUserId());
                return;
            }

            UserNotificationPreference userPref = preference.get();

            // Check if user wants to be notified for this event type
            if (!userPref.shouldNotify(request.getNotificationType())) {
                log.debug("User {} opted out of {} notifications",
                        request.getRecipientUserId(), request.getNotificationType());
                return;
            }

            // Check quiet hours
            if (userPref.isWithinQuietHours()) {
                log.debug("User {} is in quiet hours, deferring notification", request.getRecipientUserId());
                // Queue for later delivery
                return;
            }

            // Route to enabled channels
            Set<NotificationChannel> enabledChannels = new HashSet<>(userPref.getPreferredChannels());

            for (NotificationChannel channel : enabledChannels) {
                if (isChannelEnabled(channel)) {
                    sendViaChannel(request, channel, userPref);
                }
            }

        } catch (Exception e) {
            log.error("Error processing notification request", e);
        }
    }

    /**
     * Sends notification via specific channel
     */
    private void sendViaChannel(NotificationRequest request, NotificationChannel channel,
                               UserNotificationPreference userPref) {
        try {
            Notification notification = createNotification(request, channel, userPref);

            switch (channel) {
                case EMAIL:
                    if (userPref.getEmailAddress() != null) {
                        sendEmailNotification(notification, userPref.getEmailAddress());
                    }
                    break;

                case SMS:
                    if (userPref.getPhoneNumber() != null) {
                        sendSmsNotification(notification, userPref.getPhoneNumber());
                    }
                    break;

                case WHATSAPP:
                    if (userPref.getWhatsappNumber() != null) {
                        sendWhatsAppNotification(notification, userPref.getWhatsappNumber());
                    }
                    break;

                case PUSH_NOTIFICATION:
                    if (userPref.getMobilePushToken() != null) {
                        sendPushNotification(notification, userPref.getMobilePushToken());
                    }
                    break;

                default:
                    log.warn("Unknown notification channel: {}", channel);
            }

        } catch (Exception e) {
            log.error("Error sending notification via channel {}", channel, e);
        }
    }

    /**
     * Creates notification record for audit trail
     */
    private Notification createNotification(NotificationRequest request, NotificationChannel channel,
                                           UserNotificationPreference userPref) {
        Notification notification = Notification.builder()
                .recipientUserId(request.getRecipientUserId())
                .channel(channel)
                .notificationType(request.getNotificationType())
                .subject(request.getSubject())
                .message(request.getMessage())
                .workflowInstanceId(request.getWorkflowInstanceId())
                .taskInstanceId(request.getTaskInstanceId())
                .orderId(request.getOrderId())
                .patientId(request.getPatientId())
                .correlationId(request.getCorrelationId())
                .maxRetries(maxRetries)
                .build();

        // Set channel-specific recipient info
        switch (channel) {
            case EMAIL:
                notification.setRecipientEmail(userPref.getEmailAddress());
                break;
            case SMS:
                notification.setRecipientPhoneNumber(userPref.getPhoneNumber());
                break;
            case WHATSAPP:
                notification.setRecipientWhatsappNumber(userPref.getWhatsappNumber());
                break;
            case PUSH_NOTIFICATION:
                notification.setRecipientPushToken(userPref.getMobilePushToken());
                break;
        }

        return notificationRepository.save(notification);
    }

    /**
     * Sends email notification
     */
    private void sendEmailNotification(Notification notification, String emailAddress) {
        try {
            log.info("Sending email notification to: {}", emailAddress);

            boolean success = emailProvider.sendEmail(
                    emailAddress,
                    notification.getSubject(),
                    notification.getMessage()
            );

            if (success) {
                notification.markAsSent();
                notificationRepository.save(notification);
                log.info("Email notification sent successfully: {}", notification.getId());
            } else {
                notification.markAsFailed("Email provider returned false");
                notificationRepository.save(notification);
                log.warn("Email notification failed: {}", notification.getId());
            }
        } catch (Exception e) {
            log.error("Error sending email notification", e);
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    /**
     * Sends SMS notification
     */
    private void sendSmsNotification(Notification notification, String phoneNumber) {
        try {
            log.info("Sending SMS notification to: {}", phoneNumber);

            boolean success = smsProvider.sendSms(
                    phoneNumber,
                    notification.getMessage()
            );

            if (success) {
                notification.markAsSent();
                notificationRepository.save(notification);
                log.info("SMS notification sent successfully: {}", notification.getId());
            } else {
                notification.markAsFailed("SMS provider returned false");
                notificationRepository.save(notification);
                log.warn("SMS notification failed: {}", notification.getId());
            }
        } catch (Exception e) {
            log.error("Error sending SMS notification", e);
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    /**
     * Sends WhatsApp notification
     */
    private void sendWhatsAppNotification(Notification notification, String whatsappNumber) {
        try {
            log.info("Sending WhatsApp notification to: {}", whatsappNumber);

            boolean success = whatsAppProvider.sendMessage(
                    whatsappNumber,
                    notification.getMessage()
            );

            if (success) {
                notification.markAsSent();
                notificationRepository.save(notification);
                log.info("WhatsApp notification sent successfully: {}", notification.getId());
            } else {
                notification.markAsFailed("WhatsApp provider returned false");
                notificationRepository.save(notification);
                log.warn("WhatsApp notification failed: {}", notification.getId());
            }
        } catch (Exception e) {
            log.error("Error sending WhatsApp notification", e);
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    /**
     * Sends mobile push notification
     */
    private void sendPushNotification(Notification notification, String pushToken) {
        try {
            log.info("Sending push notification with token: {}", pushToken);

            boolean success = pushProvider.sendPushNotification(
                    pushToken,
                    notification.getSubject(),
                    notification.getMessage()
            );

            if (success) {
                notification.markAsSent();
                notificationRepository.save(notification);
                log.info("Push notification sent successfully: {}", notification.getId());
            } else {
                notification.markAsFailed("Push provider returned false");
                notificationRepository.save(notification);
                log.warn("Push notification failed: {}", notification.getId());
            }
        } catch (Exception e) {
            log.error("Error sending push notification", e);
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    /**
     * Checks if a notification channel is enabled
     */
    private boolean isChannelEnabled(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> enableEmailNotifications;
            case SMS -> enableSmsNotifications;
            case WHATSAPP -> enableWhatsAppNotifications;
            case PUSH_NOTIFICATION -> enablePushNotifications;
        };
    }

    /**
     * Retries failed notifications
     * Runs every 5 minutes to retry failed deliveries
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void retryFailedNotifications() {
        log.debug("Starting failed notification retry cycle");

        try {
            List<Notification> retryableNotifications = notificationRepository
                    .findRetryableNotifications();

            if (retryableNotifications.isEmpty()) {
                log.debug("No notifications to retry");
                return;
            }

            log.info("Found {} notifications to retry", retryableNotifications.size());

            for (Notification notification : retryableNotifications) {
                retryNotification(notification);
            }

        } catch (Exception e) {
            log.error("Error in notification retry cycle", e);
        }
    }

    /**
     * Retries a single notification
     */
    private void retryNotification(Notification notification) {
        try {
            log.info("Retrying notification: {} (attempt {}/{})",
                    notification.getId(), notification.getRetryCount() + 1, notification.getMaxRetries());

            switch (notification.getChannel()) {
                case EMAIL:
                    emailProvider.sendEmail(
                            notification.getRecipientEmail(),
                            notification.getSubject(),
                            notification.getMessage()
                    );
                    break;

                case SMS:
                    smsProvider.sendSms(
                            notification.getRecipientPhoneNumber(),
                            notification.getMessage()
                    );
                    break;

                case WHATSAPP:
                    whatsAppProvider.sendMessage(
                            notification.getRecipientWhatsappNumber(),
                            notification.getMessage()
                    );
                    break;

                case PUSH_NOTIFICATION:
                    pushProvider.sendPushNotification(
                            notification.getRecipientPushToken(),
                            notification.getSubject(),
                            notification.getMessage()
                    );
                    break;
            }

            notification.markAsSent();
            notificationRepository.save(notification);
            log.info("Notification retry successful: {}", notification.getId());

        } catch (Exception e) {
            log.error("Error retrying notification: {}", notification.getId(), e);
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    /**
     * Gets notification history for a user
     */
    public List<Notification> getUserNotificationHistory(String userId) {
        return notificationRepository.findByRecipientUserId(userId);
    }

    /**
     * Gets pending notifications count for a user
     */
    public long getPendingNotificationCount(String userId) {
        return notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.PENDING);
    }

    /**
     * Updates user notification preferences
     */
    public void updateUserNotificationPreference(String userId, UserNotificationPreference preference) {
        Optional<UserNotificationPreference> existing = userPreferenceRepository.findByUserId(userId);

        if (existing.isPresent()) {
            UserNotificationPreference updated = existing.get();
            updated.setPreferredChannels(preference.getPreferredChannels());
            updated.setEmailAddress(preference.getEmailAddress());
            updated.setPhoneNumber(preference.getPhoneNumber());
            updated.setWhatsappNumber(preference.getWhatsappNumber());
            updated.setMobilePushToken(preference.getMobilePushToken());
            updated.setNotifyTaskAssignment(preference.getNotifyTaskAssignment());
            updated.setNotifyTaskEscalation(preference.getNotifyTaskEscalation());
            updated.setNotifySLABreach(preference.getNotifySLABreach());
            updated.setQuietHoursEnabled(preference.getQuietHoursEnabled());
            updated.setQuietHoursStart(preference.getQuietHoursStart());
            updated.setQuietHoursEnd(preference.getQuietHoursEnd());

            userPreferenceRepository.save(updated);
        } else {
            preference.setId(UUID.randomUUID().toString());
            preference.setUserId(userId);
            userPreferenceRepository.save(preference);
        }
    }

    /**
     * Gets user notification preferences
     */
    public Optional<UserNotificationPreference> getUserNotificationPreference(String userId) {
        return userPreferenceRepository.findByUserId(userId);
    }
}
