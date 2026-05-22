package com.hospital.portal.controller;

import com.hospital.portal.dto.request.LoginRequestDTO;
import com.hospital.portal.dto.request.OtpVerifyDTO;
import com.hospital.portal.dto.response.AuthResponseDTO;
import com.hospital.portal.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Step 1: Patient enters phone number → OTP is issued.
     * POST /api/auth/request-otp
     */
    @PostMapping("/request-otp")
    public ResponseEntity<AuthResponseDTO> requestOtp(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.requestOtp(request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2: Patient submits OTP → JWT is returned.
     * POST /api/auth/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponseDTO> verifyOtp(@Valid @RequestBody OtpVerifyDTO request) {
        AuthResponseDTO response = authService.verifyOtp(request.getPhoneNumber(), request.getOtpCode());
        return ResponseEntity.ok(response);
    }
}
