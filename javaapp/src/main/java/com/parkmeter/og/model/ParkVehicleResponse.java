package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class ParkVehicleResponse {
    @SerializedName("amount")
    private int amount;
    
    @SerializedName("service_fee")
    private String serviceFee;
    
    @SerializedName("parking_id")
    private int parkingId;
    
    @SerializedName("org")
    private String org;
    
    @SerializedName("city")
    private String city;
    
    @SerializedName("zone")
    private String zone;
    
    @SerializedName("rate")
    private String rate;
    
    @SerializedName("paymentMethod")
    private String paymentMethod;
    
    @SerializedName("plate")
    private String plate;
    
    @SerializedName("from")
    private String from;
    
    @SerializedName("to")
    private String to;
    
    @SerializedName("no_of_times_plate_edited")
    private int noOfTimesPlateEdited;
    
    @SerializedName("_id")
    private String id;
    
    @SerializedName("__v")
    private int version;
    
    // Getters
    public int getAmount() { return amount; }
    public String getServiceFee() { return serviceFee; }
    public int getParkingId() { return parkingId; }
    public String getOrg() { return org; }
    public String getCity() { return city; }
    public String getZone() { return zone; }
    public String getRate() { return rate; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPlate() { return plate; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public int getNoOfTimesPlateEdited() { return noOfTimesPlateEdited; }
    public String getId() { return id; }
    public int getVersion() { return version; }
    
    // Setters
    public void setAmount(int amount) { this.amount = amount; }
    public void setServiceFee(String serviceFee) { this.serviceFee = serviceFee; }
    public void setParkingId(int parkingId) { this.parkingId = parkingId; }
    public void setOrg(String org) { this.org = org; }
    public void setCity(String city) { this.city = city; }
    public void setZone(String zone) { this.zone = zone; }
    public void setRate(String rate) { this.rate = rate; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setPlate(String plate) { this.plate = plate; }
    public void setFrom(String from) { this.from = from; }
    public void setTo(String to) { this.to = to; }
    public void setNoOfTimesPlateEdited(int noOfTimesPlateEdited) { this.noOfTimesPlateEdited = noOfTimesPlateEdited; }
    public void setId(String id) { this.id = id; }
    public void setVersion(int version) { this.version = version; }
} 
