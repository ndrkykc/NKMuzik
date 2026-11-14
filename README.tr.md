# 🎵 NK Müzik - Modern Android Müzik Çalar ve Keşfet Uygulaması

[![Android Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Dil-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![Lisans](https://img.shields.io/badge/Lisans-MIT-yellow.svg)](LICENSE)
[![Derleme Durumu](https://img.shields.io/badge/Derleme-Başarılı-brightgreen.svg)]()

**NK Müzik**; Jetpack Compose, Material 3, AndroidX Media3 (ExoPlayer), Room ve NewPipeExtractor teknolojileri ile geliştirilmiş bağımsız ve açık kaynaklı bir Android müzik uygulamasıdır. Cihaz içi yerel müziklerinizi dinlemenin yanı sıra, herhangi bir harici API anahtarına veya bulut sunucusuna ihtiyaç duymadan cihaz üzerinde YouTube ses akışlarını çözebilir ve yüksek kalitede müzik indirebilirsiniz.

[English Documentation (README.md)](README.md)

---

## ✨ Öne Çıkan Özellikler

- 🎧 **Yerel ve Çevrimiçi Müzik Oynatma:** Cihazınızdaki MP3, M4A, FLAC, WAV, OGG dosyalarını ve çevrimiçi müzik akışlarını kesinintisiz oynatın.
- 🚀 **Sunucusuz Çevrimiçi Keşif:** Herhangi bir API anahtarı veya harici sunucu olmadan `NewPipeExtractor` ile istemci tarafında (cihaz içi) YouTube müzik akışı çözme.
- ⬇️ **Arka Plan İndirme Yöneticisi:** WorkManager kullanarak müzikleri arka planda indirin ve `jaudiotagger` entegrasyonu ile kapak resmi ve sanatçı/başlık bilgilerini dosya içerisine (ID3/M4A tag) otomatik gömün.
- 🎚️ **Donanım Equalizer & Bas Güçlendirme:** `AudioFxManager` ile cihaz equalizer ön ayarlarını, frekans bantlarını ve bas güçlendirme seviyelerini tam donanımlı yönetin.
- 📑 **Oynatma Listeleri ve Favoriler:** Room veritabanı altyapısıyla özel oynatma listeleri ve favori müzikler listesi oluşturun.
- 🎨 **Dinamik Material 3 ve Özel Temalar:** Material You (Monet) dinamik renk desteği, Açık/Koyu/Zifiri Siyah AMOLED tema modları ve özel vurgu renkleri.
- 📱 **Ana Ekran Widget'ı:** Jetpack Glance altyapısı ile ana ekrandan hızlı oynatma kontrolleri.
- 🌍 **Çoklu Dil Desteği:** Türkçe ve İngilizce dil seçenekleri arası hızlı geçiş.

---

## 🛠️ Mimari ve Teknolojik Yığın

NK Müzik **Clean Architecture** prensiplerine ve Tek Yönlü Veri Akışı (UDF) içeren **MVVM** mimarisüne dayanmaktadır.

```
app/
 ├── data/
 │    ├── analytics/         # Yerel olay ve kullanım günlüğü
 │    ├── db/                # Room entity'leri, DAO'lar ve veritabanı
 │    ├── model/             # Veri transfer objeleri & UI modelleri
 │    ├── preferences/       # DataStore tercih yöneticileri
 │    ├── remote/            # Yerel yapılandırma yöneticisi
 │    ├── repository/        # Yerel & Çevrimiçi arama ve medya repoları
 │    └── scanner/           # Cihaz içi medya tarayıcısı
 ├── di/                     # Hilt Bağımlılık Enjeksiyonu modülleri
 ├── downloader/             # Arka plan indirme işçisi ve ID3 etiketleme
 ├── player/                 # Media3 ExoPlayer, MediaSessionService & AudioFx
 ├── ui/                     # Jetpack Compose arayüz ekranları ve temalar
 ├── utils/                  # Şarkı adı temizleyici, dil yöneticisi, paylaşım
 └── widget/                 # Jetpack Glance Ana Ekran Widget'ı
```

### Kullanılan Kütüphaneler ve Teknolojiler
- **UI Katmanı:** Jetpack Compose, Material 3, Compose Adaptive, Glance Widget
- **Çekirdek & Asenkron:** Kotlin Coroutines, Flow, StateFlow, ViewModel
- **Bağımlılık Enjeksiyonu:** Hilt (Dagger-Hilt)
- **Medya Motoru:** AndroidX Media3 (ExoPlayer, MediaSession, CacheDataSource)
- **Akış Çözücü:** NewPipeExtractor
- **Veritabanı & Tercihler:** Room Database, Jetpack DataStore
- **Arka Plan İşleri:** WorkManager
- **Etiketleme:** jaudiotagger (Android için optimize edilmiş ID3/M4A tag gömücü)

---

## 🚀 Kurulum ve Derleme Talimatları

### Gereksinimler
- **Android Studio:** Ladybug (2024.2.1) veya üzeri
- **JDK:** Java 17
- **Android SDK:** Target SDK 37, Min SDK 24 (Android 7.0+)

### Komut Satırından Derleme
Bu sürüm **%100 bağımsız ve açık kaynak (FOSS) dostudur**. Projeyi derlemek için herhangi bir API anahtarı veya Firebase kurulumuna ihtiyaç duymazsınız.

```bash
# Projeyi klonlayın
git clone https://github.com/ndrkykc/NKMuzik.git
cd NKMuzik

# Birim Testlerini Çalıştırın
./gradlew test

# Debug APK Oluşturun
./gradlew assembleDebug
```
Üretilen APK dosyası `app/build/outputs/apk/debug/app-debug.apk` dizininde yer alacaktır.

---

## 🛡️ Lisans

Bu proje MIT Lisansı ile lisanslanmıştır - detaylar için [LICENSE](LICENSE) dosyasına göz atabilirsiniz.
