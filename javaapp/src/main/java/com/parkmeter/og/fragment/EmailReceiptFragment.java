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
    private android.widget.LinearLayout layoutQrCodeSection;
    
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
        // Check payment status before showing QR code
        checkPaymentStatusAndGenerateQR();
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
        layoutQrCodeSection = view.findViewById(R.id.layout_qr_code_section);
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

    /**
     * Check payment status with retries before generating QR code
     */
    private void checkPaymentStatusAndGenerateQR() {
        if (transactionId == null || transactionId.isEmpty()) {
            Log.w(TAG, "Transaction ID is null or empty, cannot check payment status");
            // Hide QR code section if no transaction ID
            hideQRCodeSection();
            return;
        }
        
        // Hide QR code section initially, will show it when status is succeeded
        hideQRCodeSection();
        checkPaymentStatusWithRetries(0);
    }
    
    /**
     * Hide the entire QR code section including labels
     */
    private void hideQRCodeSection() {
        if (layoutQrCodeSection != null) {
            layoutQrCodeSection.setVisibility(View.GONE);
        }
    }
    
    /**
     * Show the entire QR code section including labels
     */
    private void showQRCodeSection() {
        if (layoutQrCodeSection != null) {
            layoutQrCodeSection.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Check payment status with retry logic (3 retries with 3 second intervals)
     */
    private void checkPaymentStatusWithRetries(int attempt) {
        final int MAX_RETRIES = 3;
        final long RETRY_INTERVAL_MS = 3000; // 3 seconds
        
        if (attempt >= MAX_RETRIES) {
            // Max retries reached, don't show QR code section
            Log.w(TAG, "Payment status check failed after " + MAX_RETRIES + " retries, not showing QR code");
            hideQRCodeSection();
            return;
        }
        
        Park45ApiService apiService = Park45ApiClient.getInstance().getApiService();
        apiService.getPaymentStatus(transactionId).enqueue(new retrofit2.Callback<com.parkmeter.og.model.PaymentStatusResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.parkmeter.og.model.PaymentStatusResponse> call, retrofit2.Response<com.parkmeter.og.model.PaymentStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.parkmeter.og.model.PaymentStatusResponse statusResponse = response.body();
                    
                    // Check if status is succeeded
                    if (statusResponse.getStatus() != null && 
                        "succeeded".equalsIgnoreCase(statusResponse.getStatus())) {
                        // Payment succeeded, show QR code section and generate QR code
                        Log.d(TAG, "Payment status succeeded, showing QR code section");
                        showQRCodeSection();
                        generateQRCode();
                    } else {
                        // Status not succeeded, retry if attempts remaining
                        Log.d(TAG, "Payment status not succeeded (status: " + statusResponse.getStatus() + "), retrying... Attempt: " + (attempt + 1));
                        hideQRCodeSection();
                        if (getActivity() != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    checkPaymentStatusWithRetries(attempt + 1);
                                }
                            }, RETRY_INTERVAL_MS);
                        }
                    }
                } else {
                    // API call failed, retry if attempts remaining
                    Log.w(TAG, "Payment status API call failed, retrying... Attempt: " + (attempt + 1));
                    if (getActivity() != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                checkPaymentStatusWithRetries(attempt + 1);
                            }
                        }, RETRY_INTERVAL_MS);
                    }
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<com.parkmeter.og.model.PaymentStatusResponse> call, Throwable t) {
                // Network error, retry if attempts remaining
                Log.w(TAG, "Payment status API call error: " + t.getMessage() + ", retrying... Attempt: " + (attempt + 1));
                if (getActivity() != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkPaymentStatusWithRetries(attempt + 1);
                        }
                    }, RETRY_INTERVAL_MS);
                }
            }
        });
    }
    
    private void generateQRCode() {
        if (transactionId == null || transactionId.isEmpty()) {
            Log.w(TAG, "Transaction ID is null or empty, cannot generate QR code");
            if (ivQrCode != null) {
                ivQrCode.setVisibility(View.GONE);
            }
            return;
        }

        try {
            // Create the URL with transaction ID
            String qrUrl = "https://parkapp.ca/api/downloadParking/" + transactionId;
            
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
                ivQrCode.setVisibility(View.VISIBLE);
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
