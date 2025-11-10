package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class GetRateByIdRequest {
    @SerializedName("id")
    private String id;
    
    @SerializedName("plate")
    private String plate;
    
    public GetRateByIdRequest(String id, String plate) {
        this.id = id;
        this.plate = plate;
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
} 
