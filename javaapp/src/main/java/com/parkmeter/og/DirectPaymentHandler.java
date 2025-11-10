package com.parkmeter.og;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.FragmentActivity;
import android.app.Activity;

import com.parkmeter.og.model.OfflineBehaviorSelection;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.model.Rate;
import com.parkmeter.og.model.RateStep;

import com.parkmeter.og.network.Park45ApiClient;
import com.parkmeter.og.network.Park45ApiService;
import com.parkmeter.og.model.ParkVehicleRequest;
import com.parkmeter.og.model.ParkVehicleResponse;
import com.parkmeter.og.utils.LiteralsHelper;
import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback;
import com.stripe.stripeterminal.external.models.CollectConfiguration;
import com.stripe.stripeterminal.external.models.CreateConfiguration;
import com.stripe.stripeterminal.external.models.PaymentIntent;
import com.stripe.stripeterminal.external.models.PaymentIntentParameters;
import com.stripe.stripeterminal.external.models.PaymentIntentStatus;
import com.stripe.stripeterminal.external.models.PaymentMethodOptionsParameters;
import com.stripe.stripeterminal.external.models.CardPresentParameters;
import com.stripe.stripeterminal.external.models.TerminalException;
import com.stripe.stripeterminal.external.models.TippingConfiguration;

import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;

/**
 * DirectPaymentHandler handles payment processing directly without showing the testing page
 * It bypasses the EventFragment and handles payment, cancellation, and timeout scenarios
 */
public class DirectPaymentHandler implements PaymentIntentCallback {
    
    private static final String TAG = "DirectPaymentHandler";
    private static final long PAYMENT_TIMEOUT_MS = 60000; // 60 seconds timeout
    
    private final FragmentActivity activity;
    private final long amount;
    private final String currency;
    private final boolean skipTipping;
    private final boolean extendedAuth;
    private final boolean incrementalAuth;
    private final OfflineBehaviorSelection offlineBehaviorSelection;
    private final String plateNumber;
    private final com.parkmeter.og.model.Zone selectedZone;
    private final com.parkmeter.og.model.Rate selectedRate;
    private final com.parkmeter.og.model.RateStep selectedRateStep;
    
    private PaymentIntent paymentIntent;
    private Cancelable collectTask;
    private Handler timeoutHandler;
    private boolean isPaymentCompleted = false;
    
    public DirectPaymentHandler(FragmentActivity activity, long amount, String currency, 
                               boolean skipTipping, boolean extendedAuth, boolean incrementalAuth, 
                               OfflineBehaviorSelection offlineBehaviorSelection, String plateNumber, 
                               com.parkmeter.og.model.Zone selectedZone, 
                               com.parkmeter.og.model.Rate selectedRate, 
                               com.parkmeter.og.model.RateStep selectedRateStep) {
        this.activity = activity;
        this.amount = amount;
        this.currency = currency;
        this.skipTipping = skipTipping;
        this.extendedAuth = extendedAuth;
        this.incrementalAuth = incrementalAuth;
        this.offlineBehaviorSelection = offlineBehaviorSelection;
        this.plateNumber = plateNumber;
        this.selectedZone = selectedZone;
        this.selectedRate = selectedRate;
        this.selectedRateStep = selectedRateStep;
    }
    
    public void startPayment() {
        // Start timeout handler
        startTimeoutHandler();
        
        // Create comprehensive metadata with parking details for Stripe dashboard
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        
        // Create a detailed description for the payment intent
        String description = createPaymentDescription();
        
        // Add detailed metadata for Stripe dashboard
        if (plateNumber != null && !plateNumber.trim().isEmpty()) {
            metadata.put("plate_number", plateNumber.trim());
        }
        if (selectedZone.getZoneName() != null && !selectedZone.getZoneName().trim().isEmpty()) {
            metadata.put("zone_name", selectedZone.getZoneName().trim());
        }
        if (selectedRate.getRateName() != null && !selectedRate.getRateName().trim().isEmpty()) {
            metadata.put("rate_name", selectedRate.getRateName().trim());
        }
        if (selectedRateStep.getTimeDesc() != null && !selectedRateStep.getTimeDesc().trim().isEmpty()) {
            metadata.put("time_description", selectedRateStep.getTimeDesc().trim());
        }
        if (selectedZone.getOrganization() != null && selectedZone.getOrganization().getOrgName() != null) {
            metadata.put("organization", selectedZone.getOrganization().getOrgName().trim());
        }
        
        // Add payment type and amount information
        metadata.put("payment_type", "parking");
        metadata.put("amount_cents", String.valueOf(amount));
        metadata.put("amount_dollars", String.format("%.2f", amount / 100.0));
        
        // Add timestamp
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
        metadata.put("payment_timestamp", sdf.format(new java.util.Date()));
        
        // Create payment intent parameters with metadata (matching EventFragment logic)
        CardPresentParameters.Builder cardPresentParametersBuilder = new CardPresentParameters.Builder();
        if (extendedAuth) {
            cardPresentParametersBuilder.setRequestExtendedAuthorization(true);
        }
        if (incrementalAuth) {
            cardPresentParametersBuilder.setRequestIncrementalAuthorizationSupport(true);
        }
        
        PaymentIntentParameters params = new PaymentIntentParameters.Builder()
                .setAmount(amount)
                .setCurrency(currency)
                .setDescription(description) // Add detailed description
                .setMetadata(metadata)
                .setPaymentMethodOptionsParameters(
                    new PaymentMethodOptionsParameters.Builder()
                        .setCardPresentParameters(cardPresentParametersBuilder.build())
                        .build()
                )
                .build();
        
        // Create configuration for offline behavior
        CreateConfiguration config = new CreateConfiguration(offlineBehaviorSelection.offlineBehavior);
        
        // Create payment intent
        Terminal.getInstance().createPaymentIntent(params, this, config);
    }
    
