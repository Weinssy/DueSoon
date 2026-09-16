# DueSoon v1.3.0 — Repository Hardening & Planning Audit

## 1. Audit Metadata
- **Date/Time:** 2026-09-16 07:46:00 WIB
- **Branch:** main
- **Current Commit:** 6f3d5dc (docs: record v1.2.0 release publication)
- **Current Version:** 1.2.0 (versionCode 3)
- **Current Tag Baseline:** v1.2.0

## 2. Roadmap Reviewed
Berdasarkan `docs/roadmap/duesoon-roadmap.md`, rilis selanjutnya (v1.3 series) berfokus pada:
- **v1.2.1:** Repository hygiene, Reliability, Notification testing, Migration testing.
- **v1.3.0:** Export, Import, Backup, Restore.

Tujuan utama v1.3.0 adalah memastikan keamanan dan portabilitas data pengguna (backup/restore). Sejalan dengan ini, roadmap v1.2.1 secara eksplisit menekankan hardening dan testing sebelum masuk ke implementasi backup/restore.

## 3. Repository Structure
Struktur repository secara umum dalam kondisi baik dan rapi.
- File architecture (`docs/releases/v1.2.0/`) terdokumentasi lengkap.
- Gradle convention dan KTS terstruktur dengan baik.
- *Tidak ditemukan file obsolete yang mengganggu build.*

## 4. Repository Hygiene
- **Tracked IDE files:** Ditemukan bahwa folder `.idea/` (termasuk `caches/deviceStreaming.xml`, `gradle.xml`, `vcs.xml`) **secara tidak sengaja ter-track oleh Git**. `.gitignore` sebenarnya sudah meng-ignore `.idea/` namun file-file ini masuk sebelum di-ignore atau di-force add.
- **Tindakan yang direkomendasikan:** Jangan menghapus file lokal, cukup untrack dari Git menggunakan `git rm -r --cached .idea` pada commit maintenance berikutnya.
- **Secrets/APK:** Bersih. Tidak ada keystore atau APK yang ter-track.

## 5. Architecture Audit
Architecture mematuhi `UI → ViewModel → Repository → Room`.
- **ViewModel** menangani UI state dengan baik (StateFlow).
- **Repository** bertindak sebagai single source of truth untuk Task operations dan alarm cancellation/scheduling.
- **Notification Engine** diabstraksi lewat `NotificationScheduler` interface, dengan implementasi `AndroidNotificationScheduler`.

**Temuan:**
- [INFO] Separasi logic tergolong kuat, namun seiring bertambahnya fitur Backup/Restore, mungkin diperlukan abstraksi spesifik (misalnya `BackupRepository` atau service khusus) agar tidak membebani `TaskRepository`.

## 6. Database Audit
- **Room version:** 3
- **Schema:** AppDatabase memiliki 2 migration (1→2, 2→3).
- **exportSchema:** Saat ini bernilai `false`.
- **Rekomendasi v1.3:** Mengingat v1.3.0 adalah rilis tentang **Backup & Restore**, kita **wajib** mengubah `exportSchema = true` agar schema JSON dapat di-track di Git. Ini penting untuk memastikan kompatibilitas skema saat melakukan import database dari versi lawas. Perubahan ini sebaiknya dilakukan pada maintenance v1.2.1.

## 7. Notification Audit
- `AndroidNotificationScheduler` menggunakan `task.id * 100 + 99` untuk isolasi snooze request code.
- `ReminderReceiver` memiliki mekanisme untuk mencegah "stale snooze alarm" dan "missing task".
- `BootReceiver` melakukan recovery alarm saat device reboot.
- **Status:** Sangat solid dan aman untuk dilanjutkan.

## 8. Testing Audit
- **Executed & PASS:**
  - `DateTimeUtilsTest.kt` (Unit)
  - `HomeFilterLogicTest.kt` (Unit)
- **Compiled & PENDING (butuh emulator):**
  - `AppDatabaseMigrationTest.kt` (AndroidTest)
  - `TaskRepositorySnoozeTest.kt` (AndroidTest)
- **Rekomendasi:** Perlu memperluas unit test untuk ViewModel atau DAO. Pada v1.3.0, proses import/export mutlak harus memiliki setidaknya JVM local test (membaca/menulis file JSON/CSV/DB dummy).

