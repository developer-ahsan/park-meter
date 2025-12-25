package com.parkmeter.og;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.parkmeter.og.fragment.ConnectedReaderFragment;
import com.parkmeter.og.fragment.EmailReceiptFragment;
import com.parkmeter.og.fragment.HomeFragment;
import com.parkmeter.og.fragment.PaymentFragment;
import com.parkmeter.og.fragment.SettingsFragment;
import com.parkmeter.og.fragment.TerminalFragment;
import com.parkmeter.og.fragment.UpdateReaderFragment;
import com.parkmeter.og.fragment.VehicleNumberFragment;
import com.parkmeter.og.fragment.ZonesFragment;
import com.parkmeter.og.fragment.discovery.DiscoveryFragment;
import com.parkmeter.og.fragment.discovery.DiscoveryMethod;
import com.parkmeter.og.fragment.event.EventFragment;

import com.parkmeter.og.fragment.offline.OfflinePaymentsLogFragment;
import com.parkmeter.og.fragment.LogViewerFragment;
import com.parkmeter.og.fragment.RateSelectionFragment;
import com.parkmeter.og.model.AppState;
import com.parkmeter.og.model.Rate;
import com.parkmeter.og.model.RateStep;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.LiteralsHelper;
import com.parkmeter.og.utils.SharedPreferencesManager;
import com.parkmeter.og.utils.LanguageManager;
import com.parkmeter.og.model.OfflineBehaviorSelection;
import com.parkmeter.og.network.TokenProvider;
import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.OfflineMode;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.callable.InternetReaderListener;
import com.stripe.stripeterminal.external.callable.MobileReaderListener;
import com.stripe.stripeterminal.external.callable.TapToPayReaderListener;
import com.stripe.stripeterminal.external.models.BatteryStatus;
import com.stripe.stripeterminal.external.models.ConnectionStatus;
import com.stripe.stripeterminal.external.models.DisconnectReason;

