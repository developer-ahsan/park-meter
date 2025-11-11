package com.parkmeter.og.fragment;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.R;
import com.parkmeter.og.network.ApiClient;

import com.parkmeter.og.network.BackendService;
import com.parkmeter.og.network.Park45ApiClient;
import com.parkmeter.og.network.Park45ApiService;
import com.parkmeter.og.model.EmailReceiptRequest;
import com.parkmeter.og.utils.AppThemeManager;
import com.parkmeter.og.utils.LiteralsHelper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * EmailReceiptFragment - Shows for 60 seconds after successful payment
 * Allows user to enter email for receipt, then returns to home
 */
public class EmailReceiptFragment extends Fragment {

    public static final String TAG = "EmailReceiptFragment";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_TRANSACTION_ID = "transaction_id";
    private static final String ARG_PARKING_ID = "parking_id";
    private static final long TIMER_DURATION = 30000; // 30 seconds
    private static final long TIMER_INTERVAL = 1000; // 1 second

    private NavigationListener navigationListener;
    private long amountPaid;
    private String transactionId;
    private String parkingId;
    
    // UI Components
    private TextView tvTimer;
    private TextView tvAmountPaid;
    private TextView tvTransactionId;
    private TextInputEditText etEmail;
    private MaterialButton btnSendEmail;
    private MaterialButton btnCancel;
    private ImageView ivQrCode;
    
    // Timer
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;

    public static EmailReceiptFragment newInstance(long amount, String parkingId, String transactionId) {
        EmailReceiptFragment fragment = new EmailReceiptFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_AMOUNT, amount);
        args.putString(ARG_TRANSACTION_ID, transactionId);
        args.putString(ARG_PARKING_ID, parkingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            amountPaid = getArguments().getLong(ARG_AMOUNT, 0);
            transactionId = getArguments().getString(ARG_TRANSACTION_ID, "");
            parkingId = getArguments().getString(ARG_PARKING_ID, "");
        }
        
        if (getActivity() instanceof NavigationListener) {
            navigationListener = (NavigationListener) getActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_email_receipt, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupUI();
        generateQRCode();
        applyTheming();
        setupClickListeners();
        startTimer();
    }

    private void initializeViews(View view) {
        tvTimer = view.findViewById(R.id.tv_timer);
        tvAmountPaid = view.findViewById(R.id.tv_amount_paid);
        tvTransactionId = view.findViewById(R.id.tv_transaction_id);
        etEmail = view.findViewById(R.id.et_email);
        btnSendEmail = view.findViewById(R.id.btn_send_email);
        btnCancel = view.findViewById(R.id.btn_cancel);
        ivQrCode = view.findViewById(R.id.iv_qr_code);
    }

    private void setupUI() {
        // Display payment details
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);
        tvAmountPaid.setText(currencyFormat.format(amountPaid / 100.0));
        
