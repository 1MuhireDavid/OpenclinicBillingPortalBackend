package com.hospital.portal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BillingLineItemDTO {

    private String debetUid;
    private String serviceName;
    private int quantity;
    private BigDecimal totalAmount;
    private BigDecimal insuranceAmount;
    private BigDecimal patientAmount;
    private String currency;
    private LocalDateTime serviceDate;
    private boolean credited;

    public String getDebetUid()            { return debetUid; }
    public String getServiceName()         { return serviceName; }
    public int getQuantity()               { return quantity; }
    public BigDecimal getTotalAmount()     { return totalAmount; }
    public BigDecimal getInsuranceAmount() { return insuranceAmount; }
    public BigDecimal getPatientAmount()   { return patientAmount; }
    public String getCurrency()            { return currency; }
    public LocalDateTime getServiceDate()  { return serviceDate; }
    public boolean isCredited()            { return credited; }

    public void setDebetUid(String v)            { this.debetUid = v; }
    public void setServiceName(String v)         { this.serviceName = v; }
    public void setQuantity(int v)               { this.quantity = v; }
    public void setTotalAmount(BigDecimal v)     { this.totalAmount = v; }
    public void setInsuranceAmount(BigDecimal v) { this.insuranceAmount = v; }
    public void setPatientAmount(BigDecimal v)   { this.patientAmount = v; }
    public void setCurrency(String v)            { this.currency = v; }
    public void setServiceDate(LocalDateTime v)  { this.serviceDate = v; }
    public void setCredited(boolean v)           { this.credited = v; }
}
