package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.Gate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GateRepository extends JpaRepository<Gate, UUID> {
    Optional<Gate> findByGateId(String gateId);
    List<Gate> findByTemplateId(UUID templateId);
    List<Gate> findByGateType(String gateType);

    @Query("SELECT g FROM Gate g WHERE g.template.id = :templateId AND g.required = true")
    List<Gate> findRequiredGatesByTemplate(@Param("templateId") UUID templateId);
}
