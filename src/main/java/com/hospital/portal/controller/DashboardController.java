package com.hospital.portal.controller;

import com.hospital.portal.dto.response.DashboardResponseDTO;
import com.hospital.portal.security.PortalPrincipal;
import com.hospital.portal.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/dashboard
     *
     * Returns the full patient dashboard: total bill, insurance coverage,
     * patient payable, amount paid, remaining balance, progress %, and recent payments.
     * The patient identity comes from the JWT — no patientId needed in the URL.
     */
    @GetMapping
    public ResponseEntity<DashboardResponseDTO> getDashboard(
            @AuthenticationPrincipal PortalPrincipal principal) {
        DashboardResponseDTO dto = dashboardService.buildDashboard(principal.getPatientUid());
        return ResponseEntity.ok(dto);
    }
}
