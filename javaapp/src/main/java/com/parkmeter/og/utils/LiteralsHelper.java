package com.parkmeter.og.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.parkmeter.og.StripeTerminalApplication;

/**
 * Enhanced helper class to access dynamic literals with string resource fallback
 * and memory leak prevention
 */
public class LiteralsHelper {
    private static final String TAG = "LiteralsHelper";

    /**
     * Get text for a given key using the current language with fallback to string resources
     */
    public static String getText(Context context, String key) {
        if (context == null || key == null || key.trim().isEmpty()) {
            Log.w(TAG, "Invalid context or key provided");
            return key != null ? key : "";
        }

        try {
            // First try dynamic literals from CSV
            DynamicLiteralsManager literalsManager = StripeTerminalApplication.getInstance().getLiteralsManager();
            if (literalsManager != null && literalsManager.isInitialized()) {
                String dynamicText = literalsManager.getText(key);
                if (dynamicText != null && !dynamicText.equals(key) && !dynamicText.trim().isEmpty()) {
                    return dynamicText;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get dynamic text for key: " + key + ", error: " + e.getMessage());
        }

        // Fallback to string resources
        return getStringResource(context, key);
    }

    /**
     * Get text for a given key using a specific language with fallback to string resources
     */
    public static String getText(Context context, String key, String languageCode) {
        if (context == null || key == null || key.trim().isEmpty()) {
            Log.w(TAG, "Invalid context or key provided");
            return key != null ? key : "";
        }

        try {
            // First try dynamic literals from CSV
            DynamicLiteralsManager literalsManager = StripeTerminalApplication.getInstance().getLiteralsManager();
            if (literalsManager != null && literalsManager.isInitialized()) {
                String dynamicText = literalsManager.getText(key, languageCode);
                if (dynamicText != null && !dynamicText.equals(key) && !dynamicText.trim().isEmpty()) {
                    return dynamicText;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get dynamic text for key: " + key + ", language: " + languageCode + ", error: " + e.getMessage());
        }

        // Fallback to string resources
        return getStringResource(context, key);
    }

    /**
     * Get string resource with safe fallback
     */
    private static String getStringResource(Context context, String key) {
        if (context == null) {
            return key;
        }

        try {
            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier(key, "string", context.getPackageName());
            if (resourceId != 0) {
                return resources.getString(resourceId);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get string resource for key: " + key + ", error: " + e.getMessage());
        }

        // Final fallback to the key itself
        return key;
    }

    /**
     * Check if a key exists in the dynamic literals
     */
    public static boolean hasKey(Context context, String key) {
        if (context == null || key == null) {
            return false;
        }

        try {
            DynamicLiteralsManager literalsManager = StripeTerminalApplication.getInstance().getLiteralsManager();
            return literalsManager != null && literalsManager.isInitialized() && literalsManager.hasKey(key);
        } catch (Exception e) {
            Log.w(TAG, "Could not check key existence: " + key + ", error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a key exists in string resources
     */
    public static boolean hasStringResource(Context context, String key) {
        if (context == null || key == null) {
            return false;
        }

        try {
            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier(key, "string", context.getPackageName());
            return resourceId != 0;
        } catch (Exception e) {
            Log.w(TAG, "Could not check string resource existence: " + key + ", error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the current language
     */
    public static String getCurrentLanguage(Context context) {
        if (context == null) {
            return "en";
        }

        try {
            DynamicLiteralsManager literalsManager = StripeTerminalApplication.getInstance().getLiteralsManager();
            if (literalsManager != null && literalsManager.isInitialized()) {
                return literalsManager.getCurrentLanguage();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get current language, error: " + e.getMessage());
        }
        
        return "en"; // Default fallback
    }

    /**
     * Check if the literals manager is initialized
     */
    public static boolean isInitialized(Context context) {
        if (context == null) {
            return false;
        }

        try {
            DynamicLiteralsManager literalsManager = StripeTerminalApplication.getInstance().getLiteralsManager();
            return literalsManager != null && literalsManager.isInitialized();
        } catch (Exception e) {
            Log.w(TAG, "Could not check initialization status, error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the number of cached literals
     */
    public static int getLiteralsCount(Context context) {
        if (context == null) {
            return 0;
        }

        try {
            DynamicLiteralsManager literalsManager = StripeTerminalApplication.getInstance().getLiteralsManager();
            if (literalsManager != null && literalsManager.isInitialized()) {
                return literalsManager.getLiteralsCount();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get literals count, error: " + e.getMessage());
        }
        
        return 0;
    }

    /**
     * Force refresh literals from CSV
     */
    public static void refreshLiterals(Context context, DynamicLiteralsManager.LiteralsDownloadCallback callback) {
        if (context == null) {
            if (callback != null) {
                callback.onFailure("Context is null");
            }
            return;
        }

        try {
            DynamicLiteralsManager literalsManager = StripeTerminalApplication.getInstance().getLiteralsManager();
            if (literalsManager != null) {
                literalsManager.downloadFreshLiterals(callback);
            } else {
                if (callback != null) {
                    callback.onFailure("Literals manager not available");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not refresh literals, error: " + e.getMessage());
            if (callback != null) {
                callback.onFailure("Error: " + e.getMessage());
            }
        }
    }
}
