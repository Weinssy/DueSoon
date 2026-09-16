# DueSoon v1.3.0 — Repository Hygiene & Schema Hardening

## 1. Audit Baseline
- **Date/Time:** 2026-09-16 07:51:00 WIB
- **Branch:** main
- **Commit sebelum perubahan:** 83137dd (docs: update README for v1.2.0 release)
- **Version:** 1.2.0 (versionCode 3)
- **v1.2.0 tag status:** Immutable and present

## 2. .idea Git Hygiene
- **Kondisi sebelum:** Folder `.idea/` ter-track oleh Git walaupun seharusnya di-ignore.
- **Jumlah tracked files:** 13 file
- **Files/categories yang ditemukan:** `.gitignore`, `.name`, `AndroidProjectSystem.xml`, `caches/deviceStreaming.xml`, `codeStyles/Project.xml`, `codeStyles/codeStyleConfig.xml`, `compiler.xml`, `deploymentTargetSelector.xml`, `gradle.xml`, `migrations.xml`, `misc.xml`, `runConfigurations.xml`, `vcs.xml`.
- **Action yang dilakukan:** Menjalankan `git rm -r --cached .idea` untuk menghapus file dari Git index (untrack) tanpa menghapus file lokal di disk.
- **Verification:** `git ls-files .idea` mengembalikan list kosong (tidak ada file tracked).
- **Kondisi setelah:** File lokal `.idea/` tetap ada, namun tidak lagi di-track oleh Git.

## 3. .gitignore Verification
- **Apakah `.idea/` ignored:** Ya, terdapat entry `/.idea/` di dalam `.gitignore` root.
- **Apakah build/local files tetap ignored:** Ya, `/build`, `/app/build`, `.gradle`, `local.properties`, `*.jks`, `*.keystore` tetap ignored.
- **Findings:** Konfigurasi `.gitignore` sudah benar. Masalah sebelumnya hanya karena file di-add secara paksa atau di-add sebelum `.gitignore` disetup.

## 4. Room Schema Export
- **Kondisi sebelum:** `exportSchema = false` pada `AppDatabase.kt`. KSP argument untuk location belum dikonfigurasi.
- **Perubahan `exportSchema`:** Diubah menjadi `exportSchema = true` di `AppDatabase.kt`.
- **Room version:** 3
- **Database version:** 3 (Tidak berubah)
- **Schema output location:** `$projectDir/schemas` (ditambahkan via `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` di `app/build.gradle.kts`).

## 5. Schema Verification
- **Schema berhasil dibuat atau tidak:** Berhasil. File schema `3.json` terbentuk setelah menjalankan build.
- **Version:** 3 (Sesuai `formatVersion: 1`, `database.version: 3`).
- **Entity verification:** `TaskEntity` (tableName: `tasks`) terekspor dengan benar.
- **snoozedUntil verification:** Field `snoozedUntil` (tipe `INTEGER`, `notNull: false`) terverifikasi ada dalam file JSON schema.

## 6. Migration Safety
- **v1→v2:** Tetap terdefinisi di `MIGRATION_1_2`.
- **v2→v3:** Tetap terdefinisi di `MIGRATION_2_3`.
- **Tidak ada migration baru:** Dikonfirmasi.
- **Hasil verification:** Schema export dan KSP tidak mengubah behavior migration. Test `AppDatabaseMigrationTest` dapat dijalankan dengan aman.

## 7. Build & Test Results
Gunakan status aktual:
- **testDebugUnitTest:** EXECUTED + PASS
- **assembleDebug:** EXECUTED + PASS
- **assembleRelease:** EXECUTED + PASS
- **assembleAndroidTest:** EXECUTED + PASS

## 8. Repository Verification
- **`.idea`:** Tidak lagi tracked, dan tetap ignored. File lokal preserved.
- **Ignored files:** Berjalan normal.
- **Tracked artifacts:** Schema `3.json` siap untuk ditambahkan ke Git (untracked saat ini). Tidak ada APK/AAR/secrets yang ter-track.
- **Secrets:** Bersih.
- **Release tag:** Tag `v1.2.0` tidak tersentuh.
- **ApplicationId/VersionName/VersionCode:** Tidak berubah (`com.duesoon.app`, `1.2.0`, `3`).

## 9. v1.3 Backup/Restore Risk
Backup/Restore v1.3 harus memiliki strategi khusus untuk mencegah SQLite corruption ketika Room masih aktif.
Implementasi v1.3 nantinya harus mempertimbangkan:
- **Room lifecycle & open database handles:** Memastikan database ditutup (`close()`) atau di-checkpoint sebelum dicopy/diimport.
- **WAL (Write-Ahead Logging):** SQLite WAL mode menyimpan perubahan di `.wal` dan `.shm`. Import/Export file `.db` saja tidak cukup jika WAL belum di-checkpoint.
- **Transaction consistency:** Export harus dilakukan di luar write transaction yang sedang berjalan.
- **Safe export/import:** File validation sebelum menimpa file asli.
- **Database replacement:** Strategi mematikan sementara Room instance saat file di-replace.
- **Process/database restart:** Perlu UI force restart atau re-instantiation `AppDatabase`.
- **Confirmation dialog & Restore failure recovery:** Mencegah user tidak sengaja menghapus data saat ini.
- **Schema compatibility:** Memastikan backup file berasal dari schema version yang masih didukung oleh migration path aplikasi.

## 10. Files Changed
1. `app/build.gradle.kts` (menambah KSP argument)
2. `app/src/main/java/com/duesoon/app/data/local/AppDatabase.kt` (`exportSchema = true`)
3. `.idea/*` (untracked, terhapus dari git index)

## 11. Risks / Notes
- **[NOTE]** Jangan lupa commit `app/schemas/com.duesoon.app.data.local.AppDatabase/3.json` agar schema historis ini tersimpan selamanya.
- **[NOTE]** Karena tidak ada instrumented test yang dijalankan secara runtime (karena tidak ada device), pastikan tim QA mengetes migrasi jika environment memungkinkan kelak.

## 12. Final Status
**PASS**
