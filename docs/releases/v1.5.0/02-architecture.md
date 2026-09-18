# STEP 11.2 — Architecture Design: DueSoon v1.5.0 (Calendar & Time UX)

**Target Release:** v1.5.0
**Date:** 2026-09-18
**Status:** DRAFT

## 1. Executive & Architecture Overview
The v1.5.0 update introduces a native, in-memory calendar filtering system and visual density indicators directly into the DueSoon Home Screen. 

To satisfy the strict requirement of zero database migrations, all calendar calculations (grouping by date, calculating density dots) and filtering (selecting a specific date) are performed in the Presentation and Domain layers. 
The existing `TaskDao.observeTasks()` Flow remains the single source of truth. As tasks stream into `HomeViewModel`, they are mapped and grouped dynamically in-memory. This guarantees that UI state (like calendar dots) perfectly mirrors the actual rendered task list without requiring complex Room sub-queries.

## 2. State & Domain Model Specifications

### 2.1. Presentation Models
New lightweight UI state models will be placed in the presentation layer (e.g., `com.duesoon.app.ui.home.calendar`):

```kotlin
enum class DayDensityDot {
    CRITICAL, // Red (OVERDUE, CRITICAL)
    WARNING,  // Orange (HIGH, ELEVATED)
    MUTED     // Gray (NORMAL, OPTIONAL)
}

@Immutable
data class CalendarDayState(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val densityDot: DayDensityDot?
)

@Immutable
data class CalendarUiState(
    val selectedDate: LocalDate? = null,
    val currentDisplayedMonth: YearMonth,
    val isExpanded: Boolean = false // Future proofing for week vs month toggle
)
```

### 2.2. ViewModel State Flow
In `HomeViewModel`, new state variables will track calendar interactions:
- `val selectedDate = MutableStateFlow<LocalDate?>(null)`
- `val currentDisplayedMonth = MutableStateFlow<YearMonth>(YearMonth.now(ZoneId.systemDefault()))`

The existing `combine` block that yields `filteredTasks` will be updated to intersect with `selectedDate`. 

## 3. Date Mapping & Timezone Architecture

Timezone correctness is crucial. Since `deadline` is stored as UTC epoch milliseconds, all temporal mapping must use `ZoneId.systemDefault()`. 

- **Clock Invariants:** `ZoneId.systemDefault()` and `System.currentTimeMillis()` must be captured dynamically during the emission cycle of the `filteredTasks` flow or explicitly upon ViewModel action dispatch. They must *not* be cached in static singletons, preventing issues where the app stays in memory across midnight boundaries.
- **Conversion Math:** 
  ```kotlin
  val zoneId = ZoneId.systemDefault()
  val localDate = Instant.ofEpochMilli(deadline).atZone(zoneId).toLocalDate()
  ```

## 4. Workload Density & Aggregation Engine

We will build a pure, stateless domain utility: `CalendarDensityCalculator`.

- **Input:** `List<Task>`, `ZoneId`, `currentTimeMillis`.
- **Filtering Logic:** 
  - Strictly **drop** tasks where `completed == true`. Completed tasks do not contribute to upcoming workload density.
  - Strictly **drop** tasks where `deadline == null`.
- **Grouping Logic:** Group the remaining tasks by `LocalDate`.
- **Aggregation Logic:** For each `LocalDate`, evaluate the `AttentionTier` of all tasks mapped to that date using `AttentionRankingEngine.calculateTier()`. Select the most severe tier and map it to a `DayDensityDot`.

*Note on Future Dates:* As defined by the Urgency Matrix, tasks in the future (`UPCOMING`) cap out at `ELEVATED` (Warning/Orange) or `NORMAL` (Muted/Gray). A `CRITICAL` dot is only possible for `DUE_TODAY` or `OVERDUE` tasks.

## 5. Compose Calendar Grid Architecture

### 5.1. Layout Strategy
We will use a fixed layout using standard Compose `Column` and `Row` for the month/week grid rather than `LazyVerticalGrid`. 
- **Reason:** A standard month view only has 6 rows of 7 days (42 cells max). `Column`/`Row` avoids the overhead of lazy composition for small, bounded grids and ensures smoother expand/collapse animations.

### 5.2. Recomposition Optimization
- The models (`CalendarDayState`) will be marked with `@Immutable`.
- We will derive the grid structure inside `HomeViewModel` (or a dedicated `CalendarViewModel` if it gets large), passing down a simple `List<CalendarDayState>` to the Composable. 
- Avoid heavy calculations (like epoch to `LocalDate` mapping) inside the `@Composable` tree.

### 5.3. Accessibility
- All calendar day cells will have a minimum touch target of `48.dp` x `48.dp`.
- Clear semantic labels for screen readers ("15th September, 3 tasks due, selected").

## 6. Filter Integration & Backward Compatibility

The calendar filter acts as a boolean AND operator alongside existing filters:
- `searchQuery` AND `selectedCategory` AND `selectedDate`.

**Invariants:**
- If a `selectedDate` is active, tasks with `deadline == null` are unconditionally **hidden**.
- If `selectedDate` is cleared (null), the list reverts to normal (unfiltered by date), showing `deadline == null` tasks at the bottom under `OPTIONAL`.
- **Widget Independence:** The Android Glance widget queries the DAO directly (or via a repository snapshot) and applies `AttentionRankingEngine`. It has zero awareness of `HomeViewModel.selectedDate`. Calendar UI selection purely alters the local active screen state, ensuring absolute Widget parity with the *unfiltered* urgency matrix.

## 7. Verification & Test Strategy

To prevent regressions, the following tests will be implemented:

1. **`CalendarDensityCalculatorTest` (Unit)**
   - Verify completed tasks are ignored.
   - Verify tasks without deadlines are ignored.
   - Verify proper tier-to-color mapping (e.g., `HIGH` -> `WARNING`, `OVERDUE` -> `CRITICAL`).
2. **`HomeViewModelCalendarTest` (Unit)**
   - Verify that selecting a date filters out tasks outside that date.
   - Verify that toggling a date off restores the full list.
   - Verify that `null` deadline tasks disappear when a date is selected.
3. **Boundary & Timezone Tests (Unit)**
   - Verify mapping behavior across midnight boundaries.
   - Verify Leap Year conversions (`Feb 29`).
