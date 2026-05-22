package com.hospital.portal.exception;

import com.hospital.portal.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handlePatientNotFound(
            PatientNotFoundException ex, HttpServletRequest req) {
        log.warn("Patient not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Patient Not Found", ex.getMessage(), req);
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvoiceNotFound(
            InvoiceNotFoundException ex, HttpServletRequest req) {
        log.warn("Invoice not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Invoice Not Found", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicatePayment(
            DuplicatePaymentException ex, HttpServletRequest req) {
        log.warn("Duplicate payment attempt: {}", ex.getMessage());
        ErrorResponseDTO err = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(), "Duplicate Payment", ex.getMessage(), req.getRequestURI());
        err.setDetails(List.of("Existing transaction ID: " + ex.getExistingTransactionId()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(MomoPaymentException.class)
    public ResponseEntity<ErrorResponseDTO> handlePaymentGatewayFailure(
            MomoPaymentException ex, HttpServletRequest req) {
        log.error("PawaPay payment error: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "Payment Gateway Error", ex.getMessage(), req);
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ErrorResponseDTO> handleOtp(
            OtpException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication Failed", ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(
            IllegalStateException ex, HttpServletRequest req) {
        log.warn("Illegal state: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Invalid Operation", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        ErrorResponseDTO err = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(), "Validation Failed",
                "One or more fields have errors", req.getRequestURI());
        err.setDetails(details);
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception [{}]: {}", req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again.", req);
    }

    private ResponseEntity<ErrorResponseDTO> build(
            HttpStatus status, String error, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(
                new ErrorResponseDTO(status.value(), error, message, req.getRequestURI()));
    }
}
