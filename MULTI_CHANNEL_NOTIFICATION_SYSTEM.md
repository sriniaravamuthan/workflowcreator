# Multi-Channel Notification System

## Overview

The HMIS Workflow Engine includes a comprehensive multi-channel notification system that automatically sends alerts via **Email**, **SMS**, **WhatsApp**, and **Mobile Push Notifications** when workflow events occur (task assignment, escalation, SLA breaches, etc.).

## Key Features

✅ **Multi-Channel Delivery**
- Simultaneously send notifications across multiple channels
- User preference-based routing (each user selects their preferred channels)
- Fallback mechanisms if primary channel fails

✅ **Smart Notification Routing**
- Event-type filtering (task assignment, escalation, SLA breach, etc.)
- Quiet hours support (no notifications during specified times)
- Retry logic for failed deliveries (up to 3 automatic retries)

✅ **Comprehensive Audit Trail**
- All notifications logged for compliance
- Notification status tracking (pending, sent, delivered, failed, bounced)
- Correlation IDs for distributed tracing

✅ **Production-Ready**
- Pluggable provider architecture (swap implementations easily)
- Mock providers for development/testing
- Async notification processing (non-blocking)
- Thread pool management with configurable timeouts

## Architecture

### Domain Model

```
Notification (Entity)
├── Recipient User ID
├── Channel (EMAIL, SMS, WHATSAPP, PUSH_NOTIFICATION)
├── Status (PENDING, SENT, DELIVERED, FAILED, BOUNCED)
├── Message Content & Subject
├── Event Context (workflow ID, task ID, order ID, patient ID)
├── Retry Management (count, max attempts)
└── Timestamps (created_at, sent_at, delivered_at, updated_at)

UserNotificationPreference (Entity)
├── User ID
├── Preferred Channels (Set<NotificationChannel>)
├── Contact Information
│   ├── Email Address
│   ├── Phone Number
│   ├── WhatsApp Number
│   └── Mobile Push Token
├── Event Preferences
│   ├── Notify Task Assignment
│   ├── Notify Task Escalation
│   ├── Notify SLA Breach
│   ├── Notify Order Created
│   ├── Notify Workflow Completion
│   └── Notify Workflow Failure
└── Quiet Hours
    ├── Quiet Hours Enabled
    ├── Start Time (HH:mm)
    └── End Time (HH:mm)
```

### Service Layer

```
NotificationService (Core)
├── notifyUser(NotificationRequest)
│   ├── Get user preferences
│   ├── Validate notification eligibility
│   ├── Check quiet hours
│   └── Route to enabled channels
├── sendViaChannel(request, channel, userPreference)
│   ├── Create notification record
│   ├── Invoke appropriate provider
│   └── Update status
├── retryFailedNotifications() [Scheduled every 5 minutes]
│   ├── Query failed notifications
│   ├── Retry eligible notifications
│   └── Update status
├── getUserNotificationHistory(userId)
├── updateUserNotificationPreference(userId, preference)
└── getUserNotificationPreference(userId)

Notification Providers (Pluggable Interfaces)
├── EmailNotificationProvider
│   ├── sendEmail(to, subject, body)
│   ├── sendHtmlEmail(to, subject, htmlBody, from)
│   └── sendBulkEmail(recipients, subject, body)
├── SMSNotificationProvider
│   ├── sendSms(phoneNumber, message)
│   ├── sendScheduledSms(phoneNumber, message, scheduleTime)
│   ├── sendBulkSms(phoneNumbers, message)
│   └── isValidPhoneNumber(phoneNumber)
├── WhatsAppNotificationProvider
│   ├── sendMessage(phoneNumber, message)
│   ├── sendMediaMessage(phoneNumber, message, mediaUrl, mediaType)
│   ├── sendTemplateMessage(phoneNumber, templateName, parameters)
│   └── isValidWhatsAppNumber(phoneNumber)
└── PushNotificationProvider
    ├── sendPushNotification(deviceToken, title, message)
    ├── sendPushNotificationWithData(deviceToken, title, message, customData)
    ├── sendBulkPushNotifications(deviceTokens, title, message)
    ├── sendPushNotificationWithDeepLink(deviceToken, title, message, deepLink)
    └── isValidDeviceToken(deviceToken)
```

### Event Consumer Integration

