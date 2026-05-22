package com.hospital.portal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReceiptDTO {

    private String receiptNumber;         // = financialTransactionId
    private String transactionId;
    private String patientName;
    private String patientUid;
    private String invoiceNumber;
    private String invoiceUid;
    private BigDecimal amountPaid;
    private String currency;
    private String paymentMethod;         // "PawaPay"
    private String payerPhone;
    private String paymentStatus;
    private BigDecimal remainingBalance;
    private LocalDateTime paidAt;
    private String hospitalName;          // configurable

    public ReceiptDTO() {}

    public String getReceiptNumber()      { return receiptNumber; }
    public String getTransactionId()      { return transactionId; }
    public String getPatientName()        { return patientName; }
    public String getPatientUid()         { return patientUid; }
    public String getInvoiceNumber()      { return invoiceNumber; }
    public String getInvoiceUid()         { return invoiceUid; }
    public BigDecimal getAmountPaid()     { return amountPaid; }
    public String getCurrency()           { return currency; }
    public String getPaymentMethod()      { return paymentMethod; }
    public String getPayerPhone()         { return payerPhone; }
    public String getPaymentStatus()      { return paymentStatus; }
    public BigDecimal getRemainingBalance(){ return remainingBalance; }
    public LocalDateTime getPaidAt()      { return paidAt; }
    public String getHospitalName()       { return hospitalName; }

    public void setReceiptNumber(String v)       { this.receiptNumber = v; }
    public void setTransactionId(String v)       { this.transactionId = v; }
    public void setPatientName(String v)         { this.patientName = v; }
    public void setPatientUid(String v)          { this.patientUid = v; }
    public void setInvoiceNumber(String v)       { this.invoiceNumber = v; }
    public void setInvoiceUid(String v)          { this.invoiceUid = v; }
    public void setAmountPaid(BigDecimal v)      { this.amountPaid = v; }
    public void setCurrency(String v)            { this.currency = v; }
    public void setPaymentMethod(String v)       { this.paymentMethod = v; }
    public void setPayerPhone(String v)          { this.payerPhone = v; }
    public void setPaymentStatus(String v)       { this.paymentStatus = v; }
    public void setRemainingBalance(BigDecimal v){ this.remainingBalance = v; }
    public void setPaidAt(LocalDateTime v)       { this.paidAt = v; }
    public void setHospitalName(String v)        { this.hospitalName = v; }
}
