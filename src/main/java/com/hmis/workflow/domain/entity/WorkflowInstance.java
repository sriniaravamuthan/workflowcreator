package com.hmis.workflow.domain.entity;

import com.hmis.workflow.domain.enums.WorkflowStatus;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * WorkflowInstance entity representing an instance of a workflow for a patient
 */
@Entity
@Table(name = "workflow_instances")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"patient", "template", "taskInstances"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstance extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String workflowInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    @Column(length = 500)
    private String notes;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false, foreignKey = @ForeignKey(name = "fk_workflow_patient"))
    @JsonIgnore
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false, foreignKey = @ForeignKey(name = "fk_workflow_template"))
    @JsonIgnore
    private WorkflowTemplate template;

    @OneToMany(mappedBy = "workflowInstance", orphanRemoval = true)
    @Builder.Default
    private Set<TaskInstance> taskInstances = new HashSet<>();

    public Integer getProgressPercentage() {
        if (taskInstances.isEmpty()) {
            return 0;
        }
        long completedTasks = taskInstances.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus().toString()))
                .count();
        return (int) ((completedTasks * 100) / taskInstances.size());
    }
}
