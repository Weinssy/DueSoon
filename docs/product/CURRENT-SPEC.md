# DueSoon — Current Product Specification

## Overview
**Current Version:** 1.6.0 (Active Development)  
**Status:** In Development (Based on v1.5.0 stable baseline)  
**Positioning:** DueSoon is a minimal, local-first Android deadline reminder app designed to help users stay aware of upcoming deadlines without the complexity of traditional project management tools.  
**Tagline:** Know what needs your attention next.

## Current Product Principles
1. **Deadline First:** Deadline should be obvious. Everything else should stay quiet.
2. **Minimal:** Simple UI without unnecessary project management bloat.
3. **Fast:** Quick task creation and interaction.
4. **Calm:** Respectful notifications.
5. **Reliable:** Dependable reminders.
6. **Local First:** The application functions fully offline without cloud dependencies.

## Current Core Loop
1. **CREATE:** Create a task.
2. **SET DEADLINE:** Assign an optional deadline.
3. **SMART REMINDER:** The app automatically schedules reminders based on the deadline.
4. **NOTIFICATION:** Receive timely local notifications.
5. **ACTION:** Complete or Snooze the task.

## Current Feature Matrix

| Capability | Status | Version | Notes |
|------------|--------|---------|-------|
| Task CRUD | Released | v1.0.0 | |
| Task without deadline | Released | v1.0.0 | |
| Deadline date/time | Released | v1.0.0 | |
| Category | Released | v1.0.0 | |
| Priority | Released | v1.0.0 | LOW, NORMAL, HIGH |
| Smart Reminder | Released | v1.0.0 | Automatically calculates reminder times |
| Custom Reminder | Planned | TBD | Not active in current implementation |
| Notifications | Released | v1.0.0 | Local Android notifications |
| Search | Released | v1.2.0 | Home search |
| Filtering | Released | v1.2.0 | By Status, Category, Priority |
| Sorting | Released | v1.2.0 | Deadline, Priority, Title, Created |
| Recurring tasks | Released | v1.2.0 | Daily, Weekly, Monthly intervals |
| Snooze | Released | v1.2.0 | 10m, 1h, Tomorrow options |
| Calendar | Released | v1.5.0 | In-memory grid with date filters and workload dots |
| Widget | Released | v1.1.0+ | Jetpack Glance Home screen widget |
| Backup | Released | v1.3.0 | Snapshot of full data |
| Export | Released | v1.3.0 | Export data to JSON via SAF |
| Import | Released | v1.3.0 | Merge tasks from JSON, generating new IDs |
| Restore | Released | v1.3.0 | Transactional destructive replace preserving original IDs |
| Smart Attention Ranking | Released | v1.4.0 | Blends deadline and priority deterministically |
| Widget Interactivity | Released | v1.6.0 | Task completion via widget |
| Quick Add | Released | v1.6.0 | Launcher shortcut to create task |

## Current Task Model
Based on the actual `Task` domain model.

**Required Fields:**
- `title` (String)
- `priority` (Enum: LOW, NORMAL, HIGH)
- `reminderType` (Enum: SMART, CUSTOM, NONE)
- `isRecurring` (Boolean)
- `completed` (Boolean)
- `createdAt` (Long)
- `updatedAt` (Long)

**Optional / Nullable Fields:**
- `id` (Long, auto-generated on insert)
- `description` (String)
- `deadline` (Long)
- `category` (String)
- `recurrenceInterval` (Enum: DAILY, WEEKLY, MONTHLY)
- `snoozedUntil` (Long)

**Behaviors:**
- Tasks without a deadline are treated as normal tasks but do not trigger Smart Reminders and are not considered overdue.
- Completion cancels pending notifications.

## Deadline and Reminder Behavior
- **Smart Reminder Rules:**
  - `> 7 days`: Reminds 3 days before, 1 day before, 3 hours before.
  - `1 to 7 days`: Reminds 1 day before, 3 hours before.
  - `< 1 day`: Reminds 3 hours before, 30 minutes before.
  - Only future times are scheduled; past triggers are ignored.
- **Timezone/Reboot:** Handled natively by Android's `AlarmManager` and `BootReceiver`.
- **Modifications:** Editing a deadline cancels old alarms and reschedules new ones.

