# STEP 8.5 — Recurring UI Polish

## 1. Status
PASS

## 2. Scope
- Implemented UI polish for the recurring tasks feature, integrating it into the `CreateTaskScreen` and `EditTaskScreen`.
- Ensured recurrence is disabled by default for new tasks.
- If a user removes a deadline from a recurring task, the recurring state is automatically disabled.
- Ensured existing intervals are loaded correctly when editing a recurring task.
- Fixed the UI strings for recurrence indicators and interval buttons to be clean, accessible, and correctly use Compose icons or emojis instead of broken characters.
- Did NOT modify Room schema, DB, DAOs, or notification/alarm structures.
- Did NOT add snooze functionality.
- Did NOT create new recurrence types.

## 3. Existing Recurring Architecture
The existing data model (`isRecurring`, `recurrenceInterval`), `DAILY`, `WEEKLY`, `MONTHLY` intervals, and `TaskRepository` behavior are completely preserved. Upon completing a recurring task, the repository calculates the next deadline correctly and automatically persists the next occurrence while scheduling notifications.

## 4. Create Task UI
Recurrence is toggled via the `RecurrenceSelector`. If the deadline is not set, the recurrence selector switch is disabled and visually muted with an explanatory subtext ("Membutuhkan tenggat"). By default, recurrence is disabled. When enabled, it allows picking an interval (Daily, Weekly, Monthly) via `FilterChip`s.

## 5. Edit Task UI
Editing a recurring task correctly loads the existing `isRecurring` and `recurrenceInterval` states. Disabling recurrence persists. If the deadline is removed during editing, recurrence is automatically disabled to prevent logic errors. The task update correctly writes back all fields to Room. Unrelated fields being updated will not affect the recurrence state.

## 6. Recurrence Types
Supported intervals strictly remain:
- DAILY ("Setiap hari")
- WEEKLY ("Setiap minggu")
- MONTHLY ("Setiap bulan")

## 7. Completion / Next Occurrence
The existing `TaskRepository.updateTask` completion logic remains intact. When marked as completed, a new instance with a newly calculated deadline is created and scheduled. The current instance's notifications are cleared.

## 8. Recurring Indicators
`TaskCard` utilizes a small label indicating recurrence alongside priority tags using a clean format: `• 🔄 [Interval]`. The broken strings were fixed in `strings.xml`. It remains quiet and unobtrusive, preserving the "Deadline should be obvious" principle.

## 9. Localization & Accessibility
All new labels and indicators leverage `strings.xml`. Added:
- `recurrence_switch_title` ("Berulang")
- `recurrence_switch_subtitle` ("Tugas akan dibuat ulang saat selesai")
- `recurrence_requires_deadline` ("Membutuhkan tenggat")
- Fixed encoding on existing `recurrence_indicator` and `recurrence_tag`.

## 10. Files Changed
- `app/src/main/res/values/strings.xml`: Localized strings updated and fixed broken characters.
- `app/src/main/java/com/duesoon/app/ui/components/RecurrenceSelector.kt`: Added `enabled` state property and fallback subtitle to explain deadline requirement.
- `app/src/main/java/com/duesoon/app/ui/task/CreateTaskViewModel.kt`: Clear `isRecurring` to false when deadline is removed.
- `app/src/main/java/com/duesoon/app/ui/task/EditTaskViewModel.kt`: Clear `isRecurring` to false when deadline is removed.
- `app/src/main/java/com/duesoon/app/ui/task/CreateTaskScreen.kt`: Bound `deadline != null` to `RecurrenceSelector`'s `enabled` prop.
- `app/src/main/java/com/duesoon/app/ui/task/EditTaskScreen.kt`: Bound `deadline != null` to `RecurrenceSelector`'s `enabled` prop.

## 11. Database Impact
UNCHANGED / NO MIGRATION
- Room entities, DAOs, and database version are untouched.

## 12. Notification Impact
UNCHANGED
- Scheduler and alarms remain exactly as implemented.

## 13. Search & Sorting Compatibility
- STEP 8.3 (Search) remains fully functional.
- STEP 8.4 (Sorting) remains fully functional.
Recurring tasks are standard `Task` entities and interact perfectly with the in-memory filtering pipeline.

## 14. Tests
- Command: `./gradlew testDebugUnitTest`
- Result: SUCCESS (all unit tests passed).

## 15. Build Verification
- Command: `./gradlew assembleDebug`
- Result: SUCCESS

## 16. Risks / Known Issues
None. The enforcement of a deadline for recurrence directly in the UI prevents backend crashes and silently invalid states.

## 17. Final Verification
- Create recurring works: Yes.
- Edit recurring works: Yes.
- Recurrence selection works: Yes.
- Non-recurring behavior remains intact: Yes.
- Next occurrence behavior remains intact: Yes.
- Search works: Yes.
- Sorting works: Yes.
- Database unchanged: Yes.
- Notifications unchanged: Yes.
- Snooze not implemented: Verified.

## 18. Git Status
- commit made: NO
- tag created: NO
- working tree state: Modified strings, component UI, screens, and viewmodels safely without touching backend code.
