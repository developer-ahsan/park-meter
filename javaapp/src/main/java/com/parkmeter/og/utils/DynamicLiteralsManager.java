package com.parkmeter.og.utils;

import android.content.Context;
import android.util.Log;

import com.parkmeter.og.model.Literal;
import com.parkmeter.og.service.LiteralsDownloadService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced DynamicLiteralsManager with memory leak prevention and robust error handling
 */
public class DynamicLiteralsManager {
    private static final String TAG = "DynamicLiteralsManager";
    private static DynamicLiteralsManager instance;
    
    private final Context context;
    private final LiteralsDownloadService downloadService;
    private Map<String, Literal> literalsMap;
    private String currentLanguage = "en";
    private boolean isInitialized = false;
    private volatile boolean isDownloading = false;

    private DynamicLiteralsManager(Context context) {
        this.context = context.getApplicationContext();
        this.downloadService = new LiteralsDownloadService(this.context);
        this.literalsMap = new ConcurrentHashMap<>(); // Thread-safe map
    }

    public static synchronized DynamicLiteralsManager getInstance(Context context) {
        if (instance == null) {
            instance = new DynamicLiteralsManager(context);
        }
        return instance;
    }

    /**
     * Initialize the literals manager by loading cached literals
     */
    public void initialize() {
        if (isInitialized) return;
        
        Log.d(TAG, "Initializing DynamicLiteralsManager...");
        
        // Load cached literals first
        literalsMap = downloadService.getLiteralsMap();
        currentLanguage = LanguageManager.getCurrentLanguage(context);
        isInitialized = true;
        
        Log.d(TAG, "Loaded " + literalsMap.size() + " cached literals for language: " + currentLanguage);
    }

    /**
     * Download fresh literals from Google Sheets with memory leak prevention
     */
    public void downloadFreshLiterals(LiteralsDownloadCallback callback) {
        if (isDownloading) {
            Log.w(TAG, "Download already in progress, ignoring request");
            if (callback != null) {
                callback.onFailure("Download already in progress");
            }
            return;
        }

        isDownloading = true;
        downloadService.downloadAndCacheLiterals(new LiteralsDownloadService.LiteralsDownloadCallback() {
            @Override
            public void onSuccess(List<Literal> literals) {
                isDownloading = false;
                try {
                    // Update the local map safely
                    if (literals != null && !literals.isEmpty()) {
                        literalsMap.clear();
                        for (Literal literal : literals) {
                            if (literal != null && literal.getKey() != null && !literal.getKey().trim().isEmpty()) {
                                literalsMap.put(literal.getKey(), literal);
                            }
                        }
                        isInitialized = true;
                        Log.d(TAG, "Successfully updated literals map with " + literalsMap.size() + " entries");
                    } else {
                        Log.w(TAG, "Received empty literals list");
                    }
                    
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing downloaded literals: " + e.getMessage(), e);
                    if (callback != null) {
                        callback.onFailure("Error processing literals: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                isDownloading = false;
                Log.e(TAG, "Failed to download fresh literals: " + error);
                if (callback != null) {
                    callback.onFailure(error);
                }
            }
        });
    }

    /**
     * Get text for a given key in the current language with enhanced error handling
     */
    public String getText(String key) {
        return getText(key, currentLanguage);
    }

    /**
     * Get text for a given key in the specified language with enhanced error handling
     */
    public String getText(String key, String languageCode) {
        if (key == null || key.trim().isEmpty()) {
            Log.w(TAG, "Invalid key provided");
            return "";
        }

        if (languageCode == null || languageCode.trim().isEmpty()) {
            languageCode = "en"; // Default to English
        }

        if (!isInitialized) {
            Log.w(TAG, "DynamicLiteralsManager not initialized, returning key: " + key);
            return key;
        }

        try {
            Literal literal = literalsMap.get(key);
            if (literal == null) {
                Log.d(TAG, "No literal found for key: " + key + ", will fallback to string resources");
                return key; // Return the key itself as fallback
            }

            String text = literal.getText(languageCode);
            if (text == null || text.trim().isEmpty()) {
                Log.w(TAG, "Empty text for key: " + key + ", language: " + languageCode + ", falling back to key");
                return key; // Return the key itself as fallback
            }

            return text;
        } catch (Exception e) {
            Log.e(TAG, "Error getting text for key: " + key + ", language: " + languageCode + ", error: " + e.getMessage());
            return key; // Return the key itself as fallback
        }
    }

    /**
     * Update the current language with validation
     */
    public void setLanguage(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            Log.w(TAG, "Invalid language code provided, keeping current: " + currentLanguage);
            return;
        }

        String newLanguage = languageCode.trim().toLowerCase();
        if (!newLanguage.equals(currentLanguage)) {
            this.currentLanguage = newLanguage;
            Log.d(TAG, "Language updated to: " + currentLanguage);
        }
    }

    /**
     * Get the current language
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Check if a key exists in the literals
     */
    public boolean hasKey(String key) {
        if (key == null || key.trim().isEmpty() || !isInitialized) {
            return false;
        }
        return literalsMap.containsKey(key);
    }

    /**
     * Get all available keys safely
     */
    public String[] getAllKeys() {
        if (!isInitialized || literalsMap.isEmpty()) {
            return new String[0];
        }
        try {
            return literalsMap.keySet().toArray(new String[0]);
        } catch (Exception e) {
            Log.e(TAG, "Error getting all keys: " + e.getMessage());
            return new String[0];
        }
    }

    /**
     * Get the number of cached literals
     */
    public int getLiteralsCount() {
        return isInitialized ? literalsMap.size() : 0;
    }

    /**
     * Check if the manager is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Check if download is in progress
     */
    public boolean isDownloading() {
        return isDownloading;
    }

    /**
     * Clear all cached literals (for memory management)
     */
    public void clearCache() {
        if (literalsMap != null) {
            literalsMap.clear();
            Log.d(TAG, "Cleared literals cache");
        }
    }

    /**
     * Get memory usage info for debugging
     */
    public String getMemoryInfo() {
        return "Literals count: " + getLiteralsCount() + 
               ", Initialized: " + isInitialized + 
               ", Downloading: " + isDownloading + 
               ", Language: " + currentLanguage;
    }

    public interface LiteralsDownloadCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
