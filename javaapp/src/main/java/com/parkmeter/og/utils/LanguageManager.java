package com.parkmeter.og.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import com.parkmeter.og.StripeTerminalApplication;

import java.util.Locale;

public class LanguageManager {
    private static final String TAG = "LanguageManager";
    private static final String PREF_LANGUAGE = "selected_language";
    private static final String DEFAULT_LANGUAGE = "en";
    
    public static void setLanguage(Context context, String languageCode) {
        try {
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);
            
            Resources resources = context.getResources();
            Configuration configuration = new Configuration(resources.getConfiguration());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLocale(locale);
            } else {
                configuration.locale = locale;
            }
            
            context.createConfigurationContext(configuration);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            
            // Save language preference
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_LANGUAGE, languageCode)
                    .apply();
            
            // Update dynamic literals manager
            try {
                DynamicLiteralsManager literalsManager = StripeTerminalApplication.getInstance().getLiteralsManager();
                if (literalsManager != null) {
                    literalsManager.setLanguage(languageCode);
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not update literals manager: " + e.getMessage());
            }
                    
            Log.d(TAG, "Language set to: " + languageCode);
        } catch (Exception e) {
            Log.e(TAG, "Error setting language: " + e.getMessage());
        }
    }
    
    public static String getCurrentLanguage(Context context) {
        return context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                .getString(PREF_LANGUAGE, DEFAULT_LANGUAGE);
    }
    
    public static void applyLanguage(Context context) {
        String savedLanguage = getCurrentLanguage(context);
        if (!savedLanguage.equals(DEFAULT_LANGUAGE)) {
            setLanguage(context, savedLanguage);
        }
    }
    
    public static boolean isLanguageChanged(Context context, String newLanguage) {
        String currentLanguage = getCurrentLanguage(context);
        return !currentLanguage.equals(newLanguage);
    }
}
