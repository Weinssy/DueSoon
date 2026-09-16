# DueSoon v1.2.0 Architecture Design

## 1. Architecture Overview
DueSoon v1.2.0 maintains the existing offline-first, local-only architecture: Compose UI -> ViewModel -> Repository -> Room Database. The notification layer remains AlarmManager-driven, backed by BroadcastReceivers. The primary architectural changes in v1.2.0 are constrained to the data layer (Room schema migration for Snooze) and in-memory flow transformations (Search and Sort).

## 2. Existing Architecture Audit
- **UI:** Jetpack Compose screens and components.
- **State:** `StateFlow` and `MutableStateFlow` in ViewModels.
- **Repository:** `TaskRepository` manages DB calls, initiates notification scheduling, and calculates recurring task deadlines.
- **Database:** Room v2, `TaskEntity`.
- **Notifications:** `SmartReminderCalculator` generates offsets; `AndroidNotificationScheduler` sets exact alarms; `ReminderReceiver` posts the UI notification. `BootReceiver` reschedules on boot.

## 3. Search Design
- **Architecture Layer:** UI / ViewModel.
- **Design:** Introduce a `MutableStateFlow<String>` in `HomeViewModel`. Update `HomeFilterLogic.filterTasks` to accept the query string.
- **Behavior:** The query filters tasks by checking `task.title.contains(query, ignoreCase = true)`.
- **State Lifetime:** Exists only in memory while the ViewModel is alive. Not persisted.
- **Interaction with Filters:** Chained with Status and Category filters using standard AND logic.
- **Empty State:** If query is blank, search filter is a no-op.

