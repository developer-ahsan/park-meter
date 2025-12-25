package com.parkmeter.og;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import com.parkmeter.og.model.AppState;
import com.parkmeter.og.utils.DynamicLiteralsManager;
import com.stripe.stripeterminal.TerminalApplicationDelegate;

public class StripeTerminalApplication extends Application {
    
    private static StripeTerminalApplication instance;
    private AppState appState;
    private DynamicLiteralsManager literalsManager;

    @Override
    public void onCreate() {
        // Should happen before super.onCreate()
        StrictMode.setThreadPolicy(
                new StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectAll()
                        .penaltyLog()
                        .build());

        StrictMode.setVmPolicy(
                new StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
                        .detectLeakedClosableObjects()
                        .penaltyLog()
                        .build());

        super.onCreate();

        instance = this;
        appState = new AppState();

        // Initialize literals manager
        literalsManager = DynamicLiteralsManager.getInstance(this);
        literalsManager.initialize();
        
        // Download fresh literals in background
        downloadFreshLiterals();

        Log.d("StripeTerminalApplication", "========== INITIALIZING STRIPE TERMINAL ==========");
        TerminalApplicationDelegate.onCreate(this);
        Log.d("StripeTerminalApplication", "TerminalApplicationDelegate.onCreate() completed");
        Log.d("StripeTerminalApplication", "==================================================");
    }

    public static StripeTerminalApplication getInstance() {
        return instance;
    }

    public AppState getAppState() {
        return appState;
    }

    public DynamicLiteralsManager getLiteralsManager() {
        return literalsManager;
    }

    private void downloadFreshLiterals() {
        literalsManager.downloadFreshLiterals(new DynamicLiteralsManager.LiteralsDownloadCallback() {
            @Override
            public void onSuccess() {
                Log.d("StripeTerminalApplication", "Successfully downloaded fresh literals");
            }

            @Override
            public void onFailure(String error) {
                Log.e("StripeTerminalApplication", "Failed to download fresh literals: " + error);
            }
        });
    }
}
