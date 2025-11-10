package com.parkmeter.og.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class RateStep implements Serializable {
    @SerializedName("time")
    private int time;
    
    @SerializedName("rate")
    private int rate;
    
    @SerializedName("time_desc")
    private String timeDesc;
    
    @SerializedName("time_diff")
    private String timeDiff;
    
    @SerializedName("day")
    private String day;
    
    @SerializedName("day_fr")
    private String dayFr;
    
    @SerializedName("service_fee")
    private int serviceFee;
    
    @SerializedName("total")
    private int total;
    
    @SerializedName("current_time")
    private String currentTime;
    
    public int getTime() {
        return time;
    }
    
    public void setTime(int time) {
        this.time = time;
    }
    
    public int getRate() {
        return rate;
    }
    
    public void setRate(int rate) {
        this.rate = rate;
    }
    
    public String getTimeDesc() {
        return timeDesc;
    }
    
    public void setTimeDesc(String timeDesc) {
        this.timeDesc = timeDesc;
    }
    
    public String getTimeDiff() {
        return timeDiff;
    }
    
    public void setTimeDiff(String timeDiff) {
        this.timeDiff = timeDiff;
    }
    
    public String getDay() {
        return day;
    }
    
    public void setDay(String day) {
        this.day = day;
    }
    
    public String getDayFr() {
        return dayFr;
    }
    
    public void setDayFr(String dayFr) {
        this.dayFr = dayFr;
    }
    
    public int getServiceFee() {
        return serviceFee;
    }
    
    public void setServiceFee(int serviceFee) {
        this.serviceFee = serviceFee;
    }
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public String getCurrentTime() {
        return currentTime;
    }
    
    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }
} 
