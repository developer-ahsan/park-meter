package com.parkmeter.og.network;

import com.parkmeter.og.BuildConfig;
import com.parkmeter.og.StripeTerminalApplication;
import com.parkmeter.og.model.AppState;
import com.parkmeter.og.model.CancelPaymentIntentRequest;
import com.parkmeter.og.model.CapturePaymentIntentRequest;
import com.parkmeter.og.model.ConnectionToken;
import com.parkmeter.og.model.ConnectionTokenRequest;
import com.parkmeter.og.model.CreateLocationRequest;
import com.parkmeter.og.model.CreateLocationResponse;
import com.parkmeter.og.model.LocationAddress;
import com.stripe.stripeterminal.external.models.ConnectionTokenException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * The `ApiClient` is a singleton object used to make calls to our backend and return their results
 */
public class ApiClient {
    
    private static OkHttpClient createOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            // Fallback to default client if SSL setup fails
            return new OkHttpClient.Builder()
                    .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();
        }
    }
    
    private static final Retrofit mRetrofit = new Retrofit.Builder()
            .baseUrl(BuildConfig.EXAMPLE_BACKEND_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private static final BackendService mService = mRetrofit.create(BackendService.class);

    @NotNull
    public static String createConnectionToken(String organizationId) throws ConnectionTokenException {
        try {
            ConnectionTokenRequest request = new ConnectionTokenRequest(organizationId);
            final Response<ConnectionToken> result = mService.getConnectionToken(request).execute();
            if (result.isSuccessful() && result.body() != null) {
                return result.body().getSecret();
            } else {
                throw new ConnectionTokenException("Creating connection token failed");
            }
        } catch (IOException e) {
            throw new ConnectionTokenException("Creating connection token failed", e);
        }
    }

    @NotNull
    public static String createConnectionToken() throws ConnectionTokenException {
        // Get dynamic organization ID from app state
        AppState appState = StripeTerminalApplication.getInstance().getAppState();
        String organizationId = appState.getSelectedOrganizationId();
        
        if (organizationId == null || organizationId.isEmpty()) {
            throw new ConnectionTokenException("No organization ID selected");
        }
        
        return createConnectionToken(organizationId);
    }

    public static void createLocation(
            @NotNull String displayName,
            @NotNull String line1,
            String line2,
            String city,
            String postalCode,
            String state,
            @NotNull String country,
            @NotNull String organizationId
    ) throws Exception {
        try {
            LocationAddress address = new LocationAddress(line1, city, state, country, postalCode);
            CreateLocationRequest request = new CreateLocationRequest(displayName, address, organizationId);
            
            final Response<CreateLocationResponse> result = mService.createLocation(request).execute();
            if (!result.isSuccessful()) {
                throw new Exception("Creating location failed");
            }
        } catch (IOException e) {
            throw new Exception("Creating location failed", e);
        }
    }

    /**
     * Create location with callback for async operation
     */
    public static void createLocationAsync(String organizationId, Callback<CreateLocationResponse> callback) {
        LocationAddress address = new LocationAddress(
            "58 rue de Richelieu, J7B 1M2, Montreal",
            "Quebec", 
            "QC", 
            "CA", 
            "J7B 1M2"
        );
        CreateLocationRequest request = new CreateLocationRequest("CA", address, organizationId);
        mService.createLocation(request).enqueue(callback);
    }

    public static void capturePaymentIntent(@NotNull String id) throws IOException {
        // Get dynamic organization ID from app state
        AppState appState = StripeTerminalApplication.getInstance().getAppState();
        String organizationId = appState.getSelectedOrganizationId();
        
        if (organizationId == null || organizationId.isEmpty()) {
            throw new IOException("No organization ID selected");
        }
        
        CapturePaymentIntentRequest request = new CapturePaymentIntentRequest(id, organizationId);
        Response<Void> response = mService.capturePaymentIntent(request).execute();
        
        if (!response.isSuccessful()) {
            String errorBody = "";
            if (response.errorBody() != null) {
                try {
                    errorBody = response.errorBody().string();
                } catch (IOException e) {
                    errorBody = "Unable to read error body";
                }
            }
            throw new IOException("Capture payment intent failed with code: " + response.code() + ", error: " + errorBody);
        }
    }
    
    /**
     * Capture payment intent with retry mechanism
     */
    public static void capturePaymentIntentWithRetry(@NotNull String id, int maxRetries) throws IOException {
        IOException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                capturePaymentIntent(id);
                return; // Success, exit retry loop
            } catch (IOException e) {
                lastException = e;
                
                if (attempt < maxRetries) {
                    try {
                        // Wait before retry (exponential backoff)
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Capture interrupted", ie);
                    }
                }
            }
        }
        
        // All retries failed
        throw lastException != null ? lastException : new IOException("Capture failed after " + maxRetries + " attempts");
    }

    public static void cancelPaymentIntent(
            String id,
            Callback<Void> callback
    ) {
        // Get dynamic organization ID from app state
        AppState appState = StripeTerminalApplication.getInstance().getAppState();
        String organizationId = appState.getSelectedOrganizationId();
        
        if (organizationId == null || organizationId.isEmpty()) {
            // Create a simple failure callback
            callback.onFailure(null, new IOException("No organization ID selected"));
            return;
        }
        
        CancelPaymentIntentRequest request = new CancelPaymentIntentRequest(id, organizationId);
        mService.cancelPaymentIntent(request).enqueue(callback);
    }
}
