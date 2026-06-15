package com.tamojit.patientservice.service;

import com.tamojit.patientservice.dto.PatientResponseDTO;
import com.tamojit.patientservice.mappper.PatientMapper;
import com.tamojit.patientservice.model.Patient;
import com.tamojit.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {
    private final PatientRepository patientRepository;

    // constructor dependency injection
    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponseDTO> getPatients() {
        List<Patient> patients = patientRepository.findAll();

        return patients.stream()
            .map(PatientMapper::toDTO)
            .toList(); // converting Patient --> PatientResponseDTO
    }
}
