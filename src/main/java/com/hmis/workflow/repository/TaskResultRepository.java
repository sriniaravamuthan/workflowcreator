package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.TaskResult;
import com.hmis.workflow.domain.entity.TaskResult.ResultType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for TaskResult entity.
 * Note: This repository intentionally does NOT have update or delete methods
 * to enforce the append-only nature of medical results.
 */
@Repository
public interface TaskResultRepository extends JpaRepository<TaskResult, UUID> {

    /**
     * Find all results for a task instance, ordered by time (oldest first).
     */
    List<TaskResult> findByTaskInstanceIdOrderByRecordedAtAsc(UUID taskInstanceId);

    /**
     * Find all results for a task instance, ordered by time (newest first).
     */
    List<TaskResult> findByTaskInstanceIdOrderByRecordedAtDesc(UUID taskInstanceId);

    /**
     * Find results by task instance and type.
     */
    List<TaskResult> findByTaskInstanceIdAndResultTypeOrderByRecordedAtAsc(UUID taskInstanceId, ResultType resultType);

    /**
     * Find results by result name (e.g., "Blood Pressure").
     */
    List<TaskResult> findByTaskInstanceIdAndResultNameOrderByRecordedAtDesc(UUID taskInstanceId, String resultName);

    /**
     * Find results by standard code (e.g., LOINC code).
     */
    List<TaskResult> findByResultCodeOrderByRecordedAtDesc(String resultCode);

    /**
     * Find unverified results for a task.
     */
    List<TaskResult> findByTaskInstanceIdAndIsVerifiedFalseOrderByRecordedAtAsc(UUID taskInstanceId);

    /**
     * Find critical results for a task.
     */
    List<TaskResult> findByTaskInstanceIdAndIsCriticalTrueOrderByRecordedAtDesc(UUID taskInstanceId);

    /**
     * Find all results for a workflow instance (across all tasks).
     */
    @Query("SELECT r FROM TaskResult r WHERE r.taskInstance.workflowInstance.id = :workflowInstanceId " +
           "ORDER BY r.recordedAt ASC")
    List<TaskResult> findByWorkflowInstanceId(@Param("workflowInstanceId") UUID workflowInstanceId);

    /**
     * Find results within a time range.
     */
    @Query("SELECT r FROM TaskResult r WHERE r.taskInstance.id = :taskInstanceId " +
           "AND r.recordedAt BETWEEN :startTime AND :endTime ORDER BY r.recordedAt ASC")
    List<TaskResult> findByTaskInstanceAndTimeRange(
            @Param("taskInstanceId") UUID taskInstanceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find all critical results for a workflow (across all tasks).
     */
    @Query("SELECT r FROM TaskResult r WHERE r.taskInstance.workflowInstance.id = :workflowInstanceId " +
           "AND r.isCritical = true ORDER BY r.recordedAt DESC")
    List<TaskResult> findCriticalResultsByWorkflowInstanceId(@Param("workflowInstanceId") UUID workflowInstanceId);

    /**
     * Find corrections for a specific result.
     */
    List<TaskResult> findByCorrectsResultIdOrderByRecordedAtAsc(String resultId);

    /**
     * Count results for a task instance.
     */
    long countByTaskInstanceId(UUID taskInstanceId);

    /**
     * Find the latest result by name for a task.
     */
    @Query("SELECT r FROM TaskResult r WHERE r.taskInstance.id = :taskInstanceId " +
           "AND r.resultName = :resultName ORDER BY r.recordedAt DESC LIMIT 1")
    TaskResult findLatestByTaskInstanceIdAndResultName(
            @Param("taskInstanceId") UUID taskInstanceId,
            @Param("resultName") String resultName);
}
