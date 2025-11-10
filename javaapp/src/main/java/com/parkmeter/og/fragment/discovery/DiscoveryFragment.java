package com.parkmeter.og.fragment.discovery;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.parkmeter.og.network.RemoteLogger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;


import com.parkmeter.og.MainActivity;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.databinding.FragmentDiscoveryBinding;

import com.parkmeter.og.viewmodel.DiscoveryViewModel;
import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.callable.DiscoveryListener;
import com.stripe.stripeterminal.external.callable.MobileReaderListener;
import com.stripe.stripeterminal.external.callable.ReaderCallback;
import com.stripe.stripeterminal.external.models.BatteryStatus;
import com.stripe.stripeterminal.external.models.DisconnectReason;
import com.stripe.stripeterminal.external.models.ConnectionConfiguration;
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration;

import com.stripe.stripeterminal.external.models.Reader;
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage;
import com.stripe.stripeterminal.external.models.ReaderEvent;
import com.stripe.stripeterminal.external.models.ReaderInputOptions;
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate;
import com.stripe.stripeterminal.external.models.TerminalException;
import com.stripe.stripeterminal.external.models.TerminalErrorCode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The `DiscoveryFragment` shows the list of recognized readers and allows the user to
 * select one to connect to.
 */
public class DiscoveryFragment extends Fragment implements DiscoveryListener, MobileReaderListener {

    public static final String TAG = "com.parkmeter.og.fragment.discovery.DiscoveryFragment";
    private static final String SIMULATED_KEY = "simulated";
    private static final String DISCOVERY_METHOD = "discovery_method";

    public static DiscoveryFragment newInstance(boolean simulated, DiscoveryMethod discoveryMethod) {
        final DiscoveryFragment fragment = new DiscoveryFragment();
        final Bundle bundle = new Bundle();
        bundle.putBoolean(SIMULATED_KEY, simulated);
        bundle.putSerializable(DISCOVERY_METHOD, discoveryMethod);
        fragment.setArguments(bundle);
        return fragment;
    }

    private DiscoveryViewModel viewModel;
    private WeakReference<MainActivity> activityRef;

    // Register the permissions callback to handles the response to the system permissions dialog.
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            this::onPermissionResult
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        DiscoveryViewModelFactory discoveryViewModelFactory = new DiscoveryViewModelFactory(requireArguments());
        viewModel = new ViewModelProvider(this, discoveryViewModelFactory).get(DiscoveryViewModel.class);
        viewModel.navigationListener = (NavigationListener) getActivity();
        activityRef = new WeakReference<>((MainActivity) getActivity());
        // No need for ReaderClickListener since we auto-connect

