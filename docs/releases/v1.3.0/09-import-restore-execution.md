# DueSoon v1.3.0 — STEP 9.8
# Import & Restore Execution Layer

**Date/Time:** 2026-09-16 09:59:40 +07:00
**Branch:** main
**Status:** IMPLEMENTED (Data Execution Layer only)

## 1. Files Changed
- `app/src/main/java/com/duesoon/app/data/local/TaskDao.kt`: Removed `onConflict = OnConflictStrategy.REPLACE` from `insertTasks` to enforce validation rules and prevent silent ID conflict resolution during restore.
- `app/src/main/java/com/duesoon/app/data/backup/BackupRestoreCoordinator.kt`: Implemented `executeImport` and `executeRestore` methods. Masked inner database exceptions.
- `app/src/test/java/com/duesoon/app/data/backup/BackupRestoreCoordinatorTest.kt`: Added explicit unit tests for the Import/Restore execution semantics and error propagation.

## 2. Import Execution Semantics
- **Operation:** Append-only bulk import.
- **ID Handling:** The imported external `PortableTask.id` values are ignored. The system strips them and maps them to `id = 0` via the domain-to-entity mapping, forcing Room to automatically generate fresh IDs, preventing any collision with existing tasks.
- **Execution:** Uses `TaskRepository.importTasks()`, which delegates to `TaskDao.insertTasks()` (a single bulk insertion list operation).

## 3. Restore Execution Semantics
- **Operation:** Destructive transactional replacement.
- **ID Handling:** Preserves the existing `id` of each imported task from the `ValidatedPortableBackup` (guaranteed by previous steps to be strictly > 0 and uniquely scoped).
- **Execution:** Uses `TaskRepository.restoreTasks()`, which delegates to the `TaskDao.replaceAllTasks()` `@Transaction` method.
- **Transaction Boundary:** The actual `DELETE FROM tasks` and bulk `insertTasks` operations are wrapped inside a single atomic Room `@Transaction`. If the insertion fails, the entire deletion rolls back. The coordinator does NOT handle raw transactions or SQLite APIs.

## 4. Exception Mapping
Any exception caught at the repository boundary inside `executeImport` and `executeRestore` (such as `SQLiteConstraintException`) is immediately mapped to `BackupResult.Error.DatabaseError`. 
To prevent exposing sensitive SQL details or stack traces to the UI level, the explicit error `e.message` is completely masked and replaced with generic user-facing text ("Failed to import tasks due to a database error.").

## 5. Excluded Boundaries
- **Notification Boundary:** No alarms were cancelled or rescheduled. Neither `NotificationScheduler` nor `AlarmManager` were invoked. This is strictly deferred to Step 9.9.
- **UI Boundary:** No modifications were made to the UI, ViewModels, Compose screens, or SAF intent launchers.

## 6. Tests Added
- `testExecuteImport_success`: Asserts standard execution logic and correct flow into the mock `TaskRepository`.
- `testExecuteRestore_success`: Asserts successful restore delegation to the repository.
- `testExecuteImport_databaseError`: Forces a mock repository RuntimeException and validates `DatabaseError` propagation with masked message.
- `testExecuteRestore_databaseError`: Forces a mock `@Transaction` failure and validates `DatabaseError` propagation with masked message.

## 7. Build & Test Results
```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease assembleAndroidTest
```
- **Result:** `BUILD SUCCESSFUL in 1m 44s`
- **Unit Tests:** `testDebugUnitTest` PASS
- **assembleDebug:** PASS
- **assembleRelease:** PASS
- **assembleAndroidTest:** PASS
- **AndroidTest Runtime:** NOT EXECUTED (No emulator device available. Only the APK compilation succeeded.)

## 8. Known Limitations
- Import and Restore do not recalculate or schedule smart reminders. Any restored task with pending deadlines will not trigger an alarm yet.
- There is currently no UI access to trigger these implemented methods.

## 9. Next Step
Proceed to **STEP 9.9** to orchestrate the Notification Reconciliation (rescheduling/canceling alarms based on imported/restored tasks) and integrate the Settings UI / file-picker ActivityResult.
