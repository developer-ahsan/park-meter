package com.parkmeter.og.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.parkmeter.og.BuildConfig;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.model.GetRateStepsRequest;
import com.parkmeter.og.model.ParkingAvailableRequest;
import com.parkmeter.og.model.ParkingAvailableResponse;
import com.parkmeter.og.model.Rate;
import com.parkmeter.og.model.RateStep;
import com.parkmeter.og.model.Zone;

import com.parkmeter.og.network.Park45ApiClient;
import com.parkmeter.og.network.Park45ApiService;
import com.parkmeter.og.utils.LiteralsHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RateStepsFragment extends Fragment {

    private static final String TAG = "RateStepsFragment";
    private static final String ARG_RATE = "rate";
    private static final String ARG_ZONE = "zone";
    private static final String ARG_VEHICLE_NUMBER = "vehicle_number";

    private NavigationListener navigationListener;
    private Rate selectedRate;
    private Zone selectedZone;
    private String vehicleNumber;
    
    private TextView tvCurrentTime;
    private TextView tvDay;
    private TextView tvEndTime;
    private TextView tvRate;
    private TextView tvServiceFee;
    private TextView tvTotal;
    private MaterialButton btnSelect;
    private LinearLayout stepsContainer;
    private ProgressBar progressBar;
    private TextView tvErrorMessage;
    private MaterialCardView cardPayDescription;
    private TextView tvPayDescription;
    private RateStep selectedRateStep;
    
    private List<RateStep> rateSteps = new ArrayList<>();
    private retrofit2.Call<java.util.List<com.parkmeter.og.model.RateStep>> rateStepsCall;
    private retrofit2.Call<com.parkmeter.og.model.ParkingAvailableResponse> parkingAvailableCall;

    public static RateStepsFragment newInstance(Rate rate, Zone zone, String vehicleNumber) {
        RateStepsFragment fragment = new RateStepsFragment();
        Bundle args = new Bundle();
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
            selectedRate = (Rate) getArguments().getSerializable(ARG_RATE);
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
        return inflater.inflate(R.layout.fragment_rate_steps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        // Apply dynamic theming to this fragment's views
        com.parkmeter.og.utils.AppThemeManager.getInstance().applyThemeToFragment(view);
        loadRateSteps();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-establish navigation listener connection if needed
        if (navigationListener == null && getActivity() instanceof NavigationListener) {
            navigationListener = (NavigationListener) getActivity();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Cancel ongoing network calls when fragment is paused to prevent memory leaks
        if (rateStepsCall != null) {
            rateStepsCall.cancel();
        }
        if (parkingAvailableCall != null) {
            parkingAvailableCall.cancel();
        }
    }

    private void initializeViews(View view) {
        // Summary card views (from include)
        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvDay = view.findViewById(R.id.tv_day);
        tvEndTime = view.findViewById(R.id.tv_end_time);
        tvRate = view.findViewById(R.id.tv_rate);
        tvServiceFee = view.findViewById(R.id.tv_service_fee);
        tvTotal = view.findViewById(R.id.tv_total);

        stepsContainer = view.findViewById(R.id.steps_container);
        btnSelect = view.findViewById(R.id.btn_select);
        progressBar = view.findViewById(R.id.progress_bar);
        tvErrorMessage = view.findViewById(R.id.tv_error_message);
        cardPayDescription = view.findViewById(R.id.card_pay_description);
        tvPayDescription = view.findViewById(R.id.tv_pay_description);

        btnSelect.setOnClickListener(v -> {
            if (selectedRateStep != null) {
                checkParkingAvailability();
            }
        });

        // Setup dynamic pay description card
        setupPayDescriptionCard();
        
        // Test literals system (remove in production)
        if (BuildConfig.DEBUG) {
            com.parkmeter.og.utils.LiteralsVerificationReport.generateReport(getContext());
        }
    }

    private void setupPayDescriptionCard() {
        // Check if fragment is still attached and views are valid
        if (!isAdded() || getActivity() == null || cardPayDescription == null || tvPayDescription == null) {
            return;
        }
        
        try {
            // Get dynamic text from literals with fallback to string resources
            String payDescriptionText = LiteralsHelper.getText(getContext(), "btn_pay_desc");
            if (payDescriptionText != null && !payDescriptionText.trim().isEmpty()) {
                tvPayDescription.setText(payDescriptionText);
            } else {
                // Fallback to a default text if both CSV and string resources fail
                tvPayDescription.setText("Click to pay");
            }
            
            // Get organization color for dynamic border
            int orgColor = com.parkmeter.og.utils.AppThemeManager.getInstance().getCurrentOrgColorInt();
            cardPayDescription.setStrokeColor(orgColor);
            tvPayDescription.setTextColor(orgColor);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up pay description card: " + e.getMessage());
            // Fallback to default text
            if (tvPayDescription != null) {
                tvPayDescription.setText("Click to pay");
            }
        }
    }

    private void loadRateSteps() {
        showLoading(true);
        hideError();
        
        Park45ApiService apiService = Park45ApiClient.getInstance().getApiService();
        
        // Create request with dynamic timezone from selected zone's city
        String timeZone = selectedZone.getCity() != null && selectedZone.getCity().getTimeZone() != null 
                ? selectedZone.getCity().getTimeZone() 
                : "America/New_York"; // Fallback to default timezone
        
        GetRateStepsRequest request = new GetRateStepsRequest(
            selectedRate.getId(),
            vehicleNumber,
            selectedRate.getRateType(),
            selectedRate.isQrCode(),
            selectedZone.getOrganization().getId(),
            timeZone,
            selectedZone.getId()
        );
        
        rateStepsCall = apiService.getRateSteps(request);
        
        rateStepsCall.enqueue(new Callback<List<RateStep>>() {
            @Override
            public void onResponse(Call<List<RateStep>> call, Response<List<RateStep>> response) {
                // Check if fragment is still attached to prevent memory leaks
                if (!isAdded() || getActivity() == null) {
                    return;
                }
                
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    rateSteps = response.body();
                    
                    if (rateSteps.isEmpty()) {
                        showError(LiteralsHelper.getText(getContext(), "no_rate_steps_available"));
                    } else {
                        setupStepsUI(rateSteps);
                        showContent();
                    }
                } else {
                    String errorMessage = LiteralsHelper.getText(getContext(), "failed_to_load_rate_steps");
                    if (response.code() == 401) {
                        errorMessage = LiteralsHelper.getText(getContext(), "authentication_failed");
                    } else if (response.code() == 404) {
                        errorMessage = LiteralsHelper.getText(getContext(), "rate_steps_not_found");
                    } else if (response.code() >= 500) {
                        errorMessage = LiteralsHelper.getText(getContext(), "server_error_try_later");
                    }
                    showError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<List<RateStep>> call, Throwable t) {
                // Check if fragment is still attached to prevent memory leaks
                if (!isAdded() || getActivity() == null) {
                    return;
                }
                
                showLoading(false);
                showError(LiteralsHelper.getText(getContext(), "network_error_check_connection"));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Cancel network calls to prevent memory leaks
        if (rateStepsCall != null) {
            rateStepsCall.cancel();
            rateStepsCall = null;
        }
        if (parkingAvailableCall != null) {
            parkingAvailableCall.cancel();
            parkingAvailableCall = null;
        }
        
        // Clear view references to prevent memory leaks
        stepsContainer = null;
        btnSelect = null;
        tvCurrentTime = null;
        tvDay = null;
        tvEndTime = null;
        tvRate = null;
        tvServiceFee = null;
        tvTotal = null;
        progressBar = null;
        tvErrorMessage = null;
        cardPayDescription = null;
        tvPayDescription = null;
        
        // Clear data collections
        if (rateSteps != null) {
            rateSteps.clear();
            rateSteps = null;
        }
        
        // Clear selected rate step reference
        selectedRateStep = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationListener = null;
    }

    private void onRateStepSelected(RateStep rateStep) {
        // Rate step selected
        selectedRateStep = rateStep;
        updateSummaryCard(rateStep);
        updateBottomButton(rateStep);
        refreshButtonsSelection();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        View root = getView();
        if (root != null) {
            View summaryCard = root.findViewById(R.id.summary_card);
            if (summaryCard != null) {
                summaryCard.setVisibility(show ? View.GONE : View.VISIBLE);
            }
            if (stepsContainer != null) stepsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
            if (btnSelect != null) btnSelect.setVisibility(show ? View.GONE : View.VISIBLE);
            if (cardPayDescription != null) cardPayDescription.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (tvErrorMessage != null) {
            tvErrorMessage.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (tvErrorMessage != null) {
            tvErrorMessage.setText(message);
            tvErrorMessage.setVisibility(View.VISIBLE);
        }
        if (stepsContainer != null) stepsContainer.setVisibility(View.GONE);
        View root = getView();
        if (root != null) {
            View summaryCard = root.findViewById(R.id.summary_card);
            if (summaryCard != null) {
                summaryCard.setVisibility(View.GONE);
            }
            if (btnSelect != null) btnSelect.setVisibility(View.GONE);
            if (cardPayDescription != null) cardPayDescription.setVisibility(View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void hideError() {
        if (tvErrorMessage != null) {
            tvErrorMessage.setVisibility(View.GONE);
        }
    }

    private void showContent() {
        View root = getView();
        if (root != null) {
            View summaryCard = root.findViewById(R.id.summary_card);
            if (summaryCard != null) {
                summaryCard.setVisibility(View.VISIBLE);
            }
        }
        if (stepsContainer != null) stepsContainer.setVisibility(View.VISIBLE);
        if (btnSelect != null) btnSelect.setVisibility(View.VISIBLE);
        if (cardPayDescription != null) cardPayDescription.setVisibility(View.VISIBLE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (tvErrorMessage != null) tvErrorMessage.setVisibility(View.GONE);
    }

    private void setupStepsUI(List<RateStep> steps) {
        // Default to first step
        selectedRateStep = steps.get(0);
        updateSummaryCard(selectedRateStep);
        updateBottomButton(selectedRateStep);

        if (stepsContainer == null) return;
        stepsContainer.removeAllViews();

        int orgColor = com.parkmeter.og.utils.AppThemeManager.getInstance().getCurrentOrgColorInt();
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        LinearLayout currentRow = null;
        for (int i = 0; i < steps.size(); i++) {
            // Create a new row every 2 buttons
            if (i % 2 == 0) {
                currentRow = new LinearLayout(requireContext());
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                stepsContainer.addView(currentRow);
            }
            
            RateStep step = steps.get(i);
            MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(16, 8, 16, 8);
            button.setLayoutParams(lp);
            button.setText(currencyFormat.format(step.getRate() / 100.0) + " | " + extractTime(step.getTimeDesc()));
            button.setAllCaps(false);
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(orgColor));
            button.setStrokeWidth(2);
            button.setRippleColor(android.content.res.ColorStateList.valueOf(orgColor));
            button.setOnClickListener(v -> onRateStepSelected(step));
            button.setTag(step);
            if (currentRow != null) {
                currentRow.addView(button);
            }
        }

        refreshButtonsSelection();
    }

    private void refreshButtonsSelection() {
        if (stepsContainer == null) return;
        int orgColor = com.parkmeter.og.utils.AppThemeManager.getInstance().getCurrentOrgColorInt();

        // Iterate through rows (LinearLayouts)
        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            View row = stepsContainer.getChildAt(i);
            if (row instanceof LinearLayout) {
                LinearLayout rowLayout = (LinearLayout) row;
                // Iterate through buttons in each row
                for (int j = 0; j < rowLayout.getChildCount(); j++) {
                    View child = rowLayout.getChildAt(j);
                    if (child instanceof MaterialButton) {
                        MaterialButton btn = (MaterialButton) child;
                        RateStep step = (RateStep) btn.getTag();
                        boolean selected = selectedRateStep != null && step == selectedRateStep;
                        if (selected) {
                            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orgColor));
                            btn.setTextColor(getResources().getColor(android.R.color.white));
                        } else {
                            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                            btn.setTextColor(orgColor);
                        }
                    }
                }
            }
        }
    }

    private void updateSummaryCard(RateStep step) {
        // Check if fragment is still attached and step is valid
        if (!isAdded() || step == null) {
            return;
        }
        
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        int orgColor = com.parkmeter.og.utils.AppThemeManager.getInstance().getCurrentOrgColorInt();
        
        if (tvCurrentTime != null) tvCurrentTime.setText(step.getCurrentTime());
        if (tvDay != null) {
            tvDay.setText(step.getDay());
            tvDay.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orgColor));
        }
        if (tvEndTime != null) tvEndTime.setText(extractTime(step.getTimeDesc()));
        if (tvRate != null) tvRate.setText(currencyFormat.format(step.getRate() / 100.0));
        if (tvServiceFee != null) tvServiceFee.setText(currencyFormat.format(step.getServiceFee() / 100.0));
        if (tvTotal != null) {
            tvTotal.setText(currencyFormat.format(step.getTotal() / 100.0));
            // Apply org color to total background by finding the parent LinearLayout
            View totalContainer = tvTotal.getParent() instanceof View ? (View) tvTotal.getParent() : null;
            if (totalContainer != null) {
                totalContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orgColor));
            }
        }
        
        // Apply org color to card border
        View root = getView();
        if (root != null) {
            View summaryCard = root.findViewById(R.id.summary_card);
            if (summaryCard instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) summaryCard).setStrokeColor(orgColor);
            }
        }
    }

    private void updateBottomButton(RateStep step) {
        // Check if fragment is still attached and views are valid
        if (!isAdded() || btnSelect == null || step == null) {
            return;
        }
        
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        btnSelect.setText(currencyFormat.format(step.getTotal() / 100.0));
        int orgColor = com.parkmeter.og.utils.AppThemeManager.getInstance().getCurrentOrgColorInt();
        btnSelect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orgColor));
        btnSelect.setTextColor(getResources().getColor(android.R.color.white));
    }

    private String extractTime(String timeDesc) {
        if (timeDesc == null) return "";
        int commaIndex = timeDesc.indexOf(',');
        if (commaIndex >= 0 && commaIndex + 1 < timeDesc.length()) {
            return timeDesc.substring(commaIndex + 1).trim();
        }
        return timeDesc;
    }

    private void checkParkingAvailability() {
        if (selectedRateStep == null || selectedRate == null || selectedZone == null || vehicleNumber == null) {
            Toast.makeText(requireContext(), LiteralsHelper.getText(getContext(), "missing_required_data"), Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple calls
        btnSelect.setEnabled(false);
        btnSelect.setText(LiteralsHelper.getText(getContext(), "checking_availability"));

        Park45ApiService apiService = Park45ApiClient.getInstance().getApiService();
        
        // Create parking availability request
        ParkingAvailableRequest request = new ParkingAvailableRequest(
            vehicleNumber,                                    // plate
            selectedZone.getId(),                            // zone
            selectedZone.getCity() != null ? selectedZone.getCity().getId() : "", // city
            selectedRate.getId(),                            // rate
            selectedZone.getOrganization().getId()           // org
        );
        
                    parkingAvailableCall = apiService.checkParkingAvailable(request);
        
        parkingAvailableCall.enqueue(new Callback<ParkingAvailableResponse>() {
            @Override
            public void onResponse(Call<ParkingAvailableResponse> call, Response<ParkingAvailableResponse> response) {
                // Check if fragment is still attached to prevent memory leaks
                if (!isAdded() || getActivity() == null) {
                    return;
                }
                
                // Re-enable button
                if (btnSelect != null) {
                    btnSelect.setEnabled(true);
                    updateBottomButton(selectedRateStep);
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    ParkingAvailableResponse availabilityResponse = response.body();
                    
                    if (availabilityResponse.isSuccess()) {
                        // Parking is available - proceed to payment
                        Toast.makeText(requireContext(), availabilityResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        if (navigationListener != null) {
                            navigationListener.onRateStepSelected(selectedRateStep, selectedRate, selectedZone, vehicleNumber);
                        }
                    } else {
                        // Parking is not available - show error message
                        Toast.makeText(requireContext(), availabilityResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMessage = "Failed to check parking availability";
                    if (response.code() == 401) {
                        errorMessage = "Authentication failed. Please check your credentials.";
                    } else if (response.code() >= 500) {
                        errorMessage = "Server error. Please try again later.";
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ParkingAvailableResponse> call, Throwable t) {
                // Check if fragment is still attached to prevent memory leaks
                if (!isAdded() || getActivity() == null) {
                    return;
                }
                
                // Re-enable button
                if (btnSelect != null) {
                    btnSelect.setEnabled(true);
                    updateBottomButton(selectedRateStep);
                }
                
                Toast.makeText(requireContext(), LiteralsHelper.getText(getContext(), "network_error_check_connection"), Toast.LENGTH_LONG).show();
            }
        });
    }
} 
