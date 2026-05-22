package com.hospital.portal.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PaymentRequestDTO {

    @NotBlank(message = "Invoice UID is required")
    private String invoiceUid;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum payment amount is 1 RWF")
    private BigDecimal amount;

    @NotBlank(message = "Payer phone number is required")
    private String payerPhone;

    // Optional — defaults to "Hospital bill payment"
    private String payerMessage;

    public String getInvoiceUid()     { return invoiceUid; }
    public BigDecimal getAmount()     { return amount; }
    public String getPayerPhone()     { return payerPhone; }
    public String getPayerMessage()   { return payerMessage != null ? payerMessage : "Hospital bill payment"; }

    public void setInvoiceUid(String invoiceUid)   { this.invoiceUid = invoiceUid; }
    public void setAmount(BigDecimal amount)       { this.amount = amount; }
    public void setPayerPhone(String payerPhone)   { this.payerPhone = payerPhone; }
    public void setPayerMessage(String msg)        { this.payerMessage = msg; }
}
