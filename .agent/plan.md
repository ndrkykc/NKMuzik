# Project Plan

Standart bir müzik çalar uygulamasında olan tüm özellikleri bünyesinde barındıran (yerel müzik çalma, çalma listeleri, bildirim ve kilit ekranı kontrolleri, arka planda / ekran kapalıyken çalma), ek olarak YouTube üzerinden arama yapılmasına, çıkan sonuçların ses olarak oynatılmasına ve gelen ham ses dosyalarının (m4a, webm vb.) doğrudan cihaz hafızasına indirilmesine olanak sağlayan tam kapsamlı bir Android müzik çalar uygulaması (NK Müzik). Uygulama ilk açılışta dosya/medya ve bildirim izinlerini istemelidir. Ek olarak Ekolayzır, Şarkı Sözleri, Uyku Zamanlayıcısı ve Dinamik Renk/Tema desteği bulunmalıdır.

## Project Brief

# NK Müzik - Proje Özeti (Project Brief)

## Özellikler (Features)

1. **Yerel Medya Çalma ve Arka Plan Hizmeti**
   - Cihaz hafızasındaki ses dosyalarını tarama, oynatma ve çalma listeleri oluşturma.
   - Ekran kapalıyken veya arka planda kesintisiz müzik çalma, bildirim çubuğu ve kilit ekranı medya kontrolleri.
   - Uygulama ilk açılışında dosya/medya erişim ve bildirim izinlerinin yönetimi.

2. **YouTube Ses Akışı ve Ham Format İndirme**
   - YouTube üzerinden arama yapabilme ve arama sonuçlarını ses akışı (streaming) olarak oynatabilme.
   - YouTube ham ses dosyalarını (m4a, webm vb.) ses kalitesi bozulmadan doğrudan cihaz hafızasına indirme.

3. **Gelişmiş Ses ve Oynatma Araçları**
   - Uygulama içi entegre ses ekolayzırı ve bas güçlendirme (Bass Boost) desteği.
   - Çalınan şarkıya ait şarkı sözlerinin (Lyrics) eşzamanlı gösterimi.
   - Belirlenen süre tamamlandığında müziği otomatik olarak durduran Uyku Zamanlayıcısı (Sleep Timer).

4. **Dinamik Tema ve Material You Desteği**
   - Aydınlık (Light) ve Karanlık (Dark) tema desteği.
   - Android Material You standartlarına uygun Dinamik Renk (Dynamic Color) uyumluluğu.

## Üst Düzey Teknik Yığın (High-Level Tech Stack)

- **Programlama Dili:** Kotlin
- **Kullanıcı Arayüzü (UI):** Jetpack Compose & Material Design 3
- **Navigasyon:** **Jetpack Navigation 3** (durum odaklı / state-driven)
- **Adaptif Arayüz Stratejisi:** **Compose Material Adaptive** (farklı ekran ve cihaz tiplerine uyumlu adaptif düzenler)
- **Medya Oynatma & Servis Yapısı:** Jetpack Media3 (ExoPlayer, MediaSession, AudioProcessor / Equalizer)
- **Asenkron İş Düzeyi:** Kotlin Coroutines & Flow

## Implementation Steps
**Total Duration:** 1h 1m 23s

### Task_1_CoreMediaAndPermissionsSetup: Implement core permissions (storage/media, notification), Jetpack Navigation 3 setup, local media scanning engine, and Media3 ExoPlayer background playback service with MediaSession.
- **Status:** COMPLETED
- **Updates:** Task 1 completed successfully: Declared media permissions and MediaSessionService in AndroidManifest.xml, configured AndroidX Media3 ExoPlayer service (PlaybackService) for background playback and lockscreen controls, implemented LocalMediaScanner and LocalMediaRepository for scanning device audio files, built PermissionHandler composable for app startup, and configured navigation architecture. Built debug APK successfully.
- **Acceptance Criteria:**
  - Permissions handling for media access and notifications implemented
  - Navigation 3 setup configured
  - Media3 ExoPlayer service with MediaSession and notification/lockscreen controls working
  - Local audio scanning utility functional
- **Duration:** 5m 59s

### Task_2_PlayerAndLibraryUI: Build Jetpack Compose Material 3 UI including Local Track Library, Playlist Management, Now Playing screen, and Dynamic Color / Material You theme support.
- **Status:** COMPLETED
- **Updates:** Task 2 completed successfully: Theme setup with Material You Dynamic Colors and ThemePreferences completed. LibraryScreen built with local tracks, search, sorting, track list item cards, and overflow options. Playlist management implemented with Room database (AppDatabase, PlaylistDao). NowPlaying screen & Mini Player created with seekbar, shuffle/repeat, play/pause controls, and MediaController state integration. Verified build assembleDebug.
- **Acceptance Criteria:**
  - Track list, album/artist view, and playlist creation/editing screens built
  - Now Playing screen with playback controls and progress bar integrated with Media3 service
  - Material You dynamic light/dark theming supported
- **Duration:** 28m 7s

### Task_3_YouTubeStreamingAndDownload: Implement YouTube search, audio streaming (m4a/webm), and background raw audio downloading functionality.
- **Status:** COMPLETED
- **Updates:** Task 3 completed successfully: Implemented YouTube Search & Extractor in YouTubeSearchRepository, configured ExoPlayer with HttpDataSource headers for smooth YouTube audio streaming, implemented DownloadAudioWorker using OkHttp and WorkManager to download raw YouTube audio (m4a/webm) as-is without re-encoding to Music/NK Muzik storage and MediaStore, updated YouTubeSearchScreen UI with live streaming and download progress state. Verified build with ./gradlew app:assembleDebug.
- **Acceptance Criteria:**
  - YouTube search and result list UI implemented
  - YouTube audio stream extractor integrated into ExoPlayer playback
  - Raw audio download feature saves m4a/webm files locally
- **Duration:** 4m 21s

### Task_4_AudioToolsAndLyrics: Implement Equalizer & Bass Boost controls, synchronized/static Lyrics view, and Sleep Timer functionality.
- **Status:** COMPLETED
- **Updates:** Task 4 completed successfully: Implemented AudioFxManager for ExoPlayer audio session ID with 5-band equalizer, presets, and Bass Boost controls. Built Equalizer UI screen and dialog. Implemented LyricsRepository supporting synchronized LRC lyrics with auto-scroll in NowPlayingBottomSheet. Built SleepTimerController supporting 15m, 30m, 45m, 60m, end-of-song timer, and countdown display. Verified build assembleDebug.
- **Acceptance Criteria:**
  - Equalizer and Bass Boost audio processing controls integrated with ExoPlayer
  - Lyrics display component integrated in Now Playing screen
  - Sleep timer functional to pause playback after set duration
- **Duration:** 4m 37s

### Task_5_RunAndVerify: Run and verify application stability, ensure build passes without crashes, check alignment with user requirements, and verify UI fidelity. Instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Task 5 completed successfully: App deployed and tested on standard phone emulator. Verified permission handling, local track scanning, Media3 background playback service, YouTube search & streaming, raw audio downloading (m4a/webm), playlist management, 5-band equalizer & bass boost, synchronized lyrics view, sleep timer countdown, and Material You dynamic theme switching. Zero crashes, all features working.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - all core features verified
- **Duration:** 18m 19s

