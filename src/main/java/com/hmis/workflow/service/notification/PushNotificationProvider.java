package com.hmis.workflow.service.notification;

import java.util.List;
import java.util.Map;

/**
 * Interface for mobile push notification providers
 * Supports Firebase Cloud Messaging (FCM), Apple Push Notification Service (APNs), OneSignal, etc.
 */
public interface PushNotificationProvider {

    /**
     * Sends a push notification to a single device
     * @param deviceToken unique device token/ID from mobile app
     * @param title notification title
     * @param message notification message body
     * @return true if notification was sent successfully
     */
    boolean sendPushNotification(String deviceToken, String title, String message);

    /**
     * Sends push notification with custom data payload
     * @param deviceToken device token
     * @param title notification title
     * @param message notification message
     * @param customData custom key-value data to include in notification
     * @return true if notification was sent successfully
     */
    boolean sendPushNotificationWithData(String deviceToken, String title, String message, Map<String, String> customData);

    /**
     * Sends push notification to multiple devices
     * @param deviceTokens list of device tokens
     * @param title notification title
     * @param message notification message
     * @return number of notifications sent successfully
     */
    int sendBulkPushNotifications(List<String> deviceTokens, String title, String message);

    /**
     * Sends push notification with deep link
     * Enables clicking notification to open specific app screen
     * @param deviceToken device token
     * @param title notification title
     * @param message notification message
     * @param deepLink deep link URL (e.g., "app://workflows/123")
     * @return true if notification was sent successfully
     */
    boolean sendPushNotificationWithDeepLink(String deviceToken, String title, String message, String deepLink);

    /**
     * Validates device token format
     * @param deviceToken token to validate
     * @return true if token appears valid
     */
    boolean isValidDeviceToken(String deviceToken);

    /**
     * Gets push notification service status
     * @return true if push service is operational
     */
    boolean isServiceHealthy();
}
