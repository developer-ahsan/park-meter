package com.parkmeter.og.fragment;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.StripeTerminalApplication;
import com.parkmeter.og.model.AppState;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.LiteralsHelper;
import com.parkmeter.og.utils.SharedPreferencesManager;

public class SettingsFragment extends Fragment {

    public static final String TAG = "SettingsFragment";

    private NavigationListener navigationListener;
    private AppState appState;
    private SharedPreferencesManager sharedPreferencesManager;
    private com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable> glideTarget;
    
    private TextView tvOrganizationName;
    private TextView tvZoneName;
    private TextView tvCityName;
    private TextView tvSettingsTitle;
    private TextView tvOrgDetailsTitle;
    private MaterialButton btnChangeZone;
    private ImageView backIcon;
    private ImageView organizationLogo;
    private com.google.android.material.progressindicator.CircularProgressIndicator organizationLogoLoader;
    private com.google.android.material.card.MaterialCardView cardOrgDetails;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof NavigationListener) {
            navigationListener = (NavigationListener) getActivity();
        }
        appState = StripeTerminalApplication.getInstance().getAppState();
        sharedPreferencesManager = new SharedPreferencesManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();
        updateUI();
    }

    private void initViews(View view) {
        tvOrganizationName = view.findViewById(R.id.tv_organization_name);
        tvZoneName = view.findViewById(R.id.tv_zone_name);
        tvCityName = view.findViewById(R.id.tv_city_name);
        tvSettingsTitle = view.findViewById(R.id.tv_settings_title);
        tvOrgDetailsTitle = view.findViewById(R.id.tv_org_details_title);
        btnChangeZone = view.findViewById(R.id.btn_change_zone);
        backIcon = view.findViewById(R.id.back_icon);
        organizationLogo = view.findViewById(R.id.organization_logo);
        organizationLogoLoader = view.findViewById(R.id.organization_logo_loader);
        cardOrgDetails = view.findViewById(R.id.card_org_details);
    }

    private void setupClickListeners() {
        btnChangeZone.setOnClickListener(v -> {
            showPinVerificationDialog();
        });

        backIcon.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void showPinVerificationDialog() {
        Zone selectedZone = appState.getSelectedZone();
        
        if (selectedZone == null) {
            // If no zone is selected, allow direct access to zone selection
            if (navigationListener != null) {
                navigationListener.onRequestZonesSelection();
            }
            return;
        }

        // Check if zone has a PIN code
        if (selectedZone.getZoneCode() == null || selectedZone.getZoneCode().isEmpty()) {
            // If no PIN code is set, allow direct access
            if (navigationListener != null) {
                navigationListener.onRequestZonesSelection();
            }
            return;
        }

        // Show PIN verification dialog
        PinVerificationDialogFragment pinDialog = PinVerificationDialogFragment.newInstance(selectedZone);
        pinDialog.setPinVerificationListener(new PinVerificationDialogFragment.PinVerificationListener() {
            @Override
            public void onPinVerified(Zone zone) {
                // PIN is correct, allow access to zone selection
                if (navigationListener != null) {
                    navigationListener.onRequestZonesSelection();
                }
            }

            @Override
            public void onPinVerificationCancelled() {
                // User cancelled PIN verification
                // Do nothing, stay on settings page
            }
        });
        pinDialog.show(getChildFragmentManager(), PinVerificationDialogFragment.TAG);
    }

    private void updateUI() {
        // Update organization name
        String orgName = appState.getOrganizationName();
        tvOrganizationName.setText(orgName != null ? orgName : LiteralsHelper.getText(getContext(), "not_selected"));

        // Update zone and city information
        Zone selectedZone = appState.getSelectedZone();
        if (selectedZone != null) {
            tvZoneName.setText(selectedZone.getZoneName());
            tvCityName.setText(selectedZone.getCity() != null ? selectedZone.getCity().getCityName() : LiteralsHelper.getText(getContext(), "not_selected"));
        } else {
            tvZoneName.setText(LiteralsHelper.getText(getContext(), "not_selected"));
            tvCityName.setText(LiteralsHelper.getText(getContext(), "not_selected"));
        }

        // Load organization logo (force refresh)
        String logoUrl = appState.getOrganizationLogoUrl();
        
        if (organizationLogo != null) {
            if (logoUrl != null && !logoUrl.isEmpty()) {
                // Show loader with dynamic color
                if (organizationLogoLoader != null) {
                    organizationLogoLoader.bringToFront();
                    organizationLogoLoader.setIndeterminate(true);
                    organizationLogoLoader.setIndicatorColor(AppThemeManager.getInstance().getCurrentOrgColorInt());
                    organizationLogoLoader.setVisibility(View.VISIBLE);
                }

                glideTarget = new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource, @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                        if (organizationLogo != null) {
                            organizationLogo.setImageDrawable(resource);
                        }
                        if (organizationLogoLoader != null) organizationLogoLoader.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                        if (organizationLogoLoader != null) organizationLogoLoader.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                        if (organizationLogoLoader != null) organizationLogoLoader.setVisibility(View.GONE);
                        if (organizationLogo != null) {
                            int color = AppThemeManager.getInstance().getCurrentOrgColorInt();
                            organizationLogo.setImageDrawable(new android.graphics.drawable.ColorDrawable(color));
                        }
                    }
                };

                Glide.with(this)
                        .load(logoUrl)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(glideTarget);
            } else {
                if (organizationLogoLoader != null) organizationLogoLoader.setVisibility(View.GONE);
                organizationLogo.setImageResource(R.drawable.ic_app_logo);
            }
        }

        // Apply organization color
        String orgColor = appState.getOrganizationColor();
        if (orgColor != null && !orgColor.isEmpty()) {
            AppThemeManager.getInstance().updateOrganizationColor(orgColor);
            // Apply to titles explicitly
            AppThemeManager.getInstance().applyHeaderColor(tvSettingsTitle);
            AppThemeManager.getInstance().applyHeaderColor(tvOrgDetailsTitle);
            // Apply to button and icons via fragment theming
            AppThemeManager.getInstance().applyThemeToFragment(getView());
            // Update card stroke to org color
            if (cardOrgDetails != null) {
                cardOrgDetails.setStrokeColor(AppThemeManager.getInstance().getCurrentOrgColorInt());
            }
        }
    }



    	@Override
	public void onResume() {
		super.onResume();
		updateUI();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		// Clear Glide target to prevent memory leaks
		if (glideTarget != null) {
			Glide.with(this).clear(glideTarget);
			glideTarget = null;
		}
		
		// Clear view references to prevent memory leaks
		tvOrganizationName = null;
		tvZoneName = null;
		tvCityName = null;
		tvSettingsTitle = null;
		tvOrgDetailsTitle = null;
		btnChangeZone = null;
		backIcon = null;
		organizationLogo = null;
		organizationLogoLoader = null;
		cardOrgDetails = null;
	}
} 
