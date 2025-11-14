package com.hmis.workflow.service.notification.impl;

import com.hmis.workflow.service.notification.PushNotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Mock Push Notification Provider for development/testing
 * In production, replace with real provider (Firebase Cloud Messaging, APNs, OneSignal, etc.)
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "notification.push.provider", havingValue = "mock", matchIfMissing = true)
public class MockPushNotificationProvider implements PushNotificationProvider {

    @Override
    public boolean sendPushNotification(String deviceToken, String title, String message) {
        log.info("MOCK PUSH NOTIFICATION");
        log.info("  Device Token: {}", maskToken(deviceToken));
        log.info("  Title: {}", title);
        log.info("  Message: {}", message);
        return true;
    }

    @Override
    public boolean sendPushNotificationWithData(String deviceToken, String title, String message, Map<String, String> customData) {
        log.info("MOCK PUSH NOTIFICATION WITH DATA");
        log.info("  Device Token: {}", maskToken(deviceToken));
        log.info("  Title: {}", title);
        log.info("  Message: {}", message);
        log.info("  Custom Data: {}", customData);
        return true;
    }

    @Override
    public int sendBulkPushNotifications(List<String> deviceTokens, String title, String message) {
        log.info("MOCK BULK PUSH NOTIFICATIONS");
        log.info("  Recipients: {}", deviceTokens.size());
        log.info("  Title: {}", title);
        log.info("  Message: {}", message);
        return deviceTokens.size();
    }

    @Override
    public boolean sendPushNotificationWithDeepLink(String deviceToken, String title, String message, String deepLink) {
        log.info("MOCK PUSH NOTIFICATION WITH DEEP LINK");
        log.info("  Device Token: {}", maskToken(deviceToken));
        log.info("  Title: {}", title);
        log.info("  Message: {}", message);
        log.info("  Deep Link: {}", deepLink);
        return true;
    }

    @Override
    public boolean isValidDeviceToken(String deviceToken) {
        // Basic validation - token should be non-empty and reasonably long
        return deviceToken != null && !deviceToken.trim().isEmpty() && deviceToken.length() > 10;
    }

    @Override
    public boolean isServiceHealthy() {
        log.debug("Mock push notification service is healthy");
        return true;
    }

    /**
     * Masks device token for logging (shows first and last 8 chars only)
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 16) {
            return "***";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 8);
    }
}
