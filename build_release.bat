@echo off
echo Building Release APK...

REM Set Android environment variables
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk

REM Add Android tools to PATH
set PATH=%ANDROID_HOME%\emulator;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools;%PATH%

echo Starting Gradle build...
call gradlew assembleRelease

if %ERRORLEVEL% EQU 0 (
    echo Build successful!
    echo APK location: javaapp\build\outputs\apk\release\javaapp-release.apk
) else (
    echo Build failed with error code %ERRORLEVEL%
)

pause 