package com.hmis.workflow.service.notification.impl;

import com.hmis.workflow.service.notification.WhatsAppNotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Mock WhatsApp Notification Provider for development/testing
 * In production, replace with real provider (WhatsApp Business API, Twilio, etc.)
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "notification.whatsapp.provider", havingValue = "mock", matchIfMissing = true)
public class MockWhatsAppNotificationProvider implements WhatsAppNotificationProvider {

    private static final Pattern WHATSAPP_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    @Override
    public boolean sendMessage(String phoneNumber, String message) {
        log.info("MOCK WHATSAPP NOTIFICATION");
        log.info("  To: {}", phoneNumber);
        log.info("  Message: {}", message);
        return true;
    }

    @Override
    public boolean sendMediaMessage(String phoneNumber, String message, String mediaUrl, String mediaType) {
        log.info("MOCK WHATSAPP MEDIA NOTIFICATION");
        log.info("  To: {}", phoneNumber);
        log.info("  Message: {}", message);
        log.info("  Media URL: {}", mediaUrl);
        log.info("  Media Type: {}", mediaType);
        return true;
    }

    @Override
    public boolean sendTemplateMessage(String phoneNumber, String templateName, Map<String, String> parameters) {
        log.info("MOCK WHATSAPP TEMPLATE NOTIFICATION");
        log.info("  To: {}", phoneNumber);
        log.info("  Template: {}", templateName);
        log.info("  Parameters: {}", parameters);
        return true;
    }

    @Override
    public boolean isValidWhatsAppNumber(String phoneNumber) {
        return WHATSAPP_PATTERN.matcher(phoneNumber).matches();
    }

    @Override
    public boolean isConnected() {
        log.debug("Mock WhatsApp service is connected");
        return true;
    }
}
