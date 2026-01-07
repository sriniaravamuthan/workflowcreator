package com.hmis.workflow.config;

/**
 * Thread-local context for audit information.
 * Used to capture the current user performing an action.
 *
 * Since the application doesn't use Spring Security, user information
 * is captured from request parameters and stored here for audit purposes.
 */
public class AuditContext {

    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();
    private static final ThreadLocal<String> correlationId = new ThreadLocal<>();

    private AuditContext() {
        // Utility class
    }

    /**
     * Set the current user for audit purposes.
     */
    public static void setCurrentUser(String user) {
        currentUser.set(user);
    }

    /**
     * Get the current user for audit purposes.
     */
    public static String getCurrentUser() {
        return currentUser.get();
    }

    /**
     * Set the correlation ID for tracing across operations.
     */
    public static void setCorrelationId(String id) {
        correlationId.set(id);
    }

    /**
     * Get the correlation ID for tracing.
     */
    public static String getCorrelationId() {
        return correlationId.get();
    }

    /**
     * Clear all context after request completes.
     */
    public static void clear() {
        currentUser.remove();
        correlationId.remove();
    }
}
