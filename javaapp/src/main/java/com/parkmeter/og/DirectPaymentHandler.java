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
import com.parkmeter.og.fragment.TapDeviceDialogFragment;

import com.parkmeter.og.utils.LiteralsHelper;
import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback;
import com.stripe.stripeterminal.external.models.CreateConfiguration;
import com.stripe.stripeterminal.external.models.PaymentIntent;
import com.stripe.stripeterminal.external.models.PaymentIntentParameters;
import com.stripe.stripeterminal.external.models.PaymentIntentStatus;
import com.stripe.stripeterminal.external.models.PaymentMethodOptionsParameters;
import com.stripe.stripeterminal.external.models.CardPresentParameters;
import com.stripe.stripeterminal.external.models.TerminalException;

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
    private String parkingId; // Store parking_id to pass to EmailReceiptFragment
    
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
        // Show tap device dialog first
        showTapDeviceDialog();
    }
    
    private void showTapDeviceDialog() {
        // Create and show the tap device dialog
        TapDeviceDialogFragment dialog = TapDeviceDialogFragment.newInstance(
            selectedZone,
            selectedRate,
            selectedRateStep,
            amount
        );
        
        // Set dismiss listener to proceed with payment intent creation
        dialog.setOnDismissListener(new TapDeviceDialogFragment.OnDismissListener() {
            @Override
            public void onDialogDismissed() {
                // Dialog auto-dismisses after 5 seconds, immediately proceed with Stripe payment intent
                createPaymentIntent();
            }
        });
        
        // Show dialog if fragment manager is available
        if (activity instanceof androidx.fragment.app.FragmentActivity) {
            dialog.show(((androidx.fragment.app.FragmentActivity) activity).getSupportFragmentManager(), "TapDeviceDialog");
        } else {
            // Fallback: if dialog can't be shown, proceed with payment intent immediately
            createPaymentIntent();
        }
    }
    
    private void createPaymentIntent() {
        // Start timeout handler
        startTimeoutHandler();
        
        // Create comprehensive metadata with parking details for Stripe dashboard
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        
        // Create a detailed description for the payment intent
        String description = createPaymentDescription();
        
        // Get current time and calculate end time for metadata
        java.util.Date currentTime = new java.util.Date();
        
        // Format dates in ISO 8601 format with timezone: '2025-12-24T15:19:00-05:00'
        String fromTime = formatISO8601WithTimezone(currentTime);
        java.util.Date endTime = calculateEndTimeDate(currentTime, selectedRateStep.getTimeDesc());
        String toTime = formatISO8601WithTimezone(endTime);
        
        // Generate parking_id (6-digit random number between 100000 and 999999)
        int parkingIdInt = (int)(100000 + Math.random() * 900000);
        parkingId = String.valueOf(parkingIdInt); // Store as instance variable
        
        // Add all park_vehicle fields to metadata
        // amount (in cents as string)
        metadata.put("amount", String.valueOf(selectedRateStep.getTotal()));
        
        // plate
        if (plateNumber != null && !plateNumber.trim().isEmpty()) {
            metadata.put("plate", plateNumber.trim());
        }
        
        // zone (zone ID)
        if (selectedZone.getId() != null) {
            metadata.put("zone", selectedZone.getId());
        }
        
        // city (city ID)
        if (selectedZone.getCity() != null && selectedZone.getCity().getId() != null) {
            metadata.put("city", selectedZone.getCity().getId());
        } else {
            metadata.put("city", "");
        }
        
        // from (start time in ISO 8601 format with timezone)
        metadata.put("from", fromTime);
        
        // to (end time in ISO 8601 format with timezone)
        metadata.put("to", toTime);
        
        // rate (rate ID)
        if (selectedRate.getId() != null) {
            metadata.put("rate", selectedRate.getId());
        }
        
        // service_fee
        metadata.put("service_fee", String.valueOf(selectedRateStep.getServiceFee()));
        
        // org (organization ID)
        if (selectedZone.getOrganization() != null && selectedZone.getOrganization().getId() != null) {
            metadata.put("org", selectedZone.getOrganization().getId());
        }
        
        // source
        metadata.put("source", "meter");
        
        // parking_id (as number string)
        metadata.put("parking_id", parkingId);
        
        
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
     * Format: {plate} is parked from {from} to {to} in {zone_name}, {org_name} - meter
     */
    private String createPaymentDescription() {
        // Get current time and calculate end time
        java.util.Date currentTime = new java.util.Date();
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMMM dd'st' yyyy, hh:mm a", java.util.Locale.US);
        String fromTime = dateFormat.format(currentTime);
        
        // Calculate end time based on time description
        String toTime = calculateEndTime(currentTime, selectedRateStep.getTimeDesc());
        
        StringBuilder description = new StringBuilder();
        
        // Add plate number (required field)
        String plate = (plateNumber != null && !plateNumber.trim().isEmpty()) ? plateNumber.trim() : "Vehicle";
        description.append(plate);
        
        description.append(" is parked from ");
        description.append(fromTime);
        description.append(" to ");
        description.append(toTime);
        description.append(" in ");
        
        // Add zone name
        String zoneName = "";
        if (selectedZone.getZoneName() != null && !selectedZone.getZoneName().trim().isEmpty()) {
            zoneName = selectedZone.getZoneName().trim();
        }
        description.append(zoneName);
        
        description.append(", ");
        
        // Add organization name
        String orgName = "";
        if (selectedZone.getOrganization() != null && selectedZone.getOrganization().getOrgName() != null) {
            orgName = selectedZone.getOrganization().getOrgName().trim();
        }
        description.append(orgName);
        
        description.append(" - meter");
        
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
        
        // v5.0.0: simplified collectPaymentMethod signature (no CollectConfiguration)
        collectTask = Terminal.getInstance().collectPaymentMethod(paymentIntent, collectCallback);
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
            } catch (IOException e) {
                // Failed to capture payment", e);
            }
        }
        
        // Navigate to success screen - park_vehicle data is now in Stripe metadata
        // Pass the parking_id (same as in metadata) to EmailReceiptFragment
        if (activity instanceof NavigationListener) {
            NavigationListener navigationListener = (NavigationListener) activity;
            String parkIdToPass = (parkingId != null && !parkingId.isEmpty()) ? parkingId : (paymentIntentId != null ? paymentIntentId : "");
            navigationListener.onPaymentSuccessful(amount, parkIdToPass, paymentIntentId != null ? paymentIntentId : "");
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
    
    // Removed: callParkVehicleAPI method - park_vehicle data is now stored in Stripe metadata
    // All park_vehicle fields (plate, zone, city, from, to, rate, service_fee, org, source, parking_id)
    // are now included in the payment intent metadata created in startPayment()
    
    /**
     * Format date in ISO 8601 format with timezone: '2025-12-24T15:19:00-05:00'
     */
    private String formatISO8601WithTimezone(java.util.Date date) {
        java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US);
        return isoFormat.format(date);
    }
    
    /**
     * Calculate end time as Date object (for ISO 8601 formatting)
     */
    private java.util.Date calculateEndTimeDate(java.util.Date startTime, String timeDesc) {
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
            
            return calendar.getTime();
        } catch (Exception e) {
            // Error calculating end time
            // Fallback to current time + 1 hour
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTime(startTime);
            calendar.add(java.util.Calendar.HOUR_OF_DAY, 1);
            return calendar.getTime();
        }
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
