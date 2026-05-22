package com.hospital.portal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponseDTO {

    private String transactionId;
    private String invoiceUid;
    private BigDecimal amount;
    private String currency;
    private String status;              // PENDING | SUCCESSFUL | FAILED
    private String message;
    private BigDecimal remainingBalance;
    private LocalDateTime timestamp;

    public PaymentResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public String getTransactionId()      { return transactionId; }
    public String getInvoiceUid()         { return invoiceUid; }
    public BigDecimal getAmount()         { return amount; }
    public String getCurrency()           { return currency; }
    public String getStatus()             { return status; }
    public String getMessage()            { return message; }
    public BigDecimal getRemainingBalance(){ return remainingBalance; }
    public LocalDateTime getTimestamp()   { return timestamp; }

    public void setTransactionId(String v)      { this.transactionId = v; }
    public void setInvoiceUid(String v)         { this.invoiceUid = v; }
    public void setAmount(BigDecimal v)         { this.amount = v; }
    public void setCurrency(String v)           { this.currency = v; }
    public void setStatus(String v)             { this.status = v; }
    public void setMessage(String v)            { this.message = v; }
    public void setRemainingBalance(BigDecimal v){ this.remainingBalance = v; }
    public void setTimestamp(LocalDateTime v)   { this.timestamp = v; }
}
