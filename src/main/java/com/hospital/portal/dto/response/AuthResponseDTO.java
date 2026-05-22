package com.hospital.portal.dto.response;

public class AuthResponseDTO {

    private String token;
    private String patientUid;
    private String fullName;
    private String phoneNumber;
    private String message;
    // Only populated when app.otp.expose-in-response=true (dev mode)
    private String devOtp;

    public AuthResponseDTO() {}

    public String getToken()       { return token; }
    public String getPatientUid()  { return patientUid; }
    public String getFullName()    { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMessage()     { return message; }
    public String getDevOtp()      { return devOtp; }

    public void setToken(String token)           { this.token = token; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }
    public void setFullName(String fullName)     { this.fullName = fullName; }
    public void setPhoneNumber(String phone)     { this.phoneNumber = phone; }
    public void setMessage(String message)       { this.message = message; }
    public void setDevOtp(String devOtp)         { this.devOtp = devOtp; }
}
