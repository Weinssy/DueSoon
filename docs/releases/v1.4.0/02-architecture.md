# STEP 10.2 — Architecture Design: DueSoon v1.4.0 (Smart Attention Ranking)

**Target Release:** v1.4.0
**Status:** APPROVED

---

## 1. Executive & Architecture Summary

The objective of v1.4.0 is to implement **Smart Attention Ranking**—allowing high-priority tasks with imminent deadlines to logically surface above low-priority tasks, without disrupting the core chronological flow.

To adhere to the architectural constraints (zero database mutations, zero schema changes, read-only presentation transformation), the ranking logic will be encapsulated purely within the **Domain Layer**. Both the main application UI (`HomeViewModel` via `HomeFilterLogic`) and the Glance widget (`DueSoonWidgetUpdater`) will share this exact same domain engine, ensuring 100% sorting parity. 

## 2. Comparative Architecture Analysis

To achieve blended ranking of Deadline and Priority, three approaches were evaluated:

### Option A: Pure Mathematical Weight/Score
- **Concept:** Calculate a dynamic `Int` or `Float` score (e.g., `(time_to_deadline * weight) - (priority * modifier)`).
- **Pros:** Extremely granular sorting.
- **Cons:** "Black box" sorting. It is impossible to confidently explain to the user *why* a task is at position #3. Highly susceptible to math drift and edge-case collisions.

### Option B: Strict Tiered Comparator
- **Concept:** A massive composite comparator chain (e.g., sort by deadline, then priority, then creation date).
- **Pros:** Absolute determinism, standard implementation.
- **Cons:** Fails the PRD goal of "blending" urgency. A High Priority task due tomorrow would mathematically never beat a Low Priority task due today if deadline strictly supersedes priority in the comparator chain.

### Option C: Hybrid Urgency Matrix (Attention Tiers)
- **Concept:** Introduce an `AttentionTier` Enum. Map the combination of `DeadlineState` and `Priority` to a specific tier using an explicit matrix. Sort by Tier -> Deadline -> Priority -> Creation Date.
- **Pros:** 100% explainable (the UI can display the Tier). Deterministic. Safely blends urgency (e.g., mapping both "Today-Low" and "Soon-High" to the same `HIGH` tier, letting the deadline secondary sorter resolve the tie).
- **Cons:** Minor boilerplate to define the matrix.

**Decision:** **Option C (Hybrid Urgency Matrix)** is selected. It perfectly satisfies the PRD's requirement for "Explainable attention ranking" and "Priority interaction" while keeping the database untouched.

---

## 3. Domain Engine Specification

### Location
`com.duesoon.app.domain.util.AttentionRankingEngine`

### The `AttentionTier` Enum
```kotlin
enum class AttentionTier {
    OVERDUE,
    CRITICAL,
    HIGH,
    ELEVATED,
    NORMAL,
    OPTIONAL,
    COMPLETED
}
```

### The Urgency Matrix Logic
The engine will expose a pure, side-effect-free function:
`fun calculateTier(task: Task, currentTime: Long): AttentionTier`

**Mapping Rules:**
1. **Overdue Invariant (AC-01):** If `DeadlineState == OVERDUE`, return `AttentionTier.OVERDUE` (ignores priority).
2. **Completed:** If `completed == true`, return `AttentionTier.COMPLETED`.
3. **No Deadline (AC-02):** If `deadline == null`, return `AttentionTier.OPTIONAL`.
4. **Active Blending:**
   - `DUE_TODAY` + `HIGH`/`NORMAL` Priority ➔ `CRITICAL`
   - `DUE_TODAY` + `LOW` Priority ➔ `HIGH`
   - `DUE_SOON` + `HIGH` Priority ➔ `HIGH`
   - `DUE_SOON` + `NORMAL`/`LOW` Priority ➔ `ELEVATED`
   - `UPCOMING` + `HIGH` Priority ➔ `ELEVATED`
   - `UPCOMING` + `NORMAL`/`LOW` Priority ➔ `NORMAL`

### Deterministic Sort Chain
In the presentation layer, the final list will be sorted using standard Kotlin comparators to guarantee determinism (AC-03):
```kotlin
tasks.sortedWith(
    compareBy<Task> { AttentionRankingEngine.calculateTier(it, currentTime).ordinal }
        .thenBy(nullsLast()) { it.deadline }
        .thenByDescending { it.priority.ordinal }
        .thenBy { it.createdAt }
        .thenBy { it.id } // Final infallible tie-breaker
)
```

---

## 4. Component Parity & Shared Logic Plan

To fulfill **AC-05 (Widget Consistency)**, the deterministic sort chain above will be injected into a single, centralized function within `HomeFilterLogic`:

`fun sortTasksByAttention(tasks: List<Task>, currentTime: Long): List<Task>`

- **Home Screen:** `HomeViewModel` will route its `SortOrder.DEADLINE` stream through this single function.
- **Glance Widget:** `DueSoonWidgetUpdater` (or the respective Glance state compiler) will fetch tasks from `TaskRepository` and route them through the exact same `HomeFilterLogic.sortTasksByAttention()` function before generating the RemoteViews/Glance hierarchy. Code duplication is completely avoided.

---

## 5. UI & Presentation Contract

- **Data Immutability:** `TaskEntity` and Room DAO remain untouched. `AttentionTier` is NOT saved to the database. It is calculated strictly on the fly (JIT) during UI state emission.
- **Explainability:** The `AttentionTier` can be explicitly exposed to the UI Components (e.g., replacing or wrapping `DeadlineLabel`), allowing the UI to display badges like "Critical" or "Elevated" based on the matrix, rather than just "Due Today".

---

## 6. Verification & Test Strategy

### Unit Testing (`AttentionRankingEngineTest`)
A robust JUnit suite will test the matrix boundaries:
1. **Overdue Sanctity:** Assert that `OVERDUE` + `LOW` priority yields `AttentionTier.OVERDUE`.
2. **Matrix Blending:** Assert that `DUE_SOON` + `HIGH` priority yields the same tier (`HIGH`) as `DUE_TODAY` + `LOW` priority.
3. **Null Deadlines:** Assert that tasks without deadlines safely fallback to `OPTIONAL` tier.

### Sort Determinism (`HomeFilterLogicTest`)
1. Create a simulated list of tasks where two tasks land in the `HIGH` tier.
2. Assert that the secondary comparator (`deadline` -> `priority` -> `createdAt` -> `id`) resolves the tie precisely and consistently across 100 iterations.

### Performance
The sorting operates purely in-memory on lightweight `Task` data classes. Kotlin's `sortedWith` utilizes Timsort (O(N log N) time complexity). For typical user lists (0 - 2,000 tasks), the calculation and sort will complete in `< 5ms` on modern Android devices, safely within the UI frame budget.
