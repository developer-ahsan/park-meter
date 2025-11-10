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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.model.GetRateByIdRequest;
import com.parkmeter.og.model.Rate;
import com.parkmeter.og.model.Zone;

import com.parkmeter.og.network.Park45ApiClient;
import com.parkmeter.og.network.Park45ApiService;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.LiteralsHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RateSelectionFragment extends Fragment {

    public static final String TAG = "RateSelectionFragment";
    private static final String ARG_ZONE = "zone";
    private static final String ARG_VEHICLE_NUMBER = "vehicle_number";

    private NavigationListener navigationListener;
    private Zone selectedZone;
    private String vehicleNumber;
    
    private TextView tvVehicleNumber;
    private TextView tvZoneName;
    private ProgressBar progressBar;
    private TextView tvErrorMessage;
    private com.google.android.material.button.MaterialButton btnRetry;
    private LinearLayout rateTabsContainer;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TabLayoutMediator tabMediator;
    
    private List<Rate> rates = new ArrayList<>();
    private RateSelectionPagerAdapter pagerAdapter;
    private retrofit2.Call<java.util.List<com.parkmeter.og.model.Rate>> ratesCall;

    public static RateSelectionFragment newInstance(Zone zone, String vehicleNumber) {
        RateSelectionFragment fragment = new RateSelectionFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ZONE, zone);
        args.putString(ARG_VEHICLE_NUMBER, vehicleNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedZone = (Zone) getArguments().getSerializable(ARG_ZONE);
            vehicleNumber = getArguments().getString(ARG_VEHICLE_NUMBER);
        }
        
        if (getActivity() instanceof NavigationListener) {
            navigationListener = (NavigationListener) getActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rate_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupUI();
        loadRates();
    }

    private void initializeViews(View view) {
        tvVehicleNumber = view.findViewById(R.id.tv_vehicle_number);
        tvZoneName = view.findViewById(R.id.tv_zone_name);
        progressBar = view.findViewById(R.id.progress_bar);
        tvErrorMessage = view.findViewById(R.id.tv_error_message);
        btnRetry = view.findViewById(R.id.btn_retry);
        rateTabsContainer = view.findViewById(R.id.rate_tabs_container);
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        
        // Setup back button
        ImageView backButton = view.findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        
        btnRetry.setOnClickListener(v -> loadRates());
    }

    private void setupUI() {
        // Apply theming
        AppThemeManager.getInstance().applyThemeToFragment(getView());
        
        // Display vehicle number and zone name
        tvVehicleNumber.setText(vehicleNumber);
        tvZoneName.setText(selectedZone.getZoneName());
        
        // Setup ViewPager - will be configured after loading rates
        pagerAdapter = new RateSelectionPagerAdapter(this, selectedZone, vehicleNumber);
        viewPager.setAdapter(pagerAdapter);
    }

    private void loadRates() {
        showLoading(true);
        hideError();
        
        Park45ApiService apiService = Park45ApiClient.getInstance().getApiService();
        GetRateByIdRequest request = new GetRateByIdRequest(selectedZone.getId(), vehicleNumber);
        
        // Use the configured authorization token
        ratesCall = apiService.getRateById(request);
        
        ratesCall.enqueue(new Callback<List<Rate>>() {
            @Override
            public void onResponse(Call<List<Rate>> call, Response<List<Rate>> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    rates = response.body();
                    if (rates.isEmpty()) {
                        showError(LiteralsHelper.getText(getContext(), "no_rates_available"));
                    } else {
                        setupTabs();
                    }
                } else {
                    String errorMessage = LiteralsHelper.getText(getContext(), "failed_to_load_rates");
                    if (response.code() == 401) {
                        errorMessage = LiteralsHelper.getText(getContext(), "authentication_failed");
                    } else if (response.code() == 404) {
                        errorMessage = LiteralsHelper.getText(getContext(), "zone_or_vehicle_not_found");
                    } else if (response.code() >= 500) {
                        errorMessage = LiteralsHelper.getText(getContext(), "server_error_try_later");
                    }
                    showError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<List<Rate>> call, Throwable t) {
                showLoading(false);
                showError(LiteralsHelper.getText(getContext(), "network_error_check_connection"));
            }
        });
    }

    private void setupTabs() {
        // Clear existing tabs
        tabLayout.removeAllTabs();
        
        // Add tabs for each rate
        for (Rate rate : rates) {
            tabLayout.addTab(tabLayout.newTab().setText(rate.getRateName()));
        }
        
        // Update pager adapter with rates
        pagerAdapter.updateRates(rates);
        
        // Setup ViewPager with tabs
        tabMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(rates.get(position).getRateName());
        });
        tabMediator.attach();
        
        // Apply dynamic org colors to TabLayout
        int orgColor = com.parkmeter.og.utils.AppThemeManager.getInstance().getCurrentOrgColorInt();
        tabLayout.setSelectedTabIndicatorColor(orgColor);
        tabLayout.setTabTextColors(getResources().getColor(R.color.colorTextSecondary), orgColor);
        
        // Show the tabs container
        rateTabsContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tabMediator != null) {
            tabMediator.detach();
            tabMediator = null;
        }
        if (viewPager != null) {
            viewPager.setAdapter(null);
        }
        // Clear view references to avoid leaking the Fragment's view
        tabLayout = null;
        viewPager = null;
        rateTabsContainer = null;
        tvVehicleNumber = null;
        tvZoneName = null;
        progressBar = null;
        tvErrorMessage = null;
        btnRetry = null;
        if (ratesCall != null) {
            ratesCall.cancel();
            ratesCall = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationListener = null;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rateTabsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.VISIBLE);
        rateTabsContainer.setVisibility(View.GONE);
    }

    private void hideError() {
        tvErrorMessage.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
    }

    // ViewPager Adapter for tab content
    private static class RateSelectionPagerAdapter extends FragmentStateAdapter {
        
        private List<Rate> rates = new ArrayList<>();
        private Zone selectedZone;
        private String vehicleNumber;
        
        public RateSelectionPagerAdapter(@NonNull Fragment fragment, Zone selectedZone, String vehicleNumber) {
            super(fragment);
            this.selectedZone = selectedZone;
            this.vehicleNumber = vehicleNumber;
        }
        
        public void updateRates(List<Rate> newRates) {
            this.rates = newRates;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Return RateStepsFragment for each tab
            Rate rate = rates.get(position);
            return RateStepsFragment.newInstance(rate, selectedZone, vehicleNumber);
        }

        @Override
        public int getItemCount() {
            return rates.size();
        }
    }
} 
