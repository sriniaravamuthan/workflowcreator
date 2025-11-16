package com.hmis.workflow.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * TemplateOrder represents an order that is part of a workflow template
 * Stores reference to an external order API and order configuration
 */
@Entity
@Table(
    name = "template_orders",
    indexes = {
        @Index(name = "idx_template_orders_template_id", columnList = "template_id"),
        @Index(name = "idx_template_orders_order_code", columnList = "order_code")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateOrder {

    @Id
    private String id;

    @Column(name = "template_id", nullable = false)
    private String templateId;

    /**
     * External order code/type from the order API system
     * e.g., "LAB_001", "IMG_002", "MED_003"
     */
    @Column(name = "order_code", nullable = false, length = 100)
    private String orderCode;

    /**
     * Name of the order (e.g., "Complete Blood Count", "Chest X-Ray")
     */
    @Column(name = "order_name", nullable = false, length = 255)
    private String orderName;

    /**
     * Description of what this order does
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * External API endpoint to fetch order details
     * e.g., "https://lab-system.hospital.com/api/orders/LAB_001"
     */
    @Column(name = "external_api_endpoint", length = 500)
    private String externalApiEndpoint;

    /**
     * HTTP method to use: GET, POST, PUT, DELETE
     */
    @Column(name = "api_method", length = 10)
    @Builder.Default
    private String apiMethod = "GET";

    /**
     * Request payload template for POST/PUT (JSON format)
     */
    @Column(name = "api_request_payload", columnDefinition = "CLOB")
    private String apiRequestPayload;

    /**
     * Whether this order is required in the workflow
     */
    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;

    /**
     * Whether this order should be triggered automatically
     */
    @Column(name = "is_automatic")
    @Builder.Default
    private Boolean isAutomatic = false;

    /**
     * Order of execution if multiple orders exist
     */
    @Column(name = "order_sequence")
    private Integer orderSequence;

    /**
     * Metadata for the order (JSON)
     */
    @Column(name = "metadata", columnDefinition = "CLOB")
    private String metadata;

    /**
     * Whether this template order is active
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = java.time.LocalDateTime.now();
    }
}
