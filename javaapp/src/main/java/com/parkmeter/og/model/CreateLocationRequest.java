package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class CreateLocationRequest {
    @SerializedName("display_name")
    private String displayName;
    
    @SerializedName("address")
    private LocationAddress address;
    
    @SerializedName("org_id")
    private String orgId;

    public CreateLocationRequest(String displayName, LocationAddress address, String orgId) {
        this.displayName = displayName;
        this.address = address;
        this.orgId = orgId;
    }

    // Getters
    public String getDisplayName() { return displayName; }
    public LocationAddress getAddress() { return address; }
    public String getOrgId() { return orgId; }

    // Setters
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setAddress(LocationAddress address) { this.address = address; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
}
