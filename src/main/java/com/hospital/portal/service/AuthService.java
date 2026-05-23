package com.hospital.portal.service;

import com.hospital.portal.domain.entity.OtpToken;
import com.hospital.portal.domain.entity.PortalUser;
import com.hospital.portal.domain.model.PatientProfile;
import com.hospital.portal.dto.response.AuthResponseDTO;
import com.hospital.portal.exception.OtpException;
import com.hospital.portal.exception.PatientNotFoundException;
import com.hospital.portal.repository.jdbc.PatientJdbcRepository;
import com.hospital.portal.repository.jpa.OtpTokenRepository;
import com.hospital.portal.repository.jpa.PortalUserRepository;
import com.hospital.portal.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PatientJdbcRepository patientRepo;
    private final PortalUserRepository portalUserRepo;
    private final OtpTokenRepository otpRepo;
    private final JwtUtil jwtUtil;
    private final SmsService smsService;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.otp.expose-in-response:false}")
    private boolean exposeOtpInResponse;

    public AuthService(PatientJdbcRepository patientRepo,
                       PortalUserRepository portalUserRepo,
                       OtpTokenRepository otpRepo,
                       JwtUtil jwtUtil,
                       SmsService smsService) {
        this.patientRepo    = patientRepo;
        this.portalUserRepo = portalUserRepo;
        this.otpRepo        = otpRepo;
        this.jwtUtil        = jwtUtil;
        this.smsService     = smsService;
    }

    /**
     * Step 1 — patient enters their phone number.
     * We look them up in OpenClinic, generate an OTP, and return it
     * (in dev mode) or would send it via SMS (in production).
     */
    @Transactional
    public AuthResponseDTO requestOtp(String phoneNumber) {
        String normalised = normalisePhone(phoneNumber);

        // Find the patient in OpenClinic's ocadmin database
        PatientProfile profile = patientRepo.findByPhoneNumber(normalised)
                .orElseThrow(() -> new PatientNotFoundException(normalised));

        // Auto-register in portal users table on first login
        if (!portalUserRepo.existsByPersonId(profile.getPersonId())) {
            PortalUser newUser = new PortalUser(
                    profile.getPatientUid(),
                    profile.getPersonId(),
                    normalised,
                    profile.getFullName());
            portalUserRepo.save(newUser);
            log.info("Auto-registered portal user for personId={}", profile.getPersonId());
        }

        // Invalidate any previous unused OTPs for this phone
        otpRepo.invalidateAllForPhone(normalised);

        // Generate new 6-digit OTP
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        OtpToken otp = new OtpToken(normalised, code, LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        otpRepo.save(otp);

        log.info("OTP issued for phone={} personId={}", normalised, profile.getPersonId());

        smsService.sendOtp(normalised, code, otpExpiryMinutes);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setMessage("OTP sent to " + mask(normalised) + ". Valid for " + otpExpiryMinutes + " minutes.");
        if (exposeOtpInResponse && !smsService.isConfigured()) {
            response.setDevOtp(code);  // dev mode only — suppressed when Twilio is active
        }
        return response;
    }

    /**
     * Step 2 — patient submits the OTP they received.
     * On success, returns a signed JWT containing the patient's identity.
     */
    @Transactional
    public AuthResponseDTO verifyOtp(String phoneNumber, String code) {
        String normalised = normalisePhone(phoneNumber);

        OtpToken otp = otpRepo
                .findTopByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(normalised)
                .orElseThrow(() -> new OtpException("No active OTP found. Please request a new one."));

        if (otp.isExpired()) {
            throw new OtpException("OTP has expired. Please request a new one.");
        }
        if (!otp.getOtpCode().equals(code)) {
            throw new OtpException("Invalid OTP code.");
        }

        otp.markUsed();
        otpRepo.save(otp);

        PortalUser user = portalUserRepo.findByPhoneNumber(normalised)
                .orElseThrow(() -> new OtpException("Portal user not found. Please request a new OTP."));

        if (!user.isActive()) {
            throw new OtpException("This account has been deactivated. Contact the hospital.");
        }

        user.setLastLogin(LocalDateTime.now());
        portalUserRepo.save(user);

        String jwt = jwtUtil.generateToken(normalised, user.getPatientUid(), user.getPersonId());

        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(jwt);
        response.setPatientUid(user.getPatientUid());
        response.setFullName(user.getFullName());
        response.setPhoneNumber(normalised);
        response.setMessage("Login successful.");

        log.info("JWT issued for patientUid={}", user.getPatientUid());
        return response;
    }

    private String normalisePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0") && digits.length() == 10)   return "250" + digits.substring(1);
        if (digits.startsWith("250") && digits.length() == 12) return digits;
        return digits;
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 6) return "****";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}
