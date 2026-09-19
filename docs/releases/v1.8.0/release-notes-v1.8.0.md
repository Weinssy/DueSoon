# DueSoon v1.8.0 — Search & Archive Hardening

## Overview
DueSoon v1.8.0 introduces a hardened, memory-efficient separation between your active tasks and completed history. By siloing completed tasks into a dedicated Archive, your primary Home screen now remains infinitely fast and responsive, no matter how many tasks you check off over time. We've also upgraded the Search experience to be instant and seamless across the entire app.

## User-Facing Changes
- **New Archive Screen:** Completed tasks no longer clutter your main feed. They are now safely stored in a dedicated Archive screen, accessible via the new Archive icon in the top right corner.
- **Lightning-Fast Search:** We've rebuilt the search engine. Typing in the search bar now instantly filters your tasks with zero lag or stutter, both in your active feed and within the Archive.
- **Bulk Cleanup:** You can now permanently delete all completed tasks at once from the Archive screen with a new "Clear Archive" button (protected by a confirmation dialog so you don't accidentally erase history).
- **Safe Restoration:** Restoring a task from the Archive safely reactivates its deadline, instantly brings it back to your Home feed, and intelligently reschedules any future reminders while ignoring past ones.

## Technical Release Notes
- **Memory Optimization (Data Stream Split):** The `HomeViewModel` has been refactored to consume `observeActiveTasks()` exclusively, ensuring that `completed = 1` items never enter the active `StateFlow` memory footprint.
- **Debounced Reactive Search:** Search inputs are now backed by a `StateFlow` pipeline with a `debounce(300L)` operator mapped to `searchArchivedTasks()` and active task filtering, preventing rapid, expensive SQLite queries on the Main thread.
- **Zero Migrations Maintained:** The SQLite database remains strictly on schema version 3 (`3.json`). All filtering is achieved natively via robust DAO queries.
- **JSON Portability Unchanged:** `PortableBackup` schemas remain 100% interoperable.
- **Idempotent Side-Effects:** Restoring a task delegates to `RestoreTaskUseCase`, which guarantees that historical alarms are pruned, active alarms are multiplexed safely via `AndroidNotificationScheduler`, and the Jetpack Glance Widget is refreshed synchronously.
