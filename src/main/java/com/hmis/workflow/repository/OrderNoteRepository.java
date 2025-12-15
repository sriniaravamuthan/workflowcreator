package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.OrderNote;
import com.hmis.workflow.domain.entity.OrderNote.NoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for OrderNote entity.
 * Note: This repository intentionally does NOT have update or delete methods
 * to enforce the append-only nature of medical notes.
 */
@Repository
public interface OrderNoteRepository extends JpaRepository<OrderNote, UUID> {

    /**
     * Find all notes for an order, ordered by time (oldest first).
     */
    List<OrderNote> findByOrderIdOrderByNotedAtAsc(UUID orderId);

    /**
     * Find all notes for an order, ordered by time (newest first).
     */
    List<OrderNote> findByOrderIdOrderByNotedAtDesc(UUID orderId);

    /**
     * Find notes by order and type.
     */
    List<OrderNote> findByOrderIdAndNoteTypeOrderByNotedAtAsc(UUID orderId, NoteType noteType);

    /**
     * Find notes by author.
     */
    List<OrderNote> findByAuthorUserOrderByNotedAtDesc(String authorUser);

    /**
     * Find flagged notes for an order.
     */
    List<OrderNote> findByOrderIdAndIsFlaggedTrueOrderByNotedAtDesc(UUID orderId);

    /**
     * Find notes within a time range.
     */
    @Query("SELECT n FROM OrderNote n WHERE n.order.id = :orderId " +
           "AND n.notedAt BETWEEN :startTime AND :endTime ORDER BY n.notedAt ASC")
    List<OrderNote> findByOrderAndTimeRange(
            @Param("orderId") UUID orderId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find all notes for a workflow instance (across all orders).
     */
    @Query("SELECT n FROM OrderNote n WHERE n.order.workflowInstance.id = :workflowInstanceId " +
           "ORDER BY n.notedAt ASC")
    List<OrderNote> findByWorkflowInstanceId(@Param("workflowInstanceId") UUID workflowInstanceId);

    /**
     * Find critical/high-priority notes for an order.
     */
    List<OrderNote> findByOrderIdAndPriorityGreaterThanOrderByNotedAtDesc(UUID orderId, Integer priority);

    /**
     * Count notes for an order.
     */
    long countByOrderId(UUID orderId);

    /**
     * Find addenda to a specific note.
     */
    List<OrderNote> findByAddendumToNoteIdOrderByNotedAtAsc(String noteId);

    /**
     * Find cancellation notes for an order.
     */
    List<OrderNote> findByOrderIdAndNoteType(UUID orderId, NoteType noteType);
}
