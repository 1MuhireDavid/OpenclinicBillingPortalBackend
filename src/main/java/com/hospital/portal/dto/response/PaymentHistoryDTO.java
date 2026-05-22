package com.hospital.portal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentHistoryDTO {

    private String transactionId;
    private String financialTransactionId;
    private String invoiceUid;
    private BigDecimal amount;
    private String currency;
    private String payerPhone;
    private String status;
    private String operator;
    private String payerMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentHistoryDTO() {}

    public String getTransactionId()          { return transactionId; }
    public String getFinancialTransactionId() { return financialTransactionId; }
    public String getInvoiceUid()             { return invoiceUid; }
    public BigDecimal getAmount()             { return amount; }
    public String getCurrency()               { return currency; }
    public String getPayerPhone()             { return payerPhone; }
    public String getStatus()                 { return status; }
    public String getOperator()               { return operator; }
    public String getPayerMessage()           { return payerMessage; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public LocalDateTime getUpdatedAt()       { return updatedAt; }

    public void setTransactionId(String v)          { this.transactionId = v; }
    public void setFinancialTransactionId(String v) { this.financialTransactionId = v; }
    public void setInvoiceUid(String v)             { this.invoiceUid = v; }
    public void setAmount(BigDecimal v)             { this.amount = v; }
    public void setCurrency(String v)               { this.currency = v; }
    public void setPayerPhone(String v)             { this.payerPhone = v; }
    public void setStatus(String v)                 { this.status = v; }
    public void setOperator(String v)               { this.operator = v; }
    public void setPayerMessage(String v)           { this.payerMessage = v; }
    public void setCreatedAt(LocalDateTime v)       { this.createdAt = v; }
    public void setUpdatedAt(LocalDateTime v)       { this.updatedAt = v; }
}
