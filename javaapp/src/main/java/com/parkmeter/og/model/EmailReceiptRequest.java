package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class EmailReceiptRequest {
    @SerializedName("email")
    private String email;
    
    @SerializedName("transaction_id")
    private String transactionId;
    
    public EmailReceiptRequest(String email, String transactionId) {
        this.email = email;
        this.transactionId = transactionId;
    }
    
    // Getters
    public String getEmail() { return email; }
    public String getTransactionId() { return transactionId; }
    
    // Setters
    public void setEmail(String email) { this.email = email; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
} 
