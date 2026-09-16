# STEP 8.4 — Sorting Implementation

## 1. Status
PASS

## 2. Scope
Implemented user-selectable task sorting within the in-memory filtering pipeline.
Sorting operates on the fully filtered task list. No persistence (DataStore or Room) was added for the sorting state. Recurring UI polish and snooze were explicitly NOT implemented.

## 3. Sorting Design
- **SortOrder**: An enum class `SortOrder` was defined with options `DEADLINE`, `PRIORITY`, `TITLE`, and `CREATED`.
- **Deadline sorting (Default)**: Uses the exact existing `DeadlineStateCalculator` logic to group tasks logically (OVERDUE, DUE_TODAY, etc.), with tie-breakers on absolute deadline, priority (descending), and finally created date. This preserves the existing "Needs Attention" behavior exactly as the product defined.
- **Priority sorting**: Orders tasks strictly by Priority descending (HIGH -> NORMAL -> LOW), falling back to deadline (nulls last) and creation date for tie-breakers.
- **Title A-Z sorting**: Orders tasks alphabetically, case-insensitive. Tie-breakers are priority descending and then created date.
- **Created newest sorting**: Orders tasks by `createdAt` descending. Tie-breakers are deadline and then priority.
- **Tie-breaker behavior**: All sorts have deterministic tie-breakers relying on the other fields.

## 4. Filter + Sorting Pipeline
The filtering logic runs synchronously and independently of DAO queries.
The sequence is:
1. Search filter
2. Status filter
3. Category filter
4. Sorting

Example: Searching "laporan" with active status and 'Kerja' category filters down the list. The resulting subset is then cleanly sorted by the selected `SortOrder` in the `HomeViewModel`'s `combine` block without requiring further database calls.

## 5. UI
The sort control is a minimal Material 3 `IconButton` (using `Icons.Filled.MoreVert` as the standard sort/more menu icon) inside the `TopAppBar`, available alongside the Search icon. Clicking it expands a `DropdownMenu` showing the four sorting options. The currently active option is highlighted using the primary color. It is quiet and unintrusive.

## 6. Localization & Accessibility
Strings added in `strings.xml`:
- `sort_title_label`: Urutkan
- `sort_deadline`: Tenggat
- `sort_priority`: Prioritas
- `sort_title`: Judul A-Z
- `sort_created`: Terbaru dibuat
- `cd_sort`: Urutkan (used as content description for the sort icon).

## 7. Files Changed
- `app/src/main/res/values/strings.xml`: Added strings for SortOrder enum and UI elements.
- `app/src/main/java/com/duesoon/app/ui/home/HomeFilterLogic.kt`: Extracted the `sortTasks` function to apply pure functional sorting logic outside of the ViewModel/UI. Defined `SortOrder` enum.
- `app/src/main/java/com/duesoon/app/ui/home/HomeViewModel.kt`: Added `_sortOrder` state and incorporated it into the `filteredTasks` combine block.
- `app/src/main/java/com/duesoon/app/ui/home/HomeScreen.kt`: Added the sort icon and `DropdownMenu` to the `TopAppBar` actions.
- `app/src/test/java/com/duesoon/app/ui/home/HomeFilterLogicTest.kt`: Added unit tests for each SortOrder option.

## 8. Database Impact
UNCHANGED / NO MIGRATION
- Room schema and DAO are untouched.

## 9. Notification Impact
UNCHANGED
- Scheduler and alarms remain completely untouched.

## 10. Tests
- Added 4 test cases covering each sorting option: `sortTasks_deadline_sortsCorrectly`, `sortTasks_priority_sortsHighToLow`, `sortTasks_title_sortsAToZ`, `sortTasks_created_sortsNewestFirst`.
- Command: `./gradlew testDebugUnitTest`
- Result: SUCCESS (43 tests total passed).

## 11. Build Verification
- Command: `./gradlew assembleDebug`
- Result: SUCCESS

## 12. Risks / Known Issues
None. The fallback sort options are deterministic. Using in-memory sorting avoids DAO overhead for state changes, perfectly matching the local-first nature without any observable lag on standard datasets.

## 13. Final Verification
- Sorting works: Yes.
- Search still works: Yes.
- Status/category filters still work: Yes.
- No recurring changes: Verified.
- No snooze changes: Verified.
- No DB changes: Verified.
- No notification changes: Verified.

## 14. Git Status
- commit made: NO
- tag created: NO
- working tree state: Modified only the 5 required files.
