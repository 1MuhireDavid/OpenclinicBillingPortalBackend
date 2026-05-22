package com.hospital.portal.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only projection of a single row from OC_PATIENTINVOICES.
 */
public class PatientBillSummary {

    private String invoiceUid;          // "{serverId}.{objectId}"
    private int serverId;
    private int objectId;
    private int invoiceNumber;
    private String patientUid;
    private String status;              // "open" | "closed"
    private BigDecimal patientBalance;  // OC_PATIENTINVOICE_BALANCE — patient's remaining owed
    private String invoiceNumber2;      // OC_PATIENTINVOICE_NUMBER (printed number)
    private LocalDateTime invoiceDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String comment;

    public PatientBillSummary() {}

    public String getInvoiceUid()         { return invoiceUid; }
    public int getServerId()              { return serverId; }
    public int getObjectId()              { return objectId; }
    public int getInvoiceNumber()         { return invoiceNumber; }
    public String getPatientUid()         { return patientUid; }
    public String getStatus()             { return status; }
    public BigDecimal getPatientBalance() { return patientBalance; }
    public String getInvoiceNumber2()     { return invoiceNumber2; }
    public LocalDateTime getInvoiceDate() { return invoiceDate; }
    public LocalDateTime getCreateTime()  { return createTime; }
    public LocalDateTime getUpdateTime()  { return updateTime; }
    public String getComment()            { return comment; }

    public void setInvoiceUid(String invoiceUid)          { this.invoiceUid = invoiceUid; }
    public void setServerId(int serverId)                 { this.serverId = serverId; }
    public void setObjectId(int objectId)                 { this.objectId = objectId; }
    public void setInvoiceNumber(int invoiceNumber)       { this.invoiceNumber = invoiceNumber; }
    public void setPatientUid(String patientUid)         { this.patientUid = patientUid; }
    public void setStatus(String status)                  { this.status = status; }
    public void setPatientBalance(BigDecimal b)           { this.patientBalance = b; }
    public void setInvoiceNumber2(String invoiceNumber2) { this.invoiceNumber2 = invoiceNumber2; }
    public void setInvoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; }
    public void setCreateTime(LocalDateTime createTime)  { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime)  { this.updateTime = updateTime; }
    public void setComment(String comment)                { this.comment = comment; }

    public boolean isOpen() { return "open".equalsIgnoreCase(status); }
}