## 4. Sorting Design
- **Architecture Layer:** UI / ViewModel.
- **Design:** Introduce an enum `SortOrder` (DEADLINE, PRIORITY, TITLE, CREATED). Add a `MutableStateFlow<SortOrder>` in `HomeViewModel`.
- **Behavior:** After `HomeFilterLogic` filters the list, `.sortedWith()` applies the chosen comparator.
- **Default:** `DeadlineState` -> `deadline` -> `priority` -> `createdAt` (preserving DueSoon's core deadline-first principle).
- **Tie Breakers:** Fallback to created date descending.

## 5. Recurring Task Design
- **Architecture Layer:** Repository / UI.
- **Design:** Existing repository logic works well (`task.copy(id = 0)` on completion). The architecture remains unchanged.
- **UI Polish:** Ensure the `RecurrenceSelector` clearly reflects the `isRecurring` and `recurrenceInterval` state in `CreateTaskViewModel` and `EditTaskViewModel`. 
- **Completed History:** We preserve the behavior where a completed recurring task remains in the database for history, while a new task is spawned for the next interval.

## 6. Snooze Design
- **Architecture Layer:** Domain, Room, Notification.
- **Model:** `snoozedUntil: Long?` is added to `Task`.
- **Stale Notification Safety:** A Snooze action carries `taskId` and `snooze duration`. Before applying Snooze, `ReminderReceiver`/Repository must verify the task still exists, is not completed, and has not been deleted. A stale notification must not recreate or resurrect a deleted/completed task. Exact implementation responsibility *requires implementation-stage verification.*
- **Multiple Snooze:** A new Snooze replaces the previous `snoozedUntil` value. Only the latest Snooze is authoritative. Do not create multiple active Snooze alarms for the same task.
- **Snooze Past Deadline:** Allowed. Example: Deadline = 10:00, Current time = 09:50, User Snoozes 1 hour. Behavior: `snoozedUntil` = 10:50. Task deadline remains 10:00 (task becomes/continues to be OVERDUE). Notification fires at 10:50. `snoozedUntil` is cleared after the snooze notification is processed. Snooze must NEVER modify the task deadline.
- **Smart Reminder Preservation:** `SmartReminderCalculator` remains responsible for normal Smart Reminder calculation. Snooze is a temporary persisted override/state. Snooze must not permanently alter the deadline, reminderType, or Smart Reminder configuration. After Snooze expires, the task returns to its normal reminder behavior.

## 7. Task Model Changes
- `Task.kt` adds `val snoozedUntil: Long? = null`.
- `TaskEntity.kt` adds `val snoozedUntil: Long? = null`.

## 8. Room v3 Schema Design
- `TaskEntity` gets a new column: `snoozedUntil INTEGER DEFAULT NULL`.
- The column allows `null` to indicate no active snooze.

## 9. Migration Design
- **Migration SQL:** `ALTER TABLE tasks ADD COLUMN snoozedUntil INTEGER DEFAULT NULL`
- **Migration Chain:** Both supported upgrade paths must be handled:
  - `v1 database -> MIGRATION_1_2 -> MIGRATION_2_3 -> v3`
  - `v2 database -> MIGRATION_2_3 -> v3`
- `snoozedUntil` must default to NULL.
- **Backward Compatibility:** Safe for existing v1 and v2 databases.

## 10. Notification/Scheduler Design
- **Request Code Strategy:** Use index `99` explicitly for Snooze alarms. *Requires implementation-stage verification* to ensure that `task.id * 100 + 99` does not collide with Smart reminders, Custom reminders, or any other existing scheduler alarm indices. Do not assume it prevents collisions completely without qualification.
- **Alarm Cancellation:** Snooze must not unnecessarily destroy the logical schedule of future Smart/Custom reminders. The design must document the minimal cancellation/rescheduling strategy required to preserve future reminders whenever possible, rather than assuming all alarms from 0..99 should always be cancelled blindly.

## 11. Receiver Design
- **ReminderReceiver:** When a snooze action is clicked (via a `PendingIntent`), it must update the DB. Since BroadcastReceiver is short-lived, it should safely update the `Task` and reschedule. Must perform stale notification safety checks (ensure task exists and is not completed).

## 12. Boot/Reboot Design
- **BootReceiver:** Reads all active tasks. If a task has a `snoozedUntil` in the past, it resets it to null (snooze expired while off). If in the future, it reschedules. Because `snoozedUntil` is persisted in Room, reboot restoration is safe and resilient.

## 13. Request Code Strategy
- *Moved to Notification/Scheduler Design (Section 10) to address specific collision verification requirements.*

## 14. Repository Responsibilities
- `TaskRepository`: Handles the `snoozeTask(taskId, snoozeDuration)` function. Updates the DB (after verifying task validity) and invokes `NotificationScheduler`.

## 15. ViewModel Responsibilities
- `HomeViewModel`: Manages search and sort states, applies Flow transformations.
- `TaskDetailViewModel` / `TasksViewModel`: May need minor updates to clear snooze if edited manually.

## 16. UI Responsibilities
- Provide Search bar and Sort dropdown. Pass events to ViewModels.

## 17. State Management
- Search and Sort are ephemeral (lost on app close) to keep the app feeling fresh.

## 18. Data Flow
`UI Action (Snooze)` -> `Receiver` -> `Repository (Stale Check -> updateTask)` -> `Room (snoozedUntil)` -> `Scheduler (Minimal Cancellation)` -> `AlarmManager`

## 19. Error/Edge Case Handling
- **Snooze past deadline:** `snoozedUntil` set past deadline. Deadline unaffected.
- **Multiple snoozes:** Overwrites the previous `snoozedUntil`.
- **Task Completed/Deleted:** Clears `snoozedUntil` and cancels alarms. Stale notifications ignored.

## 20. Test Architecture
- **Search/Sort:** Unit tests for Flow combinations and `HomeFilterLogic`.
- **Recurring:** Unit test for `calculateNextDeadline`.
- **Snooze:** Unit test for `Scheduler` logic confirming that index 99 is set, earlier smart alarms skipped, but future smart alarms preserved.
- **Database:** `MigrationTestHelper` to verify `MIGRATION_2_3` from both v1 and v2.

## 21. Backward Compatibility
- Complete backward compatibility with existing Room DBs and existing scheduled alarms.

## 22. Risks
- Database Migration failing on edge case devices.
- `BroadcastReceiver` ANR if DB operation for snooze takes too long on the main thread.
- Request code collisions if custom reminders exceed index bounds (*Requires implementation-stage verification*).

## 23. Implementation Order
1. Search (In-memory UI/ViewModel)
2. Sorting (In-memory UI/ViewModel)
3. Recurring UI Polish (UI/ViewModel)
4. Database Migration v3 (Room)
5. Snooze Logic (Repository, Scheduler, Receiver)

## 24. Explicit "NOT IMPLEMENTED IN THIS STEP" section
- Code implementation is NOT implemented in this step.
- Archive, Cloud sync, AI, complex PM, Dashboards are NOT implemented.
