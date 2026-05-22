package com.hospital.portal.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One charge line from OC_DEBETS joined with OC_PRESTATIONS for the service name.
 */
public class InvoiceLineItem {

    private String invoiceUid;
    private String debetUid;
    private String prestationUid;
    private String serviceName;
    private int quantity;
    private BigDecimal totalAmount;
    private BigDecimal insuranceAmount;
    private BigDecimal patientAmount;
    private LocalDateTime serviceDate;
    private boolean credited;

    public InvoiceLineItem() {}

    public String getInvoiceUid()         { return invoiceUid; }
    public String getDebetUid()           { return debetUid; }
    public String getPrestationUid()      { return prestationUid; }
    public String getServiceName()        { return serviceName; }
    public int getQuantity()              { return quantity; }
    public BigDecimal getTotalAmount()    { return totalAmount; }
    public BigDecimal getInsuranceAmount(){ return insuranceAmount; }
    public BigDecimal getPatientAmount()  { return patientAmount; }
    public LocalDateTime getServiceDate() { return serviceDate; }
    public boolean isCredited()           { return credited; }

    public void setInvoiceUid(String v)          { this.invoiceUid = v; }
    public void setDebetUid(String v)            { this.debetUid = v; }
    public void setPrestationUid(String v)       { this.prestationUid = v; }
    public void setServiceName(String v)         { this.serviceName = v; }
    public void setQuantity(int v)               { this.quantity = v; }
    public void setTotalAmount(BigDecimal v)     { this.totalAmount = v; }
    public void setInsuranceAmount(BigDecimal v) { this.insuranceAmount = v; }
    public void setPatientAmount(BigDecimal v)   { this.patientAmount = v; }
    public void setServiceDate(LocalDateTime v)  { this.serviceDate = v; }
    public void setCredited(boolean v)           { this.credited = v; }
}
