package com.hmis.workflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for external order API integrations
 * Allows configurable external APIs for orders (Lab, Imaging, Pharmacy, etc.)
 */
@Configuration
public class ExternalOrderApiConfig {

    /**
     * RestTemplate bean for calling external order APIs
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
