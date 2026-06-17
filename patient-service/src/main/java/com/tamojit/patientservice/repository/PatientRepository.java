package com.tamojit.patientservice.repository;

import com.tamojit.patientservice.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    boolean existsByEmail(String email);

    // For Update - checks if email exists for any Patient other than the patient whose id has been provided in query (without this, update will not happen even for a user updating their own record, since email uniqueness check will not be passed)
    boolean existsByEmailAndIdNot(String email, UUID id);
}
