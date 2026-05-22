package com.hospital.portal.exception;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(String phone) {
        super("No patient found for phone number: " + phone);
    }
    public PatientNotFoundException(int personId) {
        super("No patient found with person ID: " + personId);
    }
}
