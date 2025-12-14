package com.hmis.workflow.domain.entity;

import com.hmis.workflow.domain.enums.CompensationActionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CompensationAction entity representing recovery actions triggered when orders/tasks fail
 * Supports order cancellation, charge reversal, notifications, and custom actions
 */
@Entity
@Table(name = "compensation_actions")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "order")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompensationAction extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompensationActionType actionType;

    @Column(nullable = false, length = 255)
    private String actionDescription;

    @Column(length = 100)
    private String triggeringEvent;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean executed = false;

    @Column
    private LocalDateTime executedAt;

    @Column(length = 500)
    private String executionResult;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer retryCount = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 3")
    private Integer maxRetries = 3;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_compensation_order"))
    @JsonIgnore
    private Order order;

    public Boolean isRetryable() {
        return !executed && retryCount < maxRetries;
    }
}
