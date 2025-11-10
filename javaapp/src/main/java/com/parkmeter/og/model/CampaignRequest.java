package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class CampaignRequest {
    @SerializedName("zone_id")
    private String zoneId;
    
    public CampaignRequest(String zoneId) {
        this.zoneId = zoneId;
    }
    
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
}
