package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.TaskNote;
import com.hmis.workflow.domain.entity.TaskNote.NoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for TaskNote entity.
 * Note: This repository intentionally does NOT have update or delete methods
 * to enforce the append-only nature of medical notes.
 */
@Repository
public interface TaskNoteRepository extends JpaRepository<TaskNote, UUID> {

    /**
     * Find all notes for a task instance, ordered by time (oldest first).
     */
    List<TaskNote> findByTaskInstanceIdOrderByNotedAtAsc(UUID taskInstanceId);

    /**
     * Find all notes for a task instance, ordered by time (newest first).
     */
    List<TaskNote> findByTaskInstanceIdOrderByNotedAtDesc(UUID taskInstanceId);

    /**
     * Find notes by task instance and type.
     */
    List<TaskNote> findByTaskInstanceIdAndNoteTypeOrderByNotedAtAsc(UUID taskInstanceId, NoteType noteType);

    /**
     * Find notes by author.
     */
    List<TaskNote> findByAuthorUserOrderByNotedAtDesc(String authorUser);

    /**
     * Find flagged notes for a task instance.
     */
    List<TaskNote> findByTaskInstanceIdAndIsFlaggedTrueOrderByNotedAtDesc(UUID taskInstanceId);

    /**
     * Find notes within a time range.
     */
    @Query("SELECT n FROM TaskNote n WHERE n.taskInstance.id = :taskInstanceId " +
           "AND n.notedAt BETWEEN :startTime AND :endTime ORDER BY n.notedAt ASC")
    List<TaskNote> findByTaskInstanceAndTimeRange(
            @Param("taskInstanceId") UUID taskInstanceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find all notes for a workflow instance (across all tasks).
     */
    @Query("SELECT n FROM TaskNote n WHERE n.taskInstance.workflowInstance.id = :workflowInstanceId " +
           "ORDER BY n.notedAt ASC")
    List<TaskNote> findByWorkflowInstanceId(@Param("workflowInstanceId") UUID workflowInstanceId);

    /**
     * Find critical/high-priority notes for a task.
     */
    List<TaskNote> findByTaskInstanceIdAndPriorityGreaterThanOrderByNotedAtDesc(UUID taskInstanceId, Integer priority);

    /**
     * Count notes for a task instance.
     */
    long countByTaskInstanceId(UUID taskInstanceId);

    /**
     * Find addenda to a specific note.
     */
    List<TaskNote> findByAddendumToNoteIdOrderByNotedAtAsc(String noteId);
}
