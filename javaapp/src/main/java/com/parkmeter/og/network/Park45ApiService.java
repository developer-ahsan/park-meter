package com.parkmeter.og.network;

import com.parkmeter.og.model.GetRateByIdRequest;
import com.parkmeter.og.model.GetRateStepsRequest;
import com.parkmeter.og.model.GetZonesRequest;
import com.parkmeter.og.model.ParkingAvailableRequest;
import com.parkmeter.og.model.ParkingAvailableResponse;
import com.parkmeter.og.model.Rate;
import com.parkmeter.og.model.RateStep;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.model.ParkVehicleRequest;
import com.parkmeter.og.model.ParkVehicleResponse;
import com.parkmeter.og.model.EmailReceiptRequest;
import com.parkmeter.og.model.Campaign;
import com.parkmeter.og.model.CampaignRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface Park45ApiService {
    
    @POST("getZones")
    Call<List<Zone>> getZones(@Body GetZonesRequest requestBody);
    
    @POST("getRateById")
    Call<List<Rate>> getRateById(@Body GetRateByIdRequest requestBody);
    
    @POST("getRateSteps")
    Call<List<RateStep>> getRateSteps(@Body GetRateStepsRequest requestBody);
    
    @POST("parking_available")
    Call<ParkingAvailableResponse> checkParkingAvailable(@Body ParkingAvailableRequest requestBody);
    
    @POST("park_vehicle")
    Call<ParkVehicleResponse> parkVehicle(@Body ParkVehicleRequest requestBody);
    
    @POST("emailReciept")
    Call<Void> emailReceipt(@Body EmailReceiptRequest requestBody);
    
    @POST("current_compaign")
    Call<List<Campaign>> getCurrentCampaign(@Body CampaignRequest requestBody);
} 
