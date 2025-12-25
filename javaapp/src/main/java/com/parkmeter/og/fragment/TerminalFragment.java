package com.parkmeter.og.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.parkmeter.og.MainActivity;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.databinding.FragmentTerminalBinding;
import com.parkmeter.og.fragment.discovery.DiscoveryMethod;
import com.parkmeter.og.network.RemoteLogger;
import com.parkmeter.og.viewmodel.TerminalViewModel;
import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.callable.DiscoveryListener;
import com.stripe.stripeterminal.external.callable.ReaderCallback;
import com.stripe.stripeterminal.external.models.ConnectionConfiguration;
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration;
import com.stripe.stripeterminal.external.models.Reader;
import com.stripe.stripeterminal.external.models.TerminalException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The `TerminalFragment` is the main [Fragment] shown in the app, and handles background discovery
 * and connection to Tap to Pay readers.
 */
public class TerminalFragment extends Fragment implements DiscoveryListener {

    public static final String TAG = "com.parkmeter.og.fragment.TerminalFragment";
    private static final String SIMULATED_SWITCH = "simulated_switch";
    private static final String DISCOVERY_METHOD = "discovery_method";

    private List<DiscoveryMethod> discoveryMethods = new ArrayList<>();
    private TerminalViewModel viewModel;
    private Cancelable discoveryTask;

