package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class GetRateStepsRequest {
    @SerializedName("id")
    private String id;
    
    @SerializedName("plate")
    private String plate;
    
    @SerializedName("rate_type")
    private int rateType;
    
    @SerializedName("qr_code")
    private boolean qrCode;
    
    @SerializedName("org")
    private String org;
    
    @SerializedName("time_zone")
    private String timeZone;
    
    @SerializedName("zone")
    private String zone;
    
    public GetRateStepsRequest(String id, String plate, int rateType, boolean qrCode, String org, String timeZone, String zone) {
        this.id = id;
        this.plate = plate;
        this.rateType = rateType;
        this.qrCode = qrCode;
        this.org = org;
        this.timeZone = timeZone;
        this.zone = zone;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getPlate() {
        return plate;
    }
    
    public void setPlate(String plate) {
        this.plate = plate;
    }
    
    public int getRateType() {
        return rateType;
    }
    
    public void setRateType(int rateType) {
        this.rateType = rateType;
    }
    
    public boolean isQrCode() {
        return qrCode;
    }
    
    public void setQrCode(boolean qrCode) {
        this.qrCode = qrCode;
    }
    
    public String getOrg() {
        return org;
    }
    
    public void setOrg(String org) {
        this.org = org;
    }
    
    public String getTimeZone() {
        return timeZone;
    }
    
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
    
    public String getZone() {
        return zone;
    }
    
    public void setZone(String zone) {
        this.zone = zone;
    }
} 
