# DueSoon v1.2.0 Search Task Implementation

## 1. Objective
Implement local, in-memory title search for tasks, combined with existing status and category filters using AND logic, without introducing any database schema changes or new dependencies.

## 2. Existing implementation before change
- `HomeFilterLogic.kt` filtered tasks by `TaskStatusFilter` and a `categoryFilter` (String?).
- `HomeViewModel.kt` combined `tasks`, `statusFilter`, and `categoryFilter` flows into `filteredTasks`.
- `HomeScreen.kt` displayed a standard `TopAppBar` with just a title "DueSoon" and no search capability.
- No strings were present for search UI.

## 3. Search requirements
- Search task TITLE only.
- Case-insensitive matching.
- Update results reactively as the user types.
- Combine with existing Status and Category filters using AND logic.
- Blank/whitespace-only query acts as no search filter.
- Remain strictly in-memory.
- NO Room schema change or DAO change.
- NO new dependencies.
- Preserve existing sorting behavior.

## 4. Implementation approach
- **Domain/Logic**: Added `searchQuery: String = ""` to `HomeFilterLogic.filterTasks`. Used `trim()` on the query and filtered `task.title.contains(normalizedQuery, ignoreCase = true)`.
- **ViewModel**: Added `_searchQuery` `MutableStateFlow` to `HomeViewModel`. Added it to the `combine` flow operator for `filteredTasks`.
- **UI**: Added an `isSearching` state to `HomeScreen`. When active, `TopAppBar` shows an `OutlinedTextField` seamlessly integrated into the title area, with a Close/Clear icon. When inactive, it shows the normal title and a Search icon.
- **Resources**: Added `search_hint`, `cd_search`, and `cd_clear_search` to `strings.xml`.
- **Tests**: Re-wrote `HomeFilterLogicTest.kt` to include 11 comprehensive tests for search functionality, testing combinations with categories, statuses, case-insensitivity, and blank queries.

## 5. Files changed
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/duesoon/app/ui/home/HomeFilterLogic.kt`
- `app/src/main/java/com/duesoon/app/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/duesoon/app/ui/home/HomeScreen.kt`
- `app/src/test/java/com/duesoon/app/ui/home/HomeFilterLogicTest.kt`

## 6. UI changes
- Added a minimal M3 search interaction in `TopAppBar`.
- Replaces title with a transparent `TextField` when search icon is clicked.
- Allows user to type and instantly filters the list below.

## 7. ViewModel changes
- Added `val searchQuery: StateFlow<String>`.
- Added `fun setSearchQuery(query: String)`.
- Updated `filteredTasks` to react to `searchQuery` changes.

## 8. Filter logic changes
- `HomeFilterLogic.filterTasks` now accepts `searchQuery` and applies an AND condition: `matchesStatus && matchesCategory && matchesSearch`.

## 9. Localization changes
- Added `search_hint` ("Cari tugas...").
- Added content descriptions `cd_search` ("Cari") and `cd_clear_search` ("Hapus pencarian").

## 10. Architecture impact
- **Minimal**. The search logic resides entirely in the ViewModel and `HomeFilterLogic` as ephemeral in-memory state. No new layers or components were introduced.

## 11. Database impact
- **NONE**. No schema or DAO changes.

## 12. Notification impact
- **NONE**.

## 13. Tests added/changed
Added tests in `HomeFilterLogicTest`:
- `filterTasks_searchBlank_returnsAllTasks`
- `filterTasks_searchExactMatch_returnsMatchedTask`
- `filterTasks_searchPartialMatch_returnsMatchedTasks`
- `filterTasks_searchCaseInsensitive_returnsMatchedTasks`
- `filterTasks_searchNoMatch_returnsEmptyList`
- `filterTasks_searchWithActiveStatus_returnsMatchingActiveTasks`
- `filterTasks_searchWithCompletedStatus_returnsMatchingCompletedTasks`
- `filterTasks_searchWithCategory_returnsMatchingTasks`
- `filterTasks_searchWithStatusAndCategory_returnsMatchingTasks`
- `filterTasks_searchSpecialCharacters_returnsMatchedTasks`
- `filterTasks_searchLongTitle_returnsMatchedTasks`

## 14. Build results
- `./gradlew testDebugUnitTest`: SUCCESS
- `./gradlew assembleDebug`: SUCCESS

## 15. Manual verification checklist
- [x] Blank query returns all applicable tasks
- [x] Case-insensitive title match works
- [x] Search respects current status filter
- [x] Search respects current category filter
- [x] Empty state appears correctly when no tasks match

## 16. Risks
- Very large lists (10,000+ tasks) might experience minor UI stutter during fast typing due to in-memory filtering. However, for a typical local-first MVP task manager, performance will be imperceptible.

## 17. Known limitations
- Search only inspects the task title, not the description (as explicitly defined by product requirements).

## 18. Final status
- **COMPLETE**.
