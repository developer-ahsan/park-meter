package com.parkmeter.og.network;

import com.parkmeter.og.model.CancelPaymentIntentRequest;
import com.parkmeter.og.model.CapturePaymentIntentRequest;
import com.parkmeter.og.model.ConnectionToken;
import com.parkmeter.og.model.ConnectionTokenRequest;
import com.parkmeter.og.model.CreateLocationRequest;
import com.parkmeter.og.model.CreateLocationResponse;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import java.util.Map;

/**
 * The `BackendService` interface handles the two simple calls we need to make to our backend.
 */
public interface BackendService {

    /**
     * Get a connection token string from the backend with organization ID
     */
    @POST("connection_token")
    Call<ConnectionToken> getConnectionToken(@Body ConnectionTokenRequest request);

    /**
     * Create a new reader location with dynamic request body
     */
    @POST("create_location")
    Call<CreateLocationResponse> createLocation(@Body CreateLocationRequest request);

    /**
     * Capture a specific payment intent on our backend with organization ID
     */
    @POST("capture_payment_intent")
    Call<Void> capturePaymentIntent(@Body CapturePaymentIntentRequest request);

    /**
     * Cancel a specific payment intent on our backend with organization ID
     */
    @POST("cancel_payment_intent")
    Call<Void> cancelPaymentIntent(@Body CancelPaymentIntentRequest request);

    /**
     * Send email receipt to customer
     */
    @POST("email_receipt")
    Call<Void> sendEmailReceipt(@Body Map<String, Object> emailData);
}
