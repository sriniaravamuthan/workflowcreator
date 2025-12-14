package com.hmis.workflow.service.notification;

import java.util.Map;

/**
 * Interface for WhatsApp notification providers
 * Supports integration with WhatsApp Business API, Twilio WhatsApp, etc.
 */
public interface WhatsAppNotificationProvider {

    /**
     * Sends a WhatsApp message
     * @param phoneNumber recipient phone number with country code (e.g., +1234567890)
     * @param message message text
     * @return true if message was sent successfully
     */
    boolean sendMessage(String phoneNumber, String message);

    /**
     * Sends WhatsApp message with media
     * @param phoneNumber recipient phone number
     * @param message message text
     * @param mediaUrl URL to media file (image, video, document)
     * @param mediaType type of media (image, video, document)
     * @return true if message was sent successfully
     */
    boolean sendMediaMessage(String phoneNumber, String message, String mediaUrl, String mediaType);

    /**
     * Sends WhatsApp template message
     * Useful for pre-approved message templates for compliance
     * @param phoneNumber recipient phone number
     * @param templateName name of the template
     * @param parameters template parameters (key-value pairs)
     * @return true if message was sent successfully
     */
    boolean sendTemplateMessage(String phoneNumber, String templateName, Map<String, String> parameters);

    /**
     * Validates WhatsApp phone number
     * @param phoneNumber phone number to validate
     * @return true if number is valid WhatsApp format
     */
    boolean isValidWhatsAppNumber(String phoneNumber);

    /**
     * Gets WhatsApp business account status
     * @return true if WhatsApp Business API is connected and operational
     */
    boolean isConnected();
}
