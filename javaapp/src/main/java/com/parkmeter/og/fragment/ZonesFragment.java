package com.parkmeter.og.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.parkmeter.og.model.GetZonesRequest;
import com.parkmeter.og.model.Zone;

import com.parkmeter.og.network.Park45ApiClient;
import com.parkmeter.og.network.Park45ApiService;
import com.parkmeter.og.recyclerview.ZonesAdapter;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.LiteralsHelper;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.network.MockApiService;
import com.parkmeter.og.network.Park45ApiClient;
import com.parkmeter.og.network.Park45ApiService;
import com.parkmeter.og.recyclerview.ZonesAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ZonesFragment extends Fragment implements ZonesAdapter.OnZoneClickListener {

    public static final String TAG = "ZonesFragment";

    private NavigationListener navigationListener;
    private RecyclerView rvZones;
    private ZonesAdapter zonesAdapter;
    private TextInputEditText etSearch;
    private LinearLayout loadingLayout;
    private LinearLayout errorLayout;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;
    private ImageView btnBack;
    private String selectedZoneId;
    private retrofit2.Call<java.util.List<com.parkmeter.og.model.Zone>> zonesCall;
    private android.text.TextWatcher searchWatcher;

    public static ZonesFragment newInstance() {
        return new ZonesFragment();
    }

    public static ZonesFragment newInstance(String selectedZoneId) {
        ZonesFragment fragment = new ZonesFragment();
        fragment.selectedZoneId = selectedZoneId;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof NavigationListener) {
            navigationListener = (NavigationListener) getActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_zones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupClickListeners();

        // Apply theming to header/search/icons
        AppThemeManager.getInstance().applyThemeToFragment(view);

        loadZones();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh selection from AppState in case it changed elsewhere
        com.parkmeter.og.model.AppState appState = com.parkmeter.og.StripeTerminalApplication.getInstance().getAppState();
        String latestSelectedId = appState.getSelectedZoneId();
        if (latestSelectedId != null) {
            this.selectedZoneId = latestSelectedId;
            if (zonesAdapter != null) {
                zonesAdapter.setSelectedZoneId(latestSelectedId);
            }
        }
    }

    private void initViews(View view) {
        rvZones = view.findViewById(R.id.rv_zones);
        etSearch = view.findViewById(R.id.et_search);
        loadingLayout = view.findViewById(R.id.loading_layout);
        errorLayout = view.findViewById(R.id.error_layout);
        tvErrorMessage = view.findViewById(R.id.tv_error_message);
        btnRetry = view.findViewById(R.id.btn_retry);
        btnBack = view.findViewById(R.id.btn_back);
    }

    private void setupRecyclerView() {
        zonesAdapter = new ZonesAdapter(this);
        rvZones.setLayoutManager(new LinearLayoutManager(getContext()));
        rvZones.setAdapter(zonesAdapter);
        
        if (selectedZoneId != null) {
            zonesAdapter.setSelectedZoneId(selectedZoneId);
        }
    }

    private void setupSearch() {
        searchWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                zonesAdapter.filterZones(s.toString());
            }
        };
        etSearch.addTextChangedListener(searchWatcher);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnRetry.setOnClickListener(v -> loadZones());
    }

    private void loadZones() {
        showLoading(true);
        showError(false);

        // Make actual API call to getZones
        Park45ApiService apiService = Park45ApiClient.getInstance().getApiService();
        
        // Create request body for POST call
        GetZonesRequest requestBody = new GetZonesRequest();
        
        zonesCall = apiService.getZones(requestBody);

        zonesCall.enqueue(new Callback<List<Zone>>() {
            @Override
            public void onResponse(@NonNull Call<List<Zone>> call, @NonNull Response<List<Zone>> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Zone> zones = response.body();
                    
                    zonesAdapter.setZones(zones);
                    
                    if (selectedZoneId != null) {
                        zonesAdapter.setSelectedZoneId(selectedZoneId);
                        
                        // Verify if the selected zone exists in the loaded zones
                        boolean zoneFound = false;
                        for (Zone zone : zones) {
                            if (selectedZoneId.equals(zone.getId())) {
                                zoneFound = true;
                                break;
                            }
                        }
                    }
                    
                    // Apply theming to zones list
                    applyThemingToZonesList();
                    
                } else {
                    String errorMessage = LiteralsHelper.getText(getContext(), "failed_to_load_zones");
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        // Error reading error body
                    }
                    showError(true, errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Zone>> call, @NonNull Throwable t) {
                showLoading(false);
                String errorMessage = LiteralsHelper.getText(getContext(), "network_error_check_connection") + ": " + t.getMessage();
                showError(true, errorMessage);
            }
        });
    }

    private void showLoading(boolean show) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        rvZones.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(boolean show) {
        showError(show, null);
    }

    private void showError(boolean show, String message) {
        errorLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        rvZones.setVisibility(show ? View.GONE : View.VISIBLE);
        
        if (message != null) {
            tvErrorMessage.setText(message);
        }
    }

    private void applyThemingToZonesList() {
        if (getView() != null) {
            AppThemeManager.getInstance().applyThemeToFragment(getView());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rvZones != null) {
            rvZones.setAdapter(null);
        }
        zonesAdapter = null;
        rvZones = null;
        etSearch = null;
        loadingLayout = null;
        errorLayout = null;
        tvErrorMessage = null;
        btnRetry = null;
        btnBack = null;
        if (zonesCall != null) {
            zonesCall.cancel();
            zonesCall = null;
        }
        if (searchWatcher != null && etSearch != null) {
            etSearch.removeTextChangedListener(searchWatcher);
            searchWatcher = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationListener = null;
    }

    @Override
    public void onZoneClick(Zone zone) {
        if (navigationListener != null) {
            navigationListener.onZoneSelected(zone);
        }
    }
} 
