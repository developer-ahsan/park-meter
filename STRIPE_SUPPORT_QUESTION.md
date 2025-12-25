# Stripe Terminal Android SDK v5.0.0 - Tap to Pay Discovery Not Working

## Environment
- **Stripe Terminal SDK Version:** 5.0.0
- **Device:** Android phone with NFC support (Tap to Pay capable)
- **Target Use Case:** Tap to Pay on Android (device itself as reader)
- **Android Version:** API 26+
- **Issue:** `discoverReaders()` appears to start successfully but never calls `onUpdateDiscoveredReaders()` callback

## Problem Description

I'm integrating Stripe Terminal SDK v5.0.0 for Tap to Pay on Android. The discovery process starts without errors, but the `onUpdateDiscoveredReaders` callback is never invoked. The app shows a loading indicator indefinitely.

**Expected Behavior:** 
- Discovery should find the device itself as a Tap to Pay reader
- `onUpdateDiscoveredReaders` should be called with at least one reader

**Actual Behavior:**
- `discoverReaders()` onSuccess callback is called
- `onUpdateDiscoveredReaders` is NEVER called
- No errors are thrown
- App remains in "Discovering Readers" state indefinitely

## Code Implementation

### 1. Gradle Dependencies (`build.gradle.kts`)

```kotlin
val stripeTerminalVersion = "5.0.0"

dependencies {
    implementation("com.stripe:stripeterminal-taptopay:$stripeTerminalVersion")
    implementation("com.stripe:stripeterminal-core:$stripeTerminalVersion")
    implementation("com.stripe:stripeterminal-external:$stripeTerminalVersion")
}
```

### 2. Application Class - Terminal Initialization

```java
public class StripeTerminalApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d("StripeTerminalApplication", "========== INITIALIZING STRIPE TERMINAL ==========");
        TerminalApplicationDelegate.onCreate(this);
        Log.d("StripeTerminalApplication", "TerminalApplicationDelegate.onCreate() completed");
        Log.d("StripeTerminalApplication", "==================================================");
    }
}
```

**AndroidManifest.xml:**
```xml
<application
    android:name=".StripeTerminalApplication"
    ...>
</application>
```

### 3. Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.NFC" />
```

All permissions are granted at runtime before calling discovery.

### 4. Discovery Implementation (PaymentFragment.java)

```java
private void startAutoDiscovery() {
    if (isDiscovering) {
        return;
    }
    
    // In v5.0.0, Terminal initializes lazily - calling discoverReaders will trigger initialization
    Log.d("PaymentFragment", "========== START AUTO DISCOVERY ==========");
    Log.d("PaymentFragment", "Terminal.isInitialized(): " + Terminal.isInitialized());
    
    updateStatus("Discovering readers...", true);
    isDiscovering = true;
    
    // For Tap to Pay on Android, the device itself is the reader
    final boolean isSimulated = false; // Production mode
    Log.d("PaymentFragment", "========================================");
    Log.d("PaymentFragment", "Creating TapToPayDiscoveryConfiguration");
    Log.d("PaymentFragment", "  Mode: PRODUCTION (real payments)");
    Log.d("PaymentFragment", "  Device will act as the reader (Tap to Pay)");
    Log.d("PaymentFragment", "========================================");
    
    final DiscoveryConfiguration config = new DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSimulated);
    Log.d("PaymentFragment", "✓ TapToPayDiscoveryConfiguration created successfully");
    
    try {
        Log.d("PaymentFragment", "Attempting to get Terminal instance...");
        Terminal terminal = Terminal.getInstance();
        Log.d("PaymentFragment", "✓ Got Terminal instance successfully");
        Log.d("PaymentFragment", "Calling discoverReaders()...");
        
        discoveryTask = terminal.discoverReaders(config, discoveryListener, new Callback() {
            @Override
            public void onSuccess() {
                Log.d("PaymentFragment", "✓✓✓ Discovery started successfully");
                Log.d("PaymentFragment", "Terminal now initialized: " + Terminal.isInitialized());
                Log.d("PaymentFragment", "Waiting for onUpdateDiscoveredReaders callback...");
                
                // Set a timeout for discovery (30 seconds)
                discoveryTimeoutRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (isDiscovering && !isReaderConnected) {
                            Log.e("PaymentFragment", "✗✗✗ Discovery timeout - no readers found after 30 seconds");
                            Log.e("PaymentFragment", "Device may not support Tap to Pay or NFC may be disabled");
                            // ... timeout handling code ...
                        }
                    }
                };
                discoveryTimeoutHandler.postDelayed(discoveryTimeoutRunnable, 30000);
            }
            
            @Override
            public void onFailure(@NonNull TerminalException e) {
                Log.e("PaymentFragment", "✗✗✗ Discovery FAILED to start");
                Log.e("PaymentFragment", "Error message: " + e.getErrorMessage());
                Log.e("PaymentFragment", "Error code: " + e.getErrorCode());
                Log.e("PaymentFragment", "Error type: " + e.getClass().getSimpleName());
                // ... error handling ...
            }
        });
        Log.d("PaymentFragment", "discoverReaders() called, waiting for callbacks...");
    } catch (Exception e) {
        Log.e("PaymentFragment", "✗✗✗ EXCEPTION calling discoverReaders()", e);
        updateStatus("Discovery error: " + e.getMessage(), false);
        isDiscovering = false;
    }
    Log.d("PaymentFragment", "==========================================");
}

