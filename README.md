# ZUCCHERO Waktu Sholat AI 🕌

Aplikasi Android modern berbasis **Kotlin**, **Jetpack Compose (Material 3)**, **Room Database**, dan **Google Play Services Location API** untuk jadwal sholat presisi, kompas kiblat, asisten Islami AI, alarm adzan otomatis, serta pencatatan notifikasi WhatsApp.

---

## 📌 Informasi Aplikasi

- **Nama Aplikasi:** ZUCCHERO Waktu Sholat AI
- **Versi Aplikasi (Version Name):** 1.0
- **Version Code:** 1
- **Package Name / Application ID:** `com.aistudio.salatapp.rhyk`
- **Target SDK:** 36 (Android 15+)
- **Minimum SDK:** 24 (Android 7.0+)

---

## 📦 File APK Siap Pakai

Kedua file APK hasil kompilasi asli (bukan placeholder) tersedia di direktori `/release/` pada repositori ini:

| Varian APK | Nama File | Lokasi File | Deskripsi |
| :--- | :--- | :--- | :--- |
| **DEBUG APK** | `app-debug.apk` | `release/app-debug.apk` | Varian debug dengan log lengkap untuk pengujian dan development |
| **RELEASE APK** | `app-release.apk` | `release/app-release.apk` | Varian release teroptimasi dan tersertifikasi signing untuk pengguna umum |

---

## 📲 Cara Install APK di HP Android

1. Ambil file `app-debug.apk` atau `app-release.apk` dari folder `release/`.
2. Kirim atau salin file `.apk` ke smartphone Android Anda (bisa melalui USB, Google Drive, atau WhatsApp).
3. Buka file manager di HP, lalu ketuk file `.apk` yang ingin diinstal.
4. Jika muncul peringatan *"Install from unknown sources"* (Instal dari sumber tidak dikenal), izinkan aplikasi pengelola file Anda untuk memasang APK.
5. Klik **Install / Pasang** dan tunggu proses instalasi selesai.
6. Buka aplikasi **ZUCCHERO Waktu Sholat AI** dan berikan izin Notifikasi serta Lokasi (GPS) agar jadwal sholat otomatis terhitung sesuai lokasi Anda.

---

## 🛠️ Cara Build Ulang Project

Jika Anda mengkloning repository ini ke komputer atau lingkungan lokal, Anda dapat membangun ulang APK menggunakan Gradle:

1. **Pastikan prasyarat terpasang:**
   - JDK 17 atau JDK 21
   - Android SDK (Platform 36, Build-Tools 36.x)

2. **Build Debug APK:**
   ```bash
   gradle :app:assembleDebug
   ```
   *Output file:* `app/build/outputs/apk/debug/app-debug.apk`

3. **Build Release APK:**
   ```bash
   gradle :app:assembleRelease
   ```
   *Output file:* `app/build/outputs/apk/release/app-release.apk`

4. **Salin hasil build ke folder release:**
   ```bash
   mkdir -p release
   cp app/build/outputs/apk/debug/app-debug.apk release/app-debug.apk
   cp app/build/outputs/apk/release/app-release.apk release/app-release.apk
   ```

---

## 🛰️ Fitur Layanan GPS (LocationService)
Aplikasi menggunakan **LocationService** berbasis Google Play Services (`FusedLocationProviderClient`) untuk mengambil koordinat GPS perangkat secara akurat dan menghitung waktu sholat (Imsak, Subuh, Terbit, Dzuhur, Ashar, Maghrib, Isya) berdasarkan lintang, bujur, dan metode hisab astronomis standar.
