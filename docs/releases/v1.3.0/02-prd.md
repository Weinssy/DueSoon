# DueSoon v1.3.0 — PRD Verification

## 1. Product Goal
Tujuan utama v1.3.0 adalah memastikan pengguna memiliki kendali penuh atas data mereka secara lokal melalui fitur Export, Import, Backup, dan Restore. Mengingat DueSoon adalah aplikasi "offline-first" tanpa akun, kemampuan mencadangkan dan memulihkan data secara manual sangat krusial.

## 2. Roadmap Alignment
Sesuai dengan `README.md`, v1.3.0 mencakup:
- Export / Import backup
*(Archive dan Statistics tercantum di roadmap v1.3, tetapi PRD ini difokuskan mendalam pada prioritas utama yaitu Backup/Restore).*
Fitur sinkronisasi cloud dan AI (v2.0) secara eksplisit berada di luar scope rilis ini.

## 3. Scope
**IN SCOPE:**
- Export data (membuat file backup portabel JSON).
- Import data (menggabungkan/menimpa sebagian task ke aplikasi).
- Backup data (snapshot lengkap database).
- Restore data (mengembalikan snapshot utuh dan menggantikan database saat ini).
- Backup validation (schema compatibility & data integrity check).
- Confirmation dialog sebelum tindakan destruktif (Restore).
- Error handling (Invalid format, corrupt data).
- Local-first behavior (System file picker).

**OUT OF SCOPE:**
- Auto-cloud sync (Google Drive, Dropbox).
- Sistem Akun (Login/Register).
- Server / Backend / Cross-device real-time sync.

## 4. Non-Goals
Membangun pencadangan otomatis (Auto-backup) atau sinkronisasi seamless multi-device BUKAN tujuan v1.3.0. Seluruh interaksi pencadangan dan pemulihan diinisiasi secara sadar dan eksplisit oleh pengguna.

## 5. Terminology
- **EXPORT**: Menyimpan data task saat ini ke file portabel (JSON) secara parsial atau menyeluruh.
- **IMPORT**: Membaca data portabel (JSON) dan **menggabungkannya (merge)** dengan data aplikasi yang sudah ada.
- **BACKUP**: Snapshot lengkap data yang setara dengan full export.
- **RESTORE**: Membaca file backup dan **menggantikan (replace)** seluruh data aplikasi saat ini.

## 6. Data Contract
Format portabel v1.3 akan menggunakan JSON:
```json
{
  "schemaVersion": 1,
  "appVersion": "1.3.0",
  "exportedAt": 1729012345678,
  "tasks": [
    {
      "id": 1,
      "title": "Buy groceries",
      "description": "Milk, Bread, Eggs",
      "deadline": 1729050000000,
      "category": "Personal",
      "priority": "HIGH",
      "reminderType": "SMART",
      "isRecurring": false,
      "recurrenceInterval": null,
      "completed": false,
      "snoozedUntil": null,
      "createdAt": 1729000000000,
      "updatedAt": 1729000000000
    }
  ]
}
```
**Evaluasi:**
- `id`: Opsional saat IMPORT (diabaikan), Wajib saat RESTORE (dipertahankan).
- `title`, `priority`, `reminderType`, `isRecurring`, `completed`, `createdAt`, `updatedAt`: Wajib.
- `description`, `deadline`, `category`, `recurrenceInterval`, `snoozedUntil`: Nullable / Optional.

## 7. ID Strategy
**A. IMPORT (MERGE)**
- **Generate new IDs:** Mengabaikan `id` dari JSON. Membiarkan SQLite / Room meng-generate `id` baru (`autoGenerate = true`). Mencegah bentrokan dengan data lokal (Duplicate ID collision).

**B. RESTORE (REPLACE)**
- **Preserve IDs:** Karena sifatnya menimpa, ID asli dari file backup **dipertahankan**. Ini menjaga integritas referensi dan histori alarm persis sama seperti saat data dibackup.

## 8. Import Semantics
- **Empty import:** Tolak (Error: "Empty file").
- **Invalid JSON / Invalid schemaVersion:** Batalkan proses (Error: "Invalid or unsupported backup format").
- **Missing required fields:** Tolak file / Abort transaksi.
- **Duplicate task content:** Tetap dimasukkan sebagai task baru (karena generate ID baru). Penghapusan duplikat diserahkan manual ke pengguna.
- **Partial corruption:** Dibatalkan seluruhnya secara transaksional (All or nothing).

## 9. Restore Semantics
- **REPLACE All:** Menghapus (`DELETE FROM tasks`) seluruh database saat ini, lalu menyisipkan (INSERT) seluruh data dari backup.
- **Confirmation:** Wajib menggunakan *destructive confirmation dialog*.
- **Alarms Lifecycle:**
  1. Batalkan semua alarm terjadwal yang ada saat ini.
  2. Eksekusi Restore Transaction.
  3. Jadwalkan ulang alarm hanya untuk task aktif yang direstore (belum lewat deadline, belum selesai).

