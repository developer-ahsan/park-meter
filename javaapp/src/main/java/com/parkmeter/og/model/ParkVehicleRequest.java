package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

public class ParkVehicleRequest {
    @SerializedName("paymentMethod")
    private String paymentMethod;
    
    @SerializedName("amount")
    private String amount;
    
    @SerializedName("plate")
    private String plate;
    
    @SerializedName("zone")
    private String zone;
    
    @SerializedName("city")
    private String city;
    
    @SerializedName("from")
    private String from;
    
    @SerializedName("to")
    private String to;
    
    @SerializedName("rate")
    private String rate;
    
    @SerializedName("service_fee")
    private int serviceFee;
    
    @SerializedName("org")
    private String org;
    
    @SerializedName("source")
    private String source;
    
    @SerializedName("parking_id")
    private String parkingId;
    
    public ParkVehicleRequest(String paymentMethod, String amount, String plate, String zone, 
                            String city, String from, String to, String rate, int serviceFee, String org,
                            String source, String parkingId) {
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.plate = plate;
        this.zone = zone;
        this.city = city;
        this.from = from;
        this.to = to;
        this.rate = rate;
        this.serviceFee = serviceFee;
        this.org = org;
        this.source = source;
        this.parkingId = parkingId;
    }
    
    // Getters
    public String getPaymentMethod() { return paymentMethod; }
    public String getAmount() { return amount; }
    public String getPlate() { return plate; }
    public String getZone() { return zone; }
    public String getCity() { return city; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getRate() { return rate; }
    public int getServiceFee() { return serviceFee; }
    public String getOrg() { return org; }
    public String getSource() { return source; }
    public String getParkingId() { return parkingId; }
    
    // Setters
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setPlate(String plate) { this.plate = plate; }
    public void setZone(String zone) { this.zone = zone; }
    public void setCity(String city) { this.city = city; }
    public void setFrom(String from) { this.from = from; }
    public void setTo(String to) { this.to = to; }
    public void setRate(String rate) { this.rate = rate; }
    public void setServiceFee(int serviceFee) { this.serviceFee = serviceFee; }
    public void setOrg(String org) { this.org = org; }
    public void setSource(String source) { this.source = source; }
    public void setParkingId(String parkingId) { this.parkingId = parkingId; }
} 
