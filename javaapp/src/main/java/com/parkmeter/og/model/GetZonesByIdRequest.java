package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class GetZonesByIdRequest {
    @SerializedName("id")
    private String id;

    public GetZonesByIdRequest(String id) {
        this.id = id;
    }

    public String getId() { return id; }
}
