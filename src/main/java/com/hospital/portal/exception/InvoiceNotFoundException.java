package com.hospital.portal.exception;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(String patientUid) {
        super("No open invoice found for patient: " + patientUid);
    }
    public InvoiceNotFoundException(String invoiceUid, boolean byUid) {
        super("Invoice not found: " + invoiceUid);
    }
}
