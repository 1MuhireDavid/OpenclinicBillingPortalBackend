package com.hospital.portal.controller;

import com.hospital.portal.dto.response.ReceiptDTO;
import com.hospital.portal.security.PortalPrincipal;
import com.hospital.portal.service.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    /**
     * GET /api/receipts/{transactionId}
     *
     * Returns a digital receipt for a completed (SUCCESSFUL) MoMo payment.
     * Only the patient who made the payment can retrieve it.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<ReceiptDTO> getReceipt(
            @PathVariable String transactionId,
            @AuthenticationPrincipal PortalPrincipal principal) {
        ReceiptDTO receipt = receiptService.getReceipt(transactionId, principal.getPatientUid());
        return ResponseEntity.ok(receipt);
    }
}
