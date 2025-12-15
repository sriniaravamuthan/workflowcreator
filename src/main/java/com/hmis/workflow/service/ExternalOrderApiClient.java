package com.hmis.workflow.service;

import com.hmis.workflow.domain.entity.TemplateOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to interact with external order APIs
 * Handles HTTP calls to external order systems (Lab, Imaging, Pharmacy, etc.)
 */
@Service
@Slf4j
public class ExternalOrderApiClient {

    @Value("${external.orders.api.enabled:false}")
    private boolean orderApiEnabled;

    @Value("${external.orders.api.timeout:5000}")
    private long apiTimeout;

    @Value("${external.orders.api.auth-token:}")
    private String apiAuthToken;

    private final RestTemplate restTemplate;

    public ExternalOrderApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Calls external order API to fetch order details
     * @param templateOrder The template order with API endpoint configured
     * @return Response from external API or error details
     */
    public Map<String, Object> callOrderApi(TemplateOrder templateOrder) {
        Map<String, Object> response = new HashMap<>();

        if (!orderApiEnabled) {
            log.debug("External order API is disabled");
            response.put("success", false);
            response.put("message", "External order API is disabled");
            return response;
        }

        try {
            if (templateOrder.getExternalApiEndpoint() == null ||
                templateOrder.getExternalApiEndpoint().isEmpty()) {
                log.warn("No external API endpoint configured for order: {}", templateOrder.getOrderCode());
                response.put("success", false);
                response.put("message", "No API endpoint configured");
                return response;
            }

            log.info("Calling external order API for order: {} - Endpoint: {}",
                    templateOrder.getOrderCode(), templateOrder.getExternalApiEndpoint());

            HttpMethod method = HttpMethod.valueOf(
                    templateOrder.getApiMethod() != null ?
                    templateOrder.getApiMethod() : "GET"
            );

            HttpHeaders headers = buildHeaders();
            HttpEntity<?> entity = null;

            // For POST/PUT, include request payload
            if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method)) {
                entity = new HttpEntity<>(templateOrder.getApiRequestPayload(), headers);
            } else {
                entity = new HttpEntity<>(headers);
            }

            ResponseEntity<String> apiResponse = restTemplate.exchange(
                    templateOrder.getExternalApiEndpoint(),
                    method,
                    entity,
                    String.class
            );

            if (apiResponse.getStatusCode().is2xxSuccessful()) {
                log.info("External order API call successful for order: {}", templateOrder.getOrderCode());
                response.put("success", true);
                response.put("statusCode", apiResponse.getStatusCodeValue());
                response.put("body", apiResponse.getBody());
                response.put("message", "Order API call successful");
            } else {
                log.warn("External order API returned error for order: {} - Status: {}",
                        templateOrder.getOrderCode(), apiResponse.getStatusCode());
                response.put("success", false);
                response.put("statusCode", apiResponse.getStatusCodeValue());
                response.put("body", apiResponse.getBody());
                response.put("message", "API returned " + apiResponse.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error calling external order API for order: {}", templateOrder.getOrderCode(), e);
            response.put("success", false);
            response.put("message", "API call failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }

        return response;
    }

    /**
     * Validates that external order API is accessible
     * @param endpoint API endpoint to validate
     * @return true if endpoint is accessible
     */
    public boolean validateApiEndpoint(String endpoint) {
        if (!orderApiEnabled || endpoint == null || endpoint.isEmpty()) {
            return false;
        }

        try {
            log.info("Validating external order API endpoint: {}", endpoint);

            HttpHeaders headers = buildHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.OPTIONS,
                    entity,
                    String.class
            );

            boolean isValid = response.getStatusCode().is2xxSuccessful();
            log.info("API endpoint validation result: {} for endpoint: {}", isValid, endpoint);

            return isValid;

        } catch (Exception e) {
            log.warn("API endpoint validation failed for endpoint: {}", endpoint, e);
            return false;
        }
    }

    /**
     * Builds HTTP headers for external API calls
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");

        if (apiAuthToken != null && !apiAuthToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + apiAuthToken);
        }

        return headers;
    }

    /**
     * Gets external order API status
     */
    public Map<String, Object> getApiStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", orderApiEnabled);
        status.put("timeout", apiTimeout);
        status.put("authTokenConfigured", apiAuthToken != null && !apiAuthToken.isEmpty());
        return status;
    }
}
