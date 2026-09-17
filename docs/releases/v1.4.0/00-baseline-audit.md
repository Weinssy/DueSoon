# STEP 10.0 — DueSoon v1.4.0 Baseline Audit

**Target Release:** v1.4.0
**Date:** 2026-09-17
**Status:** IN PROGRESS

## Executive Summary
This document provides a read-only baseline audit of the DueSoon repository immediately following the v1.3.0 release. The goal is to accurately map the existing architecture, user experience, and regression risks to define a highly-focused, safe, and minimal scope for the v1.4.0 PRD. 

The audit reveals a stable architecture with solid separation of concerns. However, the current sorting logic strictly prioritizes deadlines over user-defined priorities, which may bury critical but slightly further-out tasks.

---

## 1. Repository and Release State

- **Branch:** `main`
- **Commit:** `4a6b5ea74c4256cc73cd3a82aa594bdb34100abf` (chore: bump version to 1.3.0)
- **Tag:** `v1.3.0`
- **Working Tree:** Clean (untracked v1.3 release notes).
- **Version Identity:** `versionCode = 4`, `versionName = "1.3.0"`
- **Schemas:** Room `3.json` is safely tracked.
- **.idea:** Successfully excluded from Git tracking.

---

## 2. Existing Architecture

- **Domain Model (`Task.kt`):** 
  Contains `priority` (LOW, NORMAL, HIGH), `reminderType`, `isRecurring`, `recurrenceInterval`, `completed`, and `snoozedUntil`.
- **DeadlineStateCalculator:** 
  Categorizes tasks temporally relative to `System.currentTimeMillis()` into `OVERDUE`, `DUE_TODAY`, `DUE_SOON` (<= 3 days), `UPCOMING`, `NO_DEADLINE`, and `COMPLETED`.
- **TaskRepository:** 
  Handles CRUD. Automatically creates the next recurrence instance when a recurring task is completed. Triggers `NotificationScheduler` and `DueSoonWidgetUpdater` upon mutations.
- **HomeViewModel & HomeFilterLogic:** 
  Uses a multi-tier sort. The default `SortOrder.DEADLINE` uses a hardcoded integer mapping: `OVERDUE` (0) -> `DUE_TODAY` (1) -> `DUE_SOON` (2) -> `UPCOMING` (3) -> `NO_DEADLINE` (4) -> `COMPLETED` (5). **Priority is only evaluated as a secondary tie-breaker.**
- **Notification Scheduling:** 
  `SmartReminderCalculator` calculates intervals (e.g., 3 days before, 1 day before, 3 hours before) based on time remaining. Drops past reminders automatically.
- **Backup & Restore (`BackupRestoreCoordinator`):** 
  Executes validated JSON imports. Destructive restore correctly clears old OS alarms before regenerating new ones from the restored database state.

---

## 3. Existing User Experience

- **Home Screen Sorting:** 
  Strict chronological deadline bucketing. A `LOW` priority task due today will always appear above a `HIGH` priority task due tomorrow. 
- **Deadline States:** 
  Color-coded efficiently: OVERDUE = Red, DUE_TODAY/DUE_SOON = Orange, COMPLETED = Green.
- **Empty States & Filters:** 
  Supports active/completed filters and category matching.
- **Recurring Tasks:** 
  Completion silently spawns the next occurrence; the user experience relies on the new task appearing in the list.

---

## 4. v1.4.0 Candidate Areas

### Better Attention Ranking (Feasible)
- **Current:** Strict deadline tiering. Priority is heavily diluted.
- **Risk:** Changing sorting behavior might disorient existing users, but is technically safe (no database migrations needed). 
- **Requirement:** Refactoring `HomeFilterLogic.sortTasks` to use a weighted scoring system rather than strict ordinal bucketing.

### Priority and Deadline Interaction (Feasible)
- **Current:** Independent variables.
- **Risk:** Low. Can be achieved entirely in the presentation/domain layer by introducing an `AttentionScore` or `UrgencyMatrix` without touching Room.

### Database/Schema Changes (Not Recommended)
- **Risk:** High. Altering the schema requires a `v4` migration and breaks the `v1.3.0` portable JSON schema unless careful mapping is added. Should be avoided for a minor release unless strictly necessary.

---

## 5. Regression Risk Review

If v1.4.0 focuses on **sorting and ranking logic**:
- **Backup/Restore:** ZERO risk. JSON serialization only cares about raw data, not how it's sorted on screen.
- **Room Migrations:** ZERO risk if no new fields are added.
- **Notification Reconciliation:** LOW risk, provided `SmartReminderCalculator` and `AndroidNotificationScheduler` are left untouched.
- **Widget:** The widget might need its internal sorting aligned with the new Home screen logic to prevent UX inconsistency.

---

## 6. Recommended Next Step

**Recommended Scope for v1.4.0 (STEP 10.1 - PRD):**
The v1.4.0 release should focus exclusively on **"Smart Attention Ranking"**. 

1. **In Scope:**
   - Overhaul `HomeFilterLogic` to blend `DeadlineState` and `Priority` into a cohesive attention score.
   - Ensure High Priority tasks aren't buried beneath Low Priority tasks just because of a minor deadline difference.
   - Update UI labels to reflect this urgency (e.g., highlight High Priority tasks visually).
   - Sync widget sorting with main app sorting.

2. **Out of Scope (Do Not Touch):**
   - No database migrations (Stay on Room v3).
   - No changes to the `PortableBackup` JSON schema.
   - No changes to AlarmManager/Notification execution logic.
   - No cloud features.

**Why?**
This is minimal, highly explainable, requires no AI bloat, respects DueSoon's deadline-first purpose, and carries zero data-loss or schema-corruption risk following the massive v1.3.0 architectural shift.

---

## 7. Verification

The following read-only commands were executed to verify the baseline:

- **Source Inspection:** Verified Git tree (`git status`), commit `4a6b5ea7`, and `.idea` exclusion (`git ls-files .idea`). Inspected domain logic (`Task.kt`, `DeadlineStateCalculator.kt`, `HomeFilterLogic.kt`).
- **Unit Test Execution:** `./gradlew testDebugUnitTest` executed successfully (BUILD SUCCESSFUL in 41s).
- **Runtime Testing:** Skipped intentionally (Read-only baseline audit, UI execution not required to prove domain sorting logic).
