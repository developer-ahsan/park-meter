package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Zone implements Serializable {
    @SerializedName("_id")
    private String id;
    
    @SerializedName("zone_name")
    private String zoneName;
    
    @SerializedName("city_id")
    private City city;
    
    @SerializedName("zone_type")
    private int zoneType;
    
    @SerializedName("zone_code")
    private String zoneCode;
    
    @SerializedName("org")
    private Organization organization;
    
    @SerializedName("polygon")
    private List<Coordinate> polygon;
    
    @SerializedName("is_business_pass")
    private boolean isBusinessPass;
    
    @SerializedName("can_user_kick_out")
    private boolean canUserKickOut;
    
    @SerializedName("enable_extension")
    private boolean enableExtension;
    
    @SerializedName("is_plate_editable")
    private boolean isPlateEditable;
    
    @SerializedName("no_of_times_plate_can_edit")
    private int noOfTimesPlateCanEdit;
    
    @SerializedName("visitor_pass_time")
    private int visitorPassTime;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
    
    public int getZoneType() { return zoneType; }
    public void setZoneType(int zoneType) { this.zoneType = zoneType; }
    
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    
    public List<Coordinate> getPolygon() { return polygon; }
    public void setPolygon(List<Coordinate> polygon) { this.polygon = polygon; }
    
    public boolean isBusinessPass() { return isBusinessPass; }
    public void setBusinessPass(boolean businessPass) { isBusinessPass = businessPass; }
    
    public boolean isCanUserKickOut() { return canUserKickOut; }
    public void setCanUserKickOut(boolean canUserKickOut) { this.canUserKickOut = canUserKickOut; }
    
    public boolean isEnableExtension() { return enableExtension; }
    public void setEnableExtension(boolean enableExtension) { this.enableExtension = enableExtension; }
    
    public boolean isPlateEditable() { return isPlateEditable; }
    public void setPlateEditable(boolean plateEditable) { isPlateEditable = plateEditable; }
    
    public int getNoOfTimesPlateCanEdit() { return noOfTimesPlateCanEdit; }
    public void setNoOfTimesPlateCanEdit(int noOfTimesPlateCanEdit) { this.noOfTimesPlateCanEdit = noOfTimesPlateCanEdit; }
    
    public int getVisitorPassTime() { return visitorPassTime; }
    public void setVisitorPassTime(int visitorPassTime) { this.visitorPassTime = visitorPassTime; }

    public static class City implements Serializable {
        @SerializedName("_id")
        private String id;
        
        @SerializedName("city_name")
        private String cityName;
        
        @SerializedName("time_zone")
        private String timeZone;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }
        
        public String getTimeZone() { return timeZone; }
        public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    }

    public static class Organization implements Serializable {
        @SerializedName("_id")
        private String id;
        
        @SerializedName("org_name")
        private String orgName;
        
        @SerializedName("sub_domain")
        private String subDomain;
        
        @SerializedName("service_fee")
        private int serviceFee;
        
        @SerializedName("color")
        private String color;
        
        @SerializedName("payment_gateway")
        private String paymentGateway;
        
        @SerializedName("stripe_publishable_key")
        private String stripePublishableKey;
        
        @SerializedName("stripe_secret_key")
        private String stripeSecretKey;
        
        @SerializedName("logo")
        private String logo;
        
        @SerializedName("ssl_installed")
        private boolean sslInstalled;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getOrgName() { return orgName; }
        public void setOrgName(String orgName) { this.orgName = orgName; }
        
        public String getSubDomain() { return subDomain; }
        public void setSubDomain(String subDomain) { this.subDomain = subDomain; }
        
        public int getServiceFee() { return serviceFee; }
        public void setServiceFee(int serviceFee) { this.serviceFee = serviceFee; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public String getPaymentGateway() { return paymentGateway; }
        public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }
        
        public String getStripePublishableKey() { return stripePublishableKey; }
        public void setStripePublishableKey(String stripePublishableKey) { this.stripePublishableKey = stripePublishableKey; }
        
        public String getStripeSecretKey() { return stripeSecretKey; }
        public void setStripeSecretKey(String stripeSecretKey) { this.stripeSecretKey = stripeSecretKey; }
        
        public String getLogo() { return logo; }
        public void setLogo(String logo) { this.logo = logo; }
        
        public boolean isSslInstalled() { return sslInstalled; }
        public void setSslInstalled(boolean sslInstalled) { this.sslInstalled = sslInstalled; }
    }

    public static class Coordinate implements Serializable {
        @SerializedName("lat")
        private double latitude;
        
        @SerializedName("lng")
        private double longitude;

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
    }
} 
