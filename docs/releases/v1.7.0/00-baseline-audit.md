# DueSoon v1.7.0: Baseline Audit (Recurring & Reminder Engine 2.0)

## 1. Repository & Release State
- **Git Status:** Working tree is completely clean and perfectly aligned with the `v1.6.0` tag.
- **Version Metadata:** `app/build.gradle.kts` reflects `versionCode = 7` and `versionName = "1.6.0"`.
- **Database Schema:** The Room database schema is currently on `3.json`. No uncommitted migrations exist.

## 2. Current Recurring Infrastructure
- **Schema Representation:** Inside `TaskEntity`, `recurrenceInterval` is persisted as a nullable `String`. Currently, this strictly maps to the `RecurrenceInterval` enum (`DAILY`, `WEEKLY`, `MONTHLY`) via `valueOf()`.
- **Next Date Calculation:** `TaskRepositoryImpl.calculateNextDeadline` calculates the next instance strictly relative to the original `deadline` (using `Calendar.getInstance()`), not the completion time.
- **Time Preservation:** Because it uses `Calendar.add()`, the exact time of day (hours and minutes) is natively preserved when shifting weeks or months.

## 3. Current Reminder & Alarm Scheduling Engine
- **Trigger Calculation:** `SmartReminderCalculator` receives a `Task` and generates a flat `List<Long>` of future epoch timestamps based on hardcoded Smart rules.
- **Alarm Constraints:** `AndroidNotificationScheduler.schedule` iterates over this list and generates independent exact alarms using a unique request code: `(task.id * 100 + index).toInt()`. 
- **Capacity:** Because of this indexing strategy, DueSoon's infrastructure already natively supports up to 10 unique alarms per task without any architectural changes or DB additions.

## 4. Database & Schema Migration Risk Matrix
- **Custom Recurring Rules (e.g., specific weekdays, every X days):** 
  - **Risk:** Zero Room migration required. 
  - **Strategy:** Because `recurrenceInterval` is already a `String` column in SQLite, we can store complex configurations (like RFC 5545 RRULE strings: `FREQ=WEEKLY;BYDAY=MO,WE`) safely. We only need to upgrade the Domain Model and the mapping functions in `TaskEntity.kt` to parse it gracefully rather than crashing on `Enum.valueOf()`.
- **Multi-Stage Reminders:**
  - **Risk:** Zero Room migration required.
  - **Strategy:** The scheduler natively supports multiple alarms. The domain logic just needs an expanded `ReminderType` or a new mapping system to generate the `List<Long>`, saving us from needing an additional "reminders" table.
- **PortableBackup Compatibility:**
  - Backups will remain compatible because `recurrenceInterval` is just serialized as a string. Upgrading the string format requires the import logic to safely fallback if an older enum string (e.g., `"WEEKLY"`) is encountered.

## 5. Scope Boundaries & Non-Goals for v1.7.0
To maintain DueSoon's MVP philosophy and local-first invariants, the following are strictly **OUT OF SCOPE** for this release:
- **Natural Language Parsing:** (e.g., "Remind me every 3rd Tuesday"). We will use deterministic UI pickers instead.
- **Background Worker Polling:** No `WorkManager` loops; all calculations must happen synchronously on mutation and rely on `AlarmManager` wakeup.
- **External Sync:** No integration with Google Calendar or system calendars.
- **Room Migrations:** We will aggressively pursue non-breaking polymorphic data storage (strings/JSON inside existing columns) to avoid bumping to schema 4.

## Conclusion
The v1.6.0 baseline is exceptionally robust. The forethought to use a `String` column for recurrence and indexed request codes for alarms means that **v1.7.0 can likely be executed entirely within the domain and presentation layers without touching the Room database schema.**
