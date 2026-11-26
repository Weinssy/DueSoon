# DueSoon

> Tahu apa yang perlu menjadi perhatian Anda berikutnya.

DueSoon adalah aplikasi pengingat deadline Android dan pendamping fokus belajar yang minimal, **local-first**, dan tenang: membantu Anda memprioritaskan tenggat terdekat sekaligus menjalani sesi Pomodoro terarah langsung di satu tempat.

Semua data tugas dan riwayat sesi fokus disimpan secara lokal di perangkat (Room SQLite + DataStore). Tidak ada akun, tidak ada pelacakan, dan koneksi internet hanya digunakan *on-demand* saat Anda memeriksa pembaruan aplikasi di Pengaturan.

## Prinsip Produk

> "Deadline harus jelas. Yang lain harus tetap tenang."

## Fitur Utama

### Manajemen Tugas & Pengingat
- Membuat, mengedit, menandai selesai, dan menghapus tugas
- Deadline opsional dengan status urgensi otomatis (*Terlambat*, *Hari Ini*, *Mendekati Deadline*)
- Kategori preset (`Kuliah`, `Kerja`, `Pribadi`, `Lainnya`) serta dukungan **nama mata kuliah kustom** dengan saran otomatis dan filter di Beranda
- Smart Reminder multi-tahap, Alarm layar penuh opsional, Snooze (`10 mnt`, `1 jam`, `Besok`), serta tugas berulang (*Harian*, *Mingguan*, *Bulanan*)
- Tampilan Beranda (pencarian judul/mata kuliah + filter), Kalender dengan indikator kepadatan tugas, dan Widget Jetpack Glance di layar utama

### Tab Fokus & Statistik Belajar (Integrasi Fokus Kuliah)
- **Timer Pomodoro Siklus Penuh:** Mode *Fokus*, *Istirahat Singkat*, dan *Istirahat Panjang* otomatis setiap 4 sesi fokus
- **Foreground Service & Timer Berbasis Timestamp:** Tetap berjalan akurat di latar belakang dengan notifikasi persisten (kontrol *Jeda/Lanjut*, *Lewati*, *Reset*) dan notifikasi selesai saat layar terkunci atau app di background
- **Tautan Target Tugas:** Pilih, ganti, atau selesaikan tugas langsung dari tab Fokus
- **Statistik Fokus Lokal:** Total menit & sesi minggu ini, *streak* harian beruntun, rata-rata 7 hari, grafik batang 7 hari terakhir, dan distribusi fokus per mata kuliah/kategori
- **Tema "Kertas & Tinta Kampus":** Palet warna hangat (kertas `#F6F2EA`, tinta terakota `#B8432E`, sage `#1F6E58`) di Pengaturan Tampilan

### Keamanan Data & Pembaruan Aplikasi
- **Backup & Restore Portabel (`schemaVersion = 1`):** Ekspor, Impor (merge), dan Pulihkan (replace atomik via transaksi Room) mencakup tugas dan riwayat `focus_sessions` melalui Android Storage Access Framework (SAF)
- **Impor Cadangan Fokus Kuliah:** Mengimpor berkas ekspor JSON dari web [fokus-kuliah](https://github.com/Weinssy/fokus-kuliah) (tugas, riwayat sesi, dan pengaturan Pomodoro) langsung ke DueSoon
- **In-App Update (GitHub Releases):** Cek pembaruan, lihat catatan rilis, unduh APK dengan indikator progres, dan pasang langsung melalui `FileProvider` dari menu **Pengaturan → Pembaruan Aplikasi**

## Stack Teknologi

- Kotlin, Jetpack Compose, Material 3
- Room Database (Schema v5, migrasi `1→2→3→4→5`)
- Coroutines, Flow / StateFlow, Navigation Compose
- Jetpack DataStore Preferences
- Jetpack Glance AppWidget
- Android Notification APIs, AlarmManager & Foreground Service (`specialUse`)
- Kotlinx Serialization & OkHttp (untuk pemeriksaan/unduhan rilis GitHub)
- Gradle Kotlin DSL + GitHub Actions CI/CD

## Arsitektur

```text
UI (Compose) → ViewModel → Repository / Coordinator → Room & DataStore
```

- `PomodoroTimerCoordinator` + `PomodoroTimerService` menjaga timer tetap sinkron antara UI dan latar belakang.
- `BackupRestoreCoordinator` & `FokusKuliahImportCoordinator` menjaga validasi terpisah dari lapisan penyimpanan Room dan rekonsiliasi notifikasi.

## Status Versi

- **Versi Kode Saat Ini:** `v1.9.7` (`versionCode = 17`)
- **Rilis Terpublikasi Terakhir:** `v1.9.6` ([GitHub Releases](https://github.com/Weinssy/DueSoon/releases))

## Cara Menjalankan

1. Clone repositori ini.
2. Buka project di Android Studio (JDK 17, Android SDK 34).
3. Jalankan unit test atau build debug APK:
   ```bash
   ./gradlew testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug
   ```
4. Untuk build release bertanda tangan, salin `keystore.properties.example` menjadi `keystore.properties` lalu isi kredensial keystore Anda.

## Lisensi

Proyek ini dilisensikan di bawah [MIT License](LICENSE).
