package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class ConnectionTokenRequest {
    @SerializedName("org_id")
    private String orgId;

    public ConnectionTokenRequest(String orgId) {
        this.orgId = orgId;
    }

    // Getters
    public String getOrgId() { return orgId; }

    // Setters
    public void setOrgId(String orgId) { this.orgId = orgId; }
}
