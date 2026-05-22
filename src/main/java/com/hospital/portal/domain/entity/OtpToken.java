package com.hospital.portal.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One-time password issued for phone-based login.
 * Stored in openclinic.OC_PORTAL_OTP.
 */
@Entity
@Table(name = "OC_PORTAL_OTP")
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public OtpToken() {}

    public OtpToken(String phoneNumber, String otpCode, LocalDateTime expiresAt) {
        this.phoneNumber = phoneNumber;
        this.otpCode     = otpCode;
        this.expiresAt   = expiresAt;
        this.used        = false;
        this.createdAt   = LocalDateTime.now();
    }

    public Long getId()            { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getOtpCode()     { return otpCode; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isUsed()        { return used; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void markUsed() { this.used = true; }
    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
}