```
TaskEventConsumer
├── On TASK_COMPLETED: Route to next task (auto-propagate)
├── On TASK_ESCALATED: Send escalation notification
├── On SLA_BREACH: Send breach alerts
└── Notification Helpers:
    ├── notifyTaskEscalation(task, escalatedUser)
    ├── notifyTaskAssignment(task, assignedUser)
    └── notifySLABreach(task, escalatedUser)

SLAMonitoringService
├── monitorSLABreaches() [Every 5 minutes]
│   ├── Detect breached tasks
│   ├── Auto-escalate
│   └── sendSLABreachAlert()
│       ├── Notify assignee
│       ├── Notify manager
│       └── Send via user's preferred channels
├── monitorStaleTasks() [Every 10 minutes]
├── monitorEscalatedWorkflows() [Every 15 minutes]
└── healthCheckSLAMonitoring() [Every 1 hour]
```

## Notification Types & Triggers

### 1. Task Assignment
**Trigger:** When a new task is created and assigned to a user

**Recipients:** Assigned user

**Channels:** Email, SMS, WhatsApp, Push (based on user preference)

**Subject:** "New Task Assigned: '[task name]'"

**Message Contains:**
- Task name
- Patient ID
- Due date
- Priority level
- Call to action (log in and start task)

### 2. Task Escalation
**Trigger:** When a task SLA is breached and escalated

**Recipients:** Escalated user (manager/supervisor)

**Channels:** All enabled channels (uses quiet hours)

**Subject:** "URGENT: Task '[task name]' Escalated - SLA Breach"

**Message Contains:**
- Task name
- Patient information
- Due date
- Escalation reason
- Urgency indicator
- Call to action (immediate attention needed)

### 3. SLA Breach Alert
**Trigger:** Detected every 5 minutes by SLAMonitoringService

**Recipients:**
- Primary: Task assignee
- Secondary: Manager/supervisor (if escalated)

**Channels:** Email, SMS, WhatsApp, Push (based on user preference)

**Subject (Assignee):** "URGENT: Your Task SLA Has Been Breached"

**Subject (Manager):** "CRITICAL: Task SLA Breach Escalation Required"

**Message Contains:**
- Task name
- Patient information
- Hours overdue
- Escalation status
- Who needs to take action

### 4. Order Creation
**Trigger:** When a new order is created (if user opted in)

**Recipients:** Relevant clinical staff

**Channels:** Email, SMS, WhatsApp, Push

**Message Contains:**
- Order details
- Order type
- Patient information
- Estimated cost

### 5. Workflow Completion
**Trigger:** When all workflow tasks are completed

**Recipients:** Workflow initiator/coordinator

**Channels:** Email, Push

**Message Contains:**
- Workflow name
- Patient information
- Total duration
- Summary of completed tasks

### 6. Workflow Failure
**Trigger:** When workflow execution fails

**Recipients:** Workflow coordinator, managers

**Channels:** All enabled channels

**Message Contains:**
- Workflow name
- Failure reason
- Patient information
- Recovery options

## User Preference Management

### Setting Notification Preferences

```java
// Example: Update user preferences for task assignment and escalation
UserNotificationPreference preference = UserNotificationPreference.builder()
    .userId("USER_123")
    .emailAddress("user@hospital.com")
    .phoneNumber("+1234567890")
    .whatsappNumber("+1234567890")
    .mobilePushToken("fcm_device_token_xyz")
    .preferredChannels(Set.of(
        NotificationChannel.EMAIL,
        NotificationChannel.SMS,
        NotificationChannel.PUSH_NOTIFICATION
    ))
    .notifyTaskAssignment(true)
    .notifyTaskEscalation(true)
    .notifySLABreach(true)
    .notifyWorkflowCompletion(true)
    .quietHoursEnabled(true)
    .quietHoursStart("22:00")  // 10 PM
    .quietHoursEnd("08:00")    // 8 AM
    .isActive(true)
    .build();

notificationService.updateUserNotificationPreference("USER_123", preference);
```

### Retrieving Preferences

```java
// Get user notification preferences
Optional<UserNotificationPreference> pref =
    notificationService.getUserNotificationPreference("USER_123");

// Get pending notification count
long pendingCount =
    notificationService.getPendingNotificationCount("USER_123");

// Get notification history
List<Notification> history =
    notificationService.getUserNotificationHistory("USER_123");
```

## Implementation Guide

### 1. Configuration

Add to `application.yml`:

```yaml
notification:
  max-retries: 3
  enable-email: true
  enable-sms: true
  enable-whatsapp: true
  enable-push: true
  email:
    provider: sendgrid  # mock, sendgrid, ses, smtp
  sms:
    provider: twilio    # mock, twilio, vonage, aws-sns
  whatsapp:
    provider: twilio    # mock, twilio, waba
  push:
    provider: firebase  # mock, firebase, apns, onesignal
```

### 2. Development/Testing (Using Mock Providers)

No additional configuration needed - mock providers are enabled by default.

