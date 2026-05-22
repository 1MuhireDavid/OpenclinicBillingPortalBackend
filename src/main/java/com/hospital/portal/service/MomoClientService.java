package com.hospital.portal.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PawaPay Collection API client.
 * Handles deposit initiation and status polling.
 * Auth: static Bearer token — no OAuth/token-refresh required.
 *
 * Correspondent codes (Rwanda):
 *   MTN_MOMO_RWA  — 078x / 079x numbers
 *   AIRTEL_OAPI_RWA — 073x numbers
 */
@Service
public class MomoClientService {

    private static final Logger log = LoggerFactory.getLogger(MomoClientService.class);

    private final RestTemplate restTemplate;

    @Value("${app.pawapay.api-token}")
    private String apiToken;

    @Value("${app.pawapay.base-url}")
    private String baseUrlConfig;

    @Value("${app.pawapay.callback-url:}")
    private String callbackUrl;

    @Value("${app.pawapay.correspondent:MTN_MOMO_RWA}")
    private String defaultCorrespondent;

    @Value("${app.pawapay.currency:RWF}")
    private String defaultCurrency;

    @Value("${app.pawapay.country:RWA}")
    private String country;

    public MomoClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Initiates a PawaPay deposit (customer pays).
     *
     * @return the depositId (UUID) used as the payment reference
     */
    public String requestPayment(BigDecimal amount, String currency, String invoiceId,
                                 String phoneNumber, String payerMessage, String payeeMessage) {
        String depositId = UUID.randomUUID().toString();
        String normalised = normalisePhone(phoneNumber);
        // Auto-detect network from phone prefix; fall back to configured default
        String correspondent = detectCorrespondent(normalised);

        HttpHeaders headers = buildHeaders();

        Map<String, Object> payer = new HashMap<>();
        payer.put("type", "MSISDN");
        payer.put("address", Map.of("value", normalised));

        Map<String, Object> body = new HashMap<>();
        body.put("depositId", depositId);
        body.put("amount", amount.stripTrailingZeros().toPlainString());
        body.put("currency", currency != null ? currency : defaultCurrency);
        body.put("correspondent", correspondent);
        body.put("payer", payer);
        body.put("customerTimestamp", Instant.now().toString());
        body.put("statementDescription", sanitiseStatementDescription("Invoice " + invoiceId));
        if (payerMessage != null && !payerMessage.isBlank()) {
            body.put("description", payerMessage);
        }

        try {
            ResponseEntity<PawaPayDepositResponse> response = restTemplate.postForEntity(
                    baseUrl() + "/deposits",
                    new HttpEntity<>(body, headers),
                    PawaPayDepositResponse.class);

            PawaPayDepositResponse resp = response.getBody();
            String status = resp != null ? resp.getStatus() : null;
            log.info("PawaPay deposit response: httpStatus={} bodyStatus={} depositId={}",
                    response.getStatusCode(), status, depositId);

            // ACCEPTED = request received and being processed
            // INITIATED = deposit is now being executed (some API versions)
            // DUPLICATE_IGNORED / DUPLICATION_IGNORED = same depositId already exists
            if ("ACCEPTED".equalsIgnoreCase(status)
                    || "INITIATED".equalsIgnoreCase(status)
                    || "DUPLICATE_IGNORED".equalsIgnoreCase(status)
                    || "DUPLICATION_IGNORED".equalsIgnoreCase(status)) {
                log.info("PawaPay deposit initiated: depositId={} invoiceId={} amount={} correspondent={}",
                        depositId, invoiceId, amount, correspondent);
                return depositId;
            }

            // REJECTED / FAILED → throw with the rejection reason
            if ("REJECTED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
                String reason = resp != null && resp.getRejectionReason() != null
                        ? resp.getRejectionReason().toString() : "unknown";
                throw new com.hospital.portal.exception.MomoPaymentException(
                        "PawaPay rejected deposit: " + reason);
            }

            // Any other status — surface it instead of silently treating as success
            throw new com.hospital.portal.exception.MomoPaymentException(
                    "PawaPay returned unexpected response: httpStatus=" + response.getStatusCode()
                    + " bodyStatus=" + status);

        } catch (HttpClientErrorException ex) {
            log.error("PawaPay deposit error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new com.hospital.portal.exception.MomoPaymentException(
                    "PawaPay deposit failed: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            log.error("PawaPay connection error: {}", ex.getMessage());
            throw new com.hospital.portal.exception.MomoPaymentException(
                    "Could not reach PawaPay gateway: " + ex.getMessage(), ex);
        }
    }

    /**
     * Polls PawaPay for the status of a previously initiated deposit.
     * Normalises PawaPay statuses to the internal vocabulary:
     *   INITIATED  → PENDING
     *   COMPLETED  → SUCCESSFUL
     *   FAILED     → FAILED
     */
    public String getPaymentStatus(String depositId) {
        HttpHeaders headers = buildHeaders();

        try {
            ResponseEntity<PawaPayDepositResponse[]> response = restTemplate.exchange(
                    baseUrl() + "/deposits/" + depositId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    PawaPayDepositResponse[].class);

            PawaPayDepositResponse[] body = response.getBody();
            if (body != null && body.length > 0) {
                String raw = body[0].getStatus();
                String normalised = normaliseStatus(raw);
                log.info("PawaPay status: depositId={} raw={} normalised={}", depositId, raw, normalised);
                return normalised;
            }
            return "PENDING";

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) return "PENDING";
            log.error("PawaPay status check error: {}", ex.getMessage());
            return "FAILED";
        } catch (RestClientException ex) {
            log.error("PawaPay status check connection error: {}", ex.getMessage());
            return "PENDING";
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(apiToken);
        return h;
    }

    private String baseUrl() {
        return baseUrlConfig;
    }

    /** Map PawaPay terminal statuses to internal vocabulary. */
    private String normaliseStatus(String raw) {
        if (raw == null) return "PENDING";
        return switch (raw.toUpperCase()) {
            case "COMPLETED"           -> "SUCCESSFUL";
            case "INITIATED"           -> "PENDING";
            case "DUPLICATION_IGNORED" -> "PENDING";
            case "FAILED"              -> "FAILED";
            default                    -> raw.toUpperCase();
        };
    }

    /**
     * Detects the PawaPay correspondent from a Rwandan MSISDN.
     * Falls back to the configured default correspondent for unknown prefixes.
     */
    String detectCorrespondent(String msisdn) {
        if (msisdn == null) return defaultCorrespondent;
        String digits = msisdn.replaceAll("[^0-9]", "");
        String local = digits.startsWith("250") ? digits.substring(3) : digits;
        if (local.startsWith("73")) return "AIRTEL_OAPI_RWA";
        if (local.startsWith("78") || local.startsWith("79")) return "MTN_MOMO_RWA";
        return defaultCorrespondent;
    }

    /** PawaPay allows only alphanumerics and spaces, max 22 chars. */
    String sanitiseStatementDescription(String raw) {
        if (raw == null) return "Hospital bill";
        String cleaned = raw.replaceAll("[^A-Za-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) cleaned = "Hospital bill";
        return cleaned.length() > 22 ? cleaned.substring(0, 22).trim() : cleaned;
    }

    String normalisePhone(String phone) {
        if (phone == null) return phone;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0") && digits.length() == 10)   return "250" + digits.substring(1);
        if (digits.startsWith("250") && digits.length() == 12) return digits;
        return digits;
    }

    // ─── Internal response DTOs ──────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PawaPayDepositResponse {
        @JsonProperty("depositId")       private String depositId;
        @JsonProperty("status")          private String status;
        @JsonProperty("amount")          private String amount;
        @JsonProperty("currency")        private String currency;
        @JsonProperty("correspondent")   private String correspondent;
        @JsonProperty("rejectionReason") private Object rejectionReason;
        @JsonProperty("correspondentIds") private Map<String, String> correspondentIds;

        public String getDepositId()               { return depositId; }
        public String getStatus()                  { return status; }
        public String getAmount()                  { return amount; }
        public String getCurrency()                { return currency; }
        public String getCorrespondent()           { return correspondent; }
        public Object getRejectionReason()         { return rejectionReason; }
        public Map<String, String> getCorrespondentIds() { return correspondentIds; }

        /** Returns the operator's financial transaction ID if present. */
        public String getFinancialTransactionId() {
            if (correspondentIds == null || correspondentIds.isEmpty()) return null;
            return correspondentIds.values().iterator().next();
        }
    }
}
