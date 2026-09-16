# STEP 9.10A — Runtime Integration Failure Root-Cause Analysis

**Date/Time:** 2026-09-16
**Device/Model:** POCO X6 Pro 5G
**Android Version:** 16 (User provided test env)

## Original Failures
1. `BackupRestoreIntegrationTest > testNotificationReconciliation_edgeCases`
   -> `AssertionError: Overdue should not be scheduled`
2. `BackupRestoreIntegrationTest > testRestore_isDestructiveAndCancelsOldAlarms`
   -> `AssertionError: Old alarms must be cancelled during restore`

## Investigation Findings

The investigation involved tracing the exact production paths for notification cancellation and scheduling, specifically examining `BackupRestoreCoordinator`, `AndroidNotificationScheduler`, `SmartReminderCalculator`, and the integration test class `BackupRestoreIntegrationTest`.

**Finding 1: Production Code is Functionally Correct**
- In `BackupRestoreCoordinator.reconcileNotifications()`, the task is evaluated and `notificationScheduler.schedule(task)` is called.
- In `AndroidNotificationScheduler.schedule(task)`, the first action is `cancelAll(task)`, followed by `SmartReminderCalculator.calculateReminders(task)`.
- `SmartReminderCalculator.calculateReminders(task)` strictly filters out any reminders that are not in the future (`it > currentTimeMillis`).
- For overdue tasks, the calculator returns an empty list, and consequently, zero pending intents/alarms are created by `AndroidNotificationScheduler`. This is exactly the desired behavior (overdue task -> no retroactive notification -> no newly scheduled reminder).
- For cancellation, `AndroidNotificationScheduler.cancelAll(task)` accurately targets all potential request codes (0..9 and 99) for a given task ID. Since `Intent.filterEquals` matches strictly on component, request code, and intent flags (ignoring extras), AlarmManager successfully identifies and cancels all old `PendingIntent`s matching those codes before new alarms are scheduled.

**Finding 2: The Integration Test Setup Was Flawed**
The failures encountered running `connectedDebugAndroidTest` on the device were **not** caused by failures in the Android runtime or production logic. They were caused by flaws within the `TrackingNotificationScheduler` (the test double used in `BackupRestoreIntegrationTest` to intercept scheduler calls):
- The `TrackingNotificationScheduler` unconditionally recorded tasks as "scheduled" whenever `schedule(task)` was invoked, failing to simulate the empty-list rejection behavior of `SmartReminderCalculator`. This caused the "overdue should not be scheduled" assertion to fail, even though real alarms would never be set.
- The `TrackingNotificationScheduler` failed to set the `cancelAllCalled` flag inside its `cancelAll(task)` method (the method body only had a comment). This caused the "Old alarms must be cancelled" assertion to fail.

## Root Causes

**Root cause for overdue failure:**
`TrackingNotificationScheduler.schedule()` did not consult `SmartReminderCalculator`, meaning it recorded all tasks passed to it as scheduled, even if their deadlines were deeply in the past.

**Root cause for restore cancellation failure:**
`TrackingNotificationScheduler.cancelAll()` did not mutate its internal tracking state (`cancelAllCalled = true`), causing the test assertion on that state to fail despite the Coordinator making the correct call.

## Production Changes
None. The existing architecture and logic in `AndroidNotificationScheduler`, `SmartReminderCalculator`, and `BackupRestoreCoordinator` are robust, adhere to product requirements, and function correctly.

## Test Changes
Modified `TrackingNotificationScheduler` within `BackupRestoreIntegrationTest.kt`:
1. Updated `schedule(task)` to delegate to `SmartReminderCalculator.calculateReminders(task)`. It now only adds to `scheduledTasks` if the calculated reminder list is not empty.
2. Updated `cancelAll(task)` to set `cancelAllCalled = true` and properly remove the task from `scheduledTasks`.

**Why each change was necessary:**
To ensure the test fake faithfully simulates the observable side-effects (or lack thereof) of the production `AndroidNotificationScheduler` without involving the actual `AlarmManager`, providing a reliable signal for the Coordinator's behavior.

## Verification Results
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew assembleDebug`: PASS
- `./gradlew assembleRelease`: PASS
- `./gradlew assembleAndroidTest`: PASS (APK builds successfully)
- `./gradlew connectedDebugAndroidTest`: Awaiting user verification on device.

## Remaining Limitations
None identified within the scope of this step. The test logic is now structurally aligned with the production scheduling rules.
