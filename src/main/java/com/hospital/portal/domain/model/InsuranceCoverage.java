package com.hospital.portal.domain.model;

import java.time.LocalDate;

/**
 * Read-only projection of OC_INSURANCES joined with OC_INSURARS.
 */
public class InsuranceCoverage {

    private String insuranceUid;
    private String patientUid;
    private String insurarUid;
    private String insurarName;
    private String insuranceNr;       // policy number
    private String type;
    private String member;
    private String memberEmployer;
    private String membercategory;
    private String insuranceCategoryLetter;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;

    // Live rate from OC_INSURANCECATEGORIES (null if not found — falls back to name matching)
    private Double dbPatientSharePercent;

    // Calculated by InsuranceCalculationService
    private double insuranceRatePercent;  // e.g. 85.0 for RSSB
    private double patientRatePercent;    // e.g. 15.0 for RSSB

    public InsuranceCoverage() {}

    public String getInsuranceUid()            { return insuranceUid; }
    public String getPatientUid()              { return patientUid; }
    public String getInsurarUid()              { return insurarUid; }
    public String getInsurarName()             { return insurarName; }
    public String getInsuranceNr()             { return insuranceNr; }
    public String getType()                    { return type; }
    public String getMember()                  { return member; }
    public String getMemberEmployer()          { return memberEmployer; }
    public String getMembercategory()          { return membercategory; }
    public String getInsuranceCategoryLetter() { return insuranceCategoryLetter; }
    public String getStatus()                  { return status; }
    public LocalDate getStartDate()            { return startDate; }
    public LocalDate getEndDate()              { return endDate; }
    public double getInsuranceRatePercent()    { return insuranceRatePercent; }
    public double getPatientRatePercent()      { return patientRatePercent; }

    public void setInsuranceUid(String v)              { this.insuranceUid = v; }
    public void setPatientUid(String v)                { this.patientUid = v; }
    public void setInsurarUid(String v)                { this.insurarUid = v; }
    public void setInsurarName(String v)               { this.insurarName = v; }
    public void setInsuranceNr(String v)               { this.insuranceNr = v; }
    public void setType(String v)                      { this.type = v; }
    public void setMember(String v)                    { this.member = v; }
    public void setMemberEmployer(String v)            { this.memberEmployer = v; }
    public void setMembercategory(String v)            { this.membercategory = v; }
    public void setInsuranceCategoryLetter(String v)   { this.insuranceCategoryLetter = v; }
    public void setStatus(String v)                    { this.status = v; }
    public void setStartDate(LocalDate v)              { this.startDate = v; }
    public void setEndDate(LocalDate v)                { this.endDate = v; }
    public Double getDbPatientSharePercent()             { return dbPatientSharePercent; }
    public void setDbPatientSharePercent(Double v)     { this.dbPatientSharePercent = v; }
    public void setInsuranceRatePercent(double v)      { this.insuranceRatePercent = v; }
    public void setPatientRatePercent(double v)        { this.patientRatePercent = v; }

    public boolean isActive() {
        return "active".equalsIgnoreCase(status)
                || (endDate == null || !endDate.isBefore(LocalDate.now()));
    }
}
