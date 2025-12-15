package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.OrderSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderSetRepository extends JpaRepository<OrderSet, UUID> {
    Optional<OrderSet> findByOrderSetId(String orderSetId);
    Optional<OrderSet> findByName(String name);
    List<OrderSet> findByActiveTrue();
    List<OrderSet> findByCategory(String category);
    List<OrderSet> findByClinicalCondition(String condition);
    List<OrderSet> findByAccessLevel(String accessLevel);

    @Query("SELECT os FROM OrderSet os WHERE os.active = true AND os.version = " +
           "(SELECT MAX(os2.version) FROM OrderSet os2 WHERE os2.name = os.name)")
    List<OrderSet> findLatestVersions();

    @Query("SELECT os FROM OrderSet os WHERE os.clinicalCondition = :condition AND os.active = true")
    List<OrderSet> findByCondition(@Param("condition") String condition);
}
