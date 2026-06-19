package com.tamojit.patientservice.service;

import com.tamojit.patientservice.dto.PatientRequestDTO;
import com.tamojit.patientservice.dto.PatientResponseDTO;
import com.tamojit.patientservice.exception.EmailAlreadyExistsException;
import com.tamojit.patientservice.exception.PatientNotFoundException;
import com.tamojit.patientservice.grpc.BillingServiceGrpcClient;
import com.tamojit.patientservice.kafka.KafkaProducer;
import com.tamojit.patientservice.mappper.PatientMapper;
import com.tamojit.patientservice.model.Patient;
import com.tamojit.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    // constructor dependency injection
    public PatientService(
        PatientRepository patientRepository,
        BillingServiceGrpcClient billingServiceGrpcClient,
        KafkaProducer kafkaProducer
    ) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
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

        Patient newPatient = patientRepository.save(
            PatientMapper.toModel(patientRequestDTO)
        ); // PatientRequestDTO --> Patient

        // creating Billing acc. right when patient is created --> via inter-service gRPC request
        billingServiceGrpcClient.createBillingAccount(
            newPatient.getId().toString(),
            newPatient.getName(),
            newPatient.getEmail()
        );

        // feeding patient to Kafka topic
        kafkaProducer.sendEvent(newPatient);

        return PatientMapper.toDTO(newPatient); // Patient --> PatientResponseDTO
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + id));

        if (patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
            throw new EmailAlreadyExistsException("A patient with email " + patientRequestDTO.getEmail() + " already exists");
        }

        patient.setName(patientRequestDTO.getName());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient updatedPatient = patientRepository.save(patient);
        return PatientMapper.toDTO(updatedPatient);
    }

    public void deletePatient(UUID id) {
        patientRepository.deleteById(id);
    }
}
