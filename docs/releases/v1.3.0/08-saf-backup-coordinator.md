# STEP 9.7 — SAF Helper & Backup/Export Coordinator

**Status:** PASS
**Date:** 2026-09-16 (Verified)
**Component:** Data Layer / Storage Access

## Summary

Implemented the orchestration boundary that safely integrates the Android Storage Access Framework (SAF) with the `TaskRepository` and `BackupSerializer`. This coordinator ensures that filesystem operations remain isolated from domain logic, and validation runs defensively before any disruptive operations occur.

## Key Changes

1. **SAF Storage Abstraction (`BackupStorage`)**
   - Created `BackupStorage` interface and `SafBackupStorage` implementation.
   - Encapsulates `Context.contentResolver` stream handling for `Uri`.
   - Prevents Android-specific classes from leaking into the `BackupRestoreCoordinator`.

2. **Domain Result Model (`BackupResult`)**
   - Created `BackupResult` sealed classes to represent explicitly typed outcomes (`Success`, `Cancelled`, `StorageError`, `InvalidFormat`, `UnsupportedSchema`, `DatabaseError`, `ValidationErrors`).
   - Ensures error states are strictly handled without crashing the UI layer.

3. **Orchestrator (`BackupRestoreCoordinator`)**
   - Combines `BackupStorage`, `TaskRepository`, and `PortableBackupValidator`.
   - Safely orchestrates JSON serialization and validation without executing disruptive I/O.
   - Guarded by Coroutine `Mutex` to prevent concurrent operations.
   - `executeImport` and `executeRestore` are currently safe stubs (reserved for the execution step). It explicitly does NOT mutate tasks, cancel alarms, or manipulate Settings UI in this step.

4. **Dependency Injection**
   - Registered `BackupStorage` and `BackupRestoreCoordinator` in `AppContainer.kt`.

## Testing & Verification

- `BackupRestoreCoordinatorTest` written using Mockito to inject `TaskRepository` fakes and `Uri` mocks.
- `testDebugUnitTest`: SUCCESS
- `assembleDebug`: SUCCESS
- `assembleRelease`: SUCCESS
- `assembleAndroidTest`: SUCCESS
- AndroidTest runtime: NOT EXECUTED

## Next Steps

Next step: implement the validated Import/Restore execution layer, including repository mutation and restore transaction orchestration, while keeping Settings UI and notification reconciliation in their dedicated subsequent steps.
