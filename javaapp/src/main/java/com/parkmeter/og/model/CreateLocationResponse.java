package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class CreateLocationResponse {
    @SerializedName("id")
    private String id;
    
    @SerializedName("object")
    private String object;
    
    @SerializedName("address")
    private LocationAddress address;
    
    @SerializedName("display_name")
    private String displayName;
    
    @SerializedName("livemode")
    private boolean livemode;

    // Getters
    public String getId() { return id; }
    public String getObject() { return object; }
    public LocationAddress getAddress() { return address; }
    public String getDisplayName() { return displayName; }
    public boolean isLivemode() { return livemode; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setObject(String object) { this.object = object; }
    public void setAddress(LocationAddress address) { this.address = address; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setLivemode(boolean livemode) { this.livemode = livemode; }
}
