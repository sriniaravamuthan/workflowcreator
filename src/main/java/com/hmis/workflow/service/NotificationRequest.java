package com.hmis.workflow.service;

import java.util.UUID;

/**
 * NotificationRequest DTO
 * Data transfer object for sending notifications to users
 */
public class NotificationRequest {

    private String recipientUserId;
    private String notificationType;
    private String subject;
    private String message;
    private String workflowInstanceId;
    private String taskInstanceId;
    private String orderId;
    private String patientId;
    private String correlationId;

    /**
     * Constructor for notification request
     */
    public NotificationRequest(String recipientUserId, String notificationType, String subject, String message) {
        this.recipientUserId = recipientUserId;
        this.notificationType = notificationType;
        this.subject = subject;
        this.message = message;
        this.correlationId = UUID.randomUUID().toString();
    }

    /**
     * Builder-style constructor for full notification
     */
    public NotificationRequest(String recipientUserId, String notificationType, String subject, String message,
                               String workflowInstanceId, String taskInstanceId, String orderId, String patientId) {
        this.recipientUserId = recipientUserId;
        this.notificationType = notificationType;
        this.subject = subject;
        this.message = message;
        this.workflowInstanceId = workflowInstanceId;
        this.taskInstanceId = taskInstanceId;
        this.orderId = orderId;
        this.patientId = patientId;
        this.correlationId = UUID.randomUUID().toString();
    }

    // Getters
    public String getRecipientUserId() { return recipientUserId; }
    public String getNotificationType() { return notificationType; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public String getWorkflowInstanceId() { return workflowInstanceId; }
    public String getTaskInstanceId() { return taskInstanceId; }
    public String getOrderId() { return orderId; }
    public String getPatientId() { return patientId; }
    public String getCorrelationId() { return correlationId; }

    // Setters
    public void setWorkflowInstanceId(String workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }
    public void setTaskInstanceId(String taskInstanceId) { this.taskInstanceId = taskInstanceId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
}
