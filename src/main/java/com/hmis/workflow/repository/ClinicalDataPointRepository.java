package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.ClinicalDataPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicalDataPointRepository extends JpaRepository<ClinicalDataPoint, UUID> {

    @Query("SELECT c FROM ClinicalDataPoint c WHERE c.patient.id = :patientId AND c.dataPointCode = :dataPointCode ORDER BY c.measurementTime DESC LIMIT 1")
    Optional<ClinicalDataPoint> findLatestByPatientAndCode(
            @Param("patientId") UUID patientId,
            @Param("dataPointCode") String dataPointCode);

    @Query("SELECT c FROM ClinicalDataPoint c WHERE c.patient.id = :patientId AND c.measurementTime >= :fromTime ORDER BY c.measurementTime DESC")
    List<ClinicalDataPoint> findByPatientAndTimeRange(
            @Param("patientId") UUID patientId,
            @Param("fromTime") LocalDateTime fromTime);

    @Query("SELECT c FROM ClinicalDataPoint c WHERE c.patient.id = :patientId AND c.dataPointCode = :dataPointCode ORDER BY c.measurementTime DESC")
    List<ClinicalDataPoint> findByPatientAndCode(
            @Param("patientId") UUID patientId,
            @Param("dataPointCode") String dataPointCode);

    @Query("SELECT c FROM ClinicalDataPoint c WHERE c.patient.id = :patientId ORDER BY c.measurementTime DESC")
    List<ClinicalDataPoint> findRecentByPatient(
            @Param("patientId") UUID patientId);

    @Query("SELECT c FROM ClinicalDataPoint c WHERE c.patient.id = :patientId AND c.isNormal = false ORDER BY c.measurementTime DESC")
    List<ClinicalDataPoint> findAbnormalByPatient(
            @Param("patientId") UUID patientId);

    @Query("SELECT DISTINCT c.dataPointCode FROM ClinicalDataPoint c WHERE c.patient.id = :patientId")
    List<String> findDistinctDataPointCodesByPatient(
            @Param("patientId") UUID patientId);
}
