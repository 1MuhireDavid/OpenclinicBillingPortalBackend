package com.hospital.portal.controller;

import com.hospital.portal.dto.request.PaymentRequestDTO;
import com.hospital.portal.dto.response.PaymentHistoryDTO;
import com.hospital.portal.dto.response.PaymentResponseDTO;
import com.hospital.portal.security.PortalPrincipal;
import com.hospital.portal.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /api/payments/initiate
     *
     * Initiates a PawaPay deposit. The patient receives a PIN prompt on their phone.
     * Idempotent: if a PENDING deposit already exists for the same invoice + phone
     * within 10 minutes, returns 409 with the existing deposit ID.
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponseDTO> initiatePayment(
            @Valid @RequestBody PaymentRequestDTO request,
            @AuthenticationPrincipal PortalPrincipal principal) {
        PaymentResponseDTO response = paymentService.initiatePayment(
                principal.getPatientUid(), request);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * GET /api/payments/status/{transactionId}
     *
     * Polls PawaPay for the latest status of a deposit.
     * Frontend should poll this endpoint after initiating a payment.
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<PaymentResponseDTO> checkStatus(
            @PathVariable String transactionId,
            @AuthenticationPrincipal PortalPrincipal principal) {
        PaymentResponseDTO response = paymentService.checkPaymentStatus(
                transactionId, principal.getPatientUid());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/payments/history
     * All past payments (all invoices) for the authenticated patient.
     */
    @GetMapping("/history")
    public ResponseEntity<List<PaymentHistoryDTO>> getHistory(
            @AuthenticationPrincipal PortalPrincipal principal) {
        List<PaymentHistoryDTO> history = paymentService.getPaymentHistory(principal.getPatientUid());
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/payments/history/{invoiceUid}
     * Payments for a specific invoice.
     */
    @GetMapping("/history/{invoiceUid}")
    public ResponseEntity<List<PaymentHistoryDTO>> getInvoiceHistory(
            @PathVariable String invoiceUid,
            @AuthenticationPrincipal PortalPrincipal principal) {
        List<PaymentHistoryDTO> history = paymentService.getInvoicePayments(
                invoiceUid, principal.getPatientUid());
        return ResponseEntity.ok(history);
    }

    /**
     * POST /api/payments/pawapay/callback
     *
     * Receives async deposit-status webhooks from PawaPay.
     * No auth required — PawaPay calls this endpoint directly.
     * The scheduler also polls PENDING deposits, so the system stays consistent
     * even if the webhook is delayed or missed.
     */
    @PostMapping("/pawapay/callback")
    public ResponseEntity<Void> pawaPayCallback(@RequestBody(required = false) String payload) {
        // Acknowledge fast — processing runs async so PawaPay doesn't time out.
        paymentService.processWebhookPayload(payload);
        return ResponseEntity.ok().build();
    }
}
