package com.parkmeter.og;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;

import com.parkmeter.og.model.AppState;
import com.parkmeter.og.utils.DynamicLiteralsManager;
import com.stripe.stripeterminal.TerminalApplicationDelegate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StripeTerminalApplication extends Application {

    private static final String TAG = "StripeTerminalApplication";
    private static final String WATCHDOG_TAG = "MainThreadWatchdog";

    // How often the watchdog pings the main thread.
    private static final long WATCHDOG_CHECK_INTERVAL_MS = 2_000L;
    // How long the main thread is allowed to go without responding before we log it as stuck.
    private static final long WATCHDOG_TIMEOUT_MS = 5_000L;

    private static StripeTerminalApplication instance;
    private AppState appState;
    private DynamicLiteralsManager literalsManager;

    @Override
    public void onCreate() {
        // StrictMode adds real per-call overhead (a stack trace is captured and logged on
        // every violation) and, more importantly, installing a custom policy replaces the
        // platform's default one - including its default "crash on
        // NetworkOnMainThreadException" behavior. That combination is exactly what turns a
        // real bug (an accidental blocking network call on the main thread) into a silent
        // freeze instead of a loud, easy-to-diagnose crash. Debug builds only.
        if (BuildConfig.DEBUG) {
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
        }

        super.onCreate();

        instance = this;
        appState = new AppState();

        installCrashLogging();
        startMainThreadWatchdog();

        // Initialize the literals manager off the main thread: the cache load is a file
        // read + full JSON parse, and LiteralsHelper.getText() already falls back cleanly
        // to string resources while isInitialized() is false, so nothing needs this to
        // finish synchronously during app startup.
        literalsManager = DynamicLiteralsManager.getInstance(this);
        new Thread(() -> {
            literalsManager.initialize();
            // Download fresh literals in background
            downloadFreshLiterals();
        }, "literals-init").start();

        Log.d(TAG, "========== INITIALIZING STRIPE TERMINAL ==========");
        TerminalApplicationDelegate.onCreate(this);
        Log.d(TAG, "TerminalApplicationDelegate.onCreate() completed");
        Log.d(TAG, "==================================================");
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
                Log.d(TAG, "Successfully downloaded fresh literals");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to download fresh literals: " + error);
            }
        });
    }

    /**
     * Log uncaught exceptions on any thread before handing off to the platform's default
     * handler, so a crash leaves a trace in the same log the field can pull via the
     * in-app log viewer.
     */
    private void installCrashLogging() {
        final Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception on thread '" + thread.getName() + "'", throwable);
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable);
            }
        });
    }

    /**
     * Watches for the main thread going unresponsive (an ANR-shaped freeze) and logs the
     * main thread's stack trace when it happens, so a field report of "the app froze" can
     * be turned into an actual stack via the in-app log viewer instead of a guess.
     *
     * This runs in release too - it is one sleeping background thread and is the entire
     * point of the instrumentation.
     */
    private void startMainThreadWatchdog() {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        Thread watchdog = new Thread(() -> {
            while (true) {
                final CountDownLatch latch = new CountDownLatch(1);
                mainHandler.post(latch::countDown);

                boolean responded;
                try {
                    responded = latch.await(WATCHDOG_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (!responded) {
                    logMainThreadStack();
                    // Wait for the main thread to actually catch up before resuming the
                    // normal-interval polling, so a long freeze produces one trace instead
                    // of a flood of identical ones.
                    try {
                        latch.await();
                        Log.w(WATCHDOG_TAG, "Main thread responded again after being unresponsive for over "
                                + WATCHDOG_TIMEOUT_MS + "ms.");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                try {
                    Thread.sleep(WATCHDOG_CHECK_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "main-thread-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private void logMainThreadStack() {
        StackTraceElement[] stack = Looper.getMainLooper().getThread().getStackTrace();
        StringBuilder sb = new StringBuilder("Main thread unresponsive for over ")
                .append(WATCHDOG_TIMEOUT_MS)
                .append("ms. Stack:\n");
        for (StackTraceElement element : stack) {
            sb.append("    at ").append(element).append('\n');
        }
        Log.e(WATCHDOG_TAG, sb.toString());
    }
}