        // Only show transaction ID section if transactionId is not empty
        android.widget.LinearLayout layoutTransactionId = getView().findViewById(R.id.layout_transaction_id);
        if (transactionId != null && !transactionId.isEmpty()) {
            tvTransactionId.setText(transactionId);
            if (layoutTransactionId != null) {
                layoutTransactionId.setVisibility(View.VISIBLE);
            }
        } else {
            if (layoutTransactionId != null) {
                layoutTransactionId.setVisibility(View.GONE);
            }
        }
    }

    private void generateQRCode() {
        if (parkingId == null || parkingId.isEmpty()) {
            Log.w(TAG, "Parking ID is null or empty, cannot generate QR code");
            return;
        }

        try {
            // Create the URL with parking ID
            String qrUrl = "https://parkapp.ca/api/downloadParking/" + parkingId;
            
            // Generate QR code
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrUrl, BarcodeFormat.QR_CODE, 512, 512);
            
            // Convert to bitmap
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            // Set the QR code to the ImageView
            if (ivQrCode != null) {
                ivQrCode.setImageBitmap(bitmap);
            }
            
            Log.d(TAG, "QR code generated successfully for URL: " + qrUrl);
            
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code: " + e.getMessage(), e);
            // Hide QR code section if generation fails
            if (ivQrCode != null) {
                ivQrCode.setVisibility(View.GONE);
            }
        }
    }

    private void applyTheming() {
        AppThemeManager themeManager = AppThemeManager.getInstance();
        int orgColor = themeManager.getCurrentOrgColorInt();
        
        // Apply theming to the fragment
        themeManager.applyThemeToFragment(getView());
        
        // Apply organization color to buttons
        themeManager.applyMaterialButtonColor(btnSendEmail);
        
        // Apply organization color to timer section background
        android.widget.LinearLayout layoutTimerSection = getView().findViewById(R.id.layout_timer_section);
        if (layoutTimerSection != null) {
            layoutTimerSection.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orgColor));
        }
        
        // Set timer text color to white with organization color background
        tvTimer.setTextColor(getResources().getColor(android.R.color.white));
        tvTimer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orgColor));
        
        // Apply organization color to email icon
        android.widget.ImageView ivEmailIcon = getView().findViewById(R.id.iv_email_icon);
        if (ivEmailIcon != null) {
            ivEmailIcon.setImageTintList(android.content.res.ColorStateList.valueOf(orgColor));
        }
        
        // Apply organization color to amount text
        tvAmountPaid.setTextColor(orgColor);
        
        // Apply organization color to card border
        android.view.View cardView = getView().findViewById(R.id.card_payment_details);
        if (cardView instanceof com.google.android.material.card.MaterialCardView) {
            ((com.google.android.material.card.MaterialCardView) cardView).setStrokeColor(orgColor);
        }
    }

    private void setupClickListeners() {
        btnSendEmail.setOnClickListener(v -> sendEmailReceipt());
        btnCancel.setOnClickListener(v -> returnToHome());
    }

    private void startTimer() {
        isTimerRunning = true;
        countDownTimer = new CountDownTimer(TIMER_DURATION, TIMER_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                tvTimer.setText(String.valueOf(secondsRemaining));
            }

            @Override
            public void onFinish() {
                tvTimer.setText(LiteralsHelper.getText(getContext(), "timer_default"));
                isTimerRunning = false;
                returnToHome();
            }
        };
        countDownTimer.start();
    }

    private void sendEmailReceipt() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        
        if (email.isEmpty()) {
            Toast.makeText(getContext(), LiteralsHelper.getText(getContext(), "please_enter_email"), Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), LiteralsHelper.getText(getContext(), "please_enter_valid_email"), Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (parkingId == null || parkingId.isEmpty()) {
            Toast.makeText(getContext(), LiteralsHelper.getText(getContext(), "parking_id_not_found"), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable buttons during API call
        btnSendEmail.setEnabled(false);
        btnCancel.setEnabled(false);
        btnSendEmail.setText(LiteralsHelper.getText(getContext(), "sending"));
        
        // Call the emailReceipt API
        Park45ApiService apiService = Park45ApiClient.getInstance().getApiService();
        EmailReceiptRequest request = new EmailReceiptRequest(email, parkingId);
        
        apiService.emailReceipt(request).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), LiteralsHelper.getText(getContext(), "receipt_sent_successfully"), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), LiteralsHelper.getText(getContext(), "failed_to_send_receipt"), Toast.LENGTH_SHORT).show();
                        }
                        returnToHome();
                    });
                }
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), LiteralsHelper.getText(getContext(), "network_error_try_again"), Toast.LENGTH_SHORT).show();
                        returnToHome();
                    });
                }
            }
        });
    }

    private void returnToHome() {
        
        // Stop timer if running
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerRunning = false;
        }
        
        // Navigate to home
        if (navigationListener != null) {
            navigationListener.onRequestReturnToHome();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationListener = null;
    }
    
    // Disable back button functionality
    public boolean onBackPressed() {
        // Return true to consume the back button press and prevent navigation
        return true;
    }
}
