# DueSoon v1.3.0 — Architecture & Data Contract

## 1. Architecture Baseline
Arsitektur DueSoon saat ini mengikuti pola standar Android:
- **UI:** Layar (misal SettingsScreen) memanggil ViewModel.
- **ViewModel:** Menerima intent pengguna dan memodifikasi UI state, berinteraksi dengan Repository.
- **Repository:** `TaskRepository` bertindak sebagai single source of truth untuk I/O (Database & Scheduling).
- **Data:** `AppDatabase`, `TaskDao`, dan `TaskEntity` menangani persistence lokal dengan Room versi 3.
- **Notification:** `NotificationScheduler` (interface) diimplementasikan oleh `AndroidNotificationScheduler` untuk mendaftarkan intent ke `AlarmManager`. `ReminderReceiver` merespons intent.

## 2. Target Architecture
Target arsitektur v1.3.0 tetap **local-first** dan sejalan dengan pola yang sudah ada (UI → ViewModel → Repository → Room). Penambahan difokuskan pada sebuah service/coordinator untuk mengatur Backup & Restore agar logic IO JSON dan validasi tidak mencemari `TaskRepository`.

**Komponen Baru:**
- **BackupRestoreCoordinator/Manager:** Menyatukan akses SAF, Validasi, Serialization, dan berinteraksi dengan `TaskRepository` untuk operasi database.

## 3. Module Responsibilities
- **Domain:** Berisi DTO portabel (e.g., `PortableTask`, `PortableBackup`) dan aturan validasi independen dari entitas Room.
- **Data (Repository):** `TaskRepository` ditambah operasi curah (bulk) terspesialisasi (seperti `replaceAllTasks` dalam `@Transaction`).
- **Serialization:** `kotlinx.serialization` encoder/decoder, validasi skema.
- **Storage:** Komponen pembungkus (wrapper) untuk Storage Access Framework (SAF) yang menangani pembacaan/penulisan InputStream/OutputStream secara aman.
- **Notification:** Fungsionalitas tambahan untuk membatalkan semua alarm dan menjadwalkan ulang alarm setelah restore data selesai.
- **UI/ViewModel:** Layar `Settings` dengan UI state untuk proses (loading, error, success) serta pemicu file picker.

## 4. Serialization Architecture
**Diusulkan (Proposed):** `kotlinx.serialization`
- **Alasan:** Native Kotlin, tipe-aman (type-safe), sangat ringan, dan mudah diintegrasikan dengan proyek berbasis Compose/Kotlin modern.
- **Penerapan:** Akan menggunakan plugin `org.jetbrains.kotlin.plugin.serialization`.
- DTO model dipisahkan murni dari entitas database (`TaskEntity`) maupun model internal (`Task`), agar format eksternal dapat berevolusi secara independen.

## 5. Portable Schema Contract
Skema data eksternal akan direpresentasikan oleh:

**PortableBackup:**
- `schemaVersion` (Int) - **Portable schema version: 1 (Berbeda dari Room DB version 3)**
- `exportedAt` (Long)
- `appVersion` (String)
- `tasks` (List<PortableTask>)

**PortableTask:**
- `id` (Long)
- `title` (String)
- `description` (String?)
- `deadline` (Long?)
- `category` (String?)
- `priority` (String)
- `reminderType` (String)
- `isRecurring` (Boolean)
- `recurrenceInterval` (String?)
- `completed` (Boolean)
- `snoozedUntil` (Long?)
- `createdAt` (Long)
- `updatedAt` (Long)

## 6. Validation Pipeline
Validasi *HARUS* selesai seluruhnya sebelum aksi pemulihan (restore) destruktif dilakukan.
1. **Raw file** (URI)
2. **Read bytes/text** via SAF InputStream
3. **Decode JSON** via kotlinx.serialization
4. **Validate schemaVersion:** Pastikan `schemaVersion <= 1`.
5. **Validate structure & fields:** Pastikan enum priority, reminderType, timestamps valid.
6. **Normalize:** Memastikan `recurrenceInterval` jika `isRecurring` benar, dll.
7. **Produce validated model** (List<Task>)
8. **Import/Restore execution**

**Rules:** Invalid JSON, Missing Required Fields, Invalid Enum, Unsupported Version -> **Batalkan keseluruhan**.

