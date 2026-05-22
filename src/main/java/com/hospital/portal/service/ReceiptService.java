package com.hospital.portal.service;

import com.hospital.portal.domain.model.MomoPayment;
import com.hospital.portal.domain.model.PatientBillSummary;
import com.hospital.portal.domain.model.PatientProfile;
import com.hospital.portal.dto.response.ReceiptDTO;
import com.hospital.portal.exception.InvoiceNotFoundException;
import com.hospital.portal.repository.jdbc.InvoiceJdbcRepository;
import com.hospital.portal.repository.jdbc.MomoJdbcRepository;
import com.hospital.portal.repository.jdbc.PatientJdbcRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ReceiptService {

    private final MomoJdbcRepository momoRepo;
    private final InvoiceJdbcRepository invoiceRepo;
    private final PatientJdbcRepository patientRepo;

    @Value("${app.hospital.name:Rwanda Hospital}")
    private String hospitalName;

    public ReceiptService(MomoJdbcRepository momoRepo,
                          InvoiceJdbcRepository invoiceRepo,
                          PatientJdbcRepository patientRepo) {
        this.momoRepo    = momoRepo;
        this.invoiceRepo = invoiceRepo;
        this.patientRepo = patientRepo;
    }

    /**
     * Builds a digital receipt for a completed MoMo transaction.
     * Only SUCCESSFUL transactions get a receipt; PENDING/FAILED return null.
     */
    public ReceiptDTO getReceipt(String transactionId, String patientUid) {
        MomoPayment payment = momoRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));

        if (!payment.getPatientUid().equals(patientUid)) {
            throw new IllegalStateException("Transaction does not belong to the authenticated patient.");
        }

        if (!payment.isSuccessful()) {
            throw new IllegalStateException(
                    "Receipt is only available for successful payments. Current status: " + payment.getStatus());
        }

        PatientBillSummary invoice = invoiceRepo.findByUid(payment.getInvoiceUid())
                .orElseThrow(() -> new InvoiceNotFoundException(payment.getInvoiceUid(), true));

        String[] parts   = patientUid.split("\\.");
        int personId     = parts.length > 1 ? Integer.parseInt(parts[1]) : Integer.parseInt(parts[0]);
        PatientProfile profile = patientRepo.findByPersonId(personId).orElse(null);

        BigDecimal remainingBalance = invoiceRepo.getTotalPaidByInvoiceUid(payment.getInvoiceUid());

        ReceiptDTO receipt = new ReceiptDTO();
        receipt.setReceiptNumber(
                payment.getFinancialTransactionId() != null
                ? payment.getFinancialTransactionId()
                : payment.getTransactionId());
        receipt.setTransactionId(payment.getTransactionId());
        receipt.setPatientUid(patientUid);
        receipt.setPatientName(profile != null ? profile.getFullName() : "N/A");
        receipt.setInvoiceUid(invoice.getInvoiceUid());
        receipt.setInvoiceNumber(invoice.getInvoiceNumber2());
        receipt.setAmountPaid(payment.getAmount().setScale(2, RoundingMode.HALF_UP));
        receipt.setCurrency(payment.getCurrency() != null ? payment.getCurrency() : "RWF");
        receipt.setPaymentMethod("PawaPay");
        receipt.setPayerPhone(payment.getPayerPhone());
        receipt.setPaymentStatus(payment.getStatus());
        receipt.setRemainingBalance(invoice.getPatientBalance().setScale(2, RoundingMode.HALF_UP));
        receipt.setPaidAt(payment.getUpdatedAt() != null ? payment.getUpdatedAt() : payment.getCreatedAt());
        receipt.setHospitalName(hospitalName);

        return receipt;
    }
}
