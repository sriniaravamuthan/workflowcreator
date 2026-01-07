package com.hmis.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing configuration for automatic population of audit fields.
 * Enables @CreatedBy and @LastModifiedBy annotations on entities.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Provides the current auditor (user) for JPA auditing.
     * Uses AuditContext thread-local to capture user from request context.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(AuditContext.getCurrentUser())
                .or(() -> Optional.of("SYSTEM"));
    }
}
