package com.parkmeter.og.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.parkmeter.og.R;
import com.parkmeter.og.model.Zone;
import com.parkmeter.og.utils.AppThemeManager;

public class PinVerificationDialogFragment extends DialogFragment {

    public static final String TAG = "PinVerificationDialogFragment";
    public static final String ARG_ZONE = "zone";

    private Zone zone;
    private PinVerificationListener listener;

    private EditText etPin1, etPin2, etPin3, etPin4;
    private TextView tvErrorMessage;
    private MaterialButton btnVerify, btnCancel;
    private TextView tvPinTitle, tvPinSubtitle;
    private ImageView ivLockIcon;
    
    // TextWatcher references for cleanup
    private TextWatcher verifyButtonWatcher;
    private PinTextWatcher pinTextWatcher1, pinTextWatcher2, pinTextWatcher3, pinTextWatcher4;

    public interface PinVerificationListener {
        void onPinVerified(Zone zone);
        void onPinVerificationCancelled();
    }

    public static PinVerificationDialogFragment newInstance(Zone zone) {
        PinVerificationDialogFragment fragment = new PinVerificationDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ZONE, zone);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.PinDialogTheme);
        
        if (getArguments() != null) {
            zone = (Zone) getArguments().getSerializable(ARG_ZONE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_pin_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupPinInputListeners();
        setupClickListeners();
        applyOrganizationTheme();
    }

    private void initViews(View view) {
        etPin1 = view.findViewById(R.id.et_pin_1);
        etPin2 = view.findViewById(R.id.et_pin_2);
        etPin3 = view.findViewById(R.id.et_pin_3);
        etPin4 = view.findViewById(R.id.et_pin_4);
        tvErrorMessage = view.findViewById(R.id.tv_error_message);
        btnVerify = view.findViewById(R.id.btn_verify);
        btnCancel = view.findViewById(R.id.btn_cancel);
        tvPinTitle = view.findViewById(R.id.tv_pin_title);
        tvPinSubtitle = view.findViewById(R.id.tv_pin_subtitle);
        ivLockIcon = view.findViewById(R.id.iv_lock_icon);
    }

    private void setupPinInputListeners() {
        // Set up text change listeners for auto-focus
        pinTextWatcher1 = new PinTextWatcher(etPin1, etPin2);
        pinTextWatcher2 = new PinTextWatcher(etPin2, etPin3);
        pinTextWatcher3 = new PinTextWatcher(etPin3, etPin4);
        pinTextWatcher4 = new PinTextWatcher(etPin4, null);
        
        etPin1.addTextChangedListener(pinTextWatcher1);
        etPin2.addTextChangedListener(pinTextWatcher2);
        etPin3.addTextChangedListener(pinTextWatcher3);
        etPin4.addTextChangedListener(pinTextWatcher4);

        // Set up text change listener for verify button state
        verifyButtonWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateVerifyButtonState();
            }
        };

        etPin1.addTextChangedListener(verifyButtonWatcher);
        etPin2.addTextChangedListener(verifyButtonWatcher);
        etPin3.addTextChangedListener(verifyButtonWatcher);
        etPin4.addTextChangedListener(verifyButtonWatcher);
    }

    private void setupClickListeners() {
        btnVerify.setOnClickListener(v -> verifyPin());
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPinVerificationCancelled();
            }
            dismiss();
        });
    }

    private void applyOrganizationTheme() {
        if (getView() == null) return;

        // Apply organization color to dialog elements
        int orgColor = AppThemeManager.getInstance().getCurrentOrgColorInt();
        
        // Update lock icon color
        if (ivLockIcon != null) {
            ivLockIcon.setColorFilter(orgColor);
        }
        
        // Update title and subtitle colors
        tvPinTitle.setTextColor(orgColor);
        tvPinSubtitle.setTextColor(orgColor);
        
        // Update button colors
        btnVerify.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orgColor));
        btnCancel.setStrokeColor(android.content.res.ColorStateList.valueOf(orgColor));
        btnCancel.setTextColor(orgColor);
        
        // Update PIN input colors
        int[] pinInputs = {R.id.et_pin_1, R.id.et_pin_2, R.id.et_pin_3, R.id.et_pin_4};
        for (int id : pinInputs) {
            EditText et = getView().findViewById(id);
            if (et != null) {
                et.setTextColor(orgColor);
                // Update the background drawable stroke color
                android.graphics.drawable.GradientDrawable background = (android.graphics.drawable.GradientDrawable) et.getBackground();
                if (background != null) {
                    background.setStroke(2, orgColor);
                }
            }
        }
        
        // Update dialog background border color
        View dialogRoot = getView();
        if (dialogRoot != null) {
            android.graphics.drawable.GradientDrawable dialogBackground = (android.graphics.drawable.GradientDrawable) dialogRoot.getBackground();
            if (dialogBackground != null) {
                dialogBackground.setStroke(2, orgColor);
            }
        }
    }

    private void updateVerifyButtonState() {
        boolean allFieldsFilled = !etPin1.getText().toString().isEmpty() &&
                                !etPin2.getText().toString().isEmpty() &&
                                !etPin3.getText().toString().isEmpty() &&
                                !etPin4.getText().toString().isEmpty();
        
        btnVerify.setEnabled(allFieldsFilled);
    }

    private void verifyPin() {
        String enteredPin = etPin1.getText().toString() +
                          etPin2.getText().toString() +
                          etPin3.getText().toString() +
                          etPin4.getText().toString();

        if (zone != null && zone.getZoneCode() != null && zone.getZoneCode().equals(enteredPin)) {
            // PIN is correct
            if (listener != null) {
                listener.onPinVerified(zone);
            }
            dismiss();
        } else {
            // PIN is incorrect
            showError();
            clearPinInputs();
        }
    }

    private void showError() {
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void clearPinInputs() {
        etPin1.setText("");
        etPin2.setText("");
        etPin3.setText("");
        etPin4.setText("");
        etPin1.requestFocus();
        updateVerifyButtonState();
    }

    public void setPinVerificationListener(PinVerificationListener listener) {
        this.listener = listener;
    }

    private static class PinTextWatcher implements TextWatcher {
        private EditText currentEditText;
        private EditText nextEditText;

        public PinTextWatcher(EditText current, EditText next) {
            this.currentEditText = current;
            this.nextEditText = next;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() == 1 && nextEditText != null) {
                nextEditText.requestFocus();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Remove TextWatchers to prevent memory leaks
        if (etPin1 != null && pinTextWatcher1 != null) {
            etPin1.removeTextChangedListener(pinTextWatcher1);
        }
        if (etPin2 != null && pinTextWatcher2 != null) {
            etPin2.removeTextChangedListener(pinTextWatcher2);
        }
        if (etPin3 != null && pinTextWatcher3 != null) {
            etPin3.removeTextChangedListener(pinTextWatcher3);
        }
        if (etPin4 != null && pinTextWatcher4 != null) {
            etPin4.removeTextChangedListener(pinTextWatcher4);
        }
        
        // Remove verify button watcher
        if (etPin1 != null && verifyButtonWatcher != null) {
            etPin1.removeTextChangedListener(verifyButtonWatcher);
        }
        if (etPin2 != null && verifyButtonWatcher != null) {
            etPin2.removeTextChangedListener(verifyButtonWatcher);
        }
        if (etPin3 != null && verifyButtonWatcher != null) {
            etPin3.removeTextChangedListener(verifyButtonWatcher);
        }
        if (etPin4 != null && verifyButtonWatcher != null) {
            etPin4.removeTextChangedListener(verifyButtonWatcher);
        }
        
        // Clear listener reference
        listener = null;
        
        // Clear view references
        etPin1 = null;
        etPin2 = null;
        etPin3 = null;
        etPin4 = null;
        tvErrorMessage = null;
        btnVerify = null;
        btnCancel = null;
        tvPinTitle = null;
        tvPinSubtitle = null;
        ivLockIcon = null;
        verifyButtonWatcher = null;
        pinTextWatcher1 = null;
        pinTextWatcher2 = null;
        pinTextWatcher3 = null;
        pinTextWatcher4 = null;
    }
}
