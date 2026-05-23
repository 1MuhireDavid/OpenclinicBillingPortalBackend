package com.hospital.portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private static final String PROD_URL    = "https://api.africastalking.com/version1/messaging";
    private static final String SANDBOX_URL = "https://api.sandbox.africastalking.com/version1/messaging";

    @Value("${app.africastalking.username:sandbox}")
    private String username;

    @Value("${app.africastalking.api-key:}")
    private String apiKey;

    @Value("${app.africastalking.sandbox:true}")
    private boolean sandbox;

    @Value("${app.africastalking.sender-id:}")
    private String senderId;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void sendOtp(String toPhone, String code, int expiryMinutes) {
        if (!isConfigured()) {
            log.debug("SMS skipped (Africa's Talking api-key not set) for {}", toPhone);
            return;
        }

        // Africa's Talking requires E.164 format
        String to = toPhone.startsWith("+") ? toPhone : "+" + toPhone;
        String body = "Your MRS Rich Billing Portal OTP is: " + code
                    + ". Valid for " + expiryMinutes + " minutes. Do not share this code.";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("apiKey", apiKey);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("to", to);
        form.add("message", body);
        if (senderId != null && !senderId.isBlank()) {
            form.add("from", senderId);
        }

        String url = sandbox ? SANDBOX_URL : PROD_URL;

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(form, headers),
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP SMS dispatched via Africa's Talking to {}", toPhone);
            } else {
                log.error("Africa's Talking returned {}: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("SMS delivery failed (status " + response.getStatusCode() + ")");
            }
        } catch (RuntimeException e) {
            log.error("Africa's Talking SMS failed for {}: {}", toPhone, e.getMessage());
            throw new RuntimeException("Could not send OTP via SMS. Please try again.", e);
        }
    }
}