    /**
     * Create a detailed description for the payment intent that will show in Stripe dashboard
     */
    private String createPaymentDescription() {
        StringBuilder description = new StringBuilder();
        
        // Add plate number
        if (plateNumber != null && !plateNumber.trim().isEmpty()) {
            description.append("Plate: ").append(plateNumber.trim());
        }
        
        // Add zone name
        if (selectedZone.getZoneName() != null && !selectedZone.getZoneName().trim().isEmpty()) {
            if (description.length() > 0) description.append(" | ");
            description.append("Zone: ").append(selectedZone.getZoneName().trim());
        }
        
        // Add rate name
        if (selectedRate.getRateName() != null && !selectedRate.getRateName().trim().isEmpty()) {
            if (description.length() > 0) description.append(" | ");
            description.append("Rate: ").append(selectedRate.getRateName().trim());
        }
        
        // Add time description
        if (selectedRateStep.getTimeDesc() != null && !selectedRateStep.getTimeDesc().trim().isEmpty()) {
            if (description.length() > 0) description.append(" | ");
            description.append("Time: ").append(selectedRateStep.getTimeDesc().trim());
        }
        
        // Add amount
        if (description.length() > 0) description.append(" | ");
        description.append("Amount: $").append(String.format("%.2f", amount / 100.0)).append(" CAD");
        
        // Add organization name
        if (selectedZone.getOrganization() != null && selectedZone.getOrganization().getOrgName() != null) {
            if (description.length() > 0) description.append(" | ");
            description.append("Org: ").append(selectedZone.getOrganization().getOrgName().trim());
        }
        
        // Truncate if too long (Stripe has limits)
        String finalDescription = description.toString();
        if (finalDescription.length() > 200) {
            finalDescription = finalDescription.substring(0, 197) + "...";
        }
        
        return finalDescription;
    }
    
    private void startTimeoutHandler() {
        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutHandler.postDelayed(this::handlePaymentTimeout, PAYMENT_TIMEOUT_MS);
    }
    
