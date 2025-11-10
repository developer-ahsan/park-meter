package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class CancelPaymentIntentRequest {
    @SerializedName("payment_intent_id")
    private String paymentIntentId;
    
    @SerializedName("org_id")
    private String orgId;

    public CancelPaymentIntentRequest(String paymentIntentId, String orgId) {
        this.paymentIntentId = paymentIntentId;
        this.orgId = orgId;
    }

    // Getters
    public String getPaymentIntentId() { return paymentIntentId; }
    public String getOrgId() { return orgId; }

    // Setters
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
} 