## 10. Room/SQLite Safety Requirements
Berdasarkan investigasi STEP 9.1:
- Eksekusi Restore HARUS berjalan via SQLite `@Transaction` Delete-then-Insert di level DAO.
- DILARANG menimpa (replace) raw file `.db` melalui file system. Praktik ini rawan korupsi data karena bentrok dengan SQLite WAL (Write-Ahead Logging) dan in-memory cache dari Room instance yang masih terbuka.
- Transactional rollback akan menjamin jika ada kegagalan parse di tengah proses, data lama tidak hilang.

## 11. Notification Requirements
- **EXPORT:** Tidak menyentuh Notification Scheduler.
- **IMPORT:** Task baru yang di-merge akan diproses layaknya task baru, sehingga alarm akan otomatis di-schedule untuk task yang butuh reminder.
- **RESTORE:** `NotificationScheduler.cancelAllAlarms()` harus dipanggil sebelum Restore. `NotificationScheduler.scheduleAlarm()` di-loop setelah Restore sukses. SmartReminderCalculator tetap menjadi sumber referensi validitas reminder.

## 12. Version Compatibility
Database Room Schema Version (`v3`) **TIDAK SAMA** dengan Portable Schema Version (`v1`).
- File ekspor v1.3.0 akan dilabeli `schemaVersion: 1`.
- Jika file memiliki `schemaVersion` lebih tinggi dari yang didukung aplikasi, proses di-abort ("Please update DueSoon").
- Backward compatibility akan diterapkan apabila ke depan ada portable `schemaVersion: 2`.

## 13. Privacy & Security
- Data export hanya ditulis ke penyimpanan eksternal yang disetujui pengguna (via SAF / Android File Picker).
- Tanpa Enkripsi di v1.3 (data tersimpan utuh sebagai JSON murni untuk mempermudah backup portabel transparan).
- Jangan me-log / me-print isi *Task* di Logcat untuk menjaga privasi.

## 14. UX Requirements
- **Export Flow:** Settings -> Export Backup -> System File Picker (Create File) -> Toast "Export successful".
- **Import Flow:** Settings -> Import Data -> System File Picker -> Validasi -> Import -> Toast "Tasks imported".
- **Restore Flow:** Settings -> Restore Backup -> System File Picker -> Validasi -> **Warning Dialog (Destructive)** -> Restore Data -> Toast "Restore successful".

## 15. Error States
- **Success:** "Backup created successfully." / "Data restored successfully."
- **Invalid/Corrupt file:** "The selected file is not a valid DueSoon backup."
- **Unsupported version:** "This backup requires a newer version of DueSoon."
- **Permission denied:** "Storage access is required."

## 16. Acceptance Criteria
1. Pengguna dapat mengekspor data ke file JSON lokal via File Picker.
2. File ekspor memiliki `schemaVersion`.
3. Import (merge) tidak menimpa existing IDs.
4. Restore (replace) meminta konfirmasi destruktif, mempertahankan IDs lama, dan mengganti data yang ada.
5. Restore membatalkan alarm lama dan membuat ulang alarm baru.
6. File corrupt/invalid version ditolak dengan aman (transaksi dibatalkan, data lama tetap aman).
7. Room database tidak korup setelah Restore.
8. Tidak ada cloud dependency / login screen.
9. Data Task tidak terekspos di Application Logs.

## 17. Product Decisions
### Decided
- Gunakan format JSON untuk Backup/Export.
- Restore dijalankan secara transaksional di row-level (Delete all, Insert all), bukan manipulasi raw file `.db` untuk menghindari masalah WAL.
- Import membuang ID; Restore mempertahankan ID.

### Proposed
- Gunakan `kotlinx.serialization` alih-alih `Gson` untuk keamanan tipe data dan efisiensi memori Kotlin native.

### Open Questions
- Apakah fitur "Archive" v1.3 akan dikerjakan bersamaan dengan Backup/Restore, atau menyusul?

## 18. Implementation Order
1. Tambah library `kotlinx.serialization`.
2. Buat DTO models (`PortableTask`, `PortableBackup`).
3. Tulis serialization & validation service.
4. Update `TaskDao` dengan operasi `@Transaction` untuk Restore.
5. Update `NotificationScheduler` (cancelAll, scheduleAll).
6. Update `TaskRepository` mengintegrasikan I/O.
7. Bangun UI Backup & Restore di layar Settings.
8. Hubungkan UI dengan Storage Access Framework (SAF).
9. Tulis Automated Test (JSON deserialization, DAO transaction test).
10. Final manual QA.

## 19. Risks
- **[HIGH]** Bulk rescheduling alarm pada Restore bisa lambat jika jumlah task mencapai ribuan (limitasi main thread AlarmManager API).
- **[MEDIUM]** System File Picker (SAF) terkadang bermasalah pada versi Android/OEM custom tertentu (Xiaomi/Oppo). Butuh try-catch kuat di URI read/write operations.

## 20. Final Status
**PASS**
