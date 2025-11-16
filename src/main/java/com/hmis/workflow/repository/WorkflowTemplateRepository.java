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

    @Query("SELECT w FROM WorkflowTemplate w WHERE w.active = true AND w.isDeleted = false")
    List<WorkflowTemplate> findByActiveTrue();

    @Query("SELECT w FROM WorkflowTemplate w WHERE w.category = :category AND w.isDeleted = false")
    List<WorkflowTemplate> findByCategory(@Param("category") String category);

    @Query("SELECT w FROM WorkflowTemplate w WHERE w.reviewStatus = :reviewStatus AND w.isDeleted = false")
    List<WorkflowTemplate> findByReviewStatus(@Param("reviewStatus") String reviewStatus);

    @Query("SELECT w FROM WorkflowTemplate w WHERE w.active = true AND w.reviewStatus = 'PUBLISHED' AND w.isDeleted = false")
    List<WorkflowTemplate> findAllPublished();

    @Query("SELECT w FROM WorkflowTemplate w WHERE w.isDeleted = false")
    List<WorkflowTemplate> findAllNonDeleted();

    @Query("SELECT w FROM WorkflowTemplate w WHERE w.isDeleted = true")
    List<WorkflowTemplate> findAllDeleted();

    @Query("SELECT w FROM WorkflowTemplate w WHERE w.parentTemplateId = :parentId AND w.isDeleted = false")
    List<WorkflowTemplate> findVersionsByParentTemplate(@Param("parentId") UUID parentId);

    @Query("SELECT w FROM WorkflowTemplate w WHERE w.clonedFromTemplateId = :clonedFromId AND w.isDeleted = false")
    List<WorkflowTemplate> findClonesOfTemplate(@Param("clonedFromId") UUID clonedFromId);
}
