package com.hospital.portal.domain.model;

/**
 * Read-only projection of ocadmin.Admin + AdminPrivate.
 */
public class PatientProfile {

    private int personId;
    private String patientUid;     // composite: "{serverId}.{personId}"
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String gender;
    private String dateOfBirth;
    private String nationalId;     // natreg column

    public PatientProfile() {}

    public int getPersonId()        { return personId; }
    public String getPatientUid()   { return patientUid; }
    public String getFirstName()    { return firstName; }
    public String getLastName()     { return lastName; }
    public String getFullName()     { return fullName; }
    public String getPhoneNumber()  { return phoneNumber; }
    public String getEmail()        { return email; }
    public String getGender()       { return gender; }
    public String getDateOfBirth()  { return dateOfBirth; }
    public String getNationalId()   { return nationalId; }

    public void setPersonId(int personId)            { this.personId = personId; }
    public void setPatientUid(String patientUid)     { this.patientUid = patientUid; }
    public void setFirstName(String firstName)       { this.firstName = firstName; }
    public void setLastName(String lastName)         { this.lastName = lastName; }
    public void setFullName(String fullName)         { this.fullName = fullName; }
    public void setPhoneNumber(String phoneNumber)   { this.phoneNumber = phoneNumber; }
    public void setEmail(String email)               { this.email = email; }
    public void setGender(String gender)             { this.gender = gender; }
    public void setDateOfBirth(String dateOfBirth)   { this.dateOfBirth = dateOfBirth; }
    public void setNationalId(String nationalId)     { this.nationalId = nationalId; }
}
