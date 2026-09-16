# DueSoon v1.2.0 Baseline Audit

## 1. Repository Structure
The project follows a standard modern Android architecture (UI -> ViewModel -> Repository -> Room). The root package is `com.duesoon.app` and is organized clearly by feature and layer:
- **`data`**: Contains local Room implementations (`AppDatabase.kt`, `TaskDao.kt`, `TaskEntity.kt`) and the repository layer (`TaskRepository.kt`, `UserPreferencesRepository.kt`).
- **`di`**: Basic dependency injection (`AppContainer.kt`).
- **`domain`**: Contains the core business models (`Task.kt`, `Category.kt`, `Priority`, `DeadlineState`) and utilities (`DateTimeUtils.kt`, `DeadlineStateCalculator.kt`).
- **`navigation`**: Centralized compose navigation (`AppNavigation.kt`).
- **`notification`**: AlarmManager and BroadcastReceiver logic (`SmartReminderCalculator.kt`, `AndroidNotificationScheduler.kt`, `ReminderReceiver.kt`, `BootReceiver.kt`).
- **`ui`**: Grouped by screen/feature (`calendar`, `home`, `settings`, `task`, `tasks`, `theme`) and reusable widgets (`components`).
- **`widget`**: Jetpack Glance implementation (`DueSoonWidget.kt`).

## 2. Task Model
The core `Task` model (and corresponding `TaskEntity`) currently contains the following fields:
- `id: Long = 0` (Auto-generated primary key)
- `title: String`
- `description: String?`
- `deadline: Long?`
- `category: String?`
- `priority: Priority` (Enum: LOW, NORMAL, HIGH)
- `reminderType: ReminderType` (Enum: SMART, CUSTOM, NONE)
- `isRecurring: Boolean = false`
- `recurrenceInterval: RecurrenceInterval? = null` (Enum: DAILY, WEEKLY, MONTHLY)
- `completed: Boolean = false`
- `createdAt: Long` (Timestamp in ms)
- `updatedAt: Long` (Timestamp in ms)

## 3. Recurring Task Current State
- **Does it exist?** Yes, it is already partially implemented at the repository level.
- **Relevant Files:** `Task.kt` (Enums and properties), `TaskEntity.kt` (DB columns), `TaskRepository.kt` (Logic), `RecurrenceSelector.kt` (UI component).
- **Supported Types:** Daily, Weekly, Monthly (`RecurrenceInterval` enum).
- **Storage:** Persisted in Room via `isRecurring` and `recurrenceInterval` columns.
- **UI Availability:** A `RecurrenceSelector.kt` UI component already exists in the `components` package.
- **Interaction with Notifications:** Follows standard notification flow. The new scheduled task will automatically receive alarms.
- **Rescheduling Logic:** Yes. In `TaskRepository.kt`, when a recurring task is marked as `completed = true`, it calculates the next deadline and inserts a *new* task (`task.copy(id = 0, completed = false, deadline = nextDeadline)`).
- **Completeness:** The implementation is highly functional at the data level. 

## 4. Notification Architecture
- **SmartReminderCalculator:** Calculates alarm times based on the deadline. Calculates 3-4 predefined offsets based on how far away the deadline is (e.g., >7 days away: 3 days, 1 day, and 3 hours before).
- **AndroidNotificationScheduler:** Utilizes `AlarmManager` (`setExactAndAllowWhileIdle`) to schedule PendingIntents. Generates unique request codes per task using `task.id * 100 + index` (allows up to 10 alarms per task).
- **ReminderReceiver:** BroadcastReceiver that triggers the actual `NotificationCompat.Builder` (High Priority) when the alarm fires, opening `MainActivity`.
- **BootReceiver:** On `ACTION_BOOT_COMPLETED`, reads all tasks from `TaskRepository` and reschedules active alarms so they survive reboots.
- **Cancellations:** `TaskRepository` cancels all alarms if a task is deleted, marked complete, or if notifications are disabled in settings. 
- **Overdue Tasks:** Handled via UI calculation (`DeadlineStateCalculator`). Notifications do not fire retroactively for overdue tasks.
- **Limitations relevant to Snooze:** Currently, `AndroidNotificationScheduler` relies entirely on `SmartReminderCalculator` to dictate times. There is no custom `snooze` time parameter or database field to persist a "snoozed" state across reboots.

## 5. Database
- **Room Version:** Version `2`.
- **TaskEntity Schema:** 12 columns directly mapping to the `Task` domain model.
- **Existing Migrations:** `MIGRATION_1_2` exists (added `isRecurring` and `recurrenceInterval`).
- **v1.2 Requirements:** If Snooze logic requires reboot resilience, a new field like `snoozedUntil: Long?` will be needed, which would require a Room migration to Version 3. Search and Sort would not require a schema change.

