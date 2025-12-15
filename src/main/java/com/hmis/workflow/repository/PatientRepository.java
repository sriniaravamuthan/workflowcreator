package com.hmis.workflow.repository;

import com.hmis.workflow.domain.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByPatientId(String patientId);
    Optional<Patient> findByEmail(String email);
}
