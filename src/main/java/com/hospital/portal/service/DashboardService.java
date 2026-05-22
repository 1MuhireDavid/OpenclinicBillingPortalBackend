package com.hospital.portal.service;

import com.hospital.portal.domain.model.InsuranceCoverage;
import com.hospital.portal.domain.model.MomoPayment;
import com.hospital.portal.domain.model.PatientBillSummary;
import com.hospital.portal.domain.model.PatientProfile;
import com.hospital.portal.dto.response.DashboardResponseDTO;
import com.hospital.portal.dto.response.PaymentHistoryDTO;
import com.hospital.portal.exception.InvoiceNotFoundException;
import com.hospital.portal.exception.PatientNotFoundException;
import com.hospital.portal.repository.jdbc.InsuranceJdbcRepository;
import com.hospital.portal.repository.jdbc.InvoiceJdbcRepository;
import com.hospital.portal.repository.jdbc.MomoJdbcRepository;
import com.hospital.portal.repository.jdbc.PatientJdbcRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final PatientJdbcRepository patientRepo;
    private final InvoiceJdbcRepository invoiceRepo;
    private final InsuranceJdbcRepository insuranceRepo;
    private final MomoJdbcRepository momoRepo;
    private final InsuranceCalculationService calcService;

    public DashboardService(PatientJdbcRepository patientRepo,
                            InvoiceJdbcRepository invoiceRepo,
                            InsuranceJdbcRepository insuranceRepo,
                            MomoJdbcRepository momoRepo,
                            InsuranceCalculationService calcService) {
        this.patientRepo   = patientRepo;
        this.invoiceRepo   = invoiceRepo;
        this.insuranceRepo = insuranceRepo;
        this.momoRepo      = momoRepo;
        this.calcService   = calcService;
    }

    public DashboardResponseDTO buildDashboard(String patientUid) {
        // 1. Patient demographics
        String[] parts   = patientUid.split("\\.");
        int personId     = parts.length > 1 ? Integer.parseInt(parts[1]) : Integer.parseInt(parts[0]);
        PatientProfile profile = patientRepo.findByPersonId(personId)
                .orElseThrow(() -> new PatientNotFoundException(personId));

        // 2. Active insurance
        Optional<InsuranceCoverage> insuranceOpt = insuranceRepo.findActiveByPatientUid(patientUid);
        InsuranceCoverage insurance = insuranceOpt.orElse(null);
        if (insurance != null) {
            calcService.applyRates(insurance);
        }

        // 3. Current open invoice
        Optional<PatientBillSummary> billOpt = invoiceRepo.findLatestOpenByPatientUid(patientUid);

        DashboardResponseDTO dto = new DashboardResponseDTO();
        dto.setPatientUid(patientUid);
        dto.setFullName(profile.getFullName());
        dto.setPhoneNumber(profile.getPhoneNumber());
        dto.setGender(profile.getGender());
        dto.setDateOfBirth(profile.getDateOfBirth());

        // 4. Insurance section
        if (insurance != null) {
            dto.setInsurarName(insurance.getInsurarName());
            dto.setInsuranceRatePercent(insurance.getInsuranceRatePercent());
            dto.setPatientRatePercent(insurance.getPatientRatePercent());
            dto.setPolicyNumber(insurance.getInsuranceNr());
            dto.setMemberEmployer(insurance.getMemberEmployer());
        } else {
            dto.setInsurarName("Private / Cash");
            dto.setInsuranceRatePercent(0.0);
            dto.setPatientRatePercent(100.0);
        }

        // 5. Bill section
        if (billOpt.isEmpty()) {
            dto.setBillStatus("no_bill");
            dto.setTotalBill(BigDecimal.ZERO);
            dto.setInsuranceCoverage(BigDecimal.ZERO);
            dto.setPatientPayable(BigDecimal.ZERO);
            dto.setAmountPaid(BigDecimal.ZERO);
            dto.setRemainingBalance(BigDecimal.ZERO);
            dto.setPaymentProgressPercent(0);
            dto.setRecentPayments(List.of());
            return dto;
        }

        PatientBillSummary bill = billOpt.get();
        dto.setInvoiceUid(bill.getInvoiceUid());
        dto.setInvoiceNumber(bill.getInvoiceNumber2());
        dto.setBillStatus(bill.getStatus());

        // OC_PATIENTINVOICE_BALANCE = patient's remaining owed (what OpenClinic tracks)
        BigDecimal patientBalance = bill.getPatientBalance().setScale(2, RoundingMode.HALF_UP);

        // 6. Amount already paid via MoMo
        BigDecimal amountPaid = invoiceRepo.getTotalPaidByInvoiceUid(bill.getInvoiceUid())
                                           .setScale(2, RoundingMode.HALF_UP);

        // 7. Derive total bill and insurance coverage from balance + insurance rate
        BigDecimal totalBill;
        BigDecimal insuranceCoverage;
        BigDecimal patientPayable;

        if (insurance != null && insurance.getInsuranceRatePercent() > 0) {
            // The balance stored in OC is the patient's CURRENT remaining balance (after payments).
            // To find original patient share: currentBalance + amountPaid
            BigDecimal originalPatientShare = patientBalance.add(amountPaid);
            totalBill        = calcService.deriveTotalFromPatientBalance(originalPatientShare, insurance);
            insuranceCoverage = calcService.insuranceShare(totalBill, insurance);
            patientPayable   = originalPatientShare;  // patient's initial share
        } else {
            // Private: total == patient's share
            totalBill         = patientBalance.add(amountPaid);
            insuranceCoverage = BigDecimal.ZERO;
            patientPayable    = totalBill;
        }

        dto.setTotalBill(totalBill);
        dto.setInsuranceCoverage(insuranceCoverage);
        dto.setPatientPayable(patientPayable);
        dto.setAmountPaid(amountPaid);
        dto.setRemainingBalance(patientBalance);   // what they still owe

        // 8. Progress bar percentage (how much of patient's share has been paid)
        int progress = 0;
        if (patientPayable.compareTo(BigDecimal.ZERO) > 0) {
            progress = amountPaid.multiply(BigDecimal.valueOf(100))
                                 .divide(patientPayable, 0, RoundingMode.HALF_UP)
                                 .intValue();
            progress = Math.min(progress, 100);
        }
        dto.setPaymentProgressPercent(progress);

        // 9. Recent payment history (last 5)
        List<PaymentHistoryDTO> history = momoRepo.findByInvoiceUid(bill.getInvoiceUid())
                .stream()
                .limit(5)
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());
        dto.setRecentPayments(history);

        return dto;
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
}
