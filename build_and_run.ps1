# Build and Run Android App Script
Write-Host "Building Android app..." -ForegroundColor Green

# Set Android environment variables
$env:ANDROID_HOME = "C:\Users\Dell\AppData\Local\Android\Sdk"
$env:PATH += ";$env:ANDROID_HOME\emulator;$env:ANDROID_HOME\platform-tools"

# Build the app
Write-Host "Running gradle build..." -ForegroundColor Yellow
.\gradlew assembleDebug

# Wait for emulator to be ready
Write-Host "Waiting for emulator to be ready..." -ForegroundColor Yellow
do {
    Start-Sleep -Seconds 5
    $devices = adb devices
    Write-Host "Checking devices: $devices"
} while ($devices -notmatch "device")

Write-Host "Emulator is ready!" -ForegroundColor Green

# Install the APK
$apkPath = "javaapp\build\outputs\apk\debug\javaapp-debug.apk"
if (Test-Path $apkPath) {
    Write-Host "Installing APK..." -ForegroundColor Yellow
    adb install -r $apkPath
    
    # Launch the app
    Write-Host "Launching app..." -ForegroundColor Yellow
    adb shell am start -n com.parkmeter.og.debug/.MainActivity
    
    Write-Host "App should now be running on the emulator!" -ForegroundColor Green
} else {
    Write-Host "APK not found at: $apkPath" -ForegroundColor Red
    Write-Host "Build may have failed. Check the gradle output above." -ForegroundColor Red
} 