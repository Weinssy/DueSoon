# Product Requirements Document (PRD): DueSoon v1.8.0

## 1. Vision & Objective
- **Memory Optimization:** Eliminate Garbage Collection (GC) and rendering overhead by cleanly separating active deadline tracking from historical completed task storage.
- **Fast Search Experience:** Provide a fast, responsive search experience across both active tasks (via in-memory debounce) and archived records (via direct SQLite querying), ensuring high UI fluidity.
- **Data Integrity & Isolation:** Ensure that historical records do not compromise the performance of daily active tracking, keeping DueSoon minimal, local-first, and highly reliable.

## 2. Scope Matrix

### In-Scope
- **Dedicated DAO Queries:** Implement `observeActiveTasks()` (`WHERE completed = 0`) and `observeArchivedTasks()` (`WHERE completed = 1`) in `TaskDao`.
- **Search UX:** Introduce a debounced search input (250–300ms) equipped with an instant clear (`X`) button.
- **Archive Management UI:** Create a separate Archive Screen / Bottom Sheet as a dedicated UI to view past completed tasks. Allow users to restore a task (`completed = false`) or permanently delete individual tasks.
- **Bulk Archive Cleanup:** Add a "Clear Archive" action protected by a mandatory destructive confirmation dialog. This triggers a bulk delete command at the SQLite level.
- **Category Filtering:** Enable category tag filtering to work alongside the search query seamlessly.

### Out-of-Scope
- **FTS (Full-Text Search):** Virtual tables for FTS are excluded to strictly prevent schema migrations to Room v4.
- **Search Indexing on Unstructured Data:** Indexing on fields like task description or notes is out of scope if they are not already part of schema 3.
- **Export Format Changes:** Generating CSV or PDF exports remains out of scope. The existing `PortableBackup` JSON structure remains the absolute standard.

## 3. Product Rules & Logic

### Home Feed Isolation
- **Active Task Stream:** The Home feed is strictly bound to `observeActiveTasks()`. Completed tasks are completely purged from the primary active memory pipeline.
- **Search Mechanics:** Searching on the Home feed applies an in-memory filter to the already fetched active list. The filtered outcome strictly respects the `AttentionRankingEngine` ordering.

### Archive Feed Rules
- **Access Point:** Accessible via a dedicated route, triggered from the Home header or app settings.
- **Search Mechanics:** Search queries in the Archive view bypass in-memory Kotlin matching and directly query SQLite using `LIKE` pattern matching to accommodate large historical datasets.
- **Action Operations:**
  - **Restore:** Marks the task as active (`completed = false`), returning it to the Home feed, and actively reschedules its alarms if the deadline is in the future.
  - **Delete:** Permanently erases the task from the database.

### Bulk Clear Archive
- **Destructive Gateway:** Tapping "Clear Archive" immediately invokes a destructive confirmation modal to prevent accidental data loss.
- **Execution:** Upon confirmation, a direct execution of `deleteCompletedTasks()` is called in Room, selectively purging archived tasks while leaving active tasks completely untouched.

## 4. Acceptance Criteria (AC)

- **AC-01 (Feed Isolation):** Home screen `StateFlow` receives and emits only tasks where `completed == false`.
- **AC-02 (Archive Access):** Users can navigate to a dedicated Archive view that displays all `completed == true` tasks, ordered by completion date or deadline descending.
- **AC-03 (Task Restoration):** Restoring a task from the Archive sets `completed = false`, re-surfaces it in the Home feed, and correctly reschedules any relevant alarms via `NotificationScheduler` if its deadline is set in the future.
- **AC-04 (Search Debounce):** Active search queries are debounced by 250–300ms, effectively eliminating redundant UI recomposition passes during rapid typing.
- **AC-05 (Archive Search Accuracy):** Typing a query inside the Archive correctly surfaces matching completed tasks using case-insensitive partial text matching.
- **AC-06 (Bulk Delete Safety):** Executing the bulk clear archive action permanently removes **only** completed tasks, and absolutely requires explicit user confirmation before processing.
- **AC-07 (Zero Schema Migration):** The Room database must strictly remain on schema version 3 (`3.json`). No schema bumps are permitted.
- **AC-08 (Backup Interoperability):** Backups created in DueSoon v1.8.0 can be restored cleanly on older application versions, and conversely, legacy backups must restore seamlessly on v1.8.0 without any loss of data integrity.
