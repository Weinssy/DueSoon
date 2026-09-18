# STEP 11.1 — Product Requirements Document: DueSoon v1.5.0 (Calendar & Time UX)

**Target Release:** v1.5.0
**Date:** 2026-09-18
**Status:** DRAFT

## 1. Vision & Objective
The core objective of DueSoon v1.5.0 is to introduce **Calendar & Time UX** without compromising the application's minimal, "Deadline First" philosophy. 
Users need a way to visually navigate their upcoming deadlines across a temporal grid (a calendar) to anticipate workload. However, DueSoon must avoid turning into a bloated scheduling app. The calendar will serve strictly as a visual navigation and filtering tool for point-in-time deadlines, leveraging the existing Smart Attention Ranking engine to provide at-a-glance workload density mapping.

## 2. Scope Matrix

### 2.1. In-Scope (v1.5.0)
- **Compact Calendar View:** A custom, native Jetpack Compose grid (week/month view) that is collapsible or togglable.
- **Date-Based Filtering:** Selecting a calendar date instantly filters the task list to show only tasks due on that local date.
- **Workload Density Indicators:** Visual "dots" on calendar days reflecting the highest `AttentionTier` of the tasks due on that day.
- **Quick Actions:** "Jump to Today" and "Clear Date Filter" mechanisms.
- **Timezone-Safe Local Mapping:** Strict mapping of UTC epoch timestamps to `java.time.LocalDate` using `ZoneId.systemDefault()`.
- **In-Memory Filtering:** Calendar filtering must occur in-memory on the existing `TaskDao` flow to maintain blazing fast UX without database roundtrips.

### 2.2. Out-of-Scope (Strict Non-Goals)
- ❌ **External Calendar Sync:** No integration with Google Calendar, Outlook, or CalDAV.
- ❌ **Duration/Multi-Day Events:** Tasks remain strictly point-in-time deadlines. No start-to-end spans.
- ❌ **Gantt Charts / Timelines:** No complex project management visualizers.
- ❌ **Database Schema Migrations:** No changes to Room schema 3. Zero modifications to `PortableBackup` JSON.
- ❌ **Third-Party Calendar Libraries:** No bloated external UI libraries. Only native Compose/Foundation.

## 3. Product Rules & Logic

### 3.1. Local Date Mapping
- All `deadline` fields (stored as UTC Epoch Milliseconds) must be resolved to calendar days using `ZoneId.systemDefault()`.
- A task is considered "due on a date" if its deadline falls anywhere between `00:00:00.000` and `23:59:59.999` of that local date.

### 3.2. Workload Indicator (Density Dot) Logic
Each day in the calendar grid will display at most one colored dot, derived by calculating the `AttentionTier` of all tasks due on that day and picking the *highest* severity:
- **`OVERDUE` / `CRITICAL`** ➔ Critical Color (e.g., Red / Material Theme Error).
- **`HIGH` / `ELEVATED`** ➔ Warning Color (e.g., Orange / Amber).
- **`NORMAL` / `OPTIONAL`** ➔ Secondary / Muted Color (e.g., Gray / Theme Outline).
- **No Tasks** ➔ No indicator dot.

### 3.3. Filter Interaction Flow
- **Select Date:** Tapping a date applies an active filter. The main task list instantly hides all tasks except those where the `deadline` falls on the selected local date.
- **Deselect Date (Clear):** Tapping the currently selected date again (or tapping a "Clear" button) removes the filter, returning the list to the default Smart Attention Ranking view.
- **Tasks Without Deadlines:** When a specific date filter is active, tasks with `deadline == null` are **hidden**. They are only visible in the default, unfiltered list (under the `OPTIONAL` tier).

## 4. Acceptance Criteria (AC)

- **AC-01 (Zero Dependencies):** The calendar UI is built 100% using native Jetpack Compose (`LazyVerticalGrid`, custom layouts, etc.). No third-party calendar libraries are added to `build.gradle.kts`.
- **AC-02 (Local Timezone Accuracy):** Tasks correctly align to the calendar grid strictly based on the user's active local timezone (`ZoneId.systemDefault()`), surviving DST and timezone changes.
- **AC-03 (Density Dot Accuracy):** A calendar day correctly displays the color matching the single *highest* `AttentionTier` task due on that specific date.
- **AC-04 (Filter Responsiveness):** Tapping a date applies the filter instantly in-memory via ViewModel streams. Zero `TaskDao` SQL queries are executed upon calendar interaction.
- **AC-05 (Ranking Preservation):** When viewing the filtered list for a specific date, the tasks must still strictly follow the `AttentionRankingEngine` sort order (Tie-breaking chain: `Tier` -> `Deadline` -> `Priority` -> `CreatedAt` -> `Id`).
- **AC-06 (Zero Schema Mutation):** Room version remains at `3`. DAO, Entities, and `PortableBackup` remain entirely untouched.
- **AC-07 (Smooth UI Performance):** Paging between months/weeks or toggling the calendar maintains fluid 60/120 FPS performance on moderate lists.
- **AC-08 (Widget & Notification Parity):** Interacting with or filtering the in-app calendar does **not** alter the DueSoon Glance Widget state or the `NotificationScheduler` alarm triggers.
