package com.hospital.portal.service;

import com.hospital.portal.domain.model.InsuranceCoverage;
import com.hospital.portal.domain.model.PatientBillSummary;
import com.hospital.portal.dto.response.InsuranceBreakdownDTO;
import com.hospital.portal.exception.InvoiceNotFoundException;
import com.hospital.portal.repository.jdbc.InsuranceJdbcRepository;
import com.hospital.portal.repository.jdbc.InvoiceJdbcRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InsuranceService {

    private final InsuranceJdbcRepository insuranceRepo;
    private final InvoiceJdbcRepository invoiceRepo;
    private final InsuranceCalculationService calcService;

    public InsuranceService(InsuranceJdbcRepository insuranceRepo,
                            InvoiceJdbcRepository invoiceRepo,
                            InsuranceCalculationService calcService) {
        this.insuranceRepo = insuranceRepo;
        this.invoiceRepo   = invoiceRepo;
        this.calcService   = calcService;
    }

    /**
     * Returns a full insurance breakdown for the patient's active insurance,
     * applied to their current open invoice.
     */
    public InsuranceBreakdownDTO getBreakdown(String patientUid) {
        Optional<InsuranceCoverage> coverageOpt = insuranceRepo.findActiveByPatientUid(patientUid);

        InsuranceCoverage coverage;
        if (coverageOpt.isPresent()) {
            coverage = coverageOpt.get();
            calcService.applyRates(coverage);
        } else {
            // Treat as private patient
            coverage = new InsuranceCoverage();
            coverage.setInsurarName("Private / Cash");
            coverage.setInsuranceRatePercent(0.0);
            coverage.setPatientRatePercent(100.0);
        }

        InsuranceBreakdownDTO dto = new InsuranceBreakdownDTO();
        dto.setInsuranceUid(coverage.getInsuranceUid());
        dto.setInsurarName(coverage.getInsurarName());
        dto.setPolicyNumber(coverage.getInsuranceNr());
        dto.setInsuranceType(coverage.getType());
        dto.setCategoryLetter(coverage.getInsuranceCategoryLetter());
        dto.setMemberName(coverage.getMember());
        dto.setMemberEmployer(coverage.getMemberEmployer());
        dto.setInsuranceRatePercent(coverage.getInsuranceRatePercent());
        dto.setPatientRatePercent(coverage.getPatientRatePercent());
        dto.setCoverageLabel(calcService.coverageLabel(coverage));
        dto.setValidFrom(coverage.getStartDate());
        dto.setValidTo(coverage.getEndDate());
        dto.setActive(coverage.isActive());

        // Apply to current open invoice if one exists
        Optional<PatientBillSummary> billOpt = invoiceRepo.findLatestOpenByPatientUid(patientUid);
        if (billOpt.isPresent()) {
            PatientBillSummary bill       = billOpt.get();
            BigDecimal patientBalance     = bill.getPatientBalance().setScale(2, RoundingMode.HALF_UP);
            BigDecimal amountPaid         = invoiceRepo.getTotalPaidByInvoiceUid(bill.getInvoiceUid());

            BigDecimal originalPatientShare = patientBalance.add(amountPaid);
            BigDecimal totalBill            = coverage.getInsuranceRatePercent() > 0
                    ? calcService.deriveTotalFromPatientBalance(originalPatientShare, coverage)
                    : originalPatientShare;
            BigDecimal insuranceCovers      = calcService.insuranceShare(totalBill, coverage);

            dto.setTotalBillAmount(totalBill);
            dto.setInsuranceCoversAmount(insuranceCovers);
            dto.setPatientOwesAmount(originalPatientShare);
            dto.setPatientAlreadyPaid(amountPaid.setScale(2, RoundingMode.HALF_UP));
            dto.setPatientRemainingBalance(patientBalance);
        }

        return dto;
    }

    /**
     * Returns all insurance records on file for a patient (active and past).
     */
    public List<InsuranceBreakdownDTO> getAllInsurances(String patientUid) {
        return insuranceRepo.findByPatientUid(patientUid)
                .stream()
                .map(c -> {
                    calcService.applyRates(c);
                    InsuranceBreakdownDTO dto = new InsuranceBreakdownDTO();
                    dto.setInsuranceUid(c.getInsuranceUid());
                    dto.setInsurarName(c.getInsurarName());
                    dto.setPolicyNumber(c.getInsuranceNr());
                    dto.setInsuranceType(c.getType());
                    dto.setCategoryLetter(c.getInsuranceCategoryLetter());
                    dto.setMemberName(c.getMember());
                    dto.setMemberEmployer(c.getMemberEmployer());
                    dto.setInsuranceRatePercent(c.getInsuranceRatePercent());
                    dto.setPatientRatePercent(c.getPatientRatePercent());
                    dto.setCoverageLabel(calcService.coverageLabel(c));
                    dto.setValidFrom(c.getStartDate());
                    dto.setValidTo(c.getEndDate());
                    dto.setActive(c.isActive());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