        startDiscovery();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NotNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        final FragmentDiscoveryBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_discovery, container, false
        );
        binding.setLifecycleOwner(this);
        binding.setViewModel(viewModel);

        // Hide reader list since we auto-connect
        binding.getRoot().findViewById(R.id.reader_recycler_view).setVisibility(View.GONE);

        binding.getRoot()
                .findViewById(R.id.cancel_button)
                .setOnClickListener(view -> {
                    if (viewModel.discoveryTask != null) {
                        viewModel.discoveryTask.cancel(new Callback() {
                            @Override
                            public void onSuccess() {
                                viewModel.discoveryTask = null;
                                final MainActivity activity = activityRef.get();
                                if (activity != null) {
                                    activity.runOnUiThread(activity::onCancelDiscovery);
                                }
                            }

                            @Override
                            public void onFailure(@NotNull TerminalException e) {
                                viewModel.discoveryTask = null;
                            }
                        });
                    }
                });

        // Add view logs button
        binding.getRoot()
                .findViewById(R.id.view_logs_button)
                .setOnClickListener(view -> {
                    final MainActivity activity = activityRef.get();
                    if (activity != null) {
                        activity.onRequestViewLogs();
                    }
                });

        return binding.getRoot();
    }

    @Override
    public void onStartInstallingUpdate(@NotNull ReaderSoftwareUpdate update, @Nullable Cancelable cancelable) {
        viewModel.isConnecting.setValue(false);
        viewModel.isUpdating.setValue(false);
        viewModel.discoveryTask = cancelable;
    }

    @Override
    public void onReportReaderSoftwareUpdateProgress(float progress) {
        viewModel.updateProgress.setValue(progress);
    }

    @Override
    public void onUpdateDiscoveredReaders(@NotNull List<Reader> readers) {
        final MainActivity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                viewModel.readers.setValue(readers);
                
                // Auto-connect to first reader if found
                if (!readers.isEmpty()) {
                    Reader firstReader = readers.get(0);
                    autoConnectToReader(firstReader);
                }
            });
        }
    }
    
    private void autoConnectToReader(@NotNull Reader reader) {
        // Use fixed location ID
        String connectLocationId = "tml_GJv9FgsphhQmKS";
        
        ReaderCallback readerCallback = new ReaderCallback() {
            @Override
            public void onSuccess(@NotNull Reader connectedReader) {
                final MainActivity activity = activityRef.get();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        viewModel.isConnecting.setValue(false);
                        viewModel.isUpdating.setValue(false);
                        activity.onConnectReader();
                    });
                }
            }

            @Override
            public void onFailure(@NotNull TerminalException e) {
                final MainActivity activity = activityRef.get();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        viewModel.isConnecting.setValue(false);
                        viewModel.isUpdating.setValue(false);
                        activity.onCancelDiscovery();
                    });
                }
            }
        };
        
        viewModel.isConnecting.setValue(true);
        
        Terminal.getInstance().connectReader(
            reader,
            new ConnectionConfiguration.TapToPayConnectionConfiguration(
                connectLocationId,
                true,
                activityRef.get()
            ),
            readerCallback
        );
    }

    // Unused imports
    @Override
    public void onFinishInstallingUpdate(@Nullable ReaderSoftwareUpdate update, @Nullable TerminalException e) { }

    @Override
    public void onRequestReaderInput(@NotNull ReaderInputOptions options) { }

    @Override
    public void onRequestReaderDisplayMessage(@NotNull ReaderDisplayMessage message) { }

    @Override
    public void onReportAvailableUpdate(@NotNull ReaderSoftwareUpdate update) { }

    @Override
    public void onReportReaderEvent(@NotNull ReaderEvent event) { }

    @Override
    public void onReportLowBatteryWarning() { }

    @Override
    public void onBatteryLevelUpdate(float batteryLevel, @NonNull BatteryStatus batteryStatus, boolean isCharging) { }

    @Override
    public void onDisconnect(@NonNull DisconnectReason reason) { }



    private void onPermissionResult(Map<String, Boolean> permissions) {
        // If none of the requested permissions were declined, start the discovery process.
        boolean allPermissionsGranted = permissions.entrySet().stream().allMatch(Map.Entry::getValue);

        if (allPermissionsGranted) {
            startDiscovery();
        } else {
            ((MainActivity) requireActivity()).onCancelDiscovery();
        }
    }

    private void startDiscovery() {
        // startDiscovery called
        
        if (getArguments() != null) {
            final Callback discoveryCallback = new Callback() {
                @Override
                public void onSuccess() {
                    viewModel.discoveryTask = null;
                    // Don't navigate away - let auto-connection handle it
                }

                @Override
                public void onFailure(@NotNull TerminalException e) {
                    viewModel.discoveryTask = null;
                    final MainActivity activity = activityRef.get();
                    if (activity != null) {
                        activity.onCancelDiscovery();
                    }
                }
            };

            DiscoveryMethod discoveryMethod = (DiscoveryMethod) getArguments().getSerializable(DISCOVERY_METHOD);
            
            if (checkPermission(discoveryMethod)) {
                // Force Tap to Pay with simulated mode = false for live mode
                boolean isSimulated = false;
                
                // Force Tap to Pay discovery only
                final DiscoveryConfiguration config = new DiscoveryConfiguration.TapToPayDiscoveryConfiguration(false);
                if (viewModel.discoveryTask == null && Terminal.getInstance().getConnectedReader() == null) {
                    viewModel.discoveryTask = Terminal
                            .getInstance()
                            .discoverReaders(config, this, discoveryCallback);
                }
            }
        }
    }

    private boolean checkPermission(DiscoveryMethod discoveryMethod) {
        boolean hasGpsModule = requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        String locationPermission;
        if (hasGpsModule) {
            locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;
        } else {
            locationPermission = Manifest.permission.ACCESS_COARSE_LOCATION;
        }
        
        List<String> ungrantedPermissions = new ArrayList<>();
        if (!isGranted(locationPermission)) {
            ungrantedPermissions.add(locationPermission);
        }

        if (discoveryMethod == DiscoveryMethod.BLUETOOTH_SCAN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isGranted(Manifest.permission.BLUETOOTH_SCAN)) {
                ungrantedPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (!isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
                ungrantedPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        if (!ungrantedPermissions.isEmpty()) {
            String[] ungrantedPermissionsArray = new String[ungrantedPermissions.size()];
            ungrantedPermissionsArray = ungrantedPermissions.toArray(ungrantedPermissionsArray);
            requestPermissionLauncher.launch(ungrantedPermissionsArray);
            return false;
        } else {
            return true;
        }
    }

    private boolean isGranted(String permission) {
        boolean granted = ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED;
        return granted;
    }

    static class DiscoveryViewModelFactory implements ViewModelProvider.Factory {
        private Bundle args;

        public DiscoveryViewModelFactory(Bundle args) {
            this.args = args;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new DiscoveryViewModel((DiscoveryMethod) args.getSerializable(DISCOVERY_METHOD));
        }
    }
}
