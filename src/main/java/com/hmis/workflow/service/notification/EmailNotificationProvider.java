package com.hmis.workflow.service.notification;

/**
 * Interface for email notification providers
 * Supports integration with various email services (SMTP, SendGrid, AWS SES, etc.)
 */
public interface EmailNotificationProvider {

    /**
     * Sends an email notification
     * @param to recipient email address
     * @param subject email subject
     * @param body email body (HTML or plain text)
     * @return true if email was sent successfully
     */
    boolean sendEmail(String to, String subject, String body);

    /**
     * Sends HTML email with attachments support
     * @param to recipient email address
     * @param subject email subject
     * @param htmlBody email body in HTML format
     * @param from sender email address (optional, uses default if null)
     * @return true if email was sent successfully
     */
    boolean sendHtmlEmail(String to, String subject, String htmlBody, String from);

    /**
     * Sends bulk emails
     * @param recipients list of recipient email addresses
     * @param subject email subject
     * @param body email body
     * @return number of emails sent successfully
     */
    int sendBulkEmail(java.util.List<String> recipients, String subject, String body);
}
