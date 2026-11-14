# 🎵 NK Müzik - Modern Android Music Player & Discovery App

[![Android Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

**NK Müzik** is a feature-rich, standalone open-source Android music application built using Jetpack Compose, Material 3, AndroidX Media3 (ExoPlayer), Room, and NewPipeExtractor. It offers seamless playback for local audio files as well as client-side, serverless YouTube music stream discovery and high-quality audio downloading with zero third-party API keys or cloud dependencies.

[Türkçe Dokümantasyon için tıklayın (README.tr.md)](README.tr.md)

---

## ✨ Features

- 🎧 **Local & Online Music Playback:** Enjoy seamless playback of local audio files (MP3, M4A, FLAC, WAV, OGG) and online music streaming.
- 🚀 **Serverless Online Discovery:** Powered by `NewPipeExtractor` for client-side YouTube music stream resolution without requiring third-party API keys or dedicated backend servers.
- ⬇️ **Background Audio Downloader:** Download high-quality streams directly to your device storage using WorkManager with automatic ID3/M4A metadata and cover art tagging via `jaudiotagger`.
- 🎚️ **Hardware Equalizer & Bass Boost:** Full control over device equalizer presets, custom band levels, and bass boost powered by `AudioFxManager`.
- 📑 **Playlist & Favorites Management:** Create, update, and manage custom playlists and favorite tracks backed by Room Database.
- 🎨 **Dynamic Material 3 & Custom Themes:** Adaptive Material You themes (Monet), Light/Dark/AMOLED Pitch Black modes, and accent color customization.
- 📱 **Home Screen Widget:** Glance AppWidget support for quick playback controls directly from your home screen.
- 🌍 **Multi-language Support:** Seamless in-app switching between Turkish and English languages.

---

## 🛠️ Architecture & Tech Stack

NK Müzik follows **Clean Architecture** principles and the **MVVM** pattern with Unidirectional Data Flow (UDF).

```
app/
 ├── data/
 │    ├── analytics/         # Local event & usage logger
 │    ├── db/                # Room entities, DAOs, and database configuration
 │    ├── model/             # Data transfer objects & UI domain models
 │    ├── preferences/       # DataStore preference managers
 │    ├── remote/            # Local configuration manager
 │    ├── repository/        # Local & Online search & media repositories
 │    └── scanner/           # Local device audio file scanner
 ├── di/                     # Dependency Injection modules (Hilt)
 ├── downloader/             # Background download worker & ID3 artwork embedder
 ├── player/                 # Media3 ExoPlayer, MediaSessionService & AudioFx
 ├── ui/                     # Jetpack Compose UI screens, viewmodels, and theme
 ├── utils/                  # Media title cleaner, language manager, share utils
 └── widget/                 # Jetpack Glance Home Screen Widget
```

### Key Libraries & Frameworks
- **UI:** Jetpack Compose, Material 3, Compose Adaptive, Glance Widget
- **Core & Lifecycle:** Kotlin Coroutines, Flow, StateFlow, ViewModel
- **Dependency Injection:** Hilt (Dagger-Hilt)
- **Media Engine:** AndroidX Media3 (ExoPlayer, MediaSession, CacheDataSource)
- **Stream Extractor:** NewPipeExtractor
- **Database & Storage:** Room Database, Jetpack DataStore
- **Background Tasks:** WorkManager
- **Tagging:** jaudiotagger (Android-optimized ID3 & M4A tag embedder)

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer
- **JDK:** Java 17
- **Android SDK:** Target SDK 37, Min SDK 24 (Android 7.0+)

### Build via Command Line
This version is **100% standalone and FOSS-friendly**. You do not need any API keys or Firebase setup to build and run the app.

```bash
# Clone the repository
git clone https://github.com/ndrkykc/NKMuzik.git
cd NKMuzik

# Run Unit Tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug
```
The generated APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🛡️ License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
