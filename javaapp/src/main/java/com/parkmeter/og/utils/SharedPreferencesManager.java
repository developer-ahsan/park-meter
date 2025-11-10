package com.parkmeter.og.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.parkmeter.og.model.Zone;

public class SharedPreferencesManager {
    private static final String PREF_NAME = "ParkMeterPrefs";
    private static final String KEY_SELECTED_ZONE = "selected_zone";
    private static final String KEY_IS_FIRST_TIME = "is_first_time";
    
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public SharedPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveSelectedZone(Zone zone) {
        String zoneJson = gson.toJson(zone);
        sharedPreferences.edit().putString(KEY_SELECTED_ZONE, zoneJson).apply();
    }

    public Zone getSelectedZone() {
        String zoneJson = sharedPreferences.getString(KEY_SELECTED_ZONE, null);
        if (zoneJson != null) {
            try {
                return gson.fromJson(zoneJson, Zone.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isFirstTime() {
        return sharedPreferences.getBoolean(KEY_IS_FIRST_TIME, true);
    }

    public void setFirstTime(boolean isFirstTime) {
        sharedPreferences.edit().putBoolean(KEY_IS_FIRST_TIME, isFirstTime).apply();
    }

    public void clearSelectedZone() {
        sharedPreferences.edit().remove(KEY_SELECTED_ZONE).apply();
    }

    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }
} 
