# Architecture Design: DueSoon v1.8.0 (Search & Archive Hardening)

## 1. Architectural Overview & Memory Separation
DueSoon v1.8.0 redesigns the internal data pipeline to eliminate extreme memory pressure on the Home screen caused by an infinitely growing historical task dataset. 

- **v1.7.0 Monolithic Stream (Legacy):** The entire database, including hundreds of inactive/completed tasks, was loaded into a single `Flow<List<Task>>` and filtered entirely in Kotlin memory using `.filter {}`. This resulted in heavy garbage collection and potential UI stutter during search operations.
- **v1.8.0 Isolated Streams:** The data pipeline is split at the SQLite boundary. The `HomeViewModel` subscribes strictly to active tasks, keeping its `StateFlow` lightweight and bounded. The new `ArchiveViewModel` queries only completed tasks dynamically, strictly on demand, ensuring zero memory bleed into daily operations.
- **Migration Verification:** Adding new `@Query` methods to the DAO strictly constitutes a non-breaking query modification. It does NOT alter the underlying table structure, thus safely preserving the Room schema version 3 (`3.json`) invariant.

## 2. Data Access Layer (DAO & Repository Updates)
### `TaskDao.kt`
The DAO is augmented with targeted, pre-filtered SQL queries to leverage database-level filtering and search capabilities.

```kotlin
@Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY deadline ASC")
fun observeActiveTasks(): Flow<List<TaskEntity>>

@Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY deadline DESC")
fun observeArchivedTasks(): Flow<List<TaskEntity>>

@Query("SELECT * FROM tasks WHERE completed = 1 AND title LIKE '%' || :query || '%' ORDER BY deadline DESC")
fun searchArchivedTasks(query: String): Flow<List<TaskEntity>>

@Query("DELETE FROM tasks WHERE completed = 1")
suspend fun deleteCompletedTasks(): Int
```

### `TaskRepository.kt` & `TaskRepositoryImpl.kt`
The repository interface routes these new query pipelines securely to the domain logic.
```kotlin
fun observeActiveTasks(): Flow<List<Task>>
fun observeArchivedTasks(): Flow<List<Task>>
fun searchArchivedTasks(query: String): Flow<List<Task>>
suspend fun clearArchive(): Int
```

## 3. UI State & Domain Orchestration
### `HomeViewModel` (Active Focus)
- `tasks` `StateFlow` will be migrated from `repository.observeTasks()` to `repository.observeActiveTasks()`.
- Active search will utilize Kotlin Flow operators (`debounce(300L)`) combined with `combine` to filter the *active list only* in memory. 

### `ArchiveViewModel` (Historical Querying)
- Operates independently from `HomeViewModel`.
- Utilizes `MutableStateFlow<String>` for the search query.
- The `searchQuery` is debounced by `300ms`, mapped using `flatMapLatest` (or similar reactive pattern) to emit `searchArchivedTasks(query)` or fallback to `observeArchivedTasks()` when the query is blank.

## 4. UI/Compose Architecture
- **Search Component:** Implement a unified `SearchBar` composable supporting a debounced text input and a trailing clear (`X`) icon.
- **Archive Route:** Establish a dedicated `/archive` route in Compose Navigation (or a Bottom Sheet, depending on final UX), accessible from the main Home settings header.
- **Bulk Clear Modal:** Implement a standard `AlertDialog` forcing an explicit user confirmation prior to executing `clearArchive()`.

## 5. Reminder & Restoration Logic
When a user restores a task from the Archive (`completed = false`):
1. **Database Update:** The task is updated in SQLite via `repository.updateTask()`.
2. **Re-routing:** It immediately vanishes from `observeArchivedTasks()` and reappears in `observeActiveTasks()`.
3. **Alarm Evaluation:** The `NotificationScheduler` (or equivalent domain mechanism) is immediately invoked to re-calculate and re-schedule alarms for this task if its updated deadline resides in the future, perfectly integrating with the existing v1.7.0 Smart Reminder logic.
