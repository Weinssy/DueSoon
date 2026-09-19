# DueSoon v1.8.0 Baseline Audit (Search & Archive Hardening)

## 1. Release & Repository State
- **Git State:** Clean branch `main`, verified trailing from tag `v1.7.0`.
- **Version:** Verified `versionCode = 8` and `versionName = "1.7.0"` in `app/build.gradle.kts`.
- **Tests:** `./gradlew testDebugUnitTest` executed perfectly.
- **Room Schema:** Currently strictly maintained at schema version 3 (`3.json`).

## 2. Task Query & Memory Flow (DAO & Repository)
**Finding: Critical Memory Bottleneck Identified**
- Currently, `TaskDao.observeTasks()` executes `SELECT * FROM tasks ORDER BY deadline ASC`.
- This means **every single task (including hundreds of completed tasks)** is pulled from SQLite into application memory.
- `HomeViewModel` loads this massive unpaginated list into a `StateFlow` and performs filtering entirely in-memory via Kotlin collections in `HomeFilterLogic`.
- **Risk:** If a user uses the app for a year and accumulates 1,000+ completed tasks, `HomeViewModel` will constantly allocate and re-allocate thousands of objects on the heap every time the user types a character in the search bar or changes a date filter. This will inevitably lead to GC stutter and UI lag in Compose.

## 3. Search & Text Matching Infrastructure
- **Current State:** No database-level search exists. Search is executed dynamically in Kotlin via `.filter { it.title.contains(query) }`.
- **Feasibility:** Moving this to SQLite via `WHERE title LIKE '%' || :query || '%'` is extremely beneficial to reduce memory footprint. We can do this without needing FTS4 (Full-Text Search), which would require a complex table schema migration.

## 4. Archive Isolation & Bulk Management
- **Current State:** Completed tasks are visually mixed or hidden depending on UI state but still live in the primary `Flow`.
- **Proposed Solution:** 
  - Add `observeActiveTasks()` -> `WHERE completed = 0` (for the Home feed).
  - Add `observeArchivedTasks()` -> `WHERE completed = 1` (for a new Archive feed).
  - Add `deleteCompletedTasks()` -> `DELETE FROM tasks WHERE completed = 1` for bulk clearing.

## 5. Invariant & Migration Verification
- **Room Migration Safety:** Adding new `@Query` methods to `TaskDao.kt` (such as `observeActiveTasks`, `searchTasks`, and `deleteCompletedTasks`) **does NOT trigger a schema change**. The underlying table structure remains identical. Thus, we can successfully harden the search and archive infrastructure while strictly upholding the "Zero Schema Migrations (Schema 3)" invariant.
- **Backup Safety:** `PortableBackup.kt` is purely dependent on the `Task` domain model fields, which remain unchanged. Data portability is 100% safe.

## Conclusion
The baseline audit strongly supports moving forward with v1.8.0. The memory and search optimizations can be executed entirely via DAO query enhancements without violating any architectural invariants. The next step is to draft the PRD and Architecture documents to define the Archive UI and Search debounce strategies.
