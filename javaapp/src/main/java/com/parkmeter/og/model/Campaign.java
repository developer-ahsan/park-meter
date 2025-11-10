package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class Campaign {
    @SerializedName("_id")
    private String id;
    
    @SerializedName("compaign")
    private String campaign;
    
    @SerializedName("start_date")
    private String startDate;
    
    @SerializedName("end_date")
    private String endDate;
    
    @SerializedName("org")
    private String org;
    
    @SerializedName("zone")
    private String zone;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("updatedAt")
    private String updatedAt;
    
    // Getters
    public String getId() { return id; }
    public String getCampaign() { return campaign; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getOrg() { return org; }
    public String getZone() { return zone; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setCampaign(String campaign) { this.campaign = campaign; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setOrg(String org) { this.org = org; }
    public void setZone(String zone) { this.zone = zone; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