## 7. Import Architecture
**Keputusan PRD: IMPORT = MERGE, generate new IDs**
- **Flow:**
  File → SAF Validate → Convert ke `PortableTask` → Buang `id` dari backup (set ke 0) → Petakan (Map) ke `Task` → Panggil `TaskRepository.insertTask()` → Room menghasilkan ID baru → Jadwalkan notifikasi jika aktif.
- **Partial Failure:** Jika satu baris gagal validasi di pipeline (sebelum insert), seluruh operasi dibatalkan. Jika sukses validasi, akan di-insert.
- Task diselesaikan dan overdue akan mempertahankan statusnya tanpa memicu notifikasi baru.

## 8. Restore Architecture
**Keputusan PRD: RESTORE = REPLACE, preserve IDs**
- **Flow:**
  File → Read → Decode → Validate → Normalize → **USER CONFIRMATION** → `NotificationScheduler.cancelAllAlarms()` → DB `@Transaction` (Delete ALL existing, Insert ALL restored) → Commit → Reconcile notifications (loop scheduleAlarm).
- **CRITICAL:** Jika DB transaction gagal di tengah jalan, Room me-rollback database. Current data aman, tapi alarm sudah terlanjur di-cancel. Oleh karena itu, perlu *recovery strategy*: Jika DB rollback, alarm dari data lama harus di-schedule ulang (reconcile dari DB lokal lama).

## 9. DAO Transaction Design
Akan ditambahkan DAO function baru:
```kotlin
@Transaction
suspend fun replaceAllTasks(tasks: List<TaskEntity>) {
    deleteAllTasks() // fungsi internal DAO
    insertTasks(tasks) // bulk insert (OnConflictStrategy.REPLACE)
}
```
Delete + insert berada pada transaction yang sama. Rollback dijamin oleh Room jika terjadi kegagalan saat insert.

## 10. ID Strategy
- **IMPORT:** `PortableTask.id` diabaikan, entitas di-insert dengan ID = 0 agar Room men-generate ID baru.
- **RESTORE:** `PortableTask.id` dipertahankan dan di-map langsung ke `TaskEntity.id`. Validasi memastikan tidak ada duplicate ID dalam list restored data (meskipun JSON parser biasanya otomatis menangani, validasi eksplisit penting).

## 11. Notification Reconciliation
Proses penjadwalan ulang:
- **IMPORT:** Hanya memanggil `scheduler.scheduleAlarm(task)` pada task yang baru masuk.
- **RESTORE:** Harus memanggil `cancelAllAlarms()`. Setelah `@Transaction` selesai sukses, ambil seluruh task aktif dari database, lalu loop dan panggil `scheduleAlarm(task)`.
- **Behavior:**
  - Completed / Overdue: Tidak ada alarm.
  - Notification disabled: Tidak di-schedule.
  - SnoozedUntil (future): Schedule di waktu snooze.
  - SnoozedUntil (past): Anggap sudah terlewat, jangan schedule (atau jadikan immediate jika berisiko terlewat).

## 12. SAF Architecture
Penggunaan **Storage Access Framework (SAF)**:
- **Export (BACKUP):** `Intent.ACTION_CREATE_DOCUMENT`, MIME `application/json`, suggested name `DueSoon_Backup_yyyyMMdd.json`.
- **Import/Restore:** `Intent.ACTION_OPEN_DOCUMENT`, MIME `application/json`.
- **Stream Lifecycle:** Block `use { }` akan digunakan untuk otomatis menutup `InputStream/OutputStream`.
- Cancellation oleh user (menutup file picker) ditangani via `Activity.RESULT_CANCELED` (bukan error).

## 13. Export vs Backup
**Rekomendasi Arsitektur (Proposed):**
Export dan Backup menggunakan format JSON (`PortableBackup`) **YANG SAMA**, karena membedakan dua skema untuk struktur data sederhana ini membebani pemeliharaan (maintenance) tanpa manfaat nyata (Overhead tinggi). Bedanya murni pada *Intensi UX*:
- Export -> UX: Mengambil data.
- Import -> UX: Menggabung data.
- Backup -> UX: Snapshot.
- Restore -> UX: Timpa semua data.

