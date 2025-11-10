package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for getZones API call
 * Add any required fields for the POST request body
 */
public class GetZonesRequest {
    
    // Add any required fields for the API request
    // For example, if the API requires specific parameters:
    
    @SerializedName("limit")
    private int limit = 100; // Default limit
    
    @SerializedName("offset")
    private int offset = 0; // Default offset
    
    // Add more fields as required by your API
    
    public GetZonesRequest() {
        // Default constructor
    }
    
    public GetZonesRequest(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
    }
    
    // Getters and setters
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    @Override
    public String toString() {
        return "GetZonesRequest{" +
                "limit=" + limit +
                ", offset=" + offset +
                '}';
    }
} 
