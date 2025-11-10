package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class EmailReceiptRequest {
    @SerializedName("email")
    private String email;
    
    @SerializedName("parking_id")
    private String parkingId;
    
    public EmailReceiptRequest(String email, String parkingId) {
        this.email = email;
        this.parkingId = parkingId;
    }
    
    // Getters
    public String getEmail() { return email; }
    public String getParkingId() { return parkingId; }
    
    // Setters
    public void setEmail(String email) { this.email = email; }
    public void setParkingId(String parkingId) { this.parkingId = parkingId; }
} 
