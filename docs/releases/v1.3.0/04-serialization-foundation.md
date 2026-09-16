# DueSoon v1.3.0 — STEP 9.4 Serialization Foundation

## Status
**PASS**

## Date / Time
2026-09-16 08:08 WIB

## Repository State
- **Branch**: main
- **Commit sebelum perubahan**: `af6ebe1` (v1.2.0 Tag)
- **Current HEAD**: (Uncommitted changes in working tree)
- **Working Tree Status**: Modified `libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`. Added domain backup package and test.

## Changes
- **Plugin**: Ditambahkan `org.jetbrains.kotlin.plugin.serialization` v1.9.22.
- **Dependency**: Ditambahkan `org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3`.
- **DTO**: Dibuat `PortableBackup` dan `PortableTask` di dalam package `com.duesoon.app.domain.backup`.
- **Json Configuration**: Dibuat `BackupSerializer` dengan opt-in `@OptIn(ExperimentalSerializationApi::class)`.
- **Mapper**: Dibuat `PortableTaskMapper` untuk menjembatani DTO dengan model `Task` domain.
- **Tests**: Ditambahkan `BackupSerializationTest` untuk memverifikasi fungsionalitas serialisasi murni (nullable fallbacks, enums, format structure).

## Portable Schema
- **schemaVersion**: 1
- **Fields**: Semua properti tersinkron dengan PRD.
- **Nullability**: `description`, `deadline`, `category`, `recurrenceInterval`, `snoozedUntil` bersifat nullable.
- **Enum Representation**: Direpresentasikan menggunakan `String` (melalui property `.name` dari enum pada domain dan `.valueOf()` saat deserialisasi), menghindari ordinal mapping untuk menjaga kestabilan data eksternal.

## JSON Configuration
- **`prettyPrint = true`**: Memastikan hasil backup berformat rapi untuk inspeksi manual.
- **`ignoreUnknownKeys = true`**: Mendukung kompatibilitas ke depan (forward compatibility).
- **`explicitNulls = false`**: (ExperimentalSerializationApi) Menghemat ukuran file JSON dengan menghilangkan entry yang bernilai `null`.
- **`encodeDefaults = true`**: Memastikan nilai default pada fields required (misalnya `completed: false`) tetap dituliskan secara eksplisit dalam JSON untuk mencegah ambiguitas skema.

## Separation of Concerns
- `PortableTask` sepenuhnya terisolasi dari `TaskEntity` maupun `Task`. Hal ini penting karena `TaskEntity` memuat meta-informasi terkait Room, sedangkan model `Task` domain terkait erat dengan runtime aplikasi. Memisahkan skema memastikan format backup aman berevolusi secara independen dari versi database Room (saat ini Room schema = 3).

## ID Semantics
- Serialisasi tidak bertanggung jawab atas konversi ID. DTO `PortableTask` tetap memiliki field `id: Long`. Keputusan akhir mengenai pembuangan ID (saat *Import*) atau pemertahanan ID (saat *Restore*) akan diimplementasikan pada orchestration layer (Validation & Repository execution).

## Tests
Command: `./gradlew testDebugUnitTest`
Hasil: **PASS**
Jumlah Test: 3 tes berjalan (`portable backup serialize to JSON and back to object successfully`, `ignore unknown fields keeps parsing intact`, `missing optional fields fall back to default nulls`).

## Build Verification
- `testDebugUnitTest`: **PASS**
- `assembleDebug`: **PASS**
- `assembleRelease`: **PASS**
- `assembleAndroidTest`: Tidak direkam dalam log sekuensial yang terakhir, namun compilation dependencies aman. (Runtime verification tidak dijalankan).

## Files Changed
- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `app/src/main/java/com/duesoon/app/domain/backup/PortableBackup.kt`
- `app/src/main/java/com/duesoon/app/domain/backup/PortableTask.kt`
- `app/src/main/java/com/duesoon/app/domain/backup/BackupSerializer.kt`
- `app/src/main/java/com/duesoon/app/domain/backup/PortableTaskMapper.kt`
- `app/src/test/java/com/duesoon/app/domain/backup/BackupSerializationTest.kt`

## Scope Check
Telah dipastikan STEP 9.4 **TIDAK** mengubah:
- Room schema version
- Migrasi Room database
- SAF flow atau intent creation
- Settings UI
- Restorasi transaksi
- Logika notifikasi

## Known Limitations
- Validasi bisnis (Business Validation) seperti dependensi logis antar kolom (misalnya *isRecurring* harus sejalan dengan *recurrenceInterval*) belum dilakukan.
- Validasi *schemaVersion == 1* belum difilter sebagai business rule.
- Pengecekan Duplikat ID belum dilakukan.
- Pemulihan file via SAF belum ada.
- Rekonsiliasi notifikasi setelah restore belum tersedia.

## Next Step
Direkomendasikan melangkah ke **STEP 9.5 — Portable Schema Serialization Logic & Validation**.
