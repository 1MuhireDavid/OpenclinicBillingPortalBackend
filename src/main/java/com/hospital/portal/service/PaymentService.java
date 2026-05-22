package com.hospital.portal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.portal.domain.model.MomoPayment;
import com.hospital.portal.domain.model.PatientBillSummary;
import com.hospital.portal.dto.request.PaymentRequestDTO;
import com.hospital.portal.dto.response.PaymentHistoryDTO;
import com.hospital.portal.dto.response.PaymentResponseDTO;
import com.hospital.portal.exception.DuplicatePaymentException;
import com.hospital.portal.exception.InvoiceNotFoundException;
import com.hospital.portal.repository.jdbc.InvoiceJdbcRepository;
import com.hospital.portal.repository.jdbc.MomoJdbcRepository;
import com.hospital.portal.repository.jdbc.PatientCreditJdbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int IDEMPOTENCY_WINDOW_MINUTES = 10;

    private final MomoJdbcRepository momoRepo;
    private final InvoiceJdbcRepository invoiceRepo;
    private final MomoClientService pawaPayClient;
    private final PatientCreditJdbcRepository creditRepo;
    private final ObjectMapper objectMapper;

    public PaymentService(MomoJdbcRepository momoRepo,
                          InvoiceJdbcRepository invoiceRepo,
                          MomoClientService pawaPayClient,
                          PatientCreditJdbcRepository creditRepo,
                          ObjectMapper objectMapper) {
        this.momoRepo      = momoRepo;
        this.invoiceRepo   = invoiceRepo;
        this.pawaPayClient = pawaPayClient;
        this.creditRepo    = creditRepo;
        this.objectMapper  = objectMapper;
    }

    /**
     * Processes a PawaPay deposit webhook asynchronously.
     * Acknowledged immediately by the controller (200 OK); this method does the actual work.
     *
     * Idempotent: ignores duplicate webhooks for the same depositId once the status is terminal.
     */
    @Async
    public void processWebhookPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("PawaPay webhook: empty payload");
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            // PawaPay can send either a single deposit object or wrap it in {"data": {...}}
            JsonNode deposit = root.has("depositId") ? root : root.path("data");
            String depositId = deposit.path("depositId").asText(null);
            String rawStatus = deposit.path("status").asText(null);

            if (depositId == null || rawStatus == null) {
                log.warn("PawaPay webhook: missing depositId or status in payload: {}", payload);
                return;
            }

            String newStatus = mapStatus(rawStatus);
            Optional<MomoPayment> opt = momoRepo.findByTransactionId(depositId);
            if (opt.isEmpty()) {
                log.warn("PawaPay webhook: unknown depositId={}", depositId);
                return;
            }
            MomoPayment payment = opt.get();
            String currentStatus = payment.getStatus();

            if (newStatus.equalsIgnoreCase(currentStatus)) {
                log.debug("PawaPay webhook: depositId={} status unchanged ({})", depositId, currentStatus);
                return;
            }

            String financialTxId = deposit.path("correspondentIds").elements().hasNext()
                    ? deposit.path("correspondentIds").elements().next().asText(null)
                    : null;

            momoRepo.updateStatus(depositId, newStatus, financialTxId);
            log.info("PawaPay webhook: depositId={} {} → {}", depositId, currentStatus, newStatus);

            if ("SUCCESSFUL".equalsIgnoreCase(newStatus)) {
                payment.setStatus(newStatus);
                payment.setFinancialTransactionId(financialTxId);
                writePatientCredit(payment);
            }
        } catch (Exception ex) {
            log.error("PawaPay webhook processing error: {}", ex.getMessage(), ex);
        }
    }

    private String mapStatus(String raw) {
        return switch (raw.toUpperCase()) {
            case "COMPLETED"           -> "SUCCESSFUL";
            case "INITIATED",
                 "DUPLICATION_IGNORED",
                 "SUBMITTED"           -> "PENDING";
            case "FAILED"              -> "FAILED";
            default                    -> raw.toUpperCase();
        };
    }

    /**
     * Initiates a PawaPay deposit for the given invoice.
     *
     * Idempotency: if a PENDING payment already exists for this invoice + phone
     * within the last 10 minutes, returns 409 instead of creating a duplicate.
     */
    public PaymentResponseDTO initiatePayment(String patientUid, PaymentRequestDTO request) {

        PatientBillSummary invoice = invoiceRepo.findByUid(request.getInvoiceUid())
                .orElseThrow(() -> new InvoiceNotFoundException(request.getInvoiceUid(), true));

        if (!invoice.getPatientUid().equals(patientUid)) {
            throw new IllegalStateException("Invoice " + request.getInvoiceUid()
                    + " does not belong to the authenticated patient.");
        }

        if (!invoice.isOpen()) {
            throw new IllegalStateException("Invoice " + request.getInvoiceUid()
                    + " is already closed. No further payments accepted.");
        }

        BigDecimal amount  = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal balance = invoice.getPatientBalance().setScale(2, RoundingMode.HALF_UP);

        if (amount.compareTo(balance) > 0) {
            throw new IllegalStateException(String.format(
                    "Payment amount %.2f RWF exceeds the remaining balance of %.2f RWF.",
                    amount, balance));
        }

        String normalisedPhone = pawaPayClient.normalisePhone(request.getPayerPhone());
        Optional<MomoPayment> existing = momoRepo.findRecentPendingForInvoice(
                request.getInvoiceUid(), normalisedPhone, IDEMPOTENCY_WINDOW_MINUTES);

        if (existing.isPresent()) {
            MomoPayment dup = existing.get();
            log.warn("Returning existing PENDING transaction {} for invoice {} (idempotency)",
                    dup.getTransactionId(), request.getInvoiceUid());
            throw new DuplicatePaymentException(request.getInvoiceUid(), dup.getTransactionId());
        }

        log.info("Initiating PawaPay deposit: invoiceUid={} amount={} phone={}",
                request.getInvoiceUid(), amount, normalisedPhone);

        String correspondent = pawaPayClient.detectCorrespondent(normalisedPhone);
        String depositId = pawaPayClient.requestPayment(
                amount, "RWF",
                request.getInvoiceUid(),
                normalisedPhone,
                request.getPayerMessage(),
                "Hospital bill payment");

        momoRepo.insertPaymentRequest(
                depositId,
                request.getInvoiceUid(),
                patientUid,
                amount.doubleValue(),
                "RWF",
                normalisedPhone,
                request.getPayerMessage(),
                "Hospital bill payment",
                patientUid,
                correspondent);

        log.info("PawaPay deposit PENDING: depositId={}", depositId);

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setTransactionId(depositId);
        response.setInvoiceUid(request.getInvoiceUid());
        response.setAmount(amount);
        response.setCurrency("RWF");
        response.setStatus("PENDING");
        response.setRemainingBalance(balance);
        response.setMessage("Payment request sent to PawaPay. Check your phone and approve with your PIN.");
        return response;
    }

    /**
     * Polls PawaPay for the current status of a transaction and updates OC_MOMO.
     * Writes OC_PATIENTCREDITS when the status first transitions to SUCCESSFUL.
     */
    public PaymentResponseDTO checkPaymentStatus(String transactionId, String patientUid) {
        MomoPayment payment = momoRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));

        if (!payment.getPatientUid().equals(patientUid)) {
            throw new IllegalStateException("Transaction does not belong to the authenticated patient.");
        }

        String currentStatus = payment.getStatus();
        if ("PENDING".equalsIgnoreCase(currentStatus) || "SUBMITTED".equalsIgnoreCase(currentStatus)) {
            String apiStatus = pawaPayClient.getPaymentStatus(transactionId);

            if (!apiStatus.equalsIgnoreCase(currentStatus)) {
                momoRepo.updateStatus(transactionId, apiStatus, null);
                currentStatus = apiStatus;
                log.info("PawaPay status updated: depositId={} newStatus={}", transactionId, apiStatus);

                if ("SUCCESSFUL".equalsIgnoreCase(apiStatus)) {
                    writePatientCredit(payment);
                }
            }
        }

        BigDecimal remainingBalance = invoiceRepo
                .findByUid(payment.getInvoiceUid())
                .map(PatientBillSummary::getPatientBalance)
                .orElse(BigDecimal.ZERO);

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setTransactionId(transactionId);
        response.setInvoiceUid(payment.getInvoiceUid());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(currentStatus);
        response.setRemainingBalance(remainingBalance);
        response.setMessage(statusMessage(currentStatus));
        return response;
    }

    public List<PaymentHistoryDTO> getPaymentHistory(String patientUid) {
        return momoRepo.findByPatientUid(patientUid)
                .stream()
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());
    }

    public List<PaymentHistoryDTO> getInvoicePayments(String invoiceUid, String patientUid) {
        PatientBillSummary invoice = invoiceRepo.findByUid(invoiceUid)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceUid, true));
        if (!invoice.getPatientUid().equals(patientUid)) {
            throw new IllegalStateException("Invoice does not belong to the authenticated patient.");
        }
        return momoRepo.findByInvoiceUid(invoiceUid)
                .stream()
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public void writePatientCredit(MomoPayment payment) {
        if (creditRepo.existsForTransaction(payment.getTransactionId())) {
            log.debug("OC_PATIENTCREDITS already exists for depositId={}", payment.getTransactionId());
            return;
        }
        String uid = creditRepo.insertCredit(
                payment.getInvoiceUid(),
                payment.getAmount(),
                payment.getCurrency(),
                "PawaPay depositId: " + payment.getTransactionId(),
                payment.getUpdatedAt() != null ? payment.getUpdatedAt() : payment.getCreatedAt());
        if (uid != null) {
            momoRepo.updateCreditLinks(payment.getTransactionId(), uid, null);
            creditRepo.applyCreditToInvoice(payment.getInvoiceUid(), payment.getAmount());
        }
    }

    private PaymentHistoryDTO toHistoryDTO(MomoPayment m) {
        PaymentHistoryDTO h = new PaymentHistoryDTO();
        h.setTransactionId(m.getTransactionId());
        h.setFinancialTransactionId(m.getFinancialTransactionId());
        h.setInvoiceUid(m.getInvoiceUid());
        h.setAmount(m.getAmount());
        h.setCurrency(m.getCurrency());
        h.setPayerPhone(m.getPayerPhone());
        h.setStatus(m.getStatus());
        h.setOperator(m.getOperator());
        h.setPayerMessage(m.getPayerMessage());
        h.setCreatedAt(m.getCreatedAt());
        h.setUpdatedAt(m.getUpdatedAt());
        return h;
    }

    private String statusMessage(String status) {
        return switch (status.toUpperCase()) {
            case "SUCCESSFUL" -> "Payment confirmed. Your bill has been updated.";
            case "FAILED"     -> "Payment failed. Please try again.";
            case "PENDING"    -> "Payment is pending. Approve on your phone to complete.";
            default           -> "Payment status: " + status;
        };
    }
}
