package com.hospital.portal.exception;

public class MomoPaymentException extends RuntimeException {
    public MomoPaymentException(String message) { super(message); }
    public MomoPaymentException(String message, Throwable cause) { super(message, cause); }
}