```
MOCK EMAIL NOTIFICATION
  To: user@example.com
  Subject: New Task Assigned
  Body: You have been assigned a new task...

MOCK SMS NOTIFICATION
  To: +1234567890
  Message: New task assigned: "Lab Test" for patient PAT001...

MOCK WHATSAPP NOTIFICATION
  To: +1234567890
  Message: New task assigned...

MOCK PUSH NOTIFICATION
  Device Token: abcd1234...
  Title: New Task
  Message: You have been assigned a new task...
```

### 3. Production Deployment

#### Email (SendGrid)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.sendgrid</groupId>
    <artifactId>sendgrid-java</artifactId>
    <version>4.10.1</version>
</dependency>
```

Implement `SendGridEmailNotificationProvider`:

```java
@Service
@ConditionalOnProperty(name = "notification.email.provider", havingValue = "sendgrid")
public class SendGridEmailNotificationProvider implements EmailNotificationProvider {
    // Implementation using SendGrid API
}
```

#### SMS (Twilio)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>9.2.0</version>
</dependency>
```

Implement `TwilioSMSNotificationProvider`:

```java
@Service
@ConditionalOnProperty(name = "notification.sms.provider", havingValue = "twilio")
public class TwilioSMSNotificationProvider implements SMSNotificationProvider {
    // Implementation using Twilio API
}
```

#### WhatsApp (Twilio Business API)

```java
@Service
@ConditionalOnProperty(name = "notification.whatsapp.provider", havingValue = "twilio")
public class TwilioWhatsAppNotificationProvider implements WhatsAppNotificationProvider {
    // Implementation using Twilio WhatsApp Business API
}
```

#### Push Notifications (Firebase Cloud Messaging)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>
```

Implement `FirebaseCloudMessagingProvider`:

```java
@Service
@ConditionalOnProperty(name = "notification.push.provider", havingValue = "firebase")
public class FirebaseCloudMessagingProvider implements PushNotificationProvider {
    // Implementation using Firebase Cloud Messaging
}
```

## Notification Flow Example

### Task Assignment & Escalation Flow

```
1. API Call: POST /workflows/tasks/{id}/assign
   └─ TaskInstanceService.assignTask()
      └─ taskInstance.setAssignedTo("USER_123")
         └─ TaskEventConsumer [Async via Kafka]
            └─ handleTaskEvent(TASK_CREATED)
               └─ notifyTaskAssignment(task, "USER_123")
                  └─ NotificationService.notifyUser()
                     └─ Get user preferences for USER_123
                        ├─ If Email enabled → Send via EmailProvider
                        ├─ If SMS enabled → Send via SMSProvider
                        ├─ If WhatsApp enabled → Send via WhatsAppProvider
                        └─ If Push enabled → Send via PushProvider
                           └─ Create Notification record (status=PENDING)
                              └─ Mark as SENT after provider returns
                                 └─ Update NotificationRepository

2. After 5 hours - SLA Monitoring Kicks In:
   └─ SLAMonitoringService.monitorSLABreaches() [Every 5 min]
      └─ Detects: task.dueAt < NOW() && !slaBreached
         └─ Mark as escalated
            └─ sendSLABreachAlert(task, reason)
               ├─ Notify assignee: "Your task SLA breached"
               └─ Notify manager: "Critical escalation needed"
                  └─ Both sent via user's preferred channels
                     └─ Create Notification records
                        └─ Retry failed notifications in 5 minutes
```

## Notification Status Lifecycle

```
PENDING (Initial)
   ↓
   ├─→ SENT (Provider accepted)
   │    ├─→ DELIVERED (Confirmed delivery)
   │    └─→ BOUNCED (Invalid address)
   │
   └─→ FAILED (Provider error)
        ├─→ Retry after 5 minutes (up to 3 times)
        └─→ Give up after max retries
```

## Retry Strategy

Failed notifications are automatically retried every 5 minutes:

- **Attempt 1:** Immediate send
- **Attempt 2:** Retry after 5 minutes
- **Attempt 3:** Retry after another 5 minutes (total 10 minutes)
- **Final Retry:** After another 5 minutes (total 15 minutes)
- **Timeout:** Mark as permanently failed after 15 minutes total

## Database Schema

### Notifications Table

```sql
CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    recipient_user_id VARCHAR(100) NOT NULL,
    recipient_email VARCHAR(255),
    recipient_phone_number VARCHAR(20),
    recipient_whatsapp_number VARCHAR(20),
    recipient_push_token VARCHAR(500),
    channel VARCHAR(50) NOT NULL,  -- EMAIL, SMS, WHATSAPP, PUSH
    status VARCHAR(50) NOT NULL,   -- PENDING, SENT, DELIVERED, FAILED, BOUNCED
    notification_type VARCHAR(100) NOT NULL,  -- TASK_ASSIGNMENT, SLA_BREACH, etc.
    subject VARCHAR(255),
    message CLOB NOT NULL,
    workflow_instance_id VARCHAR(36),
    task_instance_id VARCHAR(36),
    order_id VARCHAR(36),
    patient_id VARCHAR(36),
    correlation_id VARCHAR(36),
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failure_reason VARCHAR(500),
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    INDEX idx_notifications_user_id (recipient_user_id),
    INDEX idx_notifications_status (status),
    INDEX idx_notifications_channel (channel),
    INDEX idx_notifications_created_at (created_at),
    FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id)
);

