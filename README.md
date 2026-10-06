# DZGameBooster - Android Native App

A native Android application built with Kotlin that uses the official **Rikka Shizuku API** to execute rootless ADB commands directly from Android.

## Features
- **Aggressive RAM Purge**: Force-stops heavy background apps (FB, Instagram, TikTok, Chrome) before gaming.
- **Zero Animation Scale**: Eliminates window, transition, and animator durations for snappy touch response.
- **Do Not Disturb Mode**: Silences incoming notification popups.
- **Adrenoboost Toggle**: Forces GPU high performance when supported.
- **Target Launch**: Automatically launches jp.konami.pesam.

## How to Build the APK

### Method 1: Using Android Studio (PC / Mac / Linux)
1. Open Android Studio.
2. Select **File > Open...** and select this extracted directory.
3. Wait for Gradle sync to finish.
4. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The generated APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

### Method 2: Build directly on your Android phone using Termux!
You can compile this APK right on your phone without a PC:
```bash
# 1. Install Java 17 and Gradle in Termux
pkg update -y && pkg install openjdk-17 gradle git -y

# 2. Extract this project in Termux
cd /sdcard/Download/ShizukuGameBooster

# 3. Build the APK
gradle assembleDebug

# 4. Install the generated APK
termux-open app/build/outputs/apk/debug/app-debug.apk
```

### Method 3: Manual pipeline (used for releases)
```bash
cd manual && bash build.sh
```
Requirements: JDK 17, Android SDK (platform 34, build-tools 34), `kotlinc`, `aapt2`, `d8`, `zipalign`, `apksigner`.

## Releases

Installable APKs are attached to each GitHub release. Signed with the project's debug key, so newer versions install as updates over older ones.

## Contact

- Telegram Bot: https://t.me/Dominic_aiBot
- GitHub: https://github.com/zinmyo19
- Website: https://dzinlabs-site.zynelabs.workers.dev/
