package com.hospital.portal.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class DashboardResponseDTO {

    // Patient identity
    private String patientUid;
    private String fullName;
    private String phoneNumber;
    private String gender;
    private String dateOfBirth;

    // Insurance summary
    private String insurarName;
    private double insuranceRatePercent;
    private double patientRatePercent;
    private String policyNumber;
    private String memberEmployer;

    // Current bill
    private String invoiceUid;
    private String invoiceNumber;
    private String billStatus;             // "open" | "closed" | "no_bill"
    private BigDecimal totalBill;          // insurance share + patient share
    private BigDecimal insuranceCoverage;  // amount insurance covers
    private BigDecimal patientPayable;     // patient's total share on this bill
    private BigDecimal amountPaid;         // sum of SUCCESSFUL MoMo payments
    private BigDecimal remainingBalance;   // what patient still owes right now

    // Progress (0–100)
    private int paymentProgressPercent;

    // Detail lists
    private List<PaymentHistoryDTO> recentPayments;

    public String getPatientUid()            { return patientUid; }
    public String getFullName()              { return fullName; }
    public String getPhoneNumber()           { return phoneNumber; }
    public String getGender()               { return gender; }
    public String getDateOfBirth()          { return dateOfBirth; }
    public String getInsurarName()          { return insurarName; }
    public double getInsuranceRatePercent() { return insuranceRatePercent; }
    public double getPatientRatePercent()   { return patientRatePercent; }
    public String getPolicyNumber()         { return policyNumber; }
    public String getMemberEmployer()       { return memberEmployer; }
    public String getInvoiceUid()           { return invoiceUid; }
    public String getInvoiceNumber()        { return invoiceNumber; }
    public String getBillStatus()           { return billStatus; }
    public BigDecimal getTotalBill()        { return totalBill; }
    public BigDecimal getInsuranceCoverage(){ return insuranceCoverage; }
    public BigDecimal getPatientPayable()   { return patientPayable; }
    public BigDecimal getAmountPaid()       { return amountPaid; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public int getPaymentProgressPercent()  { return paymentProgressPercent; }
    public List<PaymentHistoryDTO> getRecentPayments() { return recentPayments; }

    public void setPatientUid(String v)            { this.patientUid = v; }
    public void setFullName(String v)              { this.fullName = v; }
    public void setPhoneNumber(String v)           { this.phoneNumber = v; }
    public void setGender(String v)               { this.gender = v; }
    public void setDateOfBirth(String v)          { this.dateOfBirth = v; }
    public void setInsurarName(String v)          { this.insurarName = v; }
    public void setInsuranceRatePercent(double v) { this.insuranceRatePercent = v; }
    public void setPatientRatePercent(double v)   { this.patientRatePercent = v; }
    public void setPolicyNumber(String v)         { this.policyNumber = v; }
    public void setMemberEmployer(String v)       { this.memberEmployer = v; }
    public void setInvoiceUid(String v)           { this.invoiceUid = v; }
    public void setInvoiceNumber(String v)        { this.invoiceNumber = v; }
    public void setBillStatus(String v)           { this.billStatus = v; }
    public void setTotalBill(BigDecimal v)        { this.totalBill = v; }
    public void setInsuranceCoverage(BigDecimal v){ this.insuranceCoverage = v; }
    public void setPatientPayable(BigDecimal v)   { this.patientPayable = v; }
    public void setAmountPaid(BigDecimal v)       { this.amountPaid = v; }
    public void setRemainingBalance(BigDecimal v) { this.remainingBalance = v; }
    public void setPaymentProgressPercent(int v)  { this.paymentProgressPercent = v; }
    public void setRecentPayments(List<PaymentHistoryDTO> v) { this.recentPayments = v; }
}
