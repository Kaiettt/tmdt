package com.utephonehub.dto.request;

public class UpdateProfileRequest {
    private String fullName;
    private String phoneNumber;
    // ...getter/setter...
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
