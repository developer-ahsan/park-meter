package com.parkmeter.og.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.parkmeter.og.R;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.model.Rate;
import com.parkmeter.og.model.RateStep;
import com.parkmeter.og.utils.AppThemeManager;

public class TapDeviceDialogFragment extends DialogFragment {
    
    private static final String ARG_ZONE = "zone";
    private static final String ARG_RATE = "rate";
    private static final String ARG_RATE_STEP = "rate_step";
    private static final String ARG_AMOUNT = "amount";
    private static final long AUTO_DISMISS_DELAY = 5000; // 5 seconds
    
    private Zone zone;
    private Rate rate;
    private RateStep rateStep;
    private long amount;
    
    private TextView tvTitle;
    private TextView tvMessage;
    private TextView tvZoneName;
    private TextView tvCityName;
    private TextView tvAmount;
    private ImageView ivTapIcon;
    
    private Handler autoDismissHandler;
    private Runnable autoDismissRunnable;
    
    public interface OnDismissListener {
        void onDialogDismissed();
    }
    
    private OnDismissListener dismissListener;
    
    public static TapDeviceDialogFragment newInstance(Zone zone, Rate rate, RateStep rateStep, long amount) {
        TapDeviceDialogFragment fragment = new TapDeviceDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ZONE, zone);
        args.putSerializable(ARG_RATE, rate);
        args.putSerializable(ARG_RATE_STEP, rateStep);
        args.putLong(ARG_AMOUNT, amount);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.PinDialogTheme);
        setCancelable(false);
        
        if (getArguments() != null) {
            zone = (Zone) getArguments().getSerializable(ARG_ZONE);
            rate = (Rate) getArguments().getSerializable(ARG_RATE);
            rateStep = (RateStep) getArguments().getSerializable(ARG_RATE_STEP);
            amount = getArguments().getLong(ARG_AMOUNT, 0);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_tap_device, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupViews();
        applyOrganizationTheme();
        startAutoDismissTimer();
    }
    
    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tv_tap_title);
        tvMessage = view.findViewById(R.id.tv_tap_message);
        tvZoneName = view.findViewById(R.id.tv_zone_name);
        tvCityName = view.findViewById(R.id.tv_city_name);
        tvAmount = view.findViewById(R.id.tv_amount);
        ivTapIcon = view.findViewById(R.id.iv_tap_icon);
    }
    
    private void setupViews() {
        View view = getView();
        if (view == null) return;
        
        // Set zone name
        if (zone != null && zone.getZoneName() != null) {
            tvZoneName.setText(zone.getZoneName());
        }
        
        // Set city name
        View cityContainer = view.findViewById(R.id.city_container);
        if (zone != null && zone.getCity() != null && zone.getCity().getCityName() != null) {
            tvCityName.setText(zone.getCity().getCityName());
        } else {
            if (cityContainer != null) {
                cityContainer.setVisibility(View.GONE);
            }
        }
        
        // Set amount
        View amountContainer = view.findViewById(R.id.amount_container);
        if (amount > 0) {
            String amountText = String.format("$%.2f CAD", amount / 100.0);
            tvAmount.setText(amountText);
        } else {
            if (amountContainer != null) {
                amountContainer.setVisibility(View.GONE);
            }
        }
    }
    
    private void applyOrganizationTheme() {
        if (getView() == null) return;
        
        AppThemeManager themeManager = AppThemeManager.getInstance();
        int orgColor = themeManager.getCurrentOrgColorInt();
        
        // Apply organization color to title
        if (tvTitle != null) {
            tvTitle.setTextColor(orgColor);
        }
        
        // Apply organization color to message
        if (tvMessage != null) {
            tvMessage.setTextColor(orgColor);
        }
        
        // Apply organization color to zone name
        if (tvZoneName != null) {
            tvZoneName.setTextColor(orgColor);
        }
        
        // Apply organization color to city name
        if (tvCityName != null) {
            tvCityName.setTextColor(orgColor);
        }
        
        // Apply organization color to amount
        if (tvAmount != null) {
            tvAmount.setTextColor(orgColor);
        }
        
        // Apply organization color to tap icon
        if (ivTapIcon != null) {
            ivTapIcon.setColorFilter(orgColor);
        }
        
        // Update dialog background border color
        View dialogRoot = getView();
        if (dialogRoot != null) {
            android.graphics.drawable.GradientDrawable dialogBackground = 
                (android.graphics.drawable.GradientDrawable) dialogRoot.getBackground();
            if (dialogBackground != null) {
                dialogBackground.setStroke(2, orgColor);
            }
        }
    }
    
    private void startAutoDismissTimer() {
        autoDismissHandler = new Handler(Looper.getMainLooper());
        autoDismissRunnable = new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        };
        autoDismissHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_DELAY);
    }
    
    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        
        // Cancel auto-dismiss timer if still pending
        if (autoDismissHandler != null && autoDismissRunnable != null) {
            autoDismissHandler.removeCallbacks(autoDismissRunnable);
        }
        
        // Notify listener
        if (dismissListener != null) {
            dismissListener.onDialogDismissed();
        }
    }
    
    @Override
    public void onDestroyView() {
        // Clean up handler to prevent memory leaks
        if (autoDismissHandler != null && autoDismissRunnable != null) {
            autoDismissHandler.removeCallbacks(autoDismissRunnable);
        }
        super.onDestroyView();
    }
}

