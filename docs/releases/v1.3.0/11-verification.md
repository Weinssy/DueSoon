# STEP 9.10 — Integration & Database Verification

This report serves as the formal verification gate before the final architectural audit of DueSoon v1.3.0.

## 1. Unit Test Verification (testDebugUnitTest)
The unit tests have been thoroughly tested, fixed, and verified during STEP 9.9. They validate the following core logic:
- `BackupRestoreCoordinator` execution workflows.
- `SettingsViewModel` state machine.
- `PortableBackupValidator` logic.

**Status:** PASS (`BUILD SUCCESSFUL`)

## 2. Integration Test Implementation (assembleAndroidTest)
A comprehensive integration test suite (`BackupRestoreIntegrationTest.kt`) has been added to `app/src/androidTest/` to verify the actual Room Database behavior and Coordinator execution on an Android runtime.

The following integration priorities are fully covered in the test suite:

### A. Export Verification
- **Test:** `testExport_createsValidJsonAndSchemaVersion`
- Verifies that tasks from the local SQLite Room database are properly marshalled and written to the SAF OutputStream.
- Asserts that `schemaVersion` is exactly 1 and JSON properties are correct.

### B. Import Verification
- **Test:** `testImport_appendsTasksAndHandlesExternalIds`
- Verifies that an imported backup correctly appends tasks instead of replacing them.
- Asserts that duplicate/external IDs are correctly re-mapped to new primary keys by Room.
- Asserts that new alarms are requested for newly imported active tasks.

### C. Restore Verification
- **Test:** `testRestore_isDestructiveAndCancelsOldAlarms`
- Verifies that a restore operation is fully destructive (atomically clears the old DB inside a `@Transaction`).
- Asserts that `NotificationScheduler.cancelAll()` is invoked for old alarms before new alarms are scheduled.
- Verifies new primary keys are preserved/mapped properly based on the backup.

### D. Invalid Backup Rejection
- **Test:** `testInvalidBackup_rejectedBeforeChanges`
- Verifies that a missing `schemaVersion` or malformed JSON is rejected with `BackupResult.Error.InvalidFormat`.
- Asserts that the original Room Database remains completely unchanged upon failure.

### E. Notification Edge Cases (Reconciliation)
- **Test:** `testNotificationReconciliation_edgeCases`
- Validates the derived notification state after import/restore:
  - `completed` task -> ignored.
  - `deadline = null` -> ignored.
  - `overdue` -> ignored (no retroactive firing).
  - `future snooze` -> `scheduleSnooze` is correctly called.
  - `past snooze` -> stale snooze is discarded, normal reminder scheduled instead.

**Status:** Compilation PASS (`assembleAndroidTest`).

## 3. Real Device Execution Required (connectedDebugAndroidTest)

As requested, I distinguish strictly between `assembleAndroidTest` (building the test APK) and `connectedDebugAndroidTest` (running the test on an actual device).

Since I am operating in a headless cloud environment without a connected ADB emulator/device, I have only run `assembleAndroidTest`.

**REQUIRED USER ACTION:**
To fully satisfy this verification gate, you must run the following command on your local machine with an emulator or physical device connected:
```bash
./gradlew connectedDebugAndroidTest
```

This will run `BackupRestoreIntegrationTest` natively on the device's SQLite engine.

## Next Steps
Once you have executed `connectedDebugAndroidTest` on your device and confirmed the tests pass, the repository is verified and fully hardened. We are ready for the **Final Audit (STEP 10)**.
