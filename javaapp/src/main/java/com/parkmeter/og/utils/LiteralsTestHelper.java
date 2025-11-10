package com.parkmeter.og.utils;

import android.content.Context;
import android.util.Log;

import com.parkmeter.og.StripeTerminalApplication;

/**
 * Test helper for verifying the literals system functionality
 */
public class LiteralsTestHelper {
    private static final String TAG = "LiteralsTestHelper";

    /**
     * Test the complete literals system functionality
     */
    public static void testLiteralsSystem(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot test literals system");
            return;
        }

        Log.d(TAG, "=== Starting Literals System Test ===");

        // Test 1: Check if literals manager is initialized
        testInitialization(context);

        // Test 2: Test language switching
        testLanguageSwitching(context);

        // Test 3: Test fallback mechanism
        testFallbackMechanism(context);

        // Test 4: Test memory management
        testMemoryManagement(context);

        // Test 5: Test specific keys
        testSpecificKeys(context);

        Log.d(TAG, "=== Literals System Test Complete ===");
    }

    private static void testInitialization(Context context) {
        Log.d(TAG, "--- Testing Initialization ---");
        
        boolean isInitialized = LiteralsHelper.isInitialized(context);
        Log.d(TAG, "Literals manager initialized: " + isInitialized);
        
        int literalsCount = LiteralsHelper.getLiteralsCount(context);
        Log.d(TAG, "Cached literals count: " + literalsCount);
        
        String currentLanguage = LiteralsHelper.getCurrentLanguage(context);
        Log.d(TAG, "Current language: " + currentLanguage);
    }

    private static void testLanguageSwitching(Context context) {
        Log.d(TAG, "--- Testing Language Switching ---");
        
        // Test English
        String englishText = LiteralsHelper.getText(context, "btn_pay_desc", "en");
        Log.d(TAG, "English text for btn_pay_desc: " + englishText);
        
        // Test French
        String frenchText = LiteralsHelper.getText(context, "btn_pay_desc", "fr");
        Log.d(TAG, "French text for btn_pay_desc: " + frenchText);
        
        // Test invalid language (should fallback to English)
        String invalidLangText = LiteralsHelper.getText(context, "btn_pay_desc", "invalid");
        Log.d(TAG, "Invalid language text for btn_pay_desc: " + invalidLangText);
    }

    private static void testFallbackMechanism(Context context) {
        Log.d(TAG, "--- Testing Fallback Mechanism ---");
        
        // Test existing key
        String existingKey = LiteralsHelper.getText(context, "btn_pay_desc");
        Log.d(TAG, "Existing key result: " + existingKey);
        
        // Test non-existing key (should fallback to string resource or key itself)
        String nonExistingKey = LiteralsHelper.getText(context, "non_existing_key_12345");
        Log.d(TAG, "Non-existing key result: " + nonExistingKey);
        
        // Test null key
        String nullKey = LiteralsHelper.getText(context, null);
        Log.d(TAG, "Null key result: " + nullKey);
        
        // Test empty key
        String emptyKey = LiteralsHelper.getText(context, "");
        Log.d(TAG, "Empty key result: " + emptyKey);
    }

    private static void testMemoryManagement(Context context) {
        Log.d(TAG, "--- Testing Memory Management ---");
        
        try {
            DynamicLiteralsManager manager = StripeTerminalApplication.getInstance().getLiteralsManager();
            if (manager != null) {
                String memoryInfo = manager.getMemoryInfo();
                Log.d(TAG, "Memory info: " + memoryInfo);
                
                boolean isDownloading = manager.isDownloading();
                Log.d(TAG, "Is downloading: " + isDownloading);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error testing memory management: " + e.getMessage());
        }
    }

    private static void testSpecificKeys(Context context) {
        Log.d(TAG, "--- Testing Specific Keys ---");
        
        String[] testKeys = {
            "btn_pay_desc",
            "cancel",
            "settings",
            "payment_successful",
            "scan_qr_code",
            "non_existing_key"
        };
        
        for (String key : testKeys) {
            String text = LiteralsHelper.getText(context, key);
            boolean hasKey = LiteralsHelper.hasKey(context, key);
            boolean hasStringResource = LiteralsHelper.hasStringResource(context, key);
            
            Log.d(TAG, String.format("Key: %s | Text: %s | HasKey: %s | HasStringResource: %s", 
                key, text, hasKey, hasStringResource));
        }
    }

    /**
     * Test the refresh functionality
     */
    public static void testRefreshFunctionality(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot test refresh functionality");
            return;
        }

        Log.d(TAG, "--- Testing Refresh Functionality ---");
        
        LiteralsHelper.refreshLiterals(context, new DynamicLiteralsManager.LiteralsDownloadCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Refresh successful");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Refresh failed: " + error);
            }
        });
    }

    /**
     * Get a comprehensive status report
     */
    public static String getStatusReport(Context context) {
        if (context == null) {
            return "Context is null";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Literals System Status Report ===\n");
        
        // Basic info
        report.append("Initialized: ").append(LiteralsHelper.isInitialized(context)).append("\n");
        report.append("Literals Count: ").append(LiteralsHelper.getLiteralsCount(context)).append("\n");
        report.append("Current Language: ").append(LiteralsHelper.getCurrentLanguage(context)).append("\n");
        
        // Test key results
        report.append("\n--- Key Test Results ---\n");
        String[] testKeys = {"btn_pay_desc", "cancel", "settings"};
        for (String key : testKeys) {
            String text = LiteralsHelper.getText(context, key);
            boolean hasKey = LiteralsHelper.hasKey(context, key);
            boolean hasStringResource = LiteralsHelper.hasStringResource(context, key);
            report.append(String.format("%s: %s (CSV: %s, String: %s)\n", 
                key, text, hasKey, hasStringResource));
        }
        
        // Memory info
        try {
            DynamicLiteralsManager manager = StripeTerminalApplication.getInstance().getLiteralsManager();
            if (manager != null) {
                report.append("\n--- Memory Info ---\n");
                report.append(manager.getMemoryInfo()).append("\n");
            }
        } catch (Exception e) {
            report.append("\n--- Memory Info Error ---\n");
            report.append("Error: ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
}
