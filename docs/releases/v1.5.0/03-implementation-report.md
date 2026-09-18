# STEP 11.3 — Core Calendar Domain & Density Engine Implementation Report (DueSoon v1.5.0)

**Target Release:** v1.5.0
**Date:** 2026-09-18
**Status:** COMPLETED

## 1. Implementation Summary
This phase successfully implemented the core presentation state models and the Domain layer aggregation engine (`CalendarDensityCalculator`) required to power the Calendar & Time UX in DueSoon v1.5.0. 
The implementation strictly adheres to the rule of maintaining all logic in memory and preserving the existing Room schema without any migrations.

## 2. Completed Items

### 2.1. Presentation State Models
The following immutable data classes and enums were created in `com.duesoon.app.ui.home.calendar`:
- **`DayDensityDot`:** Defines the three required visual states (`CRITICAL`, `WARNING`, `MUTED`).
- **`CalendarDayState`:** Encapsulates the UI state of a single cell in the calendar grid.
- **`CalendarUiState`:** Wraps the entire state of the calendar view, including the `selectedDate` and `currentDisplayedMonth`.

### 2.2. Calendar Density Engine
Implemented `CalendarDensityCalculator.calculateDayDots()` in `com.duesoon.app.domain.util`.
- **Logic Validation:** It successfully filters out all tasks where `completed == true` or `deadline == null` before grouping by date.
- **Timezone Safety:** Tasks are correctly mapped from their UTC epoch milliseconds to a `LocalDate` using the provided `ZoneId`.
- **Urgency Mapping:** Uses the existing `AttentionRankingEngine.calculateTier` to evaluate each task, then applies `minOfOrNull` to find the most severe tier for that specific day. This tier is then mapped to the appropriate `DayDensityDot`.

## 3. Testing & Verification
A comprehensive test suite was written at `CalendarDensityCalculatorTest.kt`. All edge cases defined in the Architecture Design were validated:
- ✅ **Completed tasks** are correctly ignored and do not generate a dot.
- ✅ **Undated tasks** (`deadline == null`) are safely ignored.
- ✅ **Same-date multiple tasks** correctly resolve to the highest severity dot (e.g., if a date has both `NORMAL` and `HIGH` tasks, the dot becomes `WARNING`).
- ✅ **Timezone Shifts:** A single UTC timestamp was tested against `ZoneId.of("UTC")` and `ZoneId.of("Asia/Tokyo")` to guarantee the calculation dynamically maps to different calendar days depending on the provided local timezone.

The test suite executed successfully (`./gradlew testDebugUnitTest` ran in 4s).

## 4. Next Steps
With the core domain logic established and rigorously verified, the pipeline is ready. The next step is to integrate `CalendarUiState` into `HomeViewModel` and wire the boolean filtering logic for `selectedDate` against the main `TaskDao.observeTasks()` Flow (STEP 11.4).