import com.stripe.stripeterminal.external.models.Reader;
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage;
import com.stripe.stripeterminal.external.models.ReaderEvent;
import com.stripe.stripeterminal.external.models.ReaderInputOptions;
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate;
import com.stripe.stripeterminal.external.models.TerminalException;
import com.stripe.stripeterminal.log.LogLevel;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@OptIn(markerClass = OfflineMode.class)
public class MainActivity extends AppCompatActivity implements
        NavigationListener,
        MobileReaderListener,
        TapToPayReaderListener,
        InternetReaderListener {

    public final OfflineModeHandler offlineModeHandler = new OfflineModeHandler(message -> {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    });

    private AppState appState;
    private SharedPreferencesManager sharedPreferencesManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("MainActivity", "========== MainActivity.onCreate() START ==========");
        
        // Switch from splash screen theme to normal theme
        setTheme(R.style.Theme_Example);
        
        super.onCreate(savedInstanceState);
        
        // Initialize Terminal SDK (per Stripe support recommendation)
        if (!Terminal.isInitialized()) {
            try {
                Terminal.init(getApplicationContext(), LogLevel.VERBOSE, new TokenProvider(), TerminalEventListener.instance, new OfflineModeHandler(new OfflineModeHandler.Callback() {
                    @Override
                    public void makeToast(String message) {
                        // Handle toast messages
                    }
                }));
                Log.d("MainActivity", "Terminal initialized successfully in onCreate");
            } catch (TerminalException e) {
                Log.e("MainActivity", "Failed to initialize Terminal: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        setContentView(R.layout.activity_main);

        // Initialize app state and shared preferences
        appState = StripeTerminalApplication.getInstance().getAppState();
        sharedPreferencesManager = new SharedPreferencesManager(this);

        // Load saved zone if exists
        Zone savedZone = sharedPreferencesManager.getSelectedZone();
        if (savedZone != null) {
            appState.setSelectedZone(savedZone);
        }
        
        Log.d("MainActivity", "Terminal initialized at onCreate: " + Terminal.isInitialized());

        // Apply saved language preference on startup
        LanguageManager.applyLanguage(this);

        // Check location permissions on app start
        checkLocationPermissions();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED
        ) {
            final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && !adapter.isEnabled()) {
                adapter.enable();
            }
        } else {
            // Failed to acquire Bluetooth permission
        }

        // If permissions are already granted, initialize immediately
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            initialize();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Download fresh literals every time the app comes to foreground
        downloadFreshLiterals();
        
        // NOTE: In v5.0.0, Terminal initializes lazily. Don't call configureTapToPayUX() here.
        // It will be called after reader connection in PaymentFragment/TerminalFragment/DiscoveryFragment
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    /**
     * Download fresh literals from the server and refresh UI
     */
    private void downloadFreshLiterals() {
        Log.d("MainActivity", "========== DOWNLOADING LITERALS ON APP RESUME ==========");
        StripeTerminalApplication app = StripeTerminalApplication.getInstance();
        if (app != null && app.getLiteralsManager() != null) {
            Log.d("MainActivity", "Initiating literals download from Google Sheets...");
            app.getLiteralsManager().downloadFreshLiterals(new com.parkmeter.og.utils.DynamicLiteralsManager.LiteralsDownloadCallback() {
                @Override
                public void onSuccess() {
                    Log.d("MainActivity", "✓ Successfully downloaded fresh literals on app resume");
                    Log.d("MainActivity", "Total literals in cache: " + app.getLiteralsManager().getLiteralsCount());
                    
                    // Refresh the current fragment UI on the main thread
                    runOnUiThread(() -> {
                        refreshCurrentFragmentUI();
                    });
                }

                @Override
                public void onFailure(String error) {
                    Log.e("MainActivity", "✗ Failed to download fresh literals on app resume: " + error);
                }
            });
        } else {
            Log.e("MainActivity", "✗ LiteralsManager is not available");
        }
    }
    
    /**
     * Refresh the UI of the current fragment after literals are updated
     */
    private void refreshCurrentFragmentUI() {
        try {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
            if (currentFragment != null) {
                Log.d("MainActivity", "Refreshing UI for fragment: " + currentFragment.getClass().getSimpleName());
                
                // Trigger onResume to refresh the fragment
                if (currentFragment instanceof HomeFragment) {
                    ((HomeFragment) currentFragment).refreshUI();
                }
                // Add more fragment types here if needed
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error refreshing fragment UI: " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackPressed() {
        // Check if current fragment is EmailReceiptFragment and disable back button
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (currentFragment instanceof EmailReceiptFragment) {
            return; // Disable back button on email receipt page
        }
        
        // Handle back navigation properly with fragment back stack
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    // Navigation callbacks

    /**
     * Callback function called when discovery has been canceled by the [DiscoveryFragment]
     */
    @Override
    public void onCancelDiscovery() {
        navigateTo(TerminalFragment.TAG, new TerminalFragment());
    }



    /**
     * Callback function called once discovery has been selected by the [TerminalFragment]
     */
    @Override
    public void onRequestDiscovery(boolean isSimulated, DiscoveryMethod discoveryMethod) {
        navigateTo(DiscoveryFragment.TAG, DiscoveryFragment.newInstance(isSimulated, discoveryMethod));
    }

    /**
     * Callback function called to exit the payment workflow
     */
    @Override
    public void onRequestExitWorkflow() {
        if (Terminal.getInstance().getConnectionStatus() == ConnectionStatus.CONNECTED) {
            navigateTo(ConnectedReaderFragment.TAG, new ConnectedReaderFragment());
        } else {
            navigateTo(TerminalFragment.TAG, new TerminalFragment());
        }
    }

    /**
     * Callback function called to start a payment by the [PaymentFragment]
     */
    @Override
    public void onRequestPayment(long amount, @NotNull String currency, boolean skipTipping, boolean extendedAuth, boolean incrementalAuth, OfflineBehaviorSelection offlineBehaviorSelection, String plateNumber, Zone selectedZone, Rate selectedRate, RateStep selectedRateStep) {
        // Handle payment directly without showing the testing page
        handleDirectPayment(amount, currency, skipTipping, extendedAuth, incrementalAuth, offlineBehaviorSelection, plateNumber, selectedZone, selectedRate, selectedRateStep);
    }
    
    /**
     * Handle payment directly without showing the testing page
     */
    private void handleDirectPayment(long amount, @NotNull String currency, boolean skipTipping, boolean extendedAuth, boolean incrementalAuth, OfflineBehaviorSelection offlineBehaviorSelection, String plateNumber, Zone selectedZone, Rate selectedRate, RateStep selectedRateStep) {
        // Create a direct payment handler that bypasses the EventFragment
        DirectPaymentHandler paymentHandler = new DirectPaymentHandler(this, amount, currency, skipTipping, extendedAuth, incrementalAuth, offlineBehaviorSelection, plateNumber, selectedZone, selectedRate, selectedRateStep);
        paymentHandler.startPayment();
    }

    /**
     * Callback function called once the payment workflow has been selected by the
     * [ConnectedReaderFragment]
     */
    @Override
    public void onSelectPaymentWorkflow() {
        navigateTo(PaymentFragment.TAG, new PaymentFragment());
    }

    /**
     * Callback function called once the read card workflow has been selected by the
     * [ConnectedReaderFragment]
     */
    @Override
    public void onRequestSaveCard() {
        navigateTo(EventFragment.TAG, EventFragment.collectSetupIntentPaymentMethod());
    }

    /**
     * Callback function called once the update reader workflow has been selected by the
     * [ConnectedReaderFragment]
     */
    @Override
    public void onSelectUpdateWorkflow() {
        navigateTo(UpdateReaderFragment.TAG, new UpdateReaderFragment());
    }

    /**
     * Callback function called once the view offline logs has been selected by the
     * [ConnectedReaderFragment]
     */
    @Override
    public void onSelectViewOfflineLogs() {
        navigateTo(OfflinePaymentsLogFragment.TAG, new OfflinePaymentsLogFragment());
    }

    @Override
    public void onRequestViewLogs() {
        navigateTo(LogViewerFragment.TAG, new LogViewerFragment());
    }

    /**
     * Callback function called when user wants to enter license plate
     */
    @Override
    public void onRequestVehicleNumber() {
        navigateTo(VehicleNumberFragment.TAG, new VehicleNumberFragment(), true, true);
    }

    /**
     * Callback function called when vehicle number has been entered and user wants to proceed to rate selection
     */
    @Override
    public void onVehicleNumberEntered(String vehicleNumber) {
        
        // Store vehicle number in app state
        appState.setVehicleNumber(vehicleNumber);
        
        Zone selectedZone = appState.getSelectedZone();
        if (selectedZone == null) {
            Toast.makeText(this, LiteralsHelper.getText(this, "please_select_zone_first"), Toast.LENGTH_SHORT).show();
            return;
        }
        
        navigateTo(RateSelectionFragment.TAG, RateSelectionFragment.newInstance(selectedZone, vehicleNumber), true, true);
    }

    /**
     * Callback function called when user wants to open zones selection
     */
    @Override
    public void onRequestZonesSelection() {
        String selectedZoneId = appState.getSelectedZone() != null ? appState.getSelectedZone().getId() : null;
        navigateTo(ZonesFragment.TAG, ZonesFragment.newInstance(selectedZoneId), true, true);
    }

    /**
     * Callback function called when a zone has been selected
     */
    @Override
    public void onZoneSelected(Zone zone) {
        
        // Update app state
        appState.setSelectedZone(zone);
        
        // Update app-wide theming with organization color
        String orgColor = zone.getOrganization().getColor();
        AppThemeManager.getInstance().updateOrganizationColor(orgColor);
        
        // Log complete selection data
        
        // Save to shared preferences
        sharedPreferencesManager.saveSelectedZone(zone);
        
        // Navigate back to home screen
        navigateTo(HomeFragment.TAG, new HomeFragment(), true, false);
    }

    /**
     * Callback function called when user wants to open settings
     */
    @Override
    public void onRequestSettings() {
        navigateTo(SettingsFragment.TAG, SettingsFragment.newInstance(), true, true);
    }

    /**
     * Callback function called when a rate has been selected
     */
    @Override
    public void onRateSelected(Rate rate, Zone zone, String vehicleNumber) {
        // Navigate to payment screen with selected rate
        // For now, navigate to TerminalFragment, but you can modify this to go to a payment screen
        navigateTo(TerminalFragment.TAG, new TerminalFragment(), true, true);
    }

    /**
     * Callback function called when a rate step has been selected
     */
    @Override
    public void onRateStepSelected(RateStep rateStep, Rate rate, Zone zone, String vehicleNumber) {
        
        // Navigate to enhanced payment screen with selected rate step
        PaymentFragment paymentFragment = PaymentFragment.newInstance(rateStep, rate, zone, vehicleNumber);
        navigateTo(PaymentFragment.TAG, paymentFragment, true, true);
    }

    /**
     * Callback function called when payment is successful
     */
    @Override
    public void onPaymentSuccessful(long amount, String parkedId, String transactionId) {
        
        // Hide overlay loader from PaymentFragment if it's the current fragment
        try {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof PaymentFragment) {
                    ((PaymentFragment) fragment).hideOverlayLoaderPublic();
                    break;
                }
            }
        } catch (Exception e) {
            // Error hiding overlay loader
        }
        
        // Navigate to email receipt page
        EmailReceiptFragment emailReceiptFragment = EmailReceiptFragment.newInstance(amount, parkedId, transactionId);
        navigateTo(EmailReceiptFragment.TAG, emailReceiptFragment, true, false); // Don't add to back stack
    }

    /**
     * Callback function called when user wants to return to home
     */
    @Override
    public void onRequestReturnToHome() {
        navigateTo(HomeFragment.TAG, new HomeFragment(), true, false); // Don't add to back stack
    }

    // Terminal event callbacks

    /**
     * Callback function called when collect payment method has been canceled
     */
    @Override
    public void onCancelCollectPaymentMethod() {
        
        // Hide overlay loader from PaymentFragment if it's the current fragment
        try {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof PaymentFragment) {
                    ((PaymentFragment) fragment).hideOverlayLoaderPublic();
                    break;
                }
            }
        } catch (Exception e) {
            // Error hiding overlay loader
        }
        
        // Show payment cancelled message
        Toast.makeText(this, LiteralsHelper.getText(this, "payment_cancelled"), Toast.LENGTH_SHORT).show();
        
        // Navigate back to rate selection screen (selection page)
        Zone selectedZone = appState.getSelectedZone();
        if (selectedZone != null) {
            String vehicleNumber = appState.getVehicleNumber();
            if (vehicleNumber != null) {
                navigateTo(RateSelectionFragment.TAG, RateSelectionFragment.newInstance(selectedZone, vehicleNumber), true, false);
            } else {
                navigateTo(HomeFragment.TAG, new HomeFragment(), true, false);
            }
        } else {
            navigateTo(HomeFragment.TAG, new HomeFragment(), true, false);
        }
    }

    /**
     * Callback function called when collect setup intent has been canceled
     */
    @Override
    public void onCancelCollectSetupIntent() {
        
        // Show payment cancelled message
        Toast.makeText(this, LiteralsHelper.getText(this, "payment_cancelled"), Toast.LENGTH_SHORT).show();
        
        // Navigate back to rate selection screen (selection page)
        Zone selectedZone = appState.getSelectedZone();
        if (selectedZone != null) {
            String vehicleNumber = appState.getVehicleNumber();
            if (vehicleNumber != null) {
                navigateTo(RateSelectionFragment.TAG, RateSelectionFragment.newInstance(selectedZone, vehicleNumber), true, false);
            } else {
                navigateTo(HomeFragment.TAG, new HomeFragment(), true, false);
            }
        } else {
            navigateTo(HomeFragment.TAG, new HomeFragment(), true, false);
        }
    }

    /**
     * Callback function called on completion of [Terminal.connectReader]
     */
    @Override
    public void onConnectReader() {
        navigateTo(ConnectedReaderFragment.TAG, new ConnectedReaderFragment());
    }

    @Override
    public void onDisconnectReader() {
        navigateTo(TerminalFragment.TAG, new TerminalFragment());
    }

    @Override
    public void onStartInstallingUpdate(@NotNull ReaderSoftwareUpdate update, @Nullable Cancelable cancelable) {
        runOnUiThread(() -> {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            Fragment currentFragment = fragments.get(fragments.size() - 1);
            if (currentFragment instanceof MobileReaderListener) {
                ((MobileReaderListener) currentFragment).onStartInstallingUpdate(update, cancelable);
            }
        });
    }

    @Override
    public void onReportReaderSoftwareUpdateProgress(float progress) {
        runOnUiThread(() -> {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            Fragment currentFragment = fragments.get(fragments.size() - 1);
            if (currentFragment instanceof MobileReaderListener) {
                ((MobileReaderListener) currentFragment).onReportReaderSoftwareUpdateProgress(progress);
            }
        });
    }

    @Override
    public void onFinishInstallingUpdate(@Nullable ReaderSoftwareUpdate update, @Nullable TerminalException e) {
        runOnUiThread(() -> {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            Fragment currentFragment = fragments.get(fragments.size() - 1);
            if (currentFragment instanceof MobileReaderListener) {
                ((MobileReaderListener) currentFragment).onFinishInstallingUpdate(update, e);
            }
        });
    }

    @Override
    public void onRequestReaderInput(@NotNull ReaderInputOptions options) {
        runOnUiThread(() -> {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            Fragment currentFragment = fragments.get(fragments.size() - 1);
            if (currentFragment instanceof MobileReaderListener) {
                ((MobileReaderListener) currentFragment).onRequestReaderInput(options);
            }
        });
    }

    @Override
    public void onRequestReaderDisplayMessage(@NotNull ReaderDisplayMessage message) {
        runOnUiThread(() -> {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            Fragment currentFragment = fragments.get(fragments.size() - 1);
            if (currentFragment instanceof MobileReaderListener) {
                ((MobileReaderListener) currentFragment).onRequestReaderDisplayMessage(message);
            }
        });
    }

    @Override
    public void onReportAvailableUpdate(@NotNull ReaderSoftwareUpdate update) {
        runOnUiThread(() -> {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            Fragment currentFragment = fragments.get(fragments.size() - 1);
            if (currentFragment instanceof MobileReaderListener) {
                ((MobileReaderListener) currentFragment).onReportAvailableUpdate(update);
            }
        });
    }

    @Override
    public void onReportReaderEvent(@NotNull ReaderEvent event) {
        runOnUiThread(() -> {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            Fragment currentFragment = fragments.get(fragments.size() - 1);
            if (currentFragment instanceof MobileReaderListener) {
                ((MobileReaderListener) currentFragment).onReportReaderEvent(event);
            }
        });
    }

    @Override
    public void onReportLowBatteryWarning() {
        runOnUiThread(() -> {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            Fragment currentFragment = fragments.get(fragments.size() - 1);
            if (currentFragment instanceof MobileReaderListener) {
                ((MobileReaderListener) currentFragment).onReportLowBatteryWarning();
            }
        });
    }

    @Override
    public void onBatteryLevelUpdate(float batteryLevel, @NonNull BatteryStatus batteryStatus, boolean isCharging) {
        runOnUiThread(() -> {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            Fragment currentFragment = fragments.get(fragments.size() - 1);
            if (currentFragment instanceof MobileReaderListener) {
                ((MobileReaderListener) currentFragment).onBatteryLevelUpdate(batteryLevel, batteryStatus, isCharging);
            }
        });
    }

    @Override
    public void onDisconnect(@NonNull DisconnectReason reason) {
        runOnUiThread(() -> {
            // Update TerminalViewModel if we're on the main screen
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
            if (currentFragment instanceof TerminalFragment) {
                TerminalFragment terminalFragment = (TerminalFragment) currentFragment;
                terminalFragment.getViewModel().setDisconnected();
            }
            
            // Also notify other fragments
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            Fragment lastFragment = fragments.get(fragments.size() - 1);
            if (lastFragment instanceof MobileReaderListener) {
                ((MobileReaderListener) lastFragment).onDisconnect(reason);
            }
        });
    }



    /**
     * ReaderReconnectionListener implementation.
     */
    @Override
    public void onReaderReconnectStarted(@NonNull Reader reader, @NonNull Cancelable cancelReconnect, @NonNull DisconnectReason reason) {
        
    }
    @Override
    public void onReaderReconnectSucceeded(@NonNull Reader reader) {
        
    }

    @Override
    public void onReaderReconnectFailed(@NonNull Reader reader) {
        
    }

    /**
     * Initialize the [Terminal] and go to the appropriate screen based on zone selection
     */
    private void initialize() {
        // NOTE: In Stripe Terminal SDK v5.0.0, Terminal initializes lazily on first use
        // Do NOT call Terminal.getInstance() or configureTapToPayUX() here
        // It will be configured after the first reader discovery/connection

        // Check if it's first time or no zone is selected
        if (sharedPreferencesManager.isFirstTime() || !appState.isZoneSelected()) {
            sharedPreferencesManager.setFirstTime(false);
            navigateTo(ZonesFragment.TAG, new ZonesFragment());
        } else {
            navigateTo(HomeFragment.TAG, new HomeFragment());
        }
    }
    
    public void configureTapToPayUX() {
        // Configure Tap to Pay UX with Front tap zone
        // This should be called after Terminal is ready (e.g., after first reader discovery/connection)
        Log.d("MainActivity", "========== CONFIGURING TAP TO PAY UX ==========");
        Log.d("MainActivity", "Terminal initialized: " + Terminal.isInitialized());
        
        if (!Terminal.isInitialized()) {
            Log.w("MainActivity", "Terminal not initialized yet, skipping Tap to Pay UX configuration");
            return;
        }
        
        try {
            // Create Front tap zone with center positioning (xBias=0.5f, yBias=0.5f)
            com.stripe.stripeterminal.external.models.TapToPayUxConfiguration.TapZone.Front frontTapZone = 
                new com.stripe.stripeterminal.external.models.TapToPayUxConfiguration.TapZone.Front(0.5f, 0.5f);
            Log.d("MainActivity", "Created Front tap zone: xBias=0.5f, yBias=0.5f");
            
            // Create UX configuration with Front tap zone (null for default color scheme and dark mode)
            com.stripe.stripeterminal.external.models.TapToPayUxConfiguration uxConfig = 
                new com.stripe.stripeterminal.external.models.TapToPayUxConfiguration(
                    frontTapZone,
                    null,  // Use default color scheme
                    null   // Use default dark mode
                );
            Log.d("MainActivity", "Created TapToPayUxConfiguration object");
            
            // Set the tap zone configuration globally for all Tap to Pay operations
            Terminal.getInstance().setTapToPayUxConfiguration(uxConfig);
            Log.d("MainActivity", "✓✓✓ SUCCESS: Tap to Pay UX configured with Front tap zone!");
            Log.d("MainActivity", "==============================================");
        } catch (Exception e) {
            Log.e("MainActivity", "✗✗✗ FAILED to set Tap to Pay UX configuration");
            Log.e("MainActivity", "Error type: " + e.getClass().getSimpleName());
            Log.e("MainActivity", "Error message: " + e.getMessage());
            Log.e("MainActivity", "Stack trace:", e);
            Log.d("MainActivity", "==============================================");
        }
    }

    /**
     * Navigate to the given fragment.
     *
     * @param fragment Fragment to navigate to.
     */
    private void navigateTo(String tag, Fragment fragment) {
        navigateTo(tag, fragment, true, false);
    }

    /**
     * Navigate to the given fragment.
     *
     * @param fragment Fragment to navigate to.
     */
    private void navigateTo(String tag, Fragment fragment, boolean replace, boolean addToBackstack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (replace) {
            // Always use the provided fragment to avoid reusing stale instances
            transaction.replace(R.id.container, fragment, tag);
        } else {
            transaction.add(R.id.container, fragment, tag);
        }

        if (addToBackstack) {
            transaction.addToBackStack(tag);
        }

        transaction.commitAllowingStateLoss();
    }

    /**
     * Check and request location permissions on app start
     */
    private void checkLocationPermissions() {
        // Check for location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            
            // Request location permissions
            ActivityCompat.requestPermissions(this, 
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                }, 
                1001); // Request code for location permissions
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == 1001) { // Location permissions
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                // Continue with app initialization
                initialize();
            } else {
                Toast.makeText(this, LiteralsHelper.getText(this, "location_permissions_required"), Toast.LENGTH_LONG).show();
                // Still continue but with limited functionality
                initialize();
            }
        }
    }
}
