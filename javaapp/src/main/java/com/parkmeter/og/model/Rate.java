package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Rate implements Serializable {
    @SerializedName("enable_custom_rate")
    private boolean enableCustomRate;
    
    @SerializedName("is_whitelist")
    private boolean isWhitelist;
    
    @SerializedName("_id")
    private String id;
    
    @SerializedName("rate_name")
    private String rateName;
    
    @SerializedName("zone_id")
    private String zoneId;
    
    @SerializedName("rate_type")
    private int rateType;
    
    @SerializedName("is_visitor_pass")
    private boolean isVisitorPass;
    
    @SerializedName("qr_code")
    private boolean qrCode;
    
    public boolean isEnableCustomRate() {
        return enableCustomRate;
    }
    
    public void setEnableCustomRate(boolean enableCustomRate) {
        this.enableCustomRate = enableCustomRate;
    }
    
    public boolean isWhitelist() {
        return isWhitelist;
    }
    
    public void setWhitelist(boolean whitelist) {
        isWhitelist = whitelist;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getRateName() {
        return rateName;
    }
    
    public void setRateName(String rateName) {
        this.rateName = rateName;
    }
    
    public String getZoneId() {
        return zoneId;
    }
    
    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
    
    public int getRateType() {
        return rateType;
    }
    
    public void setRateType(int rateType) {
        this.rateType = rateType;
    }
    
    public boolean isVisitorPass() {
        return isVisitorPass;
    }
    
    public void setVisitorPass(boolean visitorPass) {
        isVisitorPass = visitorPass;
    }
    
    public boolean isQrCode() {
        return qrCode;
    }
    
    public void setQrCode(boolean qrCode) {
        this.qrCode = qrCode;
    }
} 
