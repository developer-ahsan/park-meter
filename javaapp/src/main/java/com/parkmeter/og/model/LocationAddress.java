package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class LocationAddress {
    @SerializedName("line1")
    private String line1;
    
    @SerializedName("city")
    private String city;
    
    @SerializedName("state")
    private String state;
    
    @SerializedName("country")
    private String country;
    
    @SerializedName("postal_code")
    private String postalCode;

    public LocationAddress(String line1, String city, String state, String country, String postalCode) {
        this.line1 = line1;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postalCode = postalCode;
    }

    // Getters
    public String getLine1() { return line1; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountry() { return country; }
    public String getPostalCode() { return postalCode; }

    // Setters
    public void setLine1(String line1) { this.line1 = line1; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setCountry(String country) { this.country = country; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
}
