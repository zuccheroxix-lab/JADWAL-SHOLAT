# ZUCCHERO Waktu Sholat AI 🕌

Aplikasi Android modern berbasis **Jetpack Compose**, **MVVM Clean Architecture**, **Room Database**, dan **Google Gemini AI** untuk membantu umat Muslim dalam mengelola waktu ibadah, pengingat adzan otomatis, arah kiblat, asisten tanya jawab Islami cerdas, pencatatan notifikasi WhatsApp, serta sistem **Local Logging & Ekspor Pesan WhatsApp**.

---

## ✨ Fitur Unggulan

1. **Jadwal Sholat Akurat & Real-Time**
   - Deteksi GPS otomatis & pilihan kota manual (Jakarta, Surabaya, Bandung, Medan, Makassar, Yogyakarta, dll.).
   - Jadwal lengkap: Imsak, Subuh, Terbit, Dzuhur, Ashar, Maghrib, dan Isya.
   - Perhitungan countdown mundur menuju sholat berikutnya beserta progress bar interaktif.
   - Kalender Masehi & Hijriah terintegrasi.

2. **Sistem Notifikasi & Adzan**
   - Alarm adzan otomatis dengan pilihan muadzin (Makkah, Madinah, Al-Aqsa, Mesir, Standar).
   - Pengingat 10 menit sebelum waktu sholat tiba (*Pre-reminder*).
   - Banner notifikasi real-time di layar.
   - Pilihan getar dan lampu notifikasi.

3. **Pencatatan Lokal Riwayat Notifikasi & Ekspor WhatsApp** ⭐ *(Baru)*
   - Menyimpan seluruh riwayat alarm dan waktu sholat secara lokal ke **Room Database** (`prayer_notification_logs`).
   - Fitur **Ekspor ke WhatsApp**: Memformat riwayat waktu sholat menjadi pesan WhatsApp rapi dengan format teks tebal, miring, kutipan ayat, dan emoji Islami.
   - Pilihan ekspor perorangan atau laporan rekap harian lengkap.
   - Tombol **Salin Teks WhatsApp** untuk kemudahan berbagi ke aplikasi lain.

4. **Asisten AI Islami (Gemini 2.5 Flash)**
   - Konsultasi ibadah, fiqih sholat, doa harian, dzikir pagi/petang, dan jadwal puasa sunnah.
   - Riwayat percakapan tersimpan secara offline di Room Database.

5. **Pusat Notifikasi WhatsApp**
   - Membaca notifikasi masuk dari WhatsApp via Android `NotificationListenerService`.
   - Deteksi pesan prioritas dan pengingat penting terkait ibadah atau keluarga secara 100% aman dan lokal di perangkat pengguna.

6. **Kompas Arah Kiblat Real-time**
   - Kompas visual penunjuk arah Ka'bah (Makkah) berdasarkan koordinat bujur dan lintang perangkat.

---

## 📱 Unduh & Instal APK (Download Debug APK)

File APK siap diunduh dan dipasang pada perangkat Android:
- **Lokasi File APK:** `app/build/outputs/apk/debug/app-debug.apk`
- **Arsitektur:** Universal (`arm64-v8a`, `armeabi-v7a`, `x86_64`)
- **Minimum Android:** Android 7.0 (API Level 24) ke atas
- **Target Android:** Android 15 (API Level 36)

### Cara Mengunduh APK dari AI Studio:
1. Di panel file explorer AI Studio (sisi kiri), buka folder:
   `app` ➔ `build` ➔ `outputs` ➔ `apk` ➔ `debug`
2. Klik kanan pada file **`app-debug.apk`** lalu pilih **Download**.
3. Kirim file APK ke smartphone Android Anda dan buka untuk menginstal.

---

## 🚀 Panduan Publish ke GitHub & GitHub Releases

1. **Push Source Code ke GitHub:**
   - Di AI Studio, buka menu pengaturan (ikon gear / menu atas) dan pilih **Push to GitHub** atau ekspor project sebagai ZIP.
   - Jika menggunakan Git di terminal lokal:
     ```bash
     git init
     git add .
     git commit -m "feat: ZUCCHERO Waktu Sholat AI v1.0 with WhatsApp Export"
     git branch -M main
     git remote add origin https://github.com/USERNAME/zucchero-waktu-sholat-ai.git
     git push -u origin main
     ```

2. **Membuat GitHub Release & Upload APK:**
   - Buka repositori Anda di GitHub.
   - Di bilah kanan, klik **Releases** ➔ **Draft a new release**.
   - Masukkan Tag: `v1.0.0`
   - Beri judul: `ZUCCHERO Waktu Sholat AI v1.0.0`
   - Drag & drop file `app-debug.apk` ke area **Attach binaries by dropping them here**.
   - Klik **Publish release**. Sekarang pengguna di seluruh dunia dapat langsung mengunduh APK aplikasi ini!

---

## 🛠️ Tech Stack & Arsitektur
- **Language:** Kotlin 100%
- **UI Framework:** Jetpack Compose & Material 3 (M3)
- **Architecture:** MVVM + Clean Architecture + Repository Pattern
- **Local Persistence:** Room Database v2 (SQLite) + KSP
- **Networking:** Retrofit2 + Moshi
- **Sensors:** Google Play Services Location (FusedLocationProviderClient) & Hardware Sensor Compass
- **AI Engine:** Google Gemini API (Firebase AI / REST client)
