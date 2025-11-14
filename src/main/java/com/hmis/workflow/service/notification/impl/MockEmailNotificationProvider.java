package com.hmis.workflow.service.notification.impl;

import com.hmis.workflow.service.notification.EmailNotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mock Email Notification Provider for development/testing
 * In production, replace with real provider (SendGrid, AWS SES, SMTP, etc.)
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "notification.email.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailNotificationProvider implements EmailNotificationProvider {

    @Override
    public boolean sendEmail(String to, String subject, String body) {
        log.info("MOCK EMAIL NOTIFICATION");
        log.info("  To: {}", to);
        log.info("  Subject: {}", subject);
        log.info("  Body: {}", body);
        return true;
    }

    @Override
    public boolean sendHtmlEmail(String to, String subject, String htmlBody, String from) {
        log.info("MOCK HTML EMAIL NOTIFICATION");
        log.info("  To: {}", to);
        log.info("  From: {}", from != null ? from : "default@example.com");
        log.info("  Subject: {}", subject);
        log.info("  HTML Body: {}", htmlBody);
        return true;
    }

    @Override
    public int sendBulkEmail(List<String> recipients, String subject, String body) {
        log.info("MOCK BULK EMAIL NOTIFICATION");
        log.info("  Recipients: {}", recipients.size());
        log.info("  Subject: {}", subject);
        log.info("  Body: {}", body);
        return recipients.size();
    }
}
