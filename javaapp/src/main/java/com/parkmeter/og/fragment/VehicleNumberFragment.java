package com.parkmeter.og.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.LiteralsHelper;

import java.util.regex.Pattern;

public class VehicleNumberFragment extends Fragment {

    public static final String TAG = "VehicleNumberFragment";

    private NavigationListener navigationListener;
    private TextInputEditText vehicleNumberInput;
    private MaterialButton nextButton;

    public static VehicleNumberFragment newInstance() {
        return new VehicleNumberFragment();
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
        return inflater.inflate(R.layout.fragment_vehicle_number, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vehicleNumberInput = view.findViewById(R.id.et_vehicle_number);
        nextButton = view.findViewById(R.id.btn_next);
        ImageView backButton = view.findViewById(R.id.btn_back);

        // Apply theming to the fragment
        AppThemeManager.getInstance().applyThemeToFragment(view);

        // Set up vehicle number input with masking
        setupVehicleNumberInput();

        // Set up button click listeners
        nextButton.setOnClickListener(v -> onNextButtonClicked());
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void setupVehicleNumberInput() {
        vehicleNumberInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                String formatted = formatVehicleNumber(input);
                
                if (!input.equals(formatted)) {
                    vehicleNumberInput.removeTextChangedListener(this);
                    vehicleNumberInput.setText(formatted);
                    vehicleNumberInput.setSelection(formatted.length());
                    vehicleNumberInput.addTextChangedListener(this);
                }
                
                validateVehicleNumber(formatted);
            }
        });
    }

    private String formatVehicleNumber(String input) {
        // Keep only uppercase letters and digits, limit to 8 characters
        String cleaned = input.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (cleaned.length() > 8) {
            cleaned = cleaned.substring(0, 8);
        }
        return cleaned;
    }

    private void validateVehicleNumber(String vehicleNumber) {
        // Accept 1 to 8 chars, uppercase letters and digits only
        Pattern allowed = Pattern.compile("^[A-Z0-9]{1,8}$");
        boolean isValid = allowed.matcher(vehicleNumber).matches();

        nextButton.setEnabled(isValid && vehicleNumber.length() > 0);

        if (vehicleNumber.length() > 0 && !isValid) {
            vehicleNumberInput.setError("Use 1-8 uppercase letters and digits only (e.g., ABCD, 1234, AB12, FF1234)");
        } else {
            vehicleNumberInput.setError(null);
        }
    }

    private void onNextButtonClicked() {
        String vehicleNumber = vehicleNumberInput.getText().toString().trim();

        if (vehicleNumber.isEmpty()) {
            Toast.makeText(getContext(), LiteralsHelper.getText(getContext(), "please_enter_license_plate"), Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate vehicle number format (uppercase letters and digits only, up to 8 chars)
        Pattern allowed = Pattern.compile("^[A-Z0-9]{1,8}$");
        if (!allowed.matcher(vehicleNumber).matches()) {
            Toast.makeText(getContext(), LiteralsHelper.getText(getContext(), "invalid_license_plate_format"), Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to connection screen
        if (navigationListener != null) {
            navigationListener.onVehicleNumberEntered(vehicleNumber);
        }
    }
} 
