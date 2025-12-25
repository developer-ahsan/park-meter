package com.parkmeter.og.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.parkmeter.og.MainActivity;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.model.Rate;
import com.parkmeter.og.model.RateStep;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.LiteralsHelper;
import com.parkmeter.og.network.ApiClient;

import com.parkmeter.og.network.Park45ApiClient;
import com.parkmeter.og.network.Park45ApiService;
import com.parkmeter.og.model.CreateLocationResponse;
import com.parkmeter.og.model.ParkVehicleRequest;
import com.parkmeter.og.model.ParkVehicleResponse;
import com.parkmeter.og.model.AppState;
import com.parkmeter.og.StripeTerminalApplication;
import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.models.ConnectionStatus;
import com.stripe.stripeterminal.external.models.Reader;
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration;
import com.stripe.stripeterminal.external.models.ConnectionConfiguration;
import com.stripe.stripeterminal.external.callable.DiscoveryListener;
import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.callable.ReaderCallback;
import com.stripe.stripeterminal.external.models.TerminalException;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Enhanced PaymentFragment that shows detailed selection data and handles reader discovery/connection
 */
public class PaymentFragment extends Fragment {

    public static final String TAG = "PaymentFragment";
    private static final String ARG_RATE_STEP = "rate_step";
    private static final String ARG_RATE = "rate";
    private static final String ARG_ZONE = "zone";
    private static final String ARG_VEHICLE_NUMBER = "vehicle_number";

    private NavigationListener navigationListener;
    private AppState appState;
    private RateStep selectedRateStep;
    private Rate selectedRate;
    private Zone selectedZone;
    private String vehicleNumber;
    
    // UI Components
    private MaterialCardView cardSelectionDetails;
    private TextView tvOrgName;
    private TextView tvZoneName;
    private TextView tvVehicleNumber;
    private TextView tvRateName;
    private TextView tvTimeDesc;
    private TextView tvTotalAmount;
    private TextView tvStatusMessage;
    private ProgressBar progressBar;
    private ImageView btnBack;
    private MaterialButton btnDiscoverReader;
    private MaterialButton btnCollectPayment;
    private LinearLayout layoutConnectionStatus;
    private TextView tvConnectedReader;
    private ImageView ivReaderIcon;
    
    // State tracking
    private boolean isReaderConnected = false;
    private String connectedReaderName = "";
    private Cancelable discoveryTask;
    private boolean isDiscovering = false;
    private String dynamicLocationId = null;
    private android.os.Handler discoveryTimeoutHandler = new android.os.Handler();
    private Runnable discoveryTimeoutRunnable;
    
    // Overlay loader
    private View overlayLoader;
    private TextView tvOverlayStatus;
    private TextView tvOverlaySubStatus;
    private ProgressBar progressBarOverlay;
    private boolean isProcessingPayment = false;

    public static PaymentFragment newInstance(RateStep rateStep, Rate rate, Zone zone, String vehicleNumber) {
        PaymentFragment fragment = new PaymentFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RATE_STEP, rateStep);
        args.putSerializable(ARG_RATE, rate);
        args.putSerializable(ARG_ZONE, zone);
        args.putString(ARG_VEHICLE_NUMBER, vehicleNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedRateStep = (RateStep) getArguments().getSerializable(ARG_RATE_STEP);
            selectedRate = (Rate) getArguments().getSerializable(ARG_RATE);
            selectedZone = (Zone) getArguments().getSerializable(ARG_ZONE);
            vehicleNumber = getArguments().getString(ARG_VEHICLE_NUMBER);
        }
        
        if (getActivity() instanceof NavigationListener) {
            navigationListener = (NavigationListener) getActivity();
        }
        
        // Get app state for organization ID
        appState = StripeTerminalApplication.getInstance().getAppState();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_enhanced, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set status bar to white to avoid orange background
        if (getActivity() != null) {
            getActivity().getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        }

        initializeViews(view);
        setupOverlayLoader(view);
        setupUI();
        applyTheming();
        checkReaderConnectionStatus();
        
