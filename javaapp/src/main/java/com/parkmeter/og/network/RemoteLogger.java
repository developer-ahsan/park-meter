package com.parkmeter.og.network;

import android.util.Log;
import java.util.concurrent.TimeUnit;

/**
 * Remote logging utility to send logs to backend server for debugging
 */
public class RemoteLogger {
    private static final String TAG = "LocalLogger";

    public static void log(String tag, String message, String level) {
        // Always log locally
        switch (level.toLowerCase()) {
            case "error":
                Log.e(tag, message);
                break;
            case "warn":
                Log.w(tag, message);
                break;
            case "info":
                Log.i(tag, message);
                break;
            default:
                Log.d(tag, message);
                break;
        }
    }

    public static void d(String tag, String message) {
        log(tag, message, "debug");
    }

    public static void i(String tag, String message) {
        log(tag, message, "info");
    }

    public static void w(String tag, String message) {
        log(tag, message, "warn");
    }

    public static void e(String tag, String message) {
        log(tag, message, "error");
    }

    public static void e(String tag, String message, Throwable throwable) {
        String fullMessage = message + "\n" + Log.getStackTraceString(throwable);
        log(tag, fullMessage, "error");
    }

    // Remote logging removed - only local logging is used
} 
