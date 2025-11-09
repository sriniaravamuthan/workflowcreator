package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {
    Optional<WorkflowTemplate> findByName(String name);
    List<WorkflowTemplate> findByActiveTrue();
    List<WorkflowTemplate> findByCategory(String category);
    List<WorkflowTemplate> findByReviewStatus(String reviewStatus);

    @Query("SELECT w FROM WorkflowTemplate w WHERE w.active = true AND w.reviewStatus = 'PUBLISHED'")
    List<WorkflowTemplate> findAllPublished();
}
