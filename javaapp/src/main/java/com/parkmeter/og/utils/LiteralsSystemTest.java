package com.parkmeter.og.utils;

import android.content.Context;
import android.util.Log;

import com.parkmeter.og.StripeTerminalApplication;

/**
 * Comprehensive test for the literals system across all updated files
 */
public class LiteralsSystemTest {
    private static final String TAG = "LiteralsSystemTest";

    /**
     * Test all the string keys that were updated in the app
     */
    public static void testAllUpdatedKeys(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot test literals system");
            return;
        }

        Log.d(TAG, "=== Testing All Updated Literals Keys ===");

        // Test PaymentFragment keys
        testPaymentFragmentKeys(context);

        // Test EmailReceiptFragment keys
        testEmailReceiptFragmentKeys(context);

        // Test VehicleNumberFragment keys
        testVehicleNumberFragmentKeys(context);

        // Test RateSelectionFragment keys
        testRateSelectionFragmentKeys(context);

        // Test ZonesFragment keys
        testZonesFragmentKeys(context);

        // Test SettingsFragment keys
        testSettingsFragmentKeys(context);

        // Test HomeFragment keys
        testHomeFragmentKeys(context);

        // Test LogViewerFragment keys
        testLogViewerFragmentKeys(context);

        // Test ConnectedReaderFragment keys
        testConnectedReaderFragmentKeys(context);

        // Test MainActivity keys
        testMainActivityKeys(context);

        // Test DirectPaymentHandler keys
        testDirectPaymentHandlerKeys(context);

        // Test UpdateReaderViewModel keys
        testUpdateReaderViewModelKeys(context);

        // Test ZonesAdapter keys
        testZonesAdapterKeys(context);

