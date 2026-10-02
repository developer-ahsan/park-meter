package com.parkmeter.og.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.StripeTerminalApplication;
import com.parkmeter.og.model.AgentLoginResponse;
import com.parkmeter.og.model.AppState;
import com.parkmeter.og.model.GetZonesByIdRequest;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.network.Park45ApiClient;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CityZoneSelectionFragment extends Fragment {

    public static final String TAG = "CityZoneSelectionFragment";

    private NavigationListener navigationListener;
    private AppState appState;
    private SharedPreferencesManager prefsManager;

    private AutoCompleteTextView acvCity;
    private AutoCompleteTextView acvZone;
    private LinearLayout zoneLoadingLayout;
    private MaterialButton btnContinue;

    private List<Zone.City> cities = new ArrayList<>();
    private List<Zone> zones = new ArrayList<>();
    private Zone.City selectedCity;
    private Zone selectedZone;

    private Call<List<Zone>> zonesCall;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof NavigationListener) {
            navigationListener = (NavigationListener) getActivity();
        }
        appState = StripeTerminalApplication.getInstance().getAppState();
        prefsManager = new SharedPreferencesManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_city_zone_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        acvCity = view.findViewById(R.id.acv_city);
        acvZone = view.findViewById(R.id.acv_zone);
        zoneLoadingLayout = view.findViewById(R.id.zone_loading_layout);
        btnContinue = view.findViewById(R.id.btn_continue);

        AppThemeManager.getInstance().applyThemeToFragment(view);

        loadCities();

        btnContinue.setOnClickListener(v -> {
            if (selectedZone != null && navigationListener != null) {
                prefsManager.saveSelectedCity(selectedCity);
                navigationListener.onZoneSelected(selectedZone);
            }
        });
    }

    private void loadCities() {
        AgentLoginResponse.AgentUser user = appState.getLoggedInUser();
        if (user == null) user = prefsManager.getLoggedInUser();

        if (user == null || user.getCities() == null || user.getCities().isEmpty()) {
            Toast.makeText(getContext(), "No cities available. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        cities = user.getCities();

        List<String> cityNames = new ArrayList<>();
        for (Zone.City c : cities) {
            cityNames.add(c.getCityName());
        }

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, cityNames);
        acvCity.setAdapter(cityAdapter);

        acvCity.setOnItemClickListener((parent, v, position, id) -> {
            selectedCity = cities.get(position);
            selectedZone = null;
            acvZone.setText("");
            zones.clear();
            btnContinue.setEnabled(false);
            loadZonesForCity(selectedCity.getId());
        });

        // Restore previously saved city + zone selections
        Zone.City savedCity = prefsManager.getSelectedCity();
        Zone savedZone = prefsManager.getSelectedZone();

        if (savedCity != null) {
            acvCity.setText(savedCity.getCityName(), false);
            selectedCity = savedCity;
            loadZonesForCity(savedCity.getId(), savedZone);
        }
    }

    private void loadZonesForCity(String cityId) {
        loadZonesForCity(cityId, null);
    }

    private void loadZonesForCity(String cityId, Zone preSelectedZone) {
        if (cityId == null || cityId.isEmpty()) return;

        zoneLoadingLayout.setVisibility(View.VISIBLE);
        acvZone.setEnabled(false);

        if (zonesCall != null) zonesCall.cancel();
        zonesCall = Park45ApiClient.getInstance().getApiService()
                .getZonesById(new GetZonesByIdRequest(cityId));
        zonesCall.enqueue(new Callback<List<Zone>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Zone>> call,
                                           @NonNull Response<List<Zone>> response) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            zoneLoadingLayout.setVisibility(View.GONE);
                            acvZone.setEnabled(true);
                            if (response.isSuccessful() && response.body() != null) {
                                zones = response.body();
                                populateZoneDropdown(preSelectedZone);
                            } else {
                                Toast.makeText(getContext(), "Failed to load zones.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Zone>> call, @NonNull Throwable t) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            zoneLoadingLayout.setVisibility(View.GONE);
                            acvZone.setEnabled(true);
                            Toast.makeText(getContext(),
                                    "Network error loading zones: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void populateZoneDropdown(@Nullable Zone preSelectedZone) {
        List<String> zoneNames = new ArrayList<>();
        for (Zone z : zones) {
            zoneNames.add(z.getZoneName());
        }

        ArrayAdapter<String> zoneAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, zoneNames);
        acvZone.setAdapter(zoneAdapter);

        acvZone.setOnItemClickListener((parent, v, position, id) -> {
            selectedZone = zones.get(position);
            btnContinue.setEnabled(true);
        });

        // Pre-select saved zone if it's in the list
        if (preSelectedZone != null) {
            for (int i = 0; i < zones.size(); i++) {
                if (zones.get(i).getId().equals(preSelectedZone.getId())) {
                    selectedZone = zones.get(i);
                    acvZone.setText(selectedZone.getZoneName(), false);
                    btnContinue.setEnabled(true);
                    break;
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (zonesCall != null) {
            zonesCall.cancel();
            zonesCall = null;
        }
        acvCity = null;
        acvZone = null;
        zoneLoadingLayout = null;
        btnContinue = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationListener = null;
    }
}
