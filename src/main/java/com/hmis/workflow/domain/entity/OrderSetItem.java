package com.hmis.workflow.domain.entity;

import com.hmis.workflow.domain.enums.OrderType;
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

/**
 * OrderSetItem entity representing an individual item within an OrderSet
 * Can be an Order, Task, or Instruction
 */
@Entity
@Table(name = "order_set_items")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "orderSet")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSetItem extends BaseEntity {

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false, length = 50)
    private String itemType; // ORDER, TASK, INSTRUCTION

    @Enumerated(EnumType.STRING)
    private OrderType orderType; // If itemType is ORDER

    @Column(nullable = false, length = 255)
    private String itemName;

    @Column(length = 500)
    private String itemDescription;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean mandatory = false; // If false, clinician can skip

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isParallel = false; // Execute in parallel with others

    @Column(length = 100)
    private String dependsOnItemId; // Item this depends on completing first

    @Column(length = 100)
    private String conditionalLogicId; // ID of associated conditional logic

    @Column(columnDefinition = "TEXT")
    private String defaultParameters; // JSON-serialized default parameters

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    @ManyToOne
    @JoinColumn(name = "order_set_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orderset_item"))
    @JsonIgnore
    private OrderSet orderSet;
}
