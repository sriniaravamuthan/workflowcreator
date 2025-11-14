package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.Notification;
import com.hmis.workflow.domain.enums.NotificationChannel;
import com.hmis.workflow.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Notification entities
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    /**
     * Find all notifications for a user
     */
    List<Notification> findByRecipientUserId(String recipientUserId);

    /**
     * Find all notifications for a user with specific status
     */
    List<Notification> findByRecipientUserIdAndStatus(String recipientUserId, NotificationStatus status);

    /**
     * Find all pending notifications for a specific channel
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.channel = :channel")
    List<Notification> findPendingNotificationsByChannel(@Param("channel") NotificationChannel channel);

    /**
     * Find all failed notifications that can be retried
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < n.maxRetries ORDER BY n.createdAt ASC")
    List<Notification> findRetryableNotifications();

    /**
     * Find notifications for a workflow instance
     */
    List<Notification> findByWorkflowInstanceId(String workflowInstanceId);

    /**
     * Find notifications for a task instance
     */
    List<Notification> findByTaskInstanceId(String taskInstanceId);

    /**
     * Find notifications for an order
     */
    List<Notification> findByOrderId(String orderId);

    /**
     * Find notification by correlation ID for tracing
     */
    Optional<Notification> findByCorrelationId(String correlationId);

    /**
     * Find notifications created within a time range
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :startTime AND n.createdAt <= :endTime ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsByDateRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find notifications by type for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientUserId = :userId AND n.notificationType = :type ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsByUserAndType(
        @Param("userId") String userId,
        @Param("type") String type
    );

    /**
     * Find undelivered notifications that exceed timeout (retryable)
     */
    @Query("SELECT n FROM Notification n WHERE (n.status = 'PENDING' OR n.status = 'FAILED') AND n.retryCount < n.maxRetries ORDER BY n.createdAt ASC LIMIT 100")
    List<Notification> findUndeliveredNotifications();

    /**
     * Count pending notifications for a user
     */
    Long countByRecipientUserIdAndStatus(String recipientUserId, NotificationStatus status);

    /**
     * Count notifications by channel and status
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.channel = :channel AND n.status = :status")
    Long countByChannelAndStatus(
        @Param("channel") NotificationChannel channel,
        @Param("status") NotificationStatus status
    );
}
