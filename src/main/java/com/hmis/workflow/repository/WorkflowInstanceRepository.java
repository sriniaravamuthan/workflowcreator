package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.WorkflowInstance;
import com.hmis.workflow.domain.enums.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {
    Optional<WorkflowInstance> findByWorkflowInstanceId(String workflowInstanceId);
    List<WorkflowInstance> findByPatientId(UUID patientId);
    List<WorkflowInstance> findByStatus(WorkflowStatus status);
    List<WorkflowInstance> findByPatientIdAndStatus(UUID patientId, WorkflowStatus status);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.status IN ('ACTIVE', 'PAUSED') AND w.patient.id = :patientId")
    List<WorkflowInstance> findActiveByPatientId(@Param("patientId") UUID patientId);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.isEscalated = true")
    List<WorkflowInstance> findEscalatedWorkflows();

    @Query("SELECT w FROM WorkflowInstance w WHERE w.startedAt BETWEEN :startDate AND :endDate")
    List<WorkflowInstance> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
}
