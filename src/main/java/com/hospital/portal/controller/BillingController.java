package com.hospital.portal.controller;

import com.hospital.portal.domain.model.InvoiceLineItem;
import com.hospital.portal.domain.model.PatientBillSummary;
import com.hospital.portal.dto.response.BillingLineItemDTO;
import com.hospital.portal.dto.response.InsuranceBreakdownDTO;
import com.hospital.portal.exception.InvoiceNotFoundException;
import com.hospital.portal.repository.jdbc.InvoiceJdbcRepository;
import com.hospital.portal.security.PortalPrincipal;
import com.hospital.portal.service.InsuranceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final InvoiceJdbcRepository invoiceRepo;
    private final InsuranceService insuranceService;

    public BillingController(InvoiceJdbcRepository invoiceRepo,
                             InsuranceService insuranceService) {
        this.invoiceRepo      = invoiceRepo;
        this.insuranceService = insuranceService;
    }

    /**
     * GET /api/billing/invoices
     * All invoices (open and closed) for the authenticated patient.
     */
    @GetMapping("/invoices")
    public ResponseEntity<List<PatientBillSummary>> getAllInvoices(
            @AuthenticationPrincipal PortalPrincipal principal) {
        List<PatientBillSummary> invoices = invoiceRepo.findByPatientUid(principal.getPatientUid());
        return ResponseEntity.ok(invoices);
    }

    /**
     * GET /api/billing/invoices/open
     * Only open (unpaid/partially-paid) invoices.
     */
    @GetMapping("/invoices/open")
    public ResponseEntity<List<PatientBillSummary>> getOpenInvoices(
            @AuthenticationPrincipal PortalPrincipal principal) {
        List<PatientBillSummary> invoices = invoiceRepo.findOpenByPatientUid(principal.getPatientUid());
        return ResponseEntity.ok(invoices);
    }

    /**
     * GET /api/billing/invoices/{invoiceUid}
     * Single invoice by UID (e.g. "1.1234").
     */
    @GetMapping("/invoices/{invoiceUid}")
    public ResponseEntity<PatientBillSummary> getInvoice(
            @PathVariable String invoiceUid,
            @AuthenticationPrincipal PortalPrincipal principal) {
        PatientBillSummary invoice = invoiceRepo.findByUid(invoiceUid)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceUid, true));
        if (!invoice.getPatientUid().equals(principal.getPatientUid())) {
            throw new IllegalStateException("Invoice does not belong to the authenticated patient.");
        }
        return ResponseEntity.ok(invoice);
    }

    /**
     * GET /api/billing/invoices/{invoiceUid}/items
     * Itemised charge breakdown (consultation, drugs, lab tests, etc.) for one invoice.
     */
    @GetMapping("/invoices/{invoiceUid}/items")
    public ResponseEntity<List<BillingLineItemDTO>> getLineItems(
            @PathVariable String invoiceUid,
            @AuthenticationPrincipal PortalPrincipal principal) {
        PatientBillSummary invoice = invoiceRepo.findByUid(invoiceUid)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceUid, true));
        if (!invoice.getPatientUid().equals(principal.getPatientUid())) {
            throw new IllegalStateException("Invoice does not belong to the authenticated patient.");
        }
        List<BillingLineItemDTO> items = invoiceRepo.findLineItemsByInvoiceUid(invoiceUid)
                .stream()
                .map(this::toLineItemDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    private BillingLineItemDTO toLineItemDTO(InvoiceLineItem item) {
        BillingLineItemDTO dto = new BillingLineItemDTO();
        dto.setDebetUid(item.getDebetUid());
        dto.setServiceName(item.getServiceName());
        dto.setQuantity(item.getQuantity());
        dto.setTotalAmount(item.getTotalAmount());
        dto.setInsuranceAmount(item.getInsuranceAmount());
        dto.setPatientAmount(item.getPatientAmount());
        dto.setCurrency("RWF");
        dto.setServiceDate(item.getServiceDate());
        dto.setCredited(item.isCredited());
        return dto;
    }

    /**
     * GET /api/billing/insurance
     * Active insurance with coverage split applied to the current open invoice.
     */
    @GetMapping("/insurance")
    public ResponseEntity<InsuranceBreakdownDTO> getInsuranceBreakdown(
            @AuthenticationPrincipal PortalPrincipal principal) {
        InsuranceBreakdownDTO dto = insuranceService.getBreakdown(principal.getPatientUid());
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/billing/insurance/all
     * All insurance records (active and past) on file for this patient.
     */
    @GetMapping("/insurance/all")
    public ResponseEntity<List<InsuranceBreakdownDTO>> getAllInsurances(
            @AuthenticationPrincipal PortalPrincipal principal) {
        List<InsuranceBreakdownDTO> list = insuranceService.getAllInsurances(principal.getPatientUid());
        return ResponseEntity.ok(list);
    }
}
