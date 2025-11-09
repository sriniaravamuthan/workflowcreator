package com.hmis.workflow.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * OrderSet entity representing a collection of orders, tasks, and instructions
 * Supports parallel/sequential grouping, conditional activation, and variants
 * Designed for bundling common clinical interventions for specific conditions
 */
@Entity
@Table(name = "order_sets")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"items", "conditions"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSet extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String orderSetId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String clinicalCondition; // Condition this orderset is designed for (e.g., "Diabetes Management")

    @Column(length = 100)
    private String category;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer version = 1;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isParallel = false; // If true, items execute in parallel; if false, sequential

    @Column(columnDefinition = "TEXT")
    private String instructions; // General instructions for applying this orderset

    @Column(length = 100)
    private String createdByUser;

    @Column(length = 100)
    private String approvedByUser;

    @Column(length = 50)
    private String accessLevel; // PRIVATE, TEAM, DEPARTMENT, HOSPITAL_WIDE

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @OneToMany(mappedBy = "orderSet", orphanRemoval = true)
    @Builder.Default
    private Set<OrderSetItem> items = new HashSet<>();

    @OneToMany(mappedBy = "orderSet", orphanRemoval = true)
    @Builder.Default
    private Set<OrderSetCondition> conditions = new HashSet<>();

    public Integer getItemCount() {
        return items != null ? items.size() : 0;
    }

    public boolean canBeActivated() {
        return active && !conditions.isEmpty();
    }
}