## Search, Filtering, and Smart Attention Ranking (v1.4.0)
- **Search:** Case-insensitive string matching on task titles.
- **Filters:** Supports filtering by task status (Upcoming, Due Soon, Overdue, Completed).
- **Sorting (Smart Attention Ranking):** 
  v1.4.0 introduces the **Hybrid Urgency Matrix (Option C)**, rendering tasks through an explainable `AttentionTier`:
  1. `OVERDUE` (Overdue tasks, irrespective of priority)
  2. `CRITICAL` (Today + Normal/High)
  3. `HIGH` (Today + Low, OR Soon + High)
  4. `ELEVATED` (Soon + Normal/Low, OR Upcoming + High)
  5. `NORMAL` (Upcoming + Normal/Low)
  6. `OPTIONAL` (Tasks with no deadline)
  7. `COMPLETED`
  
  **Strict Day Boundary:** Within the same tier, secondary deadline sorting takes absolute precedence. 
  **Tie-breaking chain:** `Tier` -> `Deadline (nullsLast)` -> `Priority (descending)` -> `CreatedAt (ascending)` -> `Id (ascending)`.
- **Widget Parity:** The Home Screen and the Glance Widget share the exact same Smart Attention Ranking algorithm.

## Calendar & Time UX (v1.5.0)
- **In-Memory Date Filtering:** A native Compose grid acts as a visual date filter. Selecting a date operates strictly in-memory (no new DB queries), functioning as a boolean AND against category and search filters.
- **Null Deadline Exclusion:** When a specific date filter is active, tasks without a deadline (`deadline == null`) are strictly hidden from the list.
- **Workload Density Indicators:** Calendar days display a colored dot representing the highest `AttentionTier` due that day:
  - `CRITICAL` / `OVERDUE` -> Red (Error)
  - `HIGH` / `ELEVATED` -> Orange (Warning)
  - `NORMAL` / `OPTIONAL` -> Gray (Muted)
  - Tasks where `completed == true` are strictly excluded from generating density dots.
- **Timezone Safety:** UTC epoch deadlines are dynamically mapped to `LocalDate` using the user's active `ZoneId.systemDefault()` during Flow emission to prevent midnight crossover issues.
- **Widget & Notification Isolation:** The Glance Widget and Android NotificationScheduler remain completely oblivious to the UI calendar selection state, ensuring parity with the unfiltered baseline.

## Widgets & Quick Actions (v1.6.0)
- **Interactive Glance Widget:** Introduces `CompleteTaskActionCallback` allowing users to complete tasks directly from the home screen widget.
- **Domain Delegation & Idempotency:** The widget delegates actions to `CompleteTaskUseCase`, ensuring a safe, atomic, and idempotent task completion sequence (handling DB update, recurrence spawning, alarm cancellation, and widget refresh) that is resistant to rapid double-taps.
- **Touch-Target Separation:** The widget enforces a strictly sized 48dp checkbox hitbox for completing tasks, while tapping the task body opens the main app.
- **Quick Add (`ACTION_QUICK_ADD`):** A dedicated `(+)` icon on the widget routes users directly into the task creation flow (`Destinations.CREATE_TASK`) by propagating an explicit intent through the app's `MainActivity` and `AppNavigation`.

## Recurring Tasks and Snooze
- **Recurring:** Supports DAILY, WEEKLY, and MONTHLY intervals.
- **Snooze:** Postpones a reminder notification to 10 minutes, 1 hour, or Tomorrow.

## Backup, Export, Import, and Restore (v1.3.0)
DueSoon v1.3.0 introduces a portable JSON format (`schemaVersion: 1`).
- **EXPORT:** Creates a JSON file containing all task data using the Android Storage Access Framework (SAF) system file picker.
- **IMPORT:** Reads a JSON backup and **merges** it into the local database. It generates new IDs for imported tasks to avoid collisions.
- **RESTORE:** Reads a JSON backup and **replaces** all current local data via a SQLite `@Transaction`. It preserves the exact IDs from the backup file. Requires a destructive confirmation dialog.
- **Privacy:** Entirely offline, no cloud syncing involved.

## Current Roadmap
**Released:**
- v1.5.0 Calendar & Time UX
- v1.6.0 Widgets & Quick Actions

**Future:**
- AI assistance
- Cloud sync
- Multi-device support

**Explicitly Out of Scope:**
- Accounts / Login
- Cloud Sync (for v1.3.0)
- Cross-device real-time sync

## Evolution from PRD v1.0
- **Features added post-v1.0:** Search, Sorting, Recurring Tasks, Snooze (v1.2.0), and Backup/Restore capabilities (v1.3.0).
- **Reminder Logic:** The proposed Smart Reminder schedule from the original PRD was formalized and implemented in `SmartReminderCalculator`.
- **Data safety:** Evolved from a simple offline SQLite implementation to providing portable JSON data backups for user peace of mind (v1.3.0).