    // Permission launcher for location and internet permissions
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            this::onPermissionResult
    );

    public static DiscoveryMethod getCurrentDiscoveryMethod(Activity activity) {
        int pos = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE)
                .getInt(DISCOVERY_METHOD, 0);
        return DiscoveryMethod.values()[pos];
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        discoveryMethods.add(DiscoveryMethod.BLUETOOTH_SCAN);
        discoveryMethods.add(DiscoveryMethod.INTERNET);
        discoveryMethods.add(DiscoveryMethod.TAP_TO_PAY);
        discoveryMethods.add(DiscoveryMethod.USB);

        if (getArguments() != null) {
            viewModel = new TerminalViewModel(getArguments().getBoolean(SIMULATED_SWITCH),
                    (DiscoveryMethod) getArguments().getSerializable(DISCOVERY_METHOD),
                    discoveryMethods);
        } else {
            final FragmentActivity activity = getActivity();
            final boolean isSimulated;
            final int discoveryMethod;
            if (activity != null) {
                final SharedPreferences prefs = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE);
                if (prefs != null) {
                    isSimulated = prefs.getBoolean(SIMULATED_SWITCH, false);
                    discoveryMethod = prefs.getInt(DISCOVERY_METHOD, 0);
                } else {
                    isSimulated = false;
                    discoveryMethod = 0;
                }
            } else {
                isSimulated = false;
                discoveryMethod = 0;
            }
            viewModel = new TerminalViewModel(isSimulated, discoveryMethods.get(discoveryMethod), discoveryMethods);
        }
    }

    @Override
    public @Nullable View onCreateView(
            @NotNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        final FragmentTerminalBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_terminal, container, false);
        binding.setLifecycleOwner(this);
        binding.setViewModel(viewModel);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Start background Tap to Pay discovery
        startBackgroundDiscovery();

        view.findViewById(R.id.discover_button).setOnClickListener(v -> {
            // Restart discovery
            startBackgroundDiscovery();
        });

        // Add log viewer button
        view.findViewById(R.id.view_logs_button).setOnClickListener(v -> {
            final FragmentActivity activity2 = getActivity();
            if (activity2 instanceof NavigationListener) {
                ((NavigationListener) activity2).onRequestViewLogs();
            }
        });

        // Spinner removed from layout, no longer needed
    }

    private void startBackgroundDiscovery() {
        // Starting background Tap to Pay discovery
        
        // Check permissions first
        if (!checkPermissions()) {
            // Permissions not granted, requesting permissions
            requestPermissions();
            return;
        }

        // Check internet connectivity
        if (!checkInternetConnectivity()) {
            // No internet connectivity available
            viewModel.setNoInternet();
            return;
        }

        viewModel.setConnecting(true);

        // Cancel any existing discovery
        if (discoveryTask != null) {
            discoveryTask.cancel(new Callback() {
                @Override
                public void onSuccess() {
                    // Previous discovery cancelled
                }

                @Override
                public void onFailure(@NotNull TerminalException e) {
                    // Failed to cancel previous discovery
                }
            });
        }

        // Start new discovery
        final DiscoveryConfiguration config = new DiscoveryConfiguration.TapToPayDiscoveryConfiguration(false);
        // Created TapToPayDiscoveryConfiguration with simulated: false

        if (Terminal.getInstance().getConnectedReader() == null) {
            // Starting Terminal.discoverReaders with config
            discoveryTask = Terminal.getInstance().discoverReaders(config, this, new Callback() {
                @Override
                public void onSuccess() {
                    // Background discovery started successfully
                }

                @Override
                public void onFailure(@NotNull TerminalException e) {
                    // Background discovery failed
                    viewModel.setConnectionFailed(e.getMessage());
                }
            });
            // Background discovery task started
        } else {
            // Reader already connected, skipping discovery
            viewModel.setConnected(Terminal.getInstance().getConnectedReader());
        }
    }

    private boolean checkPermissions() {
        // Checking permissions
        
        // Check location permissions
        boolean hasLocationPermission = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasLocationPermission = ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            hasLocationPermission = ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        // Check internet permission (ACCESS_NETWORK_STATE)
        boolean hasInternetPermission = ContextCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;

        // Location permission and Internet permission status

        return hasLocationPermission && hasInternetPermission;
    }

    private void requestPermissions() {
        // Requesting permissions
        
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE
            };
        }

        requestPermissionLauncher.launch(permissions);
    }

    private void onPermissionResult(Map<String, Boolean> permissions) {
        // Permission result received
        
        boolean allGranted = true;
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            // Permission status
            if (!entry.getValue()) {
                allGranted = false;
            }
        }

        if (allGranted) {
            // All permissions granted, starting discovery
            startBackgroundDiscovery();
        } else {
            // Some permissions denied
            viewModel.setPermissionRequired();
        }
    }

    private boolean checkInternetConnectivity() {
        // Checking internet connectivity
        
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            // Internet connectivity status
            return isConnected;
        }
        
        // ConnectivityManager is null
        return false;
    }

    // DiscoveryListener implementation
    public void onStartDiscovery(@NotNull DiscoveryConfiguration config) {
        // Background discovery started
    }

    public void onStopDiscovery() {
        // Background discovery stopped
        discoveryTask = null;
    }

    @Override
    public void onUpdateDiscoveredReaders(@NotNull List<Reader> readers) {
        // Background discovery found readers
        
        if (!readers.isEmpty()) {
            Reader firstReader = readers.get(0);
            // Auto-connecting to first reader
            autoConnectToReader(firstReader);
        }
    }

    private void autoConnectToReader(@NotNull Reader reader) {
        // Use fixed location ID
        String connectLocationId = "tml_GJv9FgsphhQmKS";
        // Using fixed location ID
        
        ReaderCallback readerCallback = new ReaderCallback() {
            @Override
            public void onSuccess(@NotNull Reader connectedReader) {
                // Background auto-connection successful
                viewModel.setConnected(connectedReader);
            }

            @Override
            public void onFailure(@NotNull TerminalException e) {
                // Background auto-connection failed
                viewModel.setConnectionFailed(e.getMessage());
            }
        };
        
        // Starting background auto-connection to Tap to Pay reader
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
            readerCallback
        );
    }

    public TerminalViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void onPause() {
        super.onPause();
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            final SharedPreferences prefs = activity.getSharedPreferences(TAG, Context.MODE_PRIVATE);
            if (prefs != null) {
                prefs.edit().putBoolean(SIMULATED_SWITCH, viewModel.simulated.getValue()).apply();
                prefs.edit().putInt(DISCOVERY_METHOD, viewModel.discoveryMethodPosition.getValue()).apply();
            }
        }
    }
}