## 6. Search Capability
- **Current State:** No text-based search functionality exists.
- **Filtering:** Currently handled purely in memory by `HomeFilterLogic.kt` (filters by Status and Category).
- **Implementation Path:** Search can be implemented purely in `HomeFilterLogic` (e.g., `title.contains(query, ignoreCase = true)`) without changing the Room schema or DAO, keeping it consistent with the current offline-first, in-memory Flow transformation paradigm.

## 7. Sorting Capability
- **Current Home Sorting:** Hardcoded in `HomeViewModel.kt`. Sorts by `DeadlineState` -> `deadline` -> `priority` -> `createdAt`.
- **Current Tasks Sorting:** Grouped into "Needs Attention", "Upcoming", and "Completed", then sorted by `deadline` or `updatedAt`.
- **Reusable Logic:** No dynamic user-selected sorting logic currently exists.
- **Implementation Path:** New sorting criteria (e.g., alphabetical, by priority, by date created) do not require DB changes. They can easily be added to the `.sortedWith()` block in the ViewModels.

## 8. UI Structure
- **Screens:** `HomeScreen`, `TasksScreen` (Category groupings), `CreateTaskScreen`, `EditTaskScreen`, `TaskDetailScreen`, `CalendarScreen`, `SettingsScreen`.
- **Reusable Components:** Found in `ui/components/`, including `TaskCard`, `CategoryChip`, `CategorySelector`, `DeadlineLabel`, `PriorityIndicator`, `DateTimePickerDialog`, and `RecurrenceSelector`.

## 9. Existing Tests
- **Unit Tests (`app/src/test`):**
  - `DateTimeUtilsTest.kt` (Date/time formatting and UTC validation)
  - `HomeFilterLogicTest.kt` (Status and category filter logic validation)
- **Instrumented Tests (`app/src/androidTest`):** None exist currently.
- **Notification/Widget Tests:** None exist currently.

## 10. v1.1.0 Integrity
- **versionCode:** 2
- **versionName:** "1.1.0"
- **Room Version:** 2
- **Uncommitted Changes:** None. `git status` reports a clean working tree.
- **Current Branch:** `main` (up to date with `origin/main`).
- **Latest Commit:** `e7955b5`
- **Latest Tag:** `v1.1.0`

## 11. Technical Risks
- **A. Recurring Task:** The logic automatically duplicates tasks upon completion (`task.copy(id = 0)`). This is a safe local-first approach but will cause the database to accumulate completed historical tasks over time. 
- **B. Snooze Reminder:** High risk if resilience is required. If a user snoozes a task, and the device reboots, `BootReceiver` will currently fall back to `SmartReminderCalculator` and the snooze time will be lost. Persisting snooze requires a database migration.
- **C. Search Task:** Very low risk. Can be done entirely in memory.
- **D. Sorting:** Very low risk. Can be done entirely in memory. Primary risk is UI clutter (finding a clean place to put the Sort & Search UI).

## 12. Recommended v1.2.0 Implementation Order
1. **Search Capability** (Low risk, isolated UI/Flow update)
2. **Sorting Capability** (Low risk, UI/Flow update, can share UI space with Search)
3. **Recurring Task UI Polish** (Expose the existing data-level recurrence logic seamlessly into Create/Edit screens if it isn't fully wired yet)
4. **Snooze Reminder** (Highest risk, requires UI actions in Notification, DB migration for persistence, and AndroidNotificationScheduler refactor)

## 13. Final Recommendation
READY FOR v1.2.0 PLANNING

## Audit Status

READY FOR v1.2.0 PLANNING

- Audit date: 2026-09-16
- Current version: 1.1.0
- Current versionCode: 2
- Current branch: main
- Latest commit: e7955b5
- Latest tag: v1.1.0

## 14. Documentation Requirement Fulfillment
- **What was implemented/audited:** Performed a comprehensive baseline audit of DueSoon v1.1.0 to prepare for v1.2.0 planning. Audited repository structure, task model, recurring tasks, notification architecture, database, search/sort capabilities, UI, tests, and integrity.
- **Files changed:** None (Audit-only task).
- **Architecture impact:** None (Audit-only task).
- **Database impact:** None (Audit-only task).
- **Notification impact:** None (Audit-only task).
- **Tests performed:** None (Audit-only task).
- **Build results:** None (Audit-only task).
- **Known risks/issues:** 
  - Recurring tasks currently duplicate instances upon completion, which will accumulate history.
  - Snooze reminder implementation will likely require a DB migration for persistence across reboots.
- **Final status:** READY FOR v1.2.0 PLANNING.
