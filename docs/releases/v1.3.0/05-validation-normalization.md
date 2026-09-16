# STEP 9.5 — Portable Schema Validation & Normalization

## Status
**PASS**

## Summary
Membuat validation & normalization layer murni (side-effect-free) untuk PortableBackup dan PortableTask menggunakan `PortableBackupValidator` pada package `com.duesoon.app.domain.backup`. Layer ini bertugas memvalidasi integrity backup JSON sebelum masuk ke Data Layer, mencegah korupsi atau state yang tidak konsisten pada Room Database (sejalan dengan rule: *"Restore tidak boleh membuat database corrupt"*).

## Scope
1. Implementasi class `BackupValidationError` sebagai representasi sealed type error.
2. Implementasi class `ValidatedPortableBackup` yang menampung task valid yang sudah dinormalisasi dan di-map ke model domain.
3. Implementasi `PortableBackupValidator.validate` dengan support mode `IMPORT` dan `RESTORE`.
4. Unit testing komprehensif pada `PortableBackupValidatorTest`.

## Validation Rules Implemented
- **Schema Validation:** Menolak backup jika `schemaVersion` tidak disupport (saat ini wajib `1`). Error: `UnsupportedSchema`.
- **Empty Check:** Menolak empty array `tasks` (EmptyBackup) baik untuk IMPORT maupun RESTORE untuk mencegah destructive restore yang tidak disengaja. (Sesuai panduan *OPEN DECISION* untuk safety).
- **Field & Formatting:** Menolak field blank (seperti `title` empty/blank) dan melakukan normalisasi (`title.trim()`).
- **Enum Validation:** Memastikan tipe enum valid (`priority`, `reminderType`, `recurrenceInterval`).
- **Timestamp Logic:** Memastikan tidak ada timestamp negatif dan `updatedAt` tidak boleh lebih tua dari `createdAt`.
- **Recurring Logic:** Jika `isRecurring == true`, maka `recurrenceInterval` tidak boleh null. Jika `false`, harus null.
- **Completed Logic:** `completed` state diijinkan independen dari deadline (bisa true meskipun deadline null/past).
- **ID Strategy (Mode-Specific):** 
  - **IMPORT:** Mengijinkan ID bernilai 0 dan duplikat (sebab akan di-generate/auto-increment oleh database).
  - **RESTORE:** Mewajibkan ID harus unik di dalam list JSON, dan harus positif (>0). Error: `DuplicateId` & `InvalidId`.

## Next Step
Melanjutkan ke **STEP 9.6 — Data Layer Bulk Operations** untuk mengimplementasikan transaksi SQLite DAO yang aman (`insertTaskBatch`, `clearAndInsertTasksBatch`) tanpa UI logic.
