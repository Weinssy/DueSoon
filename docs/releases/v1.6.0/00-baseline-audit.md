# DueSoon v1.6.0 Baseline Audit (Widgets & Quick Actions)

**Target Release:** v1.6.0
**Baseline:** v1.5.0
**Date:** 2026-09-18
**Status:** COMPLETED

## 1. Repository & Release State
- **Git Branch:** `main` (Clean at `v1.5.0` tag representation).
- **Version Metadata:** Verified `versionCode = 6` and `versionName = "1.5.0"` in `app/build.gradle.kts`.
- **Database Schema:** SQLite Room schema remains pristine at version 3 (`3.json`).
- **Tests:** The `testDebugUnitTest` suite executes successfully (100% pass rate).

## 2. Current Glance Widget Architecture
The `DueSoonWidget` currently utilizes AndroidX Glance to render a read-only list.
- **Data Flow:** Retrieves data securely via `app.container.taskRepository.observeTasks().first()`.
- **Sorting Logic:** Correctly delegates to `HomeFilterLogic.sortTasksByAttention(..., currentTime)`.
- **Rendering Limits:** It is safely constrained via `.take(3)` to prevent `RemoteViews` payload bloat.
- **Interactivity:** Currently, the entire widget surface uses `actionStartActivity<MainActivity>()`. There are no granular interactive elements (e.g., checkboxes, add buttons) implemented yet.

## 3. Quick Actions Feasibility & Mutation Flow

### 3.1. Quick Complete (ActionCallback)
Implementing a direct "Complete" checkbox on the widget is feasible via a Glance `ActionCallback`. 
**Required Execution Flow:**
1. Action receives the `taskId`.
2. Locates the task via `TaskRepository`.
3. If `isRecurring == true`, computes the next recurrence interval (this logic must be pure or extracted to a domain use-case accessible from the Widget context).
4. Persists the `completed = true` (or new recurrence) state to Room.
5. Invokes `NotificationScheduler.cancelTaskAlarms(context, taskId)` to clear pending alarms.
6. Invokes `DueSoonWidgetUpdater.updateAll(context)` to refresh the widget UI immediately.

### 3.2. Quick Add (Intent Redirection)
Implementing a "+" button to instantly open the Add Task dialog requires deep-linking or intent routing.
**Required Execution Flow:**
1. Glance `actionStartActivity<MainActivity>()` configured with an explicit intent action (e.g., `com.duesoon.app.ACTION_ADD_TASK`).
2. `MainActivity` captures this intent within its `onCreate` / `onNewIntent` lifecycle.
3. Propagates the intent via a Compose `LaunchedEffect` or ViewModel state to immediately surface the `AddTaskDialog`.

## 4. Glance Limitations & Performance Constraints
- **Race Conditions (Rapid Tapping):** Because `ActionCallback` execution is asynchronous, users might double-tap a checkbox before the widget UI refreshes. We must ensure the repository mutation logic is idempotent (e.g., marking an already completed task as completed is safe).
- **Size Constraints:** `.take(3)` is currently hardcoded. When introducing interactive buttons, we must ensure the `Row` height doesn't overflow standard 2x2 or 3x2 widget cell bounds on dense launchers.
- **WorkManager Limits:** While Glance uses WorkManager internally for updates, direct SQLite mutations from the Callback are typically synchronous enough for simple CRUD without requiring custom Coroutine Workers, provided they run on `Dispatchers.IO`.

## 5. Scope Boundaries & Non-Goals for v1.6.0
The following items remain strictly **OUT OF SCOPE** to maintain the minimal, offline-first philosophy:
- **Full In-Widget Editing:** Tapping a task to rename or change its date entirely within the widget (too complex for RemoteViews; should just open the app).
- **Scrollable Lists:** Due to Glance/RemoteViews scroll limitations and fragmentation across launchers, the list will remain statically capped (e.g., max 3-5 tasks).
- **Room Schema Migrations:** No new fields (like `widgetOrder`) are needed. We rely purely on the existing `AttentionRankingEngine`.
- **Background Polling:** The widget will update strictly reactively (when DB changes or via standard Glance lifecycle), not via aggressive polling alarms.