        Log.d(TAG, "=== All Literals Keys Test Complete ===");
    }

    private static void testPaymentFragmentKeys(Context context) {
        Log.d(TAG, "--- Testing PaymentFragment Keys ---");
        testKey(context, "reader_connected");
        testKey(context, "ready_to_connect_reader");
        testKey(context, "no_reader_connected");
        testKey(context, "discovering_readers");
        testKey(context, "retry_payment");
        testKey(context, "connected");
        testKey(context, "please_connect_reader");
        testKey(context, "failed_to_process_parking");
        testKey(context, "network_error_try_again");
    }

    private static void testEmailReceiptFragmentKeys(Context context) {
        Log.d(TAG, "--- Testing EmailReceiptFragment Keys ---");
        testKey(context, "timer_default");
        testKey(context, "please_enter_email");
        testKey(context, "please_enter_valid_email");
        testKey(context, "parking_id_not_found");
        testKey(context, "sending");
        testKey(context, "receipt_sent_successfully");
        testKey(context, "failed_to_send_receipt");
        testKey(context, "network_error_try_again");
    }

    private static void testVehicleNumberFragmentKeys(Context context) {
        Log.d(TAG, "--- Testing VehicleNumberFragment Keys ---");
        testKey(context, "please_enter_license_plate");
        testKey(context, "invalid_license_plate_format");
    }

    private static void testRateSelectionFragmentKeys(Context context) {
        Log.d(TAG, "--- Testing RateSelectionFragment Keys ---");
        testKey(context, "no_rates_available");
        testKey(context, "failed_to_load_rates");
        testKey(context, "authentication_failed");
        testKey(context, "zone_or_vehicle_not_found");
        testKey(context, "server_error_try_later");
        testKey(context, "network_error_check_connection");
    }

    private static void testZonesFragmentKeys(Context context) {
        Log.d(TAG, "--- Testing ZonesFragment Keys ---");
        testKey(context, "failed_to_load_zones");
        testKey(context, "network_error_check_connection");
    }

    private static void testSettingsFragmentKeys(Context context) {
        Log.d(TAG, "--- Testing SettingsFragment Keys ---");
        testKey(context, "not_selected");
    }

    private static void testHomeFragmentKeys(Context context) {
        Log.d(TAG, "--- Testing HomeFragment Keys ---");
        testKey(context, "enter_license_plate_button");
        testKey(context, "selected_zone");
        testKey(context, "select_zone");
        testKey(context, "no_zone_selected");
        testKey(context, "park45_meter");
        testKey(context, "english");
        testKey(context, "french");
        testKey(context, "select_language");
        testKey(context, "cancel");
    }

    private static void testLogViewerFragmentKeys(Context context) {
        Log.d(TAG, "--- Testing LogViewerFragment Keys ---");
        testKey(context, "show_all");
        testKey(context, "filter_discovery_button");
        testKey(context, "log_viewer_header");
        testKey(context, "timestamp_label");
        testKey(context, "total_logs_label");
        testKey(context, "showing_last_label");
        testKey(context, "filter_label");
        testKey(context, "discovery_only");
        testKey(context, "all_logs");
        testKey(context, "search_label");
        testKey(context, "log_separator");
        testKey(context, "error_loading_logs");
    }

    private static void testConnectedReaderFragmentKeys(Context context) {
        Log.d(TAG, "--- Testing ConnectedReaderFragment Keys ---");
        testKey(context, "reader_description");
    }

    private static void testMainActivityKeys(Context context) {
        Log.d(TAG, "--- Testing MainActivity Keys ---");
        testKey(context, "please_select_zone_first");
        testKey(context, "payment_cancelled");
        testKey(context, "location_permissions_required");
    }

    private static void testDirectPaymentHandlerKeys(Context context) {
        Log.d(TAG, "--- Testing DirectPaymentHandler Keys ---");
        testKey(context, "payment_timeout");
        testKey(context, "failed_to_create_payment");
        testKey(context, "payment_confirmation_failed");
        testKey(context, "payment_collection_failed");
        testKey(context, "payment_captured_successfully");
        testKey(context, "capture_failed");
        testKey(context, "payment_cancelled");
    }

    private static void testUpdateReaderViewModelKeys(Context context) {
        Log.d(TAG, "--- Testing UpdateReaderViewModel Keys ---");
        testKey(context, "checking_for_update");
        testKey(context, "install_explanation");
        testKey(context, "update_complete");
        testKey(context, "update_progress");
        testKey(context, "update_explanation");
    }

    private static void testZonesAdapterKeys(Context context) {
        Log.d(TAG, "--- Testing ZonesAdapter Keys ---");
        testKey(context, "unknown_city");
        testKey(context, "code_label");
        testKey(context, "na");
    }

    private static void testKey(Context context, String key) {
        try {
            String text = LiteralsHelper.getText(context, key);
            boolean hasKey = LiteralsHelper.hasKey(context, key);
            boolean hasStringResource = LiteralsHelper.hasStringResource(context, key);
            
            Log.d(TAG, String.format("✓ %s: '%s' (CSV: %s, String: %s)", 
                key, text, hasKey, hasStringResource));
                
            // Verify text is not empty and not just the key
            if (text == null || text.trim().isEmpty() || text.equals(key)) {
                Log.w(TAG, String.format("⚠ Warning: Key '%s' returned fallback text: '%s'", key, text));
            }
            
        } catch (Exception e) {
            Log.e(TAG, String.format("✗ Error testing key '%s': %s", key, e.getMessage()));
        }
    }

    /**
     * Test language switching functionality
     */
    public static void testLanguageSwitching(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot test language switching");
            return;
        }

        Log.d(TAG, "=== Testing Language Switching ===");

        String[] testKeys = {
            "cancel", "settings", "payment_successful", 
            "scan_qr_code", "btn_pay_desc"
        };

        for (String key : testKeys) {
            String englishText = LiteralsHelper.getText(context, key, "en");
            String frenchText = LiteralsHelper.getText(context, key, "fr");
            
            Log.d(TAG, String.format("Key: %s", key));
            Log.d(TAG, String.format("  EN: '%s'", englishText));
            Log.d(TAG, String.format("  FR: '%s'", frenchText));
            
            if (englishText.equals(frenchText)) {
                Log.w(TAG, String.format("  ⚠ Warning: English and French text are identical for key '%s'", key));
            }
        }

        Log.d(TAG, "=== Language Switching Test Complete ===");
    }

    /**
     * Test memory management and performance
     */
    public static void testMemoryManagement(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot test memory management");
            return;
        }

        Log.d(TAG, "=== Testing Memory Management ===");

        try {
            DynamicLiteralsManager manager = StripeTerminalApplication.getInstance().getLiteralsManager();
            if (manager != null) {
                String memoryInfo = manager.getMemoryInfo();
                Log.d(TAG, "Memory Info: " + memoryInfo);
                
                boolean isDownloading = manager.isDownloading();
                Log.d(TAG, "Is Downloading: " + isDownloading);
                
                int literalsCount = manager.getLiteralsCount();
                Log.d(TAG, "Literals Count: " + literalsCount);
                
                if (literalsCount > 0) {
                    Log.d(TAG, "✓ Literals system is working with " + literalsCount + " cached literals");
                } else {
                    Log.w(TAG, "⚠ Warning: No literals cached, system may be using fallbacks");
                }
            } else {
                Log.e(TAG, "✗ Error: Literals manager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ Error testing memory management: " + e.getMessage());
        }

        Log.d(TAG, "=== Memory Management Test Complete ===");
    }

    /**
     * Run all tests
     */
    public static void runAllTests(Context context) {
        Log.d(TAG, "🚀 Starting Comprehensive Literals System Tests");
        
        testAllUpdatedKeys(context);
        testLanguageSwitching(context);
        testMemoryManagement(context);
        
        Log.d(TAG, "🎉 All Literals System Tests Complete!");
    }
}
