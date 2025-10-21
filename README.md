# Eraser - Native Android App

**Pornography Addiction Recovery App with VPN-Based Content Blocking**

## Overview

Eraser is a native Android application designed to help users overcome pornography addiction through:
- **VPN-based network-level content blocking** (156,000+ adult domains)
- **Flashcard intervention system** (60 motivational cards with truly random selection)
- **Journey tracking** with streaks, achievements, and daily check-ins
- **Analytics and reports** to monitor progress
- **Gamification** with rewards and milestones

This is a complete rewrite of the original Flutter app in native Android using Kotlin and Jetpack Compose.

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room 2.6.1
- **Dependency Injection**: Hilt 2.48
- **Navigation**: Jetpack Navigation Component
- **Async**: Coroutines + Flow
- **Background Tasks**: WorkManager 2.9.0
- **Build System**: Gradle 8.12 with Kotlin DSL
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Project Structure

```
eraser-native-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/eraser/recovery/
│   │   │   │   ├── data/              # Data layer
│   │   │   │   │   ├── local/         # Room database, DAOs, entities
│   │   │   │   │   └── repository/    # Repository implementations
│   │   │   │   ├── domain/            # Business logic
│   │   │   │   │   ├── flashcard/     # Flashcard service (60 cards, random)
│   │   │   │   │   ├── journey/       # Journey tracking, streaks
│   │   │   │   │   ├── statistics/    # Analytics and reports
│   │   │   │   │   ├── achievement/   # Achievement system
│   │   │   │   │   └── notification/  # Notification service
│   │   │   │   ├── ui/                # UI layer (Jetpack Compose)
│   │   │   │   │   ├── screens/       # All app screens
│   │   │   │   │   │   ├── onboarding/
│   │   │   │   │   │   ├── home/
│   │   │   │   │   │   ├── safepage/
│   │   │   │   │   │   ├── rewards/
│   │   │   │   │   │   ├── reports/
│   │   │   │   │   │   ├── settings/
│   │   │   │   │   │   ├── history/
│   │   │   │   │   │   └── checkin/
│   │   │   │   │   ├── theme/         # Material Design 3 theme
│   │   │   │   │   └── navigation/    # Navigation graph
│   │   │   │   ├── vpn/               # VPN service (CURRENTLY NON-FUNCTIONAL)
│   │   │   │   │   ├── EraserVpnService.kt
│   │   │   │   │   ├── VpnManager.kt
│   │   │   │   │   ├── Tun2SocksManager.kt (requires native library)
│   │   │   │   │   ├── Socks5ProxyServer.kt
│   │   │   │   │   └── BlocklistService.kt (156,000+ domains)
│   │   │   │   ├── di/                # Hilt dependency injection modules
│   │   │   │   └── MainActivity.kt    # Single activity
│   │   │   ├── res/                   # Resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                      # Unit tests
│   └── build.gradle.kts               # App-level Gradle config
├── gradle/                            # Gradle wrapper
├── build.gradle.kts                   # Project-level Gradle config
├── settings.gradle.kts                # Gradle settings
├── .gitignore
└── README.md
```

## Features

### ✅ Implemented Features

1. **Onboarding Flow** (6 pages)
2. **Home Screen** with VPN toggle (UI only)
3. **Safe Page Screen** with flashcard intervention
4. **Rewards Screen** with achievements
5. **Reports Screen** with analytics
6. **Settings Screen**
7. **Blocked History Screen**
8. **Daily Check-in Screen**
9. **Database** (Room) with 6 entities
10. **Flashcard System** (60 cards, truly random)
11. **Journey Tracking** with streaks
12. **Achievement System** (15+ achievements)
13. **Statistics Service**
14. **Notification Service**

### ❌ Non-Functional Features (CRITICAL)

1. **VPN Service** - Native library `libtun2socks-jni.so` missing
2. **Content Blocking** - VPN doesn't route traffic
3. **Real-world Intervention** - Flashcards never trigger from actual blocking

## Current Status

### ✅ Working
- App launches without crashes
- All UI screens render correctly
- Navigation works
- Database operations work
- Flashcard random selection works
- Journey tracking works (without VPN)
- Achievements unlock correctly
- Emulator-friendly (no confusing VPN errors)

### ❌ Not Working
- **VPN functionality** (requires native library or alternative)
- **Adult content blocking** (depends on VPN)
- **Physical device testing** (VPN won't work)

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK 34
- Gradle 8.12

### Build Steps

1. Clone the repository:
```bash
git clone https://github.com/itsoumya-d/eraser-android-native.git
cd eraser-android-native
```

2. Open in Android Studio
3. Sync Gradle dependencies
4. Build APK:
```bash
./gradlew clean assembleDebug
```

5. Install on device/emulator:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Dependencies

### Core
- `androidx.core:core-ktx:1.12.0`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0`

### Compose
- `androidx.compose:compose-bom:2024.02.00`
- `androidx.compose.material3:material3`

### Room Database
- `androidx.room:room-runtime:2.6.1`
- `androidx.room:room-ktx:2.6.1`

### Hilt
- `com.google.dagger:hilt-android:2.48`

### WorkManager
- `androidx.work:work-runtime-ktx:2.9.0`

## License

[To be determined]

## Contributors

- Soumya Debnath (@itsoumya-d)

---

**Note**: VPN functionality is not yet implemented. The app is suitable for UI/UX testing on emulator only.
