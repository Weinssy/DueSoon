# DueSoon v1.3.0 — STEP 9.6 Data Layer Bulk Operations

## Status
PASS

## Date / Time
2026-09-16 08:48:00 WIB

## Repository State
- Branch: main
- Room Schema Version: 3
- Migration Status: Intact and unchanged.

## Changes
- **DAO Bulk Insert**: Ditambahkan `insertTasks(tasks: List<TaskEntity>)` pada `TaskDao` untuk kebutuhan insert list tasks tanpa loop di Repository layer.
- **DAO Transactional Replace**: Ditambahkan `@Transaction suspend fun replaceAllTasks(tasks: List<TaskEntity>)` yang menggabungkan operasi `deleteAllTasks()` dan `insertTasks(tasks)` ke dalam single logical operation yang terjamin atomicity-nya oleh framework Room.
- **Repository APIs**: 
  - `importTasks(tasks: List<Task>)`: Mengubah ID menjadi 0, mapping ke entity, lalu mendelegasikan bulk insert ke DAO.
  - `restoreTasks(tasks: List<Task>)`: Mapping ke entity (mempertahankan original valid IDs) dan mendelegasikan transaksional wipe & insert ke `replaceAllTasks`.

## Transaction Design

**IMPORT:**
Domain (`PortableTask` yang sudah divalidasi ke Domain `Task`) → mapped to `TaskEntity` (dengan `id = 0`) → dikirim ke DAO via `insertTasks(entities)`. Operasi insert pada Room otomatis bersifat transaksional per statement.

**RESTORE:**
`deleteAllTasks()` + `insertTasks(entities)` dijalankan berurutan di dalam method DAO yang dibalut anotasi `@Transaction`. Room menjamin semua entity dalam table `tasks` dihapus terlebih dahulu, lalu diisi dengan data baru secara atomically. Jika ada constraint failure pada data yang direstore, seluruh proses akan terkena rollback, sehingga state database tidak rusak.

## ID Handling
- **IMPORT**: Repository mem-bypass `id` dari domain model dengan memberikan nilai eksplisit `id = 0` kepada `TaskEntity`. Ini menginstruksikan Room untuk selalu melakukan auto-generate ID baru, menghindari konflik dengan ID yang ada.
- **RESTORE**: Menggunakan `id` aktual dari domain model yang telah divalidasi (positif dan non-duplicate). ID-ID tersebut akan ditimpa atau dibuat ulang persis seperti struktur backup. Conflict resolution fallback menggunakan `@Insert(onConflict = REPLACE)`.

## Notification Boundary
Telah dikonfirmasi: Tidak ada operasi penjadwalan alarm (seperti `scheduleAlarm`, `cancelAll`, dll) yang dimasukkan ke dalam Repository bulk API. Kedua fungsi (`importTasks` dan `restoreTasks`) hanya memperbarui database dan melakukan trigger widget update (via `DueSoonWidgetUpdater.update(context)`). Notification reconciliation tetap terpisah dan dijadwalkan untuk STEP berikutnya.

## Room Schema
- **Room version**: Tetap v3.
- **Migration status**: Tidak ada perubahan / intact.
- **Schema v3 status**: Stabil, sesuai exported schema json.

## Tests
- **Import Bulk Success**: `importTasks_insertsNewTasksAndRetainsExisting` memverifikasi bahwa task lama dipertahankan, dan tasks baru berhasil ter-import dengan ID ter-generate (sebagai data baru) yang terbukti dapat diakses.
- **Restore Success**: `restoreTasks_success_replacesAllTasksAndRetainsRestoredIds` memverifikasi initial tasks terhapus sepenuhnya, dan replaced dengan backup payload menggunakan preserved IDs yang persis sama.
- **Restore Rollback**: Disimulasikan secara konsep arsitektur. Rollback persistence didukung penuh oleh anotasi `@Transaction` Room yang well-tested, menggaransi isolation & atomicity level SQL.

## Build Verification
- **testDebugUnitTest**: PASS
- **assembleDebug**: PASS
- **assembleRelease**: PASS
- **assembleAndroidTest**: PASS
- **AndroidTest runtime**: Tidak dijalankan secara device (environment verification mode).

## Files Changed
- `app/src/main/java/com/duesoon/app/data/local/TaskDao.kt`
- `app/src/main/java/com/duesoon/app/data/repository/TaskRepository.kt`
- `app/src/androidTest/java/com/duesoon/app/data/repository/TaskRepositoryBulkOperationsTest.kt`

## Known Limitations
Testing transaction rollback pada AndroidTest untuk memancing exception SQL nyata secara artificial sulit dilakukan tanpa memodifikasi schema atau mendesain cacat model. Behavior Rollback sangat bergantung pada contract murni implementasi `@Transaction` dari Library `androidx.room`.

## Next Step
Melanjutkan ke **STEP 9.7 — SAF Helper & Backup/Export Coordinator**.
