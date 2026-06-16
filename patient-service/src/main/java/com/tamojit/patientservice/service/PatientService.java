package com.tamojit.patientservice.service;

import com.tamojit.patientservice.dto.PatientRequestDTO;
import com.tamojit.patientservice.dto.PatientResponseDTO;
import com.tamojit.patientservice.exception.EmailAlreadyExistsException;
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

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("A patient with email " + patientRequestDTO.getEmail() + " already exists");
        }

        Patient patient = patientRepository.save(
            PatientMapper.toModel(patientRequestDTO)
        ); // PatientRequestDTO --> Patient

        return PatientMapper.toDTO(patient); // Patient --> PatientResponseDTO
    }
}