CREATE TABLE user_notification_preferences (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL UNIQUE,
    email_address VARCHAR(255),
    phone_number VARCHAR(20),
    whatsapp_number VARCHAR(20),
    mobile_push_token VARCHAR(500),
    notify_task_assignment BOOLEAN DEFAULT true,
    notify_task_escalation BOOLEAN DEFAULT true,
    notify_sla_breach BOOLEAN DEFAULT true,
    notify_order_created BOOLEAN DEFAULT false,
    notify_workflow_completion BOOLEAN DEFAULT true,
    notify_workflow_failure BOOLEAN DEFAULT true,
    quiet_hours_enabled BOOLEAN DEFAULT false,
    quiet_hours_start VARCHAR(5),  -- HH:mm format
    quiet_hours_end VARCHAR(5),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    INDEX idx_user_notif_pref_user_id (user_id),
    INDEX idx_user_notif_pref_active (is_active)
);

CREATE TABLE user_notification_channels (
    user_preference_id VARCHAR(36) NOT NULL,
    channel VARCHAR(50) NOT NULL,  -- EMAIL, SMS, WHATSAPP, PUSH
    PRIMARY KEY (user_preference_id, channel),
    FOREIGN KEY (user_preference_id) REFERENCES user_notification_preferences(id) ON DELETE CASCADE
);
```

## Monitoring & Observability

### Metrics

```
notification.channels.email.pending - Count of pending emails
notification.channels.sms.pending - Count of pending SMS messages
notification.channels.whatsapp.pending - Count of pending WhatsApp messages
notification.channels.push.pending - Count of pending push notifications

notification.channels.email.delivered - Count of delivered emails
notification.channels.sms.delivered - Count of delivered SMS messages
notification.channels.whatsapp.delivered - Count of delivered WhatsApp messages
notification.channels.push.delivered - Count of delivered push notifications

notification.channels.email.failed - Count of failed emails
notification.channels.sms.failed - Count of failed SMS messages
notification.channels.whatsapp.failed - Count of failed WhatsApp messages
notification.channels.push.failed - Count of failed push notifications

notification.processing_time_ms - Time to process and send notification
notification.retry_attempts - Number of retry attempts per notification
```

### Health Checks

```java
GET /health/notifications
{
  "status": "UP",
  "components": {
    "emailProvider": {
      "status": "UP",
      "details": {
        "pending": 5,
        "failed": 0,
        "provider": "mock"
      }
    },
    "smsProvider": {
      "status": "UP",
      "details": {
        "pending": 2,
        "failed": 0,
        "provider": "mock"
      }
    },
    "whatsappProvider": {
      "status": "UP",
      "details": {
        "pending": 1,
        "failed": 0,
        "provider": "mock"
      }
    },
    "pushProvider": {
      "status": "UP",
      "details": {
        "pending": 3,
        "failed": 0,
        "provider": "mock"
      }
    }
  }
}
```

## Compliance & Security

### HIPAA Compliance

- ✅ Secure transmission (HTTPS/TLS for all provider APIs)
- ✅ Audit trails (all notifications logged)
- ✅ Encryption in transit (enforced by providers)
- ✅ Retention policies (configurable per notification type)
- ✅ User consent management (preferences stored and enforced)

### GDPR Compliance

- ✅ Right to be forgotten (can delete user notifications)
- ✅ Data portability (notification history exportable)
- ✅ Consent management (user opt-in/opt-out by notification type)
- ✅ Purpose limitation (notifications only sent for workflow events)

## API Endpoints (Future)

```
# User Notification Preferences
GET    /api/notifications/preferences/{userId}
PUT    /api/notifications/preferences/{userId}

# Notification History
GET    /api/notifications/history/{userId}
GET    /api/notifications/{notificationId}

# Manual Notification Retry
POST   /api/notifications/{notificationId}/retry

# Statistics
GET    /api/notifications/statistics
GET    /api/notifications/status-summary
```

## Conclusion

The multi-channel notification system provides healthcare teams with instant, reliable alerts through their preferred communication channels, ensuring critical workflow events (task assignments, escalations, SLA breaches) are communicated immediately and never missed.
