package com.hmis.workflow.service.notification;

/**
 * Interface for SMS notification providers
 * Supports integration with various SMS services (Twilio, AWS SNS, Vonage, etc.)
 */
public interface SMSNotificationProvider {

    /**
     * Sends an SMS notification
     * @param phoneNumber recipient phone number (E.164 format preferred: +1234567890)
     * @param message SMS message content (max 160 characters for standard SMS)
     * @return true if SMS was sent successfully
     */
    boolean sendSms(String phoneNumber, String message);

    /**
     * Sends SMS with scheduling
     * @param phoneNumber recipient phone number
     * @param message SMS message content
     * @param scheduleTime time to send the SMS (as long timestamp)
     * @return true if SMS was scheduled successfully
     */
    boolean sendScheduledSms(String phoneNumber, String message, long scheduleTime);

    /**
     * Sends bulk SMS to multiple recipients
     * @param phoneNumbers list of recipient phone numbers
     * @param message SMS message content
     * @return number of SMS sent successfully
     */
    int sendBulkSms(java.util.List<String> phoneNumbers, String message);

    /**
     * Validates phone number format
     * @param phoneNumber phone number to validate
     * @return true if phone number is valid
     */
    boolean isValidPhoneNumber(String phoneNumber);
}
