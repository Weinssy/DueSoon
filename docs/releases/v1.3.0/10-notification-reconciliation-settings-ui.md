# DueSoon v1.3.0 — STEP 9.9
# Notification Reconciliation & Settings UI

**Date/Time:** 2026-09-16 10:18:49 +07:00
**Branch:** main
**Status:** IMPLEMENTED (Reconciliation & Settings UI)

## 1. Files Changed
- `app/src/main/java/com/duesoon/app/data/backup/BackupRestoreCoordinator.kt`: Added `NotificationScheduler` and `UserPreferencesRepository` via constructor injection. Added explicit `reconcileNotifications()` flow post-database-transaction.
- `app/src/main/java/com/duesoon/app/di/AppContainer.kt`: Provided Notification/Preferences dependencies to `BackupRestoreCoordinator`.
- `app/src/main/java/com/duesoon/app/ui/AppViewModelProvider.kt`: Injected `BackupRestoreCoordinator` into `SettingsViewModel`.
- `app/src/main/java/com/duesoon/app/ui/settings/SettingsViewModel.kt`: Implemented `BackupUiState` state-machine and exposed Backup/Restore orchestrations (export, import, restore).
- `app/src/main/java/com/duesoon/app/ui/settings/SettingsScreen.kt`: Introduced "Data & Backup" section utilizing Material 3. Implemented SAF Document choosers (`CreateDocument`, `OpenDocument`) and critical Warning/Confirmation dialogs.
- `app/src/main/res/values/strings.xml`: Added localized strings for Backup/Restore, warnings, and dialogs.
- `app/src/test/java/com/duesoon/app/ui/settings/SettingsViewModelTest.kt`: Added UI state-machine validation logic.
- `app/src/test/java/com/duesoon/app/data/backup/BackupRestoreCoordinatorTest.kt`: Extended tests to verify Mockito interactions with `NotificationScheduler`.

## 2. Notification Reconciliation Architecture
The architectural boundary cleanly strictly separates Database mutability from AlarmManager execution. 
- The Room Database serves as the sole source of truth.
- `NotificationScheduler` interacts with OS scheduling purely as a *side-effect* following successful persistence.
- Failure within the reconciliation step (e.g. invalid Intent building) will naturally drop, but the user's local database remains functionally sound.

## 3. Import Behavior
Upon successful `importTasks(validatedBackup.tasks)` execution, the coordinator computes the delta (Tasks existing AFTER import but missing BEFORE import) representing the freshly created entities complete with newly generated autoincrement IDs. These explicit entities are then routed to `reconcileNotifications(tasks)`. Old alarms are preserved natively.

## 4. Restore Behavior
Following successful `@Transaction` `replaceAllTasks()` completion:
1. `oldTasks.forEach { notificationScheduler.cancelAll(it) }` systematically cancels pending reminder bounds scoped to the pre-restore data snapshot.
2. `restoredTasks = taskRepository.observeTasks().first()` queries the newly authoritative reality from Room.
3. `reconcileNotifications(restoredTasks)` binds the alarms.
4. **Failure semantics**: If `restoreTasks` throws `SQLiteConstraintException`, the exception propagates mapping to `BackupResult.Error.DatabaseError`. The alarms cancellation logic is **skipped entirely**. Thus, `AlarmManager` remains functionally accurate to the unmutated state.

## 5. Snooze Behavior
- **Future Snooze**: Handled correctly; invokes `scheduleSnooze`.
- **Past Snooze**: Automatically intercepted during `reconcileNotifications()`. Rather than blindly forwarding stale timestamps to AlarmManager (resulting in a retroactive bombardment of immediate firing alarms), the coordinator leverages `taskRepository.clearSnooze(task.id)` to silently normalize the database structure for an imported stale task.

## 6. Error Handling
Consistent with standard UX rules, internal errors explicitly map to `BackupResult.Error`. The ViewModel propagates these errors via `BackupUiState.Error`, which renders safely to the end user as a localized snackbar (e.g., "Storage error occurred", "Invalid backup file format"). Database internal IDs or SQL trace paths are stripped via boundary masking.

## 7. Settings UI Flow & ActivityResult Behavior
The Settings section exposes:
- **Export Backup**: Opens SAF `CreateDocument`. Returns a JSON URI directly fed to `coordinator.exportBackup(uri)`.
- **Import Data**: Opens SAF `OpenDocument`. Feeds to `coordinator.prepareImport(uri)`. If validated successfully, presents `AwaitingImportConfirmation` requiring explicit manual confirmation.
- **Restore Backup**: Opens SAF `OpenDocument`. Feeds to `coordinator.prepareRestore(uri)`. Validated result transitions to `AwaitingRestoreConfirmation` dialog (clearly conveying the *DANGER: destructive replacement* caveat via red tint and clear descriptive language). 

## 8. Build & Test Results
```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease assembleAndroidTest
```
- **Result**: `BUILD SUCCESSFUL`
- **testDebugUnitTest**: PASS (Contains `SettingsViewModelTest` and `BackupRestoreCoordinatorTest`)
- **assembleDebug**: PASS
- **assembleRelease**: PASS
- **assembleAndroidTest**: PASS
- **Android runtime test**: NOT EXECUTED (No connected Android environment / emulator).

## 9. Known Limitations
- The underlying `AlarmManager` is subjected to standard OS restrictions (Doze Mode limits, App Standby buckets). While `setExactAndAllowWhileIdle` is used, highly frequent identical timestamps may still incur OS-level coalescing.
- `SettingsViewModel` currently uses `whileSubscribed(5000)`. If a large Import takes longer than 5 seconds while backgrounded, state emission edge cases may arise.

## 10. Next Step
The core architectural layers for Backup/Import/Restore are firmly implemented. We can proceed to final holistic product-level QA audits.
