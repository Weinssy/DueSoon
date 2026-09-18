# Architecture Design: DueSoon v1.6.0 (Widgets & Quick Actions)

**Target Release:** v1.6.0
**Baseline:** v1.5.0
**Date:** 2026-09-18
**Status:** DRAFT

## 1. Executive & Architecture Overview
In DueSoon v1.6.0, the Glance Widget transitions from a static presentation surface into an interactive component. The core architectural challenge is preventing business logic from leaking into `RemoteViews` callbacks. We resolve this by enforcing a strict separation of concerns: Glance components handle only UI definitions and basic touch intents, delegating all data mutations, side effects, and re-renders to a unified domain layer.

**End-to-End Mutation Flow:**
`Glance ActionCallback` → `CompleteTaskUseCase` → `TaskRepository` / Room → `NotificationScheduler` → `DueSoonWidgetUpdater`

This flow guarantees that whether a task is completed from the widget or from within the main application, the exact same idempotency checks, recurrence calculations, and alarm cancellations occur.

## 2. Domain Layer: `CompleteTaskUseCase`
To centralize completion logic, we introduce a new Domain Use-Case.

- **Package:** `com.duesoon.app.domain.usecase.CompleteTaskUseCase`
- **Dependencies:** 
  - `TaskRepository` (for database interaction)
  - `NotificationScheduler` (for side-effects)
  - `DueSoonWidgetUpdater` (for immediate rendering feedback)
  *(Note: To maintain pure domain boundaries, side-effects like Widget updates and Notifications may be abstracted behind interfaces or delegated to a presentation coordinator if needed, but for simplicity in this offline app, direct injection/invocation via Application Context is acceptable).*

**Atomic Execution Sequence:**
1. Retrieve task by `taskId` from `TaskRepository`.
2. **Guard Check:** If `task == null` or `task.completed == true`, return immediately (Safe no-op ensuring AC-05 Idempotency).
3. Call `NotificationScheduler.cancelTaskAlarms(context, taskId)` to clear pending alarms.
4. **Recurrence Handling:** If `task.isRecurring == true`, calculate the next due date and persist a new active task via `TaskRepository.insertTask()`.
5. Update the existing task state to `completed = true` via `TaskRepository.updateTask()`.
6. Invoke `DueSoonWidgetUpdater.updateAll(context)` to force an immediate re-render of the widget, removing the completed task.

## 3. Glance ActionCallback & RemoteViews Hierarchy
The widget UI is composed of standard Glance components, but interaction targets are split.

- **Class:** `CompleteTaskActionCallback : ActionCallback`
- **Parameters:** Utilizes `ActionParameters.Key<Long>("taskId")` to securely pass the target task ID from the UI to the background callback.

**Touch Target Separation:**
- **Checkbox / Complete Icon:** 
  - Rendered with a minimum 48dp hitbox.
  - Action: `actionRunCallback<CompleteTaskActionCallback>(parametersOf(taskIdKey to task.id))`.
- **Task Body Container:** 
  - Action: `actionStartActivity<MainActivity>()` (with an optional `EXTRA_TASK_ID` to open specific task details in the future).
- **Header Layout:** 
  - Includes App Title/Icon.
  - Optional refresh status indicator.
  - `(+)` Quick Add button.

## 4. Quick Add Intent Architecture
To support sub-second task creation straight from the home screen, the widget header includes a Quick Add routing mechanism.

- **Action Constant:** `com.duesoon.app.action.QUICK_ADD`
- **PendingIntent Configuration (Glance Header):**
  ```kotlin
  actionStartActivity(
      Intent(context, MainActivity::class.java).apply { 
          action = ACTION_QUICK_ADD
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP 
      }
  )
  ```
- **MainActivity Handling:**
  - `MainActivity` inspects the Intent action during `onCreate()` and `onNewIntent()`.
  - If `ACTION_QUICK_ADD` is detected, `MainActivity` updates a UI state channel (e.g., `showAddTaskBottomSheet = true` in `HomeViewModel` or a root Compose state).
  - This completely bypasses the user having to manually tap the floating action button, placing them instantly into the input flow.

## 5. Verification & Test Strategy

### 5.1. Unit Test Plan
A dedicated test suite for `CompleteTaskUseCaseTest` will be authored to verify atomic behavior without needing UI components:
- **Normal Completion:** Completing a non-recurring task safely cancels alarms and updates Room to `completed = true`.
- **Recurring Completion:** Completing a recurring task cancels current alarms, marks the old task as complete, and successfully spawns a new active task for the next interval.
- **Idempotency Guard:** Invoking the use-case with an already completed task ID (simulating a double-tap race condition) does not trigger redundant alarm updates or duplicate recurrence creation.

### 5.2. Integration / Instrumentation Plan
- **Intent Handling:** Ensure that launching `MainActivity` with `ACTION_QUICK_ADD` correctly surfaces the task creation dialog in Compose.
- **Glance Constraints:** Verify that the widget correctly renders exactly 3 tasks under extreme edge cases (e.g., 50 pending critical tasks) and that the Complete touch target does not overlap with the main body touch target on dense displays.

---
*No database schema migrations are permitted. Ensure Room schema (`3.json`) and `PortableBackup` remain unaltered.*
