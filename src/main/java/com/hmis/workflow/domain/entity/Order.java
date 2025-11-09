package com.hmis.workflow.domain.entity;

import com.hmis.workflow.domain.enums.OrderStatus;
import com.hmis.workflow.domain.enums.OrderType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Order entity representing clinical orders (lab tests, imaging, medications, procedures)
 * Follows 8-state lifecycle: Proposed → Authorized → Activated → In Progress → Resulted → Verified → Closed/Cancelled
 */
@Entity
@Table(name = "orders")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"workflowInstance", "orderedByUser", "compensationActions"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Column(nullable = false, length = 255)
    private String orderDescription;

    @Column(length = 100)
    private String orderCode;

    @Column(length = 100)
    private String departmentTarget;

    @Column(length = 100)
    private String orderedByUser;

    @Column(length = 100)
    private String authorizedByUser;

    @Column
    private LocalDateTime authorizedAt;

    @Column
    private LocalDateTime activatedAt;

    @Column
    private LocalDateTime resultedAt;

    @Column
    private LocalDateTime verifiedAt;

    @Column
    private LocalDateTime closedAt;

    @Column
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualCost;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer priority = 0; // 0=Normal, 1=High, 2=Critical

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne
    @JoinColumn(name = "workflow_instance_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_workflow"))
    @JsonIgnore
    private WorkflowInstance workflowInstance;

    @OneToMany(mappedBy = "order", orphanRemoval = true)
    @Builder.Default
    private Set<CompensationAction> compensationActions = new HashSet<>();

    /**
     * Check if order can transition to next state
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this.status) {
            case PROPOSED -> newStatus == OrderStatus.AUTHORIZED || newStatus == OrderStatus.CANCELLED;
            case AUTHORIZED -> newStatus == OrderStatus.ACTIVATED || newStatus == OrderStatus.CANCELLED;
            case ACTIVATED -> newStatus == OrderStatus.IN_PROGRESS || newStatus == OrderStatus.CANCELLED;
            case IN_PROGRESS -> newStatus == OrderStatus.RESULTED || newStatus == OrderStatus.DISPENSED
                    || newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED;
            case RESULTED, DISPENSED, COMPLETED -> newStatus == OrderStatus.VERIFIED || newStatus == OrderStatus.CANCELLED;
            case VERIFIED -> newStatus == OrderStatus.CLOSED;
            case CLOSED, CANCELLED -> false;
        };
    }
}
