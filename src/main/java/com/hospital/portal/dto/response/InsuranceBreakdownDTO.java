package com.hospital.portal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InsuranceBreakdownDTO {

    private String insuranceUid;
    private String insurarName;
    private String policyNumber;
    private String insuranceType;
    private String categoryLetter;
    private String memberName;
    private String memberEmployer;
    private double insuranceRatePercent;
    private double patientRatePercent;
    private String coverageLabel;        // e.g. "RSSB — 85 % covered"
    private LocalDate validFrom;
    private LocalDate validTo;
    private boolean active;

    // Applied to the current open bill
    private BigDecimal totalBillAmount;
    private BigDecimal insuranceCoversAmount;
    private BigDecimal patientOwesAmount;
    private BigDecimal patientAlreadyPaid;
    private BigDecimal patientRemainingBalance;

    public String getInsuranceUid()              { return insuranceUid; }
    public String getInsurarName()               { return insurarName; }
    public String getPolicyNumber()              { return policyNumber; }
    public String getInsuranceType()             { return insuranceType; }
    public String getCategoryLetter()            { return categoryLetter; }
    public String getMemberName()                { return memberName; }
    public String getMemberEmployer()            { return memberEmployer; }
    public double getInsuranceRatePercent()      { return insuranceRatePercent; }
    public double getPatientRatePercent()        { return patientRatePercent; }
    public String getCoverageLabel()             { return coverageLabel; }
    public LocalDate getValidFrom()              { return validFrom; }
    public LocalDate getValidTo()                { return validTo; }
    public boolean isActive()                    { return active; }
    public BigDecimal getTotalBillAmount()       { return totalBillAmount; }
    public BigDecimal getInsuranceCoversAmount() { return insuranceCoversAmount; }
    public BigDecimal getPatientOwesAmount()     { return patientOwesAmount; }
    public BigDecimal getPatientAlreadyPaid()    { return patientAlreadyPaid; }
    public BigDecimal getPatientRemainingBalance(){ return patientRemainingBalance; }

    public void setInsuranceUid(String v)              { this.insuranceUid = v; }
    public void setInsurarName(String v)               { this.insurarName = v; }
    public void setPolicyNumber(String v)              { this.policyNumber = v; }
    public void setInsuranceType(String v)             { this.insuranceType = v; }
    public void setCategoryLetter(String v)            { this.categoryLetter = v; }
    public void setMemberName(String v)                { this.memberName = v; }
    public void setMemberEmployer(String v)            { this.memberEmployer = v; }
    public void setInsuranceRatePercent(double v)      { this.insuranceRatePercent = v; }
    public void setPatientRatePercent(double v)        { this.patientRatePercent = v; }
    public void setCoverageLabel(String v)             { this.coverageLabel = v; }
    public void setValidFrom(LocalDate v)              { this.validFrom = v; }
    public void setValidTo(LocalDate v)                { this.validTo = v; }
    public void setActive(boolean v)                   { this.active = v; }
    public void setTotalBillAmount(BigDecimal v)       { this.totalBillAmount = v; }
    public void setInsuranceCoversAmount(BigDecimal v) { this.insuranceCoversAmount = v; }
    public void setPatientOwesAmount(BigDecimal v)     { this.patientOwesAmount = v; }
    public void setPatientAlreadyPaid(BigDecimal v)    { this.patientAlreadyPaid = v; }
    public void setPatientRemainingBalance(BigDecimal v){ this.patientRemainingBalance = v; }
}