## 14. Restore Safety
- **DILARANG:** Melakukan overwrite/copy langsung pada file mentah `.db` / `.db-wal` / `.db-shm`.
- **Wajib:** Menggunakan DAO `@Transaction` seperti didefinisikan di Phase 9.
- Data yang sudah divalidasi akan dimuat utuh (in-memory list) sebelum transaksi dieksekusi, memastikan tidak ada I/O exception dari SAF saat database sedang terbuka untuk write.

## 15. UI/ViewModel State
State pada `SettingsViewModel` (Sealed Interface):
- `Idle`
- `Processing(operation: OperationType)`
- `ConfirmRestore(uri: Uri)`
- `Success(message: String)`
- `Error(type: BackupError)`

UI tidak mengizinkan aksi dobel. Tombol Backup/Restore akan dinonaktifkan (disabled) saat `Processing`.

## 16. Error & Recovery Model
Model Error (Sealed Class):
- `BackupError.StorageError` (SAF / file permission)
- `BackupError.InvalidFormat` (JSON parse failed)
- `BackupError.UnsupportedSchema` (Schema version > 1)
- `BackupError.DatabaseError` (Transaction failed)
- `BackupError.Cancelled` (Bukan error fatal, UI ignore)

## 17. Testing Architecture
- **Unit Test:** Serialize/Deserialize `PortableBackup`, Validasi rules, penanganan missing/nullable fields (terutama default nulls), duplicate ID stripping.
- **Instrumented Test:** DAO transaction (memastikan data hilang & terisi baru).
- **Integration (Manual/Test):** SAF file picker mocking (di ViewModel/UI test), end-to-end import/restore dan pemeriksaan status `NotificationScheduler`.

## 18. Security & Privacy
- **Privacy:** Tugas (Tasks) murni di JSON lokal. Tidak ada SDK jaringan, tidak ada upload, tidak ada logcat yang berisi judul task.
- **Encryption:** (Out of Scope / Non-goal) Sesuai PRD.

## 19. Architecture Diagram
```text
[ Settings UI ]
      | (Intent / SAF Result)
      v
[ SettingsViewModel ] <--- State ---
      |
      | (Delegates complex flow)
      v
[ BackupRestoreCoordinator ]
      |
      +---> [ SAF Handler ] (Read/Write JSON via URI)
      |
      +---> [ Serializer & Validator ] (kotlinx.serialization)
      |
      +---> [ TaskRepository ]
      |          |
      |          v
      |     [ TaskDao ] (@Transaction delete + insert)
      |          |
      |          v
      |     [ Room Database (SQLite) ]
      |
      +---> [ NotificationScheduler ] (CancelAll / Schedule)
```

## 20. Implementation Sequence
**Proposed Implementation Sequence:**
- **STEP 9.4:** Serialization foundation (`kotlinx.serialization` dependency, DTO models `PortableTask`, `PortableBackup`).
- **STEP 9.5:** Serialization logic & Validation layer.
- **STEP 9.6:** Data layer implementation (`TaskDao.replaceAllTasks()`, Repository updates).
- **STEP 9.7:** SAF Helper & Coordinator (Export & Backup flows).
- **STEP 9.8:** Import & Restore logic (Merge vs IDs Replace, Transaction integration).
- **STEP 9.9:** Notification Reconciliation (CancelAll & Schedule restored tasks).
- **STEP 9.10:** Settings UI integration.
- **STEP 9.11:** Testing & Hardening.
- **STEP 9.12:** Release verification v1.3.0.

## 21. Risks
- **[HIGH]** Room Transaction bulk insert memory spike jika JSON memuat puluhan ribu task (sangat jarang terjadi pada aplikasi to-do, namun in-memory JSON parse memakan RAM).
- **[HIGH]** Inkonsistensi Alarm jika DB gagal (rollback) tetapi Alarm sudah terlanjur dibatalkan (di-cancel) sebelum DB diganti. Coordinator *wajib* menangkap exception dan melakukan re-schedule alarm dari sisa data yang selamat di DB jika restore gagal.

## 22. Open Questions
- Apakah file JSON perlu disimpan dalam arsip `.zip`, atau cukup teks murni `.json`? (Diusulkan JSON murni untuk kesederhanaan).

## 23. Final Status
**PASS**
