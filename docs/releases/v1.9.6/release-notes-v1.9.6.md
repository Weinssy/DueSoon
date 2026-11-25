# DueSoon v1.9.6 Release Notes

**Version:** `1.9.6` (`versionCode = 16`)  
**Previous Release:** `1.9.5` (`versionCode = 15`)

## Ringkasan Perubahan

### 1. Integrasi Native `fokus-kuliah` (Tab Fokus & Statistik Belajar)
- **Tab Fokus di Bottom Navigation:** Menambahkan tab `Fokus` sejajar dengan `Beranda`, `Kalender`, dan `Pengaturan` sehingga sesi Pomodoro dapat dijalankan langsung tanpa harus membuka detail tugas terlebih dahulu.
- **Siklus Pomodoro Penuh & Timer Berbasis Timestamp:** Mendukung mode `Fokus`, `Istirahat Singkat`, dan `Istirahat Panjang` otomatis setiap 4 sesi fokus, durasi kustom (disimpan di DataStore), auto-start sesi berikutnya, bunyi & getaran penanda, tombol lewati sesi, serta penghitungan mundur berbasis timestamp agar akurat.
- **Tautan Target Tugas:** Pengguna dapat menautkan, mengganti, atau melepas target tugas aktif langsung dari layar Fokus, melihat total sesi/menit per tugas, serta menyelesaikan tugas langsung dari sesi fokus.
- **Statistik Fokus Lokal (Room v5):** Menambahkan tabel `focus_sessions` melalui migrasi aman `MIGRATION_4_5` (`AppDatabase` v4 → v5) untuk menampilkan total menit & sesi mingguan (Senin–Minggu), streak harian beruntun, rata-rata 7 hari, grafik batang 7 hari terakhir, dan distribusi fokus per kategori/mata kuliah.
- **Impor Cadangan JSON `fokus-kuliah`:** Menambahkan menu *Impor dari Fokus Kuliah* di Pengaturan untuk menggabungkan tugas, riwayat fokus harian/mata kuliah, dan pengaturan Pomodoro dari berkas ekspor JSON web `fokus-kuliah` tanpa mengubah skema backup v1 DueSoon.

### 2. In-App Update (GitHub Releases)
- **Pemeriksaan Pembaruan di Pengaturan:** Menambahkan menu *Pembaruan Aplikasi* yang mengecek rilis terbaru dari `https://api.github.com/repos/Weinssy/DueSoon/releases/latest` dengan perbandingan semantic versioning (`SemanticVersion`).
- **Unduh & Pasang APK Langsung:** Menampilkan dialog catatan rilis (changelog), mengunduh aset `.apk` ke cache internal aplikasi dengan indikator progres, serta meluncurkan instalasi melalui `FileProvider` lengkap dengan pemeriksaan izin *Instal aplikasi yang tidak dikenal*.

### 3. Perapihan Kode
- Menghapus duplikasi `UserPreferencesState` yang tidak terpakai di `ThemePreferences.kt`.

## Verifikasi

- **Build & Kompilasi (`.\gradlew.bat testDebugUnitTest assembleDebug --console=plain`):** LULUS (`BUILD SUCCESSFUL`).
- **Unit Test (`testDebugUnitTest`):** LULUS — 103/103 pengujian di 19 test suite (`0 failures`, `0 errors`).
- **Instrumented Test Compilation (`compileDebugAndroidTestKotlin`):** Perubahan `AppDatabaseMigrationTest.kt` (termasuk `testMigration4To5`) bersih, namun terdapat 1 error bawaan pada file tak tersentuh `BackupRestoreIntegrationTest.kt:80` (`recurrenceInterval` sudah tidak ada di `Task`).
- **Instrumented Test Runtime:** Belum dijalankan pada emulator/perangkat fisik.
