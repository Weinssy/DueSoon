# STEP 11.0 — DueSoon v1.5.0 Baseline Audit (Calendar & Time UX)

**Target Release:** v1.5.0
**Date:** 2026-09-18
**Status:** COMPLETED

## Objective
This document serves as a read-only architectural baseline audit of the repository immediately following the v1.4.0 release. It maps existing date/time infrastructure, query structures, and UI capabilities to establish safe architectural boundaries for developing **v1.5.0 — Calendar & Time UX**.

---

## 1. Repository & Release State
- **Git State:** `main` branch is clean.
- **Version Metadata:** `app/build.gradle.kts` is correctly set to `versionCode = 5`, `versionName = "1.4.0"`.
- **Database Schema:** Room schema remains on version 3 (`3.json` is authoritative).
- **Tests:** The test suite (`./gradlew testDebugUnitTest`) successfully passes locally in 2 seconds, verifying a stable baseline.

## 2. Existing Date & Time Infrastructure

### 2.1. Domain Model (`Task.kt` / `TaskEntity.kt`)
- All temporal fields (`deadline`, `createdAt`, `updatedAt`, `snoozedUntil`) are represented as `Long` (Unix Epoch Millis in UTC).
- Tasks are strictly "point-in-time" items; there is no representation of "duration" or "start-to-end" bounds.

### 2.2. Temporal Logic (`DeadlineStateCalculator.kt`)
- Uses `java.util.Calendar` to calculate absolute bucketing (`OVERDUE`, `DUE_TODAY`, `DUE_SOON`, `UPCOMING`).
- **Risk:** Time bucketing relies on simple millisecond-to-days division (`diff / (1000 * 60 * 60 * 24)`), which handles typical time ranges well but might need timezone awareness refinement if calendar-based filtering spans days strictly based on local time.

### 2.3. User Interface (`DateTimePickerDialog.kt`)
- Employs native Jetpack Compose Material 3 `DatePickerDialog` (for date) chained into an `AlertDialog` containing a `TimePicker` (for time).
- Implemented as a simple sequential wizard. 

## 3. Query & Repository Capabilities
- **Current Approach:** `TaskDao.kt` contains a single `observeTasks()` query (`SELECT * FROM tasks ORDER BY deadline ASC`) that loads all tasks into a single Flow.
- **Calendar Query Feasibility:** There are currently no indexed date-range queries (`WHERE deadline BETWEEN :start AND :end`).
- **Architectural Conclusion:** Because DueSoon is a minimalist app, the total number of non-archived tasks will likely remain small. A month/week calendar view can safely perform in-memory grouping and filtering on the existing `observeTasks()` Flow without requiring new DAO queries or Room schema changes.

## 4. Candidate Areas for v1.5.0 (Calendar & Time UX)

Based on the baseline constraints, the following features are viable and architecturally safe for v1.5.0:

1. **Compact Calendar View:** 
   - A custom Jetpack Compose grid implementation for week/month views.
   - **Why Custom?** Adhering to the core rule of zero unnecessary dependencies, native Compose `LazyVerticalGrid` or custom layout logic is preferred over heavy third-party calendar libraries.
2. **Date-Based Filtering:**
   - Selecting a date on the calendar will filter the main task list to show only tasks due on that local date.
3. **Density Indicators / Workload Dots:**
   - Using the existing `AttentionRankingEngine.calculateTier()`, the calendar can group tasks by date and color a dot using the *highest* `AttentionTier` for that day, providing an at-a-glance workload map.
4. **UX Refinements:**
   - Streamlining how users pick dates directly from the calendar or improving the existing `DateTimePickerDialog` flow.

## 5. Scope Boundaries & Non-Goals

To maintain the local-first, minimal nature of DueSoon, the following features are strictly **OUT OF SCOPE** for v1.5.0:
- ❌ External calendar syncing (e.g., Google Calendar APIs, Outlook sync).
- ❌ Multi-day tasks or events with start/end durations.
- ❌ Complex Gantt charts or timeline UI blocks.
- ❌ Database migrations or new indexed DAO queries (relying on in-memory mapping).
- ❌ Re-architecting the alarm manager scheduling system.
