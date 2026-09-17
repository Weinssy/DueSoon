# Release v1.3.0 - Data Portability & Settings

Welcome to **DueSoon v1.3.0**! This major release introduces full offline data portability, allowing you to seamlessly back up, export, and restore your tasks securely using the Android Storage Access Framework.

## 🚀 What's New
- **Backup & Export (JSON):** Export all your tasks, deadlines, and snooze states into a safe, human-readable `.json` portable format.
- **Import Tasks:** Safely append tasks from a backup file into your existing list without overwriting your current data. Duplicate or conflicting external IDs are safely re-assigned.
- **Restore (Destructive):** Revert your DueSoon database entirely from a backup file. All tasks, IDs, and states will exactly mirror the snapshot at the time of export.
- **Settings Screen:** A brand new settings page provides easy access to the Export, Import, and Restore functionalities.

## 🔒 Security & Data Integrity
- **100% Offline & Local-First:** All backups are saved locally to your Android device using SAF. Zero cloud dependency.
- **Notification Reconciliation:** Upon restoring a backup, DueSoon intelligently re-syncs all Android system alarms. Overdue alarms are suppressed, future deadlines are safely re-scheduled, and expired snooze states are silently discarded to prevent notification spam.
- **Strict Validation:** A rigorous parser and validator ensure corrupted, incomplete, or incompatible backups are blocked instantly before any changes happen to your database.

## 📦 Asset Verification
To ensure the integrity of the APK, verify it against the SHA-256 hash below:
* **APK Name:** `DueSoon-v1.3.0-release.apk`
* **SHA-256:** `C91FE2B71CDFC3972B7DA45936AC199B81631B9B3C3F7002FBDB0DEFE5B2E075`

---
*No new dependencies were added in this release. Please report any bugs via the issue tracker.*