private final DiscoveryListener discoveryListener = new DiscoveryListener() {
    @Override
    public void onUpdateDiscoveredReaders(@NonNull List<Reader> readers) {
        Log.d("PaymentFragment", "========== onUpdateDiscoveredReaders ==========");
        Log.d("PaymentFragment", "Readers count: " + readers.size());
        for (int i = 0; i < readers.size(); i++) {
            Reader r = readers.get(i);
            Log.d("PaymentFragment", "  Reader[" + i + "]: " + r.getSerialNumber() + " (Type: " + r.getDeviceType() + ")");
        }
        Log.d("PaymentFragment", "============================================");
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (!readers.isEmpty()) {
                    // Cancel discovery timeout since we found a reader
                    if (discoveryTimeoutRunnable != null) {
                        discoveryTimeoutHandler.removeCallbacks(discoveryTimeoutRunnable);
                    }
                    
                    // Auto-connect to the first available reader
                    Reader reader = readers.get(0);
                    connectToReader(reader);
                } else {
                    Log.d("PaymentFragment", "No readers found yet, still searching...");
                }
            });
        }
    }
};
```

### 5. Connection Implementation

```java
private void connectToReader(Reader reader) {
    Log.d("PaymentFragment", "========== CONNECT TO READER ==========");
    Log.d("PaymentFragment", "Reader: " + reader.getSerialNumber());
    Log.d("PaymentFragment", "Terminal.isInitialized(): " + Terminal.isInitialized());
    
    updateStatus("Connecting to " + reader.getSerialNumber() + "...", true);
    
    String connectLocationId = dynamicLocationId != null ? dynamicLocationId : "tml_GJv9FgsphhQmKS";
    
    Terminal.getInstance().connectReader(
        reader,
        new ConnectionConfiguration.TapToPayConnectionConfiguration(
            connectLocationId,
            true,
            (MainActivity) getActivity()
        ),
        new ReaderCallback() {
            @Override
            public void onSuccess(@NonNull Reader reader) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        connectedReaderName = reader.getSerialNumber();
                        isReaderConnected = true;
                        
                        // Configure Tap to Pay UX with Front tap zone
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).configureTapToPayUX();
                        }
                        
                        // ... success handling ...
                    });
                }
            }
            
            @Override
            public void onFailure(@NonNull TerminalException e) {
                Log.e("PaymentFragment", "Connection failed: " + e.getErrorMessage());
                // ... error handling ...
            }
        }
    );
}
```

## Logs Output

### What I See:
```
D/PaymentFragment: ========== START AUTO DISCOVERY ==========
D/PaymentFragment: Terminal.isInitialized(): false
D/PaymentFragment: ========================================
D/PaymentFragment: Creating TapToPayDiscoveryConfiguration
D/PaymentFragment:   Mode: PRODUCTION (real payments)
D/PaymentFragment:   Device will act as the reader (Tap to Pay)
D/PaymentFragment: ========================================
D/PaymentFragment: ✓ TapToPayDiscoveryConfiguration created successfully
D/PaymentFragment: Attempting to get Terminal instance...
D/PaymentFragment: ✓ Got Terminal instance successfully
D/PaymentFragment: Calling discoverReaders()...
D/PaymentFragment: ✓✓✓ Discovery started successfully
D/PaymentFragment: Terminal now initialized: true
D/PaymentFragment: Waiting for onUpdateDiscoveredReaders callback...
D/PaymentFragment: discoverReaders() called, waiting for callbacks...
D/PaymentFragment: ==========================================

[... 30 seconds pass ...]

E/PaymentFragment: ✗✗✗ Discovery timeout - no readers found after 30 seconds
```

### What I DON'T See (but expect):
```
D/PaymentFragment: ========== onUpdateDiscoveredReaders ==========
D/PaymentFragment: Readers count: 1
```

## Questions

1. **Is `TapToPayDiscoveryConfiguration` the correct configuration for Tap to Pay on Android where the device itself acts as the reader?** Or should I use a different discovery configuration?

2. **Why would `discoverReaders()` succeed (onSuccess callback called) but `onUpdateDiscoveredReaders` never be invoked?** Are there any prerequisites or setup steps I'm missing?

3. **Does Tap to Pay on Android require any additional setup beyond `TerminalApplicationDelegate.onCreate()`?** The v5.0.0 migration guide shows this is the new way to initialize, but is there something else needed for discovery to work?

4. **Is there a way to check if the device is actually capable of Tap to Pay before attempting discovery?** Some sort of capability check API?

5. **Are there any known issues with v5.0.0 discovery for Tap to Pay on Android?** Should I consider downgrading to v4.6.0 or waiting for v5.0.1?

6. **What conditions must be met for `onUpdateDiscoveredReaders` to be called?** Is there a way to debug why this callback is never invoked?

## Additional Information

- Device has NFC enabled
- Location services are enabled
- All required permissions are granted
- No errors or exceptions are logged
- The same codebase worked with v4.6.0 (with different initialization)
- Stripe account is properly configured for Tap to Pay

## Request

Could you please provide:
1. A working code example for Tap to Pay discovery in v5.0.0
2. Any documentation on debugging discovery issues
3. Confirmation of the correct DiscoveryConfiguration to use
4. Any known issues or workarounds for this version

Thank you!

