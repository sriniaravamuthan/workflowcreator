package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.TaskInstance;
import com.hmis.workflow.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskInstanceRepository extends JpaRepository<TaskInstance, UUID> {
    Optional<TaskInstance> findByTaskInstanceId(String taskInstanceId);
    List<TaskInstance> findByWorkflowInstanceId(UUID workflowInstanceId);
    List<TaskInstance> findByStatus(TaskStatus status);
    List<TaskInstance> findByAssignedTo(String assignedTo);

    @Query("SELECT t FROM TaskInstance t WHERE t.assignedTo = :assignedTo AND t.status IN ('PENDING', 'IN_PROGRESS')")
    List<TaskInstance> findPendingAndInProgressByAssignee(@Param("assignedTo") String assignedTo);

    @Query("SELECT t FROM TaskInstance t WHERE t.slaBreached = false AND t.dueAt < :now AND t.status != 'COMPLETED'")
    List<TaskInstance> findSLABreachedTasks(@Param("now") LocalDateTime now);

    @Query("SELECT t FROM TaskInstance t WHERE t.isEscalated = true AND t.status != 'COMPLETED'")
    List<TaskInstance> findEscalatedTasks();

    @Query("SELECT t FROM TaskInstance t WHERE t.status = 'FAILED' AND t.isRetryable = true")
    List<TaskInstance> findRetryableTasks();
}
