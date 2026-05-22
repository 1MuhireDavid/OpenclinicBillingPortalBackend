package com.hospital.portal.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only projection of OC_MOMO.
 */
public class MomoPayment {

    private String transactionId;
    private String financialTransactionId;
    private String invoiceUid;
    private String patientUid;
    private BigDecimal amount;
    private String currency;
    private String payerPhone;
    private String status;              // PENDING | SUBMITTED | SUCCESSFUL | FAILED
    private String operator;           // MTN | Orange | PawaPay
    private String payerMessage;
    private String payeeMessage;
    private String patientCreditUid;
    private String wicketCreditUid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MomoPayment() {}

    public String getTransactionId()         { return transactionId; }
    public String getFinancialTransactionId(){ return financialTransactionId; }
    public String getInvoiceUid()            { return invoiceUid; }
    public String getPatientUid()            { return patientUid; }
    public BigDecimal getAmount()            { return amount; }
    public String getCurrency()              { return currency; }
    public String getPayerPhone()            { return payerPhone; }
    public String getStatus()               { return status; }
    public String getOperator()             { return operator; }
    public String getPayerMessage()         { return payerMessage; }
    public String getPayeeMessage()         { return payeeMessage; }
    public String getPatientCreditUid()     { return patientCreditUid; }
    public String getWicketCreditUid()      { return wicketCreditUid; }
    public LocalDateTime getCreatedAt()     { return createdAt; }
    public LocalDateTime getUpdatedAt()     { return updatedAt; }

    public void setTransactionId(String v)          { this.transactionId = v; }
    public void setFinancialTransactionId(String v) { this.financialTransactionId = v; }
    public void setInvoiceUid(String v)             { this.invoiceUid = v; }
    public void setPatientUid(String v)             { this.patientUid = v; }
    public void setAmount(BigDecimal v)             { this.amount = v; }
    public void setCurrency(String v)               { this.currency = v; }
    public void setPayerPhone(String v)             { this.payerPhone = v; }
    public void setStatus(String v)                 { this.status = v; }
    public void setOperator(String v)               { this.operator = v; }
    public void setPayerMessage(String v)           { this.payerMessage = v; }
    public void setPayeeMessage(String v)           { this.payeeMessage = v; }
    public void setPatientCreditUid(String v)       { this.patientCreditUid = v; }
    public void setWicketCreditUid(String v)        { this.wicketCreditUid = v; }
    public void setCreatedAt(LocalDateTime v)       { this.createdAt = v; }
    public void setUpdatedAt(LocalDateTime v)       { this.updatedAt = v; }

    public boolean isSuccessful() {
        return "SUCCESSFUL".equalsIgnoreCase(status) || "successful".equals(status);
    }
}
