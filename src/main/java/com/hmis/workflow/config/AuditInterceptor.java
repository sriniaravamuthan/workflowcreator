package com.hmis.workflow.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * HTTP interceptor to capture audit context from request headers.
 * Captures X-User-Id header for audit trail and generates correlation ID.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private static final String USER_HEADER = "X-User-Id";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                            @NonNull HttpServletResponse response,
                            @NonNull Object handler) {
        // Capture user from header if provided
        String userId = request.getHeader(USER_HEADER);
        if (userId != null && !userId.isBlank()) {
            AuditContext.setCurrentUser(userId);
        }

        // Capture or generate correlation ID
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        AuditContext.setCorrelationId(correlationId);

        // Add correlation ID to response for tracing
        response.setHeader(CORRELATION_HEADER, correlationId);

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                               @NonNull HttpServletResponse response,
                               @NonNull Object handler,
                               Exception ex) {
        // Clear context after request completes
        AuditContext.clear();
    }
}
