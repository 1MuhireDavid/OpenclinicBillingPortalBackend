package com.hospital.portal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoginRequestDTO {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+?250|0)?[0-9]{9}$", message = "Enter a valid Rwandan phone number")
    private String phoneNumber;

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
