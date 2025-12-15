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

    /**
     * Find workflows by patient, encounter, and template.
     * Used to check for duplicate workflows before creating a new one.
     * Only considers active/paused workflows (not completed/cancelled/failed).
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.patient.id = :patientId " +
           "AND w.template.id = :templateId " +
           "AND (w.encounterId = :encounterId OR (w.encounterId IS NULL AND :encounterId IS NULL)) " +
           "AND w.status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED')")
    List<WorkflowInstance> findActiveByPatientEncounterAndTemplate(
            @Param("patientId") UUID patientId,
            @Param("encounterId") String encounterId,
            @Param("templateId") UUID templateId);

    /**
     * Find workflows by encounter ID.
     */
    List<WorkflowInstance> findByEncounterId(String encounterId);

    /**
     * Find workflows by visit ID.
     */
    List<WorkflowInstance> findByVisitId(String visitId);

    /**
     * Find active workflows by encounter ID.
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.encounterId = :encounterId " +
           "AND w.status IN ('ACTIVE', 'PAUSED')")
    List<WorkflowInstance> findActiveByEncounterId(@Param("encounterId") String encounterId);
}
