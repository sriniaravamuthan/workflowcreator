package com.hmis.workflow.service.notification.impl;

import com.hmis.workflow.service.notification.SMSNotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Mock SMS Notification Provider for development/testing
 * In production, replace with real provider (Twilio, AWS SNS, Vonage, etc.)
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "notification.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSMSNotificationProvider implements SMSNotificationProvider {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    @Override
    public boolean sendSms(String phoneNumber, String message) {
        log.info("MOCK SMS NOTIFICATION");
        log.info("  To: {}", phoneNumber);
        log.info("  Message: {}", message);
        log.info("  Length: {} characters", message.length());
        return true;
    }

    @Override
    public boolean sendScheduledSms(String phoneNumber, String message, long scheduleTime) {
        log.info("MOCK SCHEDULED SMS NOTIFICATION");
        log.info("  To: {}", phoneNumber);
        log.info("  Message: {}", message);
        log.info("  Scheduled for: {}", scheduleTime);
        return true;
    }

    @Override
    public int sendBulkSms(List<String> phoneNumbers, String message) {
        log.info("MOCK BULK SMS NOTIFICATION");
        log.info("  Recipients: {}", phoneNumbers.size());
        log.info("  Message: {}", message);
        return phoneNumbers.size();
    }

    @Override
    public boolean isValidPhoneNumber(String phoneNumber) {
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }
}
