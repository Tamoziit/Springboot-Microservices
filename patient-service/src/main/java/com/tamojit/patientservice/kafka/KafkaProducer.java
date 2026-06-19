/* Kafka Producer - sends events to patient Kafka topic */
package com.tamojit.patientservice.kafka;

import com.tamojit.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate; // message type: key(string): message(byte[] base64)

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient) {
        PatientEvent event = PatientEvent.newBuilder()
            .setPatientId(patient.getId().toString())
            .setName(patient.getName())
            .setEmail(patient.getEmail())
            .setEventType("PATIENT_CREATED") // sub-topics/groups of a kafka topic [custom]
            .build();

        try {
            kafkaTemplate.send("patient", event.toByteArray()); // sending event in base64 byte array format to `patient` topic
        } catch (Exception e) {
            log.error("Error sending PATIENT_CREATED event to kafka: ", e);
        }
    }
}
