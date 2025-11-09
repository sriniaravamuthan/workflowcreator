package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityIdOrderByActionTimestampDesc(String entityId);
    List<AuditLog> findByWorkflowInstanceIdOrderByActionTimestampDesc(UUID workflowInstanceId);
    List<AuditLog> findByPatientIdOrderByActionTimestampDesc(UUID patientId);
    List<AuditLog> findByActor(String actor);

    @Query("SELECT a FROM AuditLog a WHERE a.actionTimestamp BETWEEN :startDate AND :endDate ORDER BY a.actionTimestamp DESC")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM AuditLog a WHERE a.isLegalHold = true")
    List<AuditLog> findLegalHoldLogs();
}
