package com.hospital.portal.exception;

public class DuplicatePaymentException extends RuntimeException {
    private final String existingTransactionId;

    public DuplicatePaymentException(String invoiceUid, String existingTransactionId) {
        super("A payment is already pending for invoice " + invoiceUid +
              ". Existing transaction: " + existingTransactionId);
        this.existingTransactionId = existingTransactionId;
    }

    public String getExistingTransactionId() { return existingTransactionId; }
}
