# Product Requirements Document: DueSoon v1.6.0 (Widgets & Quick Actions)

**Target Release:** v1.6.0
**Status:** DRAFT
**Date:** 2026-09-18

## 1. Vision & Objective
Expand DueSoon's Glance Widget from a passive display into a highly efficient, interactive cockpit for managing point-in-time deadlines. The primary objective is to allow users to acknowledge, complete, and add tasks in under 1 second without opening the full application, maintaining DueSoon's minimalist and fast interaction philosophy.

## 2. Scope Matrix

### In-Scope
- **Interactive "Complete" Action:** Add a checkbox or icon button to each widget task item, driven by a Glance `ActionCallback`.
- **Complete Side-Effect Orchestration:** 
  - Automatically cancel scheduled alarms via `NotificationScheduler`.
  - Automatically calculate and generate the next recurring occurrence if `isRecurring == true`.
- **Idempotency:** Prevent race conditions (e.g., rapid double-taps on the complete button) by ensuring state mutation is idempotent.
- **Quick Add Routing:** Add a "+" button to the widget header. This button will fire an explicit Intent (`com.duesoon.app.action.QUICK_ADD`) that routes directly to `MainActivity` and immediately opens the task creation dialog.
- **Reactive Updates:** The widget must re-render immediately following any database mutation to provide instant visual feedback.
- **Capped Layout:** Maintain a capped, static list (e.g., maximum 3-4 tasks) utilizing the existing `AttentionRankingEngine` to determine the most urgent tasks.

### Out-of-Scope
- In-widget text editing, renaming, date rescheduling, or category assignment.
- Scrollable lists within RemoteViews (to avoid launcher fragmentation and rendering issues).
- Background polling workers or continuous data refresh loops (to preserve battery life).
- Any Room database schema migrations or changes to the `PortableBackup` JSON schema.

## 3. Product Rules & Logic

### 3.1. Quick Complete Flow
- **Trigger:** User taps the complete checkbox on a specific widget task.
- **Execution:** 
  - The Glance `ActionCallback` receives the interaction and queries the domain logic.
  - **If Active (`completed == false`):** The task is marked as `completed = true`. Any pending alarms are instantly canceled. If the task is recurring, the next instance is spawned based on its recurrence interval.
  - **If Completed (Race Condition):** If the database indicates the task is already completed, the callback performs a safe no-op.
- **Feedback:** The widget UI is immediately re-rendered, dropping the completed task and shifting the next ranked task into view.

### 3.2. Quick Add Routing
- **Trigger:** User taps the (+) button in the widget header.
- **Execution:** Fires an explicit Intent (`com.duesoon.app.action.QUICK_ADD`).
- **Routing:** `MainActivity` intercepts the Intent during launch or via `onNewIntent`. It bypasses the standard idle state and directly invokes the Compose `AddTaskBottomSheet` (or equivalent dialog), placing the user immediately into the creation flow with keyboard focus ready.

### 3.3. Visual Presentation
- **Task Item Layout:** Must include a minimum 48dp touch target for the checkbox/complete action, followed by the Task Title, and the Urgency Badge/Deadline Label.
- **Urgency & Sorting:** The widget strictly inherits its sort order and color coding from the `AttentionRankingEngine` (e.g., Critical/Overdue = Red, High/Elevated = Orange, Normal = Gray).

## 4. Acceptance Criteria (AC)

- **AC-01 (Direct Completion):** Tapping the complete button on a widget item marks it as completed in the Room database without launching the full application.
- **AC-02 (Recurrence Parity):** Completing a recurring task directly from the widget successfully generates the next occurrence, matching in-app behavior.
- **AC-03 (Alarm Cancellation):** Completing a task from the widget immediately cancels all associated pending alarms via `NotificationScheduler`.
- **AC-04 (Immediate Re-render):** The widget UI refreshes reactively and immediately post-completion, removing the completed task and surfacing the next highest-ranked task.
- **AC-05 (Idempotency):** Rapidly tapping the complete button multiple times does not result in duplicate recurring tasks being created or application crashes.
- **AC-06 (Quick Add Deep-link):** Tapping the (+) button on the widget launches `MainActivity` and instantly displays the Add Task UI.
- **AC-07 (Ranking Consistency):** The tasks displayed in the widget remain strictly sorted and colored according to the `AttentionRankingEngine` rules.
- **AC-08 (Zero Schema Changes):** The Room database schema remains explicitly at version 3 (`3.json`), and the `PortableBackup` schema is completely untouched.
