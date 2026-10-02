package com.parkmeter.og.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.parkmeter.og.model.AgentLoginResponse;
import com.parkmeter.og.model.Zone;

public class SharedPreferencesManager {
    private static final String PREF_NAME = "ParkMeterPrefs";
    private static final String KEY_SELECTED_ZONE = "selected_zone";
    private static final String KEY_IS_FIRST_TIME = "is_first_time";
    private static final String KEY_USER_JSON = "user_json";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_LAST_EMAIL = "last_email";
    private static final String KEY_SELECTED_CITY = "selected_city";

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

    // --- Login session ---

    public void saveLoginSession(AgentLoginResponse.AgentUser user, String token,
                                 boolean rememberMe, String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_JSON, gson.toJson(user));
        editor.putString(KEY_TOKEN, token);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        if (email != null) {
            editor.putString(KEY_LAST_EMAIL, email);
        }
        editor.apply();
    }

    public AgentLoginResponse.AgentUser getLoggedInUser() {
        String json = sharedPreferences.getString(KEY_USER_JSON, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, AgentLoginResponse.AgentUser.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String getAuthToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public boolean isRememberMe() {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
    }

    public String getLastEmail() {
        return sharedPreferences.getString(KEY_LAST_EMAIL, null);
    }

    public void saveSelectedCity(Zone.City city) {
        sharedPreferences.edit().putString(KEY_SELECTED_CITY, gson.toJson(city)).apply();
    }

    public Zone.City getSelectedCity() {
        String json = sharedPreferences.getString(KEY_SELECTED_CITY, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, Zone.City.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void clearLoginSession() {
        boolean rememberMe = isRememberMe();
        String lastEmail = getLastEmail();
        sharedPreferences.edit().clear().apply();
        if (rememberMe && lastEmail != null) {
            sharedPreferences.edit()
                .putBoolean(KEY_REMEMBER_ME, true)
                .putString(KEY_LAST_EMAIL, lastEmail)
                .apply();
        }
    }
}
