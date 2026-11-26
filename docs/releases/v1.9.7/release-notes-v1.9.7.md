# DueSoon v1.9.7 Release Notes

**Version:** `1.9.7` (`versionCode = 17`)  
**Previous Release:** `1.9.6` (`versionCode = 16`)

## Ringkasan Perubahan

### 1. Pomodoro Foreground Service & Notifikasi Persisten
- **Eksekusi Latar Belakang (`PomodoroTimerService` & `PomodoroTimerCoordinator`):** Memindahkan status aktif timer Pomodoro ke `PomodoroTimerCoordinator` di level aplikasi dan menambahkan `PomodoroTimerService` (`foregroundServiceType="specialUse"`).
- **Notifikasi Berjalan & Selesai:** Menampilkan notifikasi hitung mundur persisten lengkap dengan tombol *Jeda/Lanjut*, *Lewati*, dan *Reset*, membuka langsung tab **Fokus** via `MainActivity.ACTION_OPEN_FOCUS`, serta menjaga status timer di DataStore agar tahan pemulihan proses.

### 2. Dukungan Mata Kuliah Khusus (Autocomplete & Filter)
- **Input & Saran Mata Kuliah (`CategorySelector`):** Selain 4 preset cepat (`Kuliah`, `Kerja`, `Pribadi`, `Lainnya`), pengguna kini dapat mengetik nama mata kuliah bebas serta memilih saran mata kuliah yang pernah digunakan (`CreateTaskViewModel`, `EditTaskViewModel`).
- **Filter & Pencarian Beranda (`HomeScreen` & `HomeViewModel`):** Menambahkan bar filter *chip* per mata kuliah/kategori di Beranda dan memperluas pencarian teks ke judul, kategori/mata kuliah, dan deskripsi tugas.

### 3. Tema "Kertas & Tinta Kampus"
- Menambahkan `AccentPalette.CAMPUS_PAPER` (`Kertas & Tinta Kampus`) di `ThemePreferences.kt`, `Color.kt`, `Theme.kt`, dan `SettingsScreen.kt` dengan palet hangat dari `fokus-kuliah` (`#F6F2EA` kertas, `#FFFDF8` kartu, `#B8432E` tinta terakota, `#1F6E58` tinta sage) termasuk varian mode gelap yang konsisten.

### 4. Backup & Restore Riwayat Sesi Fokus (`focus_sessions`)
- Memperluas `PortableBackup` (`schemaVersion = 1`) dengan field opsional `focusSessions: List<PortableFocusSession>` (`PortableFocusSessionMapper`, `PortableBackupValidator`, `BackupRestoreCoordinator`) secara *backward/forward compatible*.
- Operasi `executeImport` (merge) dan `executeRestore` (replace) kini berjalan dalam satu transaksi Room (`database.withTransaction`) across `tasks` dan `focus_sessions`.

### 5. Perbaikan Pengujian, Pembersihan Dead Code, Dokumentasi & CI/CD
- **Perbaikan `BackupRestoreIntegrationTest.kt`:** Memperbaiki parameter `recurrenceRule` dan menambahkan verifikasi ekspor `focusSessions`.
- **Pembersihan Dead Code:** Menghapus `TasksScreen.kt` dan `TasksViewModel.kt` yang sudah tidak terpakai di navigasi.
- **Sinkronisasi Dokumentasi:** Memperbarui `README.md`, `AGENTS.md`, `docs/product/CURRENT-SPEC.md`, dan `docs/roadmap/duesoon-roadmap.md` ke kondisi rilis `v1.9.7`.
- **GitHub Actions CI/CD:** Menambahkan `.github/workflows/ci.yml` dan `.github/workflows/release.yml` untuk verifikasi build otomatis serta build & publikasi APK release bertanda tangan saat tag `v*` di-push.

## Verifikasi

- **Command:** `.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug --console=plain`
- **Build & Debug APK (`assembleDebug`):** LULUS (`BUILD SUCCESSFUL`).
- **Unit Test (`testDebugUnitTest`):** LULUS — **110/110** pengujian di 21 test suite (`0 failures`, `0 errors`, `0 skipped`).
- **Instrumented Test Compilation (`compileDebugAndroidTestKotlin`):** LULUS tanpa error.
- **Instrumented Test Runtime (`connectedDebugAndroidTest`):** Belum dijalankan (tidak ada perangkat fisik/emulator Android terpasang pada mesin ini).
