package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserNotificationPreference entities
 */
@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, String> {

    /**
     * Find notification preference by user ID
     */
    Optional<UserNotificationPreference> findByUserId(String userId);

    /**
     * Check if user has active notification preferences
     */
    boolean existsByUserIdAndIsActiveTrue(String userId);
}
