package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class ParkingAvailableRequest {
    @SerializedName("plate")
    private String plate;
    
    @SerializedName("zone")
    private String zone;
    
    @SerializedName("city")
    private String city;
    
    @SerializedName("rate")
    private String rate;
    
    @SerializedName("org")
    private String org;
    
    public ParkingAvailableRequest(String plate, String zone, String city, String rate, String org) {
        this.plate = plate;
        this.zone = zone;
        this.city = city;
        this.rate = rate;
        this.org = org;
    }
    
    public String getPlate() {
        return plate;
    }
    
    public void setPlate(String plate) {
        this.plate = plate;
    }
    
    public String getZone() {
        return zone;
    }
    
    public void setZone(String zone) {
        this.zone = zone;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getRate() {
        return rate;
    }
    
    public void setRate(String rate) {
        this.rate = rate;
    }
    
    public String getOrg() {
        return org;
    }
    
    public void setOrg(String org) {
        this.org = org;
    }
} 