## 9. Edge Case Audit

| Edge Case | Current Behavior | Expected Behavior | Status |
| :--- | :--- | :--- | :--- |
| Task tanpa deadline | Tidak dijadwalkan reminder | Tidak ada reminder, masuk ke Home biasa | PASS |
| Deadline sudah lewat saat diedit | Tidak membuat alarm masa lalu | Sama | PASS |
| Task di-complete / di-delete | Alarm dibatalkan via Repository | Alarm dibatalkan | PASS |
| Snooze melewati deadline | Menunggu waktu snooze habis | Tetap trigger di waktu snooze | PASS |
| Reboot device | BootReceiver me-reschedule alarm | Recovery alarm aktif | PASS |
| Recurring task completed | Task baru (id=0) terbuat dengan deadline baru | Task baru terbuat & di-schedule alarm-nya | PASS |
| Stale snooze alarm | Diabaikan oleh receiver (cek db `snoozedUntil`) | Stale alarm di-drop | PASS |

## 10. Dependency Audit
Menggunakan `libs.versions.toml`:
- Compose BOM: 2024.02.01
- AGP: 8.3.0
- Room: 2.6.1
- Navigation: 2.7.7
- Glance: 1.0.0
**Rekomendasi:** Tidak ada mass upgrade yang mendesak. Dependency stabil untuk v1.3. Jika nanti membutuhkan JSON serialization untuk Export/Import, kita perlu menambahkan `kotlinx.serialization` (direkomendasikan) atau Gson.

## 11. README & Documentation Audit
- `README.md` sudah menunjuk ke v1.2.0 sebagai versi stabil, dengan highlight fitur yang tepat. Roadmap pada README telah disinkronisasi.
- Semua dokumentasi `docs/releases/v1.2.0/` terkonfirmasi lengkap dan valid.

## 12. Release Process Audit
Proses rilis v1.2.0 terbukti aman, konsisten, dengan release commit (`af6ebe1`) dan annotated tag `v1.2.0`. GitHub Release CLI tidak didukung environment lokal, tetapi fallback manual didokumentasikan. Tag ini immutable.

## 13. v1.3 Implementation Readiness
Berdasarkan Roadmap:

**Feature 1: Backup & Export (JSON/CSV atau SQLite copy)**
- **UI Impact:** Perlu penambahan menu di Settings (Export Data, Import Data).
- **Database Impact:** File system access, berisiko terhadap data corrupt jika proses import menimpa DB Room saat active.
- **Dependency Requirement:** `kotlinx.serialization` (jika JSON) atau library IO.
- **Risk:** [HIGH] Risiko data loss jika user salah meng-import file kosong/salah skema. Perlu confirmation dialog dan rollback mechanism.

**Pre-requisite (v1.2.1):**
- Aktifkan `exportSchema = true` di Room.
- Hapus `.idea/` dari git tracking.

## 14. Risk Register
- **[HIGH]** Data Corruption pada fitur Import v1.3.0 jika tidak ditangani hati-hati (butuh close/re-open DB / WAL mode handling).
- **[MEDIUM]** File `.idea` yang ter-track di Git mengotori repository.
- **[MEDIUM]** `exportSchema = false` mempersulit validasi skema saat fitur backup/import di masa depan.
- **[LOW]** Tidak adanya automated instrumented tests pipeline membuat deteksi regresi Room lambat.

## 15. Recommended Next Step
**Opsi 2: Lakukan Maintenance/Hardening step terlebih dahulu (v1.2.1).**
Repository belum 100% siap untuk v1.3 (Backup/Restore). Sebelum menulis kode untuk Export/Import, repository *harus* dibersihkan dari file `.idea`, dan konfigurasi Room *harus* disesuaikan (`exportSchema = true`) agar skema historis tersimpan dengan aman sebelum kita bermain dengan ekspor-impor data.

Oleh karena itu, step selanjutnya haruslah **STEP 9.1 - Repository Hygiene & Schema Hardening (v1.2.1)** sebelum memulai pengembangan v1.3.0.

## 16. Final Status
**PASS WITH NOTES**
