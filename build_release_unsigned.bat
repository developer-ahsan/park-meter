@echo off
echo Building unsigned release APK...

REM Clean previous builds
call gradlew.bat clean

REM Build unsigned release
call gradlew.bat assembleRelease

echo Build completed!
echo Check javaapp/build/outputs/apk/release/ for the APK file
pause
