package com.parkmeter.og;

import com.parkmeter.og.fragment.discovery.DiscoveryMethod;
import com.parkmeter.og.model.OfflineBehaviorSelection;
import com.parkmeter.og.model.Zone;

import org.jetbrains.annotations.NotNull;

/**
 * An `Activity` that should be notified when various navigation activities have been triggered
 */
public interface NavigationListener {
    /**
     * Notify the `Activity` that collecting payment method has been canceled
     */
    void onCancelCollectPaymentMethod();

    /**
     * Notify the `Activity` that collecting setup intent has been canceled
     */
    void onCancelCollectSetupIntent();

    /**
     * Notify the `Activity` that discovery should begin
     */
    void onRequestDiscovery(boolean isSimulated, DiscoveryMethod discoveryMethod);

    /**
     * Notify the `Activity` that discovery has been canceled
     */
    void onCancelDiscovery();

    /**
     * Notify the `Activity` that the [Reader] has been disconnected
     */
    void onDisconnectReader();

    /**
     * Notify the `Activity` that the user wants to exit the current workflow
     */
    void onRequestExitWorkflow();

    /**
     * Notify the `Activity` that the user wants to initiate a payment
     */
    void onRequestPayment(long amount, @NotNull String currency, boolean skipTipping, boolean extendedAuth, boolean incrementalAuth, OfflineBehaviorSelection offlineBehaviorSelection, String plateNumber, com.parkmeter.og.model.Zone selectedZone, com.parkmeter.og.model.Rate selectedRate, com.parkmeter.og.model.RateStep selectedRateStep);

    /**
     * Notify the `Activity` that a [Reader] has been connected
     */
    void onConnectReader();

    /**
     * Notify the `Activity` that the user wants to start the payment workflow
     */
    void onSelectPaymentWorkflow();

    /**
     * Notify the `Activity` that the user wants to start the workflow to save a card
     */
    void onRequestSaveCard();

    /**
     * Notify the `Activity` that the user wants to start the update reader workflow
     */
    void onSelectUpdateWorkflow();

    /**
     * Notify the `Activity` that the user wants to view the offline logs
     */
    void onSelectViewOfflineLogs();
    
    void onRequestViewLogs();

    /**
     * Notify the `Activity` that the user wants to enter license plate
     */
    void onRequestVehicleNumber();

    /**
     * Notify the `Activity` that vehicle number has been entered and user wants to proceed to connection
     */
    void onVehicleNumberEntered(String vehicleNumber);

    /**
     * Notify the `Activity` that user wants to open zones selection
     */
    void onRequestZonesSelection();

    /**
     * Notify the `Activity` that a zone has been selected
     */
    void onZoneSelected(Zone zone);

    /**
     * Notify the `Activity` that user wants to open settings
     */
    void onRequestSettings();

    /**
     * Notify the `Activity` that a rate has been selected
     */
    void onRateSelected(com.parkmeter.og.model.Rate rate, com.parkmeter.og.model.Zone zone, String vehicleNumber);

    /**
     * Notify the `Activity` that a rate step has been selected
     */
    void onRateStepSelected(com.parkmeter.og.model.RateStep rateStep, com.parkmeter.og.model.Rate rate, com.parkmeter.og.model.Zone zone, String vehicleNumber);

    /**
     * Notify the `Activity` that payment was successful and should show email receipt page
     */
    void onPaymentSuccessful(long amount, String parkedId, String transactionId);

    /**
     * Notify the `Activity` that user wants to return to home page
     */
    void onRequestReturnToHome();

}