        // Start automatic flow
        startAutomaticPaymentFlow();
    }

    private void initializeViews(View view) {
        cardSelectionDetails = view.findViewById(R.id.card_selection_details);
        tvOrgName = view.findViewById(R.id.tv_org_name);
        tvZoneName = view.findViewById(R.id.tv_zone_name);
        tvVehicleNumber = view.findViewById(R.id.tv_vehicle_number);
        tvRateName = view.findViewById(R.id.tv_rate_name);
        tvTimeDesc = view.findViewById(R.id.tv_time_desc);
        tvTotalAmount = view.findViewById(R.id.tv_total_amount);
        tvStatusMessage = view.findViewById(R.id.tv_status_message);
        progressBar = view.findViewById(R.id.progress_bar);
        btnBack = view.findViewById(R.id.btn_back);
        btnDiscoverReader = view.findViewById(R.id.btn_discover_reader);
        btnCollectPayment = view.findViewById(R.id.btn_collect_payment);
        layoutConnectionStatus = view.findViewById(R.id.layout_connection_status);
        tvConnectedReader = view.findViewById(R.id.tv_connected_reader);
        ivReaderIcon = view.findViewById(R.id.iv_reader_icon);
        
        setupClickListeners();
    }

    private void setupOverlayLoader(View parentView) {
        // Inflate overlay loader
        LayoutInflater inflater = LayoutInflater.from(getContext());
        overlayLoader = inflater.inflate(R.layout.overlay_loader, null);
        
        // Get overlay views
        tvOverlayStatus = overlayLoader.findViewById(R.id.tv_overlay_status);
        tvOverlaySubStatus = overlayLoader.findViewById(R.id.tv_overlay_sub_status);
        progressBarOverlay = overlayLoader.findViewById(R.id.progress_bar_overlay);
        
        // Add overlay to parent view
        if (parentView instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) parentView.getParent();
            if (parent != null) {
                parent.addView(overlayLoader);
                overlayLoader.setVisibility(View.GONE);
            }
        }
    }

    private void startAutomaticPaymentFlow() {
        // Check if amount is greater than 0
        if (selectedRateStep.getTotal() > 0) {
            // Check if Terminal is initialized before accessing
            if (!Terminal.isInitialized()) {
                Log.d("PaymentFragment", "Terminal not initialized yet, starting discovery to initialize");
                startReaderDiscovery();
                return;
            }
            
            // First check if reader is already connected
            Terminal terminal = Terminal.getInstance();
            ConnectionStatus status = terminal.getConnectionStatus();
            
            if (status == ConnectionStatus.CONNECTED) {
                Reader reader = terminal.getConnectedReader();
                if (reader != null) {
                    // Reader is already connected, use it directly
                    connectedReaderName = reader.getSerialNumber();
                    isReaderConnected = true;
                    updateConnectionStatus(true);
                    updateStatus(LiteralsHelper.getText(getContext(), "reader_connected") + ": " + connectedReaderName, false);
                    btnCollectPayment.setEnabled(true);
                    
                    // Automatically start payment process
                    updateOverlayLoader("Creating Payment", "Preparing transaction...");
                    startAutomaticPayment();
                    return;
                }
            }
            
            // Reader not connected, proceed with normal flow
            showOverlayLoader("Creating Location", "Setting up payment location...");
            createLocationAndStartDiscovery();
        } else {
            showOverlayLoader("Processing Free Parking", "Setting up parking session...");
            handleZeroAmountPayment();
        }
    }

    private void createLocationAndStartDiscovery() {
        updateOverlayLoader("Creating Location", "Setting up payment location...");
        
        // Get dynamic organization ID from app state
        String organizationId = appState.getSelectedOrganizationId();
        if (organizationId == null || organizationId.isEmpty()) {
            organizationId = "66c12693a0cd2e395d34af8e"; // Fallback
        }
        
        ApiClient.createLocationAsync(organizationId, new retrofit2.Callback<CreateLocationResponse>() {
            @Override
            public void onResponse(retrofit2.Call<CreateLocationResponse> call, retrofit2.Response<CreateLocationResponse> response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (response.isSuccessful() && response.body() != null) {
                            dynamicLocationId = response.body().getId();
                            updateOverlayLoader("Connecting to Reader", "Please wait while we connect...");
                            startAutoDiscovery();
                        } else {
                            updateOverlayLoader("Location Creation Failed", "Using fallback location...");
                            // Use fallback location ID
                            dynamicLocationId = "tml_GJv9FgsphhQmKS";
                            // Continue with discovery after short delay
                            new android.os.Handler().postDelayed(() -> {
                                updateOverlayLoader("Connecting to Reader", "Please wait while we connect...");
                                startAutoDiscovery();
                            }, 1500);
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<CreateLocationResponse> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateOverlayLoader("Location Creation Failed", "Using fallback location...");
                        // Use fallback location ID
                        dynamicLocationId = "tml_GJv9FgsphhQmKS";
                        // Continue with discovery after short delay
                        new android.os.Handler().postDelayed(() -> {
                            updateOverlayLoader("Connecting to Reader", "Please wait while we connect...");
                            startAutoDiscovery();
                        }, 1500);
                    });
                }
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            // Stop any ongoing discovery before navigating back
            if (discoveryTask != null) {
                discoveryTask.cancel(new com.stripe.stripeterminal.external.callable.Callback() {
                    @Override
                    public void onSuccess() {
                        // Discovery stopped successfully
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull com.stripe.stripeterminal.external.models.TerminalException e) {
                        // Even if cancellation fails, go back
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    }
                });
            } else {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });
        
        btnCollectPayment.setOnClickListener(v -> {
            // Button is now mainly for manual retry if needed
            if (selectedRateStep.getTotal() > 0 && !isProcessingPayment) {
                showOverlayLoader("Processing Payment", "Retrying payment...");
                startAutomaticPayment();
            }
        });
    }

    private void setupUI() {
        // Display selection details
        tvOrgName.setText(selectedZone.getOrganization().getOrgName());
        tvZoneName.setText(selectedZone.getZoneName());
        tvVehicleNumber.setText(vehicleNumber);
        tvRateName.setText(selectedRate.getRateName());
        tvTimeDesc.setText(selectedRateStep.getTimeDesc());
        
        // Display actual total amount from selected rate step
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);
        double actualAmountInDollars = selectedRateStep.getTotal() / 100.0; // Convert cents to dollars
        String formattedActualAmount = currencyFormat.format(actualAmountInDollars);
        tvTotalAmount.setText(formattedActualAmount);
        
        // Set actual amount
        
        // Ensure total amount is visible
        tvTotalAmount.setVisibility(View.VISIBLE);
        tvTotalAmount.setTextColor(getResources().getColor(android.R.color.white));
        
        // Hide discover reader button - discovery will be automatic
        btnDiscoverReader.setVisibility(View.GONE);
        
        // Initial status
        updateStatus(LiteralsHelper.getText(getContext(), "ready_to_connect_reader"), false);
        btnCollectPayment.setEnabled(false);
    }

    private void applyTheming() {
        AppThemeManager themeManager = AppThemeManager.getInstance();
        int orgColor = themeManager.getCurrentOrgColorInt();
        
        // Apply theming to the fragment
        themeManager.applyThemeToFragment(getView());
        
        // Apply organization color to all text elements
        themeManager.applyHeaderColor(tvOrgName);
        tvOrgName.setTextColor(orgColor);
        
        // Apply organization color to zone name
        tvZoneName.setTextColor(orgColor);
        
        // Apply organization color to vehicle number
        tvVehicleNumber.setTextColor(orgColor);
        
        // Apply organization color to rate name
        tvRateName.setTextColor(orgColor);
        
        // Apply organization color to time description
        tvTimeDesc.setTextColor(orgColor);
        
        // Ensure total amount is white text on organization color background
        tvTotalAmount.setTextColor(getResources().getColor(android.R.color.white));
        tvTotalAmount.setVisibility(View.VISIBLE);
        
        // Apply organization color to total amount background
        View totalAmountContainer = tvTotalAmount.getParent() instanceof View ? (View) tvTotalAmount.getParent() : null;
        if (totalAmountContainer != null) {
            totalAmountContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orgColor));
        }
        
        // Apply organization color to status message
        tvStatusMessage.setTextColor(orgColor);
        
        // Apply organization color to progress bar
        progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(orgColor));
        
        // Apply organization color to buttons
        themeManager.applyMaterialButtonColor(btnDiscoverReader);
        themeManager.applyMaterialButtonColor(btnCollectPayment);
        
        // Apply organization color to reader icon
        themeManager.applyImageViewTint(ivReaderIcon);
        
        // Apply organization color to back button
        btnBack.setColorFilter(null); // Clear any color filter
        btnBack.setImageTintList(android.content.res.ColorStateList.valueOf(orgColor));
        
        // Apply organization color to title
        TextView tvTitle = getView().findViewById(R.id.tv_title);
        if (tvTitle != null) {
            tvTitle.setTextColor(orgColor);
        }
        
        // Apply card styling with organization color
        cardSelectionDetails.setStrokeColor(orgColor);
        cardSelectionDetails.setStrokeWidth(2);
        
        // Apply organization color to connection status layout background
        if (layoutConnectionStatus != null) {
            layoutConnectionStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orgColor));
        }
        
        // Apply organization color to connected reader text
        if (tvConnectedReader != null) {
            tvConnectedReader.setTextColor(getResources().getColor(android.R.color.white));
        }
        
        // Comprehensive theming applied
    }

    private void checkReaderConnectionStatus() {
        // Check if Terminal is initialized before accessing
        if (!Terminal.isInitialized()) {
            Log.d("PaymentFragment", "Terminal not initialized yet, skipping reader connection check");
            return;
        }
        
        Terminal terminal = Terminal.getInstance();
        ConnectionStatus status = terminal.getConnectionStatus();
        
        if (status == ConnectionStatus.CONNECTED) {
            Reader reader = terminal.getConnectedReader();
            if (reader != null) {
                connectedReaderName = reader.getSerialNumber();
                isReaderConnected = true;
                updateConnectionStatus(true);
                updateStatus(LiteralsHelper.getText(getContext(), "reader_connected") + ": " + connectedReaderName, false);
                btnCollectPayment.setEnabled(true);
            }
        } else {
            isReaderConnected = false;
            connectedReaderName = "";
            updateConnectionStatus(false);
            updateStatus(LiteralsHelper.getText(getContext(), "no_reader_connected"), false);
            btnCollectPayment.setEnabled(false);
        }
    }

    private void startReaderDiscovery() {
        if (isDiscovering) {
            return; // Already discovering
        }
        
        updateStatus(LiteralsHelper.getText(getContext(), "discovering_readers"), true);
        // isDiscovering = true; // Commented out per Stripe support recommendation
        
        // Start discovery on the same screen
        startAutoDiscovery();
    }
    
    private void startAutoDiscovery() {
        if (isDiscovering) {
            return;
        }
        
        // In v5.0.0, Terminal initializes lazily - calling discoverReaders will trigger initialization
        Log.d("PaymentFragment", "========== START AUTO DISCOVERY ==========");
        Log.d("PaymentFragment", "Terminal.isInitialized(): " + Terminal.isInitialized());
        
        updateStatus(LiteralsHelper.getText(getContext(), "discovering_readers"), true);
        updateOverlayLoader("Discovering Readers", "Searching for available payment readers...");
        isDiscovering = true;
        
        // For Tap to Pay on Android, the device itself is the reader
        // Using TapToPayDiscoveryConfiguration for v5.0.0
        final boolean isSimulated = false; // Production mode
        Log.d("PaymentFragment", "========================================");
        Log.d("PaymentFragment", "Creating TapToPayDiscoveryConfiguration");
        Log.d("PaymentFragment", "  Mode: " + (isSimulated ? "SIMULATED (test cards)" : "PRODUCTION (real payments)"));
        Log.d("PaymentFragment", "  Device will act as the reader (Tap to Pay)");
        Log.d("PaymentFragment", "========================================");
        
        final DiscoveryConfiguration config = new DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSimulated);
        Log.d("PaymentFragment", "✓ TapToPayDiscoveryConfiguration created successfully");
        
        try {
            Log.d("PaymentFragment", "Attempting to get Terminal instance...");
            Terminal terminal = Terminal.getInstance();
            Log.d("PaymentFragment", "✓ Got Terminal instance successfully");
            Log.d("PaymentFragment", "Calling discoverReaders()...");
            
            discoveryTask = terminal.discoverReaders(config, discoveryListener, new Callback() {
                @Override
                public void onSuccess() {
                    Log.d("PaymentFragment", "✓✓✓ Discovery started successfully");
                    Log.d("PaymentFragment", "Terminal now initialized: " + Terminal.isInitialized());
                    Log.d("PaymentFragment", "Waiting for onUpdateDiscoveredReaders callback...");
                    
                    // Set a timeout for discovery (30 seconds)
                    discoveryTimeoutRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isDiscovering && !isReaderConnected) {
                                Log.e("PaymentFragment", "✗✗✗ Discovery timeout - no readers found after 30 seconds");
                                Log.e("PaymentFragment", "Device may not support Tap to Pay or NFC may be disabled");
                                if (discoveryTask != null) {
                                    discoveryTask.cancel(new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d("PaymentFragment", "Discovery cancelled due to timeout");
                                        }
                                        @Override
                                        public void onFailure(@NonNull TerminalException e) {
                                            Log.e("PaymentFragment", "Failed to cancel discovery: " + e.getErrorMessage());
                                        }
                                    });
                                }
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        isDiscovering = false;
                                        updateStatus("No reader found. Please check:\n1. Device supports Tap to Pay\n2. NFC is enabled\n3. Stripe account is configured", false);
                                        updateOverlayLoader("Discovery Timeout", "No readers found. Check device compatibility.");
                                        btnCollectPayment.setEnabled(true);
                                        btnCollectPayment.setText(LiteralsHelper.getText(getContext(), "retry_payment"));
                                        new android.os.Handler().postDelayed(() -> hideOverlayLoader(), 5000);
                                    });
                                }
                            }
                        }
                    };
                    discoveryTimeoutHandler.postDelayed(discoveryTimeoutRunnable, 30000); // 30 second timeout
                }
                
                @Override
                public void onFailure(@NonNull TerminalException e) {
                    Log.e("PaymentFragment", "✗✗✗ Discovery FAILED to start");
                    Log.e("PaymentFragment", "Error message: " + e.getErrorMessage());
                    Log.e("PaymentFragment", "Error code: " + e.getErrorCode());
                    Log.e("PaymentFragment", "Error type: " + e.getClass().getSimpleName());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateStatus("Discovery failed: " + e.getErrorMessage(), false);
                            updateOverlayLoader("Discovery Failed", "Error: " + e.getErrorMessage());
                            isDiscovering = false;
                            btnCollectPayment.setEnabled(true);
                            btnCollectPayment.setText(LiteralsHelper.getText(getContext(), "retry_payment"));
                            
                            // Hide overlay after 3 seconds
                            new android.os.Handler().postDelayed(() -> hideOverlayLoader(), 3000);
                        });
                    }
                }
            });
            Log.d("PaymentFragment", "discoverReaders() called, waiting for callbacks...");
        } catch (Exception e) {
            Log.e("PaymentFragment", "✗✗✗ EXCEPTION calling discoverReaders()", e);
            updateStatus("Discovery error: " + e.getMessage(), false);
            isDiscovering = false;
        }
        Log.d("PaymentFragment", "==========================================");
    }
    
    private final DiscoveryListener discoveryListener = new DiscoveryListener() {
        @Override
        public void onUpdateDiscoveredReaders(@NonNull List<Reader> readers) {
            Log.d("PaymentFragment", "========== onUpdateDiscoveredReaders ==========");
            Log.d("PaymentFragment", "Readers count: " + readers.size());
            for (int i = 0; i < readers.size(); i++) {
                Reader r = readers.get(i);
                Log.d("PaymentFragment", "  Reader[" + i + "]: " + r.getSerialNumber() + " (Type: " + r.getDeviceType() + ")");
            }
            Log.d("PaymentFragment", "============================================");
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!readers.isEmpty()) {
                        // Cancel discovery timeout since we found a reader
                        if (discoveryTimeoutRunnable != null) {
                            discoveryTimeoutHandler.removeCallbacks(discoveryTimeoutRunnable);
                            Log.d("PaymentFragment", "Discovery timeout cancelled - reader found");
                        }
                        
                        // Auto-connect to the first available reader
                        Reader reader = readers.get(0);
                        Log.d("PaymentFragment", "Auto-connecting to reader: " + reader.getSerialNumber());
                        updateOverlayLoader("Connecting to Reader", "Found: " + reader.getSerialNumber());
                        connectToReader(reader);
                    } else {
                        Log.d("PaymentFragment", "No readers found yet, still searching...");
                        updateOverlayLoader("Searching for Reader", "Looking for Tap to Pay device...");
                    }
                });
            }
        }
    };
    
    private void connectToReader(Reader reader) {
        Log.d("PaymentFragment", "========== CONNECT TO READER ==========");
        Log.d("PaymentFragment", "Reader: " + reader.getSerialNumber());
        Log.d("PaymentFragment", "Terminal.isInitialized(): " + Terminal.isInitialized());
        
        updateStatus("Connecting to " + reader.getSerialNumber() + "...", true);
        updateOverlayLoader("Connecting to Reader", "Establishing connection with " + reader.getSerialNumber());
        
        // Use dynamic location ID from API response, fallback to default if not available
        String connectLocationId = dynamicLocationId != null ? dynamicLocationId : "tml_GJv9FgsphhQmKS";
        
        // Note: TapToPayUxConfiguration is available in Stripe Terminal SDK v5.0.0+
        // However, the TapToPayConnectionConfiguration constructor in v5.0.0 only accepts:
        // (String locationId, boolean autoReconnectOnUnexpectedDisconnect, TapToPayReaderListener listener)
        // UX configuration will be added in a future SDK update or configured differently
        
        Terminal.getInstance().connectReader(
            reader,
            new ConnectionConfiguration.TapToPayConnectionConfiguration(
                connectLocationId,
                true,
                (MainActivity) getActivity()
            ),
            new ReaderCallback() {
            @Override
            public void onSuccess(@NonNull Reader reader) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        connectedReaderName = reader.getSerialNumber();
                        isReaderConnected = true;
                        updateConnectionStatus(true);
                        updateStatus(LiteralsHelper.getText(getContext(), "reader_connected") + ": " + connectedReaderName, false);
                        btnCollectPayment.setEnabled(true);
                        isDiscovering = false;
                        
                        // Configure Tap to Pay UX with Front tap zone (now that Terminal is initialized)
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).configureTapToPayUX();
                        }
                        
                        // Stop discovery
                        if (discoveryTask != null) {
                            discoveryTask.cancel(new Callback() {
                                @Override
                                public void onSuccess() {
                                    // Discovery stopped
                                }
                                
                                @Override
                                public void onFailure(@NonNull TerminalException e) {
                                    // Ignore failure
                                }
                            });
                        }
                        
                        // Automatically start payment process if amount > 0
                        if (selectedRateStep.getTotal() > 0) {
                            updateOverlayLoader("Creating Payment", "Preparing transaction...");
                            startAutomaticPayment();
                        } else {
                            hideOverlayLoader();
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(@NonNull TerminalException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatus("Connection failed: " + e.getErrorMessage(), false);
                        updateOverlayLoader("Connection Failed", "Error: " + e.getErrorMessage());
                        isDiscovering = false;
                        btnCollectPayment.setEnabled(true);
                        btnCollectPayment.setText(LiteralsHelper.getText(getContext(), "retry_payment"));
                        
                        // Hide overlay after 3 seconds
                        new android.os.Handler().postDelayed(() -> hideOverlayLoader(), 3000);
                    });
                }
            }
        });
    }

    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            layoutConnectionStatus.setVisibility(View.VISIBLE);
            tvConnectedReader.setText(LiteralsHelper.getText(getContext(), "connected") + ": " + connectedReaderName);
            ivReaderIcon.setImageResource(R.drawable.ic_check);
        } else {
            layoutConnectionStatus.setVisibility(View.GONE);
        }
    }

    private void updateStatus(String message, boolean showProgress) {
        tvStatusMessage.setText(message);
        progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check connection status when returning from discovery
        checkReaderConnectionStatus();
    }
    
    private void showOverlayLoader(String status, String subStatus) {
        if (overlayLoader != null) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    overlayLoader.setVisibility(View.VISIBLE);
                    if (tvOverlayStatus != null) tvOverlayStatus.setText(status);
                    if (tvOverlaySubStatus != null) tvOverlaySubStatus.setText(subStatus);
                    
                    // Apply organization color to progress bar
                    if (progressBarOverlay != null) {
                        AppThemeManager themeManager = AppThemeManager.getInstance();
                        progressBarOverlay.setIndeterminateTintList(
                            android.content.res.ColorStateList.valueOf(themeManager.getCurrentOrgColorInt())
                        );
                    }
                });
            }
        }
    }

    private void updateOverlayLoader(String status, String subStatus) {
        if (overlayLoader != null && overlayLoader.getVisibility() == View.VISIBLE) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (tvOverlayStatus != null) tvOverlayStatus.setText(status);
                    if (tvOverlaySubStatus != null) tvOverlaySubStatus.setText(subStatus);
                });
            }
        }
    }

    private void hideOverlayLoader() {
        if (overlayLoader != null) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    overlayLoader.setVisibility(View.GONE);
                });
            }
        }
    }
    
    /**
     * Public method to hide overlay loader - can be called from MainActivity
     */
    public void hideOverlayLoaderPublic() {
        hideOverlayLoader();
        isProcessingPayment = false;
        btnCollectPayment.setEnabled(true);
    }

    private void startAutomaticPayment() {
        // Use actual amount from selected rate step
        long actualAmount = selectedRateStep.getTotal(); // Amount in cents
        
        if (actualAmount == 0) {
            // Handle zero amount - call park_vehicle API directly
            handleZeroAmountPayment();
        } else {
            // Handle non-zero amount - proceed with payment
            if (!isReaderConnected) {
                hideOverlayLoader();
                updateStatus(LiteralsHelper.getText(getContext(), "please_connect_reader"), false);
                return;
            }
            
            isProcessingPayment = true;
            btnCollectPayment.setEnabled(false);
            
            updateOverlayLoader("Processing Payment", "Amount: " + 
                java.text.NumberFormat.getCurrencyInstance(java.util.Locale.CANADA)
                    .format(actualAmount / 100.0));
            
            // Navigate to payment collection with actual amount and parking details
            if (navigationListener != null) {
                navigationListener.onRequestPayment(
                    actualAmount, // Actual amount from rate step
                    "cad", // Canadian dollars
                    false, // skipTipping
                    false, // extendedAuth
                    false, // incrementalAuth
                    com.parkmeter.og.model.OfflineBehaviorSelection.DEFAULT,
                    vehicleNumber, // Plate number
                    selectedZone, // Zone object (not just name)
                    selectedRate, // Rate object (not just name)
                    selectedRateStep // RateStep object (not just time desc)
                );
            }
        }
    }
    
    private void handleZeroAmountPayment() {
        updateOverlayLoader("Processing Free Parking", "Setting up parking session...");
        
        // Get current time and calculate end time
        java.util.Date currentTime = new java.util.Date();
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMMM dd'st' yyyy, hh:mm a", java.util.Locale.US);
        String fromTime = dateFormat.format(currentTime);
        
        // Calculate end time based on time description
        String toTime = calculateEndTime(currentTime, selectedRateStep.getTimeDesc());
        
        // Generate parking_id (6-digit random number between 100000 and 999999)
        int parkingId = (int)(100000 + Math.random() * 900000);
        
        // Create park vehicle request
        ParkVehicleRequest request = new ParkVehicleRequest(
            "", // paymentMethod - empty for zero amount
            String.valueOf(selectedRateStep.getTotal()), // amount in cents
            vehicleNumber, // plate
            selectedZone.getId(), // zone
            selectedZone.getCity() != null ? selectedZone.getCity().getId() : "", // city
            fromTime, // from
            toTime, // to
            selectedRate.getId(), // rate
            selectedRateStep.getServiceFee(), // service_fee from rate step
            selectedZone.getOrganization().getId(), // org
            "meter", // source
            String.valueOf(parkingId) // parking_id
        );
        
        // Call park_vehicle API (only for zero amount payments)
        Park45ApiService apiService = Park45ApiClient.getInstance().getApiService();
        apiService.parkVehicle(request).enqueue(new retrofit2.Callback<ParkVehicleResponse>() {
            @Override
            public void onResponse(Call<ParkVehicleResponse> call, Response<ParkVehicleResponse> response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (response.isSuccessful() && response.body() != null) {
                            ParkVehicleResponse parkResponse = response.body();
                            
                            // Navigate to email receipt with _id (as required by email API)
                            if (navigationListener != null) {
                                navigationListener.onPaymentSuccessful(0, parkResponse.getId(), "");
                            }
                        } else {
                            android.widget.Toast.makeText(getContext(), LiteralsHelper.getText(getContext(), "failed_to_process_parking"), android.widget.Toast.LENGTH_SHORT).show();
                            hideOverlayLoader();
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Call<ParkVehicleResponse> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(getContext(), LiteralsHelper.getText(getContext(), "network_error_try_again"), android.widget.Toast.LENGTH_SHORT).show();
                        hideOverlayLoader();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop discovery if running
        if (discoveryTask != null) {
            discoveryTask.cancel(new Callback() {
                @Override
                public void onSuccess() {
                    // Discovery stopped
                }
                
                @Override
                public void onFailure(@NonNull TerminalException e) {
                    // Ignore failure
                }
            });
        }
        
        // Remove overlay loader
        if (overlayLoader != null && overlayLoader.getParent() != null) {
            ((ViewGroup) overlayLoader.getParent()).removeView(overlayLoader);
        }
    }
}