    private void handlePaymentTimeout() {
        if (!isPaymentCompleted) {
            // Cancel any ongoing collection
            if (collectTask != null) {
                collectTask.cancel(new Callback() {
                    @Override
                    public void onSuccess() {
                        // Collection cancelled due to timeout
                    }
                    
                    @Override
                    public void onFailure(@NonNull TerminalException e) {
                        // Failed to cancel collection on timeout
                    }
                });
            }
            
            // Show timeout message and return to selection
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, LiteralsHelper.getText(activity, "payment_timeout"), Toast.LENGTH_SHORT).show();
                returnToSelectionScreen();
            });
        }
    }
    
    private void returnToSelectionScreen() {
        if (activity instanceof NavigationListener) {
            NavigationListener navigationListener = (NavigationListener) activity;
            navigationListener.onCancelCollectPaymentMethod();
        }
    }
    
    // PaymentIntentCallback implementation
    
    @Override
    public void onSuccess(@NonNull PaymentIntent paymentIntent) {
        this.paymentIntent = paymentIntent;
        // Payment intent created successfully: " + paymentIntent.getId());
        
        // Start collecting payment method
        startCollectingPaymentMethod();
    }
    
    @Override
    public void onFailure(@NonNull TerminalException e) {
        // Failed to create payment intent", e);
        handlePaymentFailure(LiteralsHelper.getText(activity, "failed_to_create_payment").replace("%1$s", e.getErrorMessage()));
    }
    
    @OptIn(markerClass = com.stripe.stripeterminal.external.InternalApi.class)
    private void startCollectingPaymentMethod() {
        // Starting payment method collection");
        // Payment intent ID: " + paymentIntent.getId());
        // Payment intent status: " + paymentIntent.getStatus());
        // Payment intent amount: " + paymentIntent.getAmount());
        // Payment intent currency: " + paymentIntent.getCurrency());
        
        CollectConfiguration collectConfig = new CollectConfiguration.Builder()
                .skipTipping(skipTipping)
                .setMoto(false) // DO_NOT_ENABLE_MOTO = false
                .setTippingConfiguration(new TippingConfiguration.Builder().build())
                .build();
        
        // Use a separate callback for payment method collection
        PaymentIntentCallback collectCallback = new PaymentIntentCallback() {
            @Override
            public void onSuccess(@NonNull PaymentIntent collectedPaymentIntent) {
                // Payment method collected successfully");
                
                // Confirm payment intent
                Terminal.getInstance().confirmPaymentIntent(collectedPaymentIntent, new PaymentIntentCallback() {
                    @Override
                    public void onSuccess(@NonNull PaymentIntent confirmedPaymentIntent) {
                        // Payment intent confirmed successfully");
                        handlePaymentSuccess();
                    }
                    
                    @Override
                    public void onFailure(@NonNull TerminalException e) {
                        // Failed to confirm payment intent", e);
                        handlePaymentFailure(LiteralsHelper.getText(activity, "payment_confirmation_failed").replace("%1$s", e.getErrorMessage()));
                    }
                });
            }
            
            @Override
            public void onFailure(@NonNull TerminalException e) {
                // Payment method collection failed", e);
                handlePaymentFailure(LiteralsHelper.getText(activity, "payment_collection_failed").replace("%1$s", e.getErrorMessage()));
            }
        };
        
        collectTask = Terminal.getInstance().collectPaymentMethod(paymentIntent, collectCallback, collectConfig);
    }
    
    private void handlePaymentFailure(String errorMessage) {
        // Payment failed: " + errorMessage);
        
        // Cancel timeout handler
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        
        activity.runOnUiThread(() -> {
            Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
            returnToSelectionScreen();
        });
    }
    
    private void handlePaymentSuccess() {
        // Payment completed successfully");
        // Payment intent status: " + paymentIntent.getStatus());
        // Payment intent ID: " + paymentIntent.getId());
        
        isPaymentCompleted = true;
        
        // Cancel timeout handler
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        
        // Capture payment intent immediately after confirmation (matching EventFragment logic)
        String paymentIntentId = paymentIntent.getId();
        if (paymentIntentId != null) {
            try {
                // Capturing payment intent: " + paymentIntentId);
                com.parkmeter.og.network.ApiClient.capturePaymentIntent(paymentIntentId);
                // Payment captured successfully");
                
                // Call park_vehicle API after successful capture
                callParkVehicleAPI(paymentIntentId);
                
            } catch (IOException e) {
                // Failed to capture payment", e);
                // Continue with park_vehicle API call even if capture fails
                callParkVehicleAPI(paymentIntentId);
            }
        } else {
            // Payment intent ID is null");
            // Still try to call park_vehicle API
            callParkVehicleAPI(null);
        }
    }
    

    
    /**
     * Manual capture method that can be called from UI if needed
     */
    public static void manualCapturePayment(String paymentIntentId, Activity activity) {
        if (activity == null) return;
        
        new Thread(() -> {
            try {
                com.parkmeter.og.network.ApiClient.capturePaymentIntent(paymentIntentId);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, LiteralsHelper.getText(activity, "payment_captured_successfully"), Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, LiteralsHelper.getText(activity, "capture_failed").replace("%1$s", e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * Check payment intent status for debugging
     */
    public static void logPaymentStatus(String paymentIntentId) {
        // Payment status debug information
    }
    
    private void callParkVehicleAPI(String paymentIntentId) {
        // Get current time and calculate end time
        java.util.Date currentTime = new java.util.Date();
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMMM dd'st' yyyy, hh:mm a", java.util.Locale.US);
        String fromTime = dateFormat.format(currentTime);
        
        // Calculate end time based on time description
        String toTime = calculateEndTime(currentTime, selectedRateStep.getTimeDesc());
        
        // Create park vehicle request (same format as zero amount case)
        ParkVehicleRequest request = new ParkVehicleRequest(
            paymentIntentId, // paymentMethod - payment intent ID for non-zero amount
            String.valueOf(selectedRateStep.getTotal()), // amount in cents
            plateNumber, // plate
            selectedZone.getId(), // zone ID
            selectedZone.getCity() != null ? selectedZone.getCity().getId() : "", // city ID
            fromTime, // from
            toTime, // to
            selectedRate.getId(), // rate ID
            selectedRateStep.getServiceFee(), // service_fee from rate step
            selectedZone.getOrganization().getId() // org ID
        );
        
        // Calling park_vehicle API after successful payment");
        // Request: " + request.getPlate() + " in zone: " + request.getZone() + " payment: " + request.getPaymentMethod());
        
        // Call park_vehicle API
        Park45ApiService apiService = Park45ApiClient.getInstance().getApiService();
                    apiService.parkVehicle(request).enqueue(new retrofit2.Callback<ParkVehicleResponse>() {
            @Override
            public void onResponse(Call<ParkVehicleResponse> call, Response<ParkVehicleResponse> response) {
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (response.isSuccessful() && response.body() != null) {
                            ParkVehicleResponse parkResponse = response.body();
                            // Park vehicle successful - _id: " + parkResponse.getId() + ", parking_id: " + parkResponse.getParkingId());
                            
                            // Navigate to email receipt with _id (as required by email API)
                            if (activity instanceof NavigationListener) {
                                NavigationListener navigationListener = (NavigationListener) activity;
                                navigationListener.onPaymentSuccessful(amount, parkResponse.getId(), paymentIntentId);
                            }
                        } else {
                            // Park vehicle API failed with code: " + response.code());
                            // Still navigate to email receipt with payment intent ID as fallback
                            if (activity instanceof NavigationListener) {
                                NavigationListener navigationListener = (NavigationListener) activity;
                                navigationListener.onPaymentSuccessful(amount, paymentIntentId, paymentIntentId);
                            }
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Call<ParkVehicleResponse> call, Throwable t) {
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        // Park vehicle API call failed
                        // Still navigate to email receipt with payment intent ID as fallback
                        if (activity instanceof NavigationListener) {
                            NavigationListener navigationListener = (NavigationListener) activity;
                            navigationListener.onPaymentSuccessful(amount, paymentIntentId, paymentIntentId);
                        }
                    });
                }
            }
        });
    }
    
    private String calculateEndTime(java.util.Date startTime, String timeDesc) {
        try {
            // Parse time description (e.g., "2 hours", "30 minutes")
            String[] parts = timeDesc.toLowerCase().split(" ");
            int duration = Integer.parseInt(parts[0]);
            String unit = parts[1];
            
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTime(startTime);
            
            if (unit.contains("hour")) {
                calendar.add(java.util.Calendar.HOUR_OF_DAY, duration);
            } else if (unit.contains("minute")) {
                calendar.add(java.util.Calendar.MINUTE, duration);
            }
            
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMMM dd'st' yyyy, hh:mm a", java.util.Locale.US);
            return dateFormat.format(calendar.getTime());
        } catch (Exception e) {
            // Error calculating end time
            // Fallback to current time + 1 hour
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTime(startTime);
            calendar.add(java.util.Calendar.HOUR_OF_DAY, 1);
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMMM dd'st' yyyy, hh:mm a", java.util.Locale.US);
            return dateFormat.format(calendar.getTime());
        }
    }
    

    
    public void cancelPayment() {
        // Payment cancelled by user");
        
        // Cancel timeout handler
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        
        // Cancel collection if ongoing
        if (collectTask != null) {
            collectTask.cancel(new Callback() {
                @Override
                public void onSuccess() {
                    // Payment collection cancelled successfully");
                }
                
                @Override
                public void onFailure(@NonNull TerminalException e) {
                    // Failed to cancel payment collection", e);
                }
            });
        }
        
        // Cancel payment intent if created
        if (paymentIntent != null) {
            Terminal.getInstance().cancelPaymentIntent(paymentIntent, new PaymentIntentCallback() {
                @Override
                public void onSuccess(@NonNull PaymentIntent cancelledPaymentIntent) {
                    // Payment intent cancelled successfully");
                }
                
                @Override
                public void onFailure(@NonNull TerminalException e) {
                    // Failed to cancel payment intent", e);
                }
            });
        }
        
        // Return to selection screen
        activity.runOnUiThread(() -> {
            Toast.makeText(activity, LiteralsHelper.getText(activity, "payment_cancelled"), Toast.LENGTH_SHORT).show();
            returnToSelectionScreen();
        });
    }
} 
