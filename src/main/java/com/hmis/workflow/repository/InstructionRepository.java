package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.Instruction;
import com.hmis.workflow.domain.enums.InstructionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstructionRepository extends JpaRepository<Instruction, UUID> {
    Optional<Instruction> findByInstructionId(String instructionId);
    List<Instruction> findByWorkflowInstanceId(UUID workflowInstanceId);
    List<Instruction> findByInstructionType(InstructionType type);

    @Query("SELECT i FROM Instruction i WHERE i.workflowInstance.id = :workflowInstanceId AND i.blocking = true AND i.acknowledged = false")
    List<Instruction> findBlockingUnacknowledgedInstructions(@Param("workflowInstanceId") UUID workflowInstanceId);

    @Query("SELECT i FROM Instruction i WHERE i.active = true AND i.acknowledged = false")
    List<Instruction> findPendingInstructions();
}
