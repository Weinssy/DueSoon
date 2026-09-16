# DueSoon v1.2.0 Product Requirements Document

## 1. Executive Summary
DueSoon v1.2.0 is the next minor release focusing on improving task discoverability and notification flexibility while maintaining the core "Calm, Clear, Fast, Reliable, Minimal" product principles. The core focus will be introducing local text search, basic sorting, and UI polish for recurring tasks, alongside an important new "Snooze Reminder" feature.

## 2. Problem Statement
Users currently cannot quickly find specific tasks by name when their lists grow long. Additionally, users lack flexibility when reminded of a task at an inconvenient time; they must either ignore it or mark it complete. Finally, while recurring task logic exists at the data layer, its user experience needs polish to ensure expectations around task duplication and history are clearly met.

## 3. v1.2.0 Goals
- Enable users to quickly search for tasks by title.
- Provide minimal but useful sorting options.
- Polish the recurring task UI to ensure clear interaction.
- Implement a reliable Snooze action for task reminders.

## 4. Non-Goals
- Complex project management or subtasks.
- Server/backend sync, authentication, or collaboration features.
- Advanced querying (e.g., regex, complex AND/OR filters beyond existing category/status).
- Re-architecting the entire notification system.

## 5. Target User Experience
- **Search**: A simple search bar or icon on the Home screen that seamlessly filters the task list in real-time.
- **Sorting**: An unobtrusive sort menu that respects the core principle: "Deadline should be obvious."
- **Recurring UI**: Clear toggles and interval selectors in Create/Edit screens.
- **Snooze**: A quick action on the notification to delay the reminder by predefined durations (e.g., 15 mins, 1 hour).

## 6. Product Principles
1. Calm
2. Clear
3. Fast
4. Reliable
5. Minimal
*Core Statement*: "DueSoon helps users know what needs their attention next."
*Design Principle*: "Deadline should be obvious. Everything else should stay quiet."

## 7. Feature Scope

### Search Task (P0)
- **Existing functionality**: None. In-memory filtering by category/status exists.
- **Required change**: Add a text field and update `HomeFilterLogic.kt` to include a title filter.
- **New behavior**: Real-time list filtering by task title (case-insensitive).
- **Files/components likely affected**: `HomeViewModel.kt`, `HomeScreen.kt`, `HomeFilterLogic.kt`.
- **Database impact**: None (in-memory).
- **Notification impact**: None.
- **Testing requirements**: Unit tests for `HomeFilterLogic.filterTasks` with text query. UI tests/manual checks.
- **Risk level**: Low.

### Sorting (P0)
- **Existing functionality**: Hardcoded sorting (`DeadlineState` -> `deadline` -> `priority` -> `createdAt`).
- **Required change**: Add sort state and update the Flow combination in ViewModels.
- **New behavior**: Allow sorting by Deadline (default), Priority, Title (A-Z), Created (newest).
- **Files/components likely affected**: `HomeViewModel.kt`, `HomeScreen.kt`.
- **Database impact**: None (in-memory Flow sort).
- **Notification impact**: None.
- **Testing requirements**: Unit tests for ViewModel flow emissions.
- **Risk level**: Low.

### Recurring Task UI Polish (P0)
- **Existing functionality**: Data-level recurrence exists. `RecurrenceSelector.kt` exists.
- **Required change**: Audit and wire the selector into Create/Edit screens. Clarify what happens on completion.
- **New behavior**: User can easily toggle DAILY/WEEKLY/MONTHLY. 
- **Files/components likely affected**: `CreateTaskScreen.kt`, `EditTaskScreen.kt`, `RecurrenceSelector.kt`.
- **Database impact**: None.
- **Notification impact**: None.
- **Testing requirements**: Manual validation of task completion duplication and next deadline calculation.
- **Risk level**: Low to Medium (UX risk regarding duplication accumulation).

### Snooze Reminder (P1)
- **Existing functionality**: None. Notifications can only be dismissed or trigger app opening.
- **Required change**: Add Snooze action to Notification. Persist snooze state. Update Scheduler.
- **New behavior**: User clicks "Snooze 1h" on notification. Notification is dismissed, new exact alarm is set for 1h later.
- **Files/components likely affected**: `ReminderReceiver.kt`, `AndroidNotificationScheduler.kt`, `TaskEntity.kt`, `Task.kt`, `AppDatabase.kt`.
- **Database impact**: Requires `snoozedUntil: Long?` field and Room v3 migration (for reboot resilience).
- **Notification impact**: Significant. `BootReceiver` and `Scheduler` must respect `snoozedUntil` alongside `SmartReminderCalculator`.
- **Testing requirements**: Unit tests for snooze scheduling logic. Manual tests involving device reboot.
- **Risk level**: High.

## 8. Priority Definition
- **P0**: Search, Sorting, Recurring Task UI Polish.
- **P1**: Snooze Reminder.

## 9. User Stories
1. As a user, I want to type a keyword to instantly find a specific task so I don't have to scroll through long lists.
2. As a user, I want to sort my tasks by priority so I can focus on high-priority items first.
3. As a user, I want to easily set a task to repeat weekly so I don't have to recreate it.
4. As a user, I want to snooze a task reminder for an hour when I am busy, without losing the reminder entirely.

## 10. Functional Requirements
- **Search**: Must filter by `title` (case-insensitive). Must combine with Category/Status filters (AND logic). Must handle empty states gracefully.
- **Sorting**: Must offer Deadline, Priority, Title, Created Date. Must retain state during session.
- **Recurring**: Must correctly display current recurrence interval. Turning off recurrence stops future task generation.
- **Snooze**: Must support predefined durations (e.g. 15m, 1h). Must persist across reboots. Must override earlier smart reminders if the snooze time pushes past them.

## 11. UX Requirements
- Search bar must be minimal (e.g., expandable icon or top app bar integration).
- Sort options must use standard Material 3 menus/bottom sheets.
- Recurrence selector must clearly indicate the interval.
- Snooze must be accessible directly from the Android notification via Action buttons.

## 12. Edge Cases
- **Search**: Extremely long titles. Special characters in titles.
- **Sorting**: Tasks with identical deadlines/priorities (fallback to created date).
- **Recurring**: Completing a recurring task that is already overdue by multiple intervals. *Requires implementation-stage verification.*
- **Snooze**: Snoozing past the actual deadline. Rebooting while a snooze is active. Snoozing a task that is subsequently deleted.

## 13. Acceptance Criteria
- Typing in search instantly filters the visible list.
- Selecting a sort option immediately reorders the list.
- Setting a task to DAILY and completing it results in a new task scheduled for tomorrow.
- Clicking "Snooze 1h" on a notification hides it and triggers a new notification exactly 1 hour later, even if the device restarts.

## 14. Data / Database Impact
- **Search/Sort/Recurring**: None.
- **Snooze**: Planned Room v3 migration to add `snoozedUntil: Long?` to `TaskEntity`.

## 15. Notification / Alarm Impact
- **Snooze**: High impact. Scheduler must evaluate `snoozedUntil` to set the exact alarm, modifying the output of `SmartReminderCalculator` for that specific time window.

## 16. Architecture Impact
- Planned only. The current UI -> ViewModel -> Repository -> Room structure will be maintained.

## 17. Backward Compatibility
- Room v3 migration must gracefully handle existing v1 and v2 databases. `snoozedUntil` defaults to `null`.
- Existing tasks will seamlessly support search and sort.

## 18. Testing Strategy
- Add unit tests for `HomeFilterLogic` (Search) and ViewModels (Sort).
- Add unit test for Snooze logic.
- Perform manual regression testing on existing notifications.
- Perform manual testing of Room v3 migration paths.

## 19. Risks
- Database migration failure (v2 to v3) causing app crash on launch.
- Snooze logic conflicting with `SmartReminderCalculator`, causing infinite notification loops or dropped alarms.

## 20. Release Criteria
- All P0 and P1 features implemented.
- 0 critical/high bugs.
- Room v3 migration successfully tested from v1 and v2.
- No performance degradation in list scrolling when searching.

## 21. Explicit Out-of-Scope List
- Statistics, Analytics, Productivity dashboards
- Archive system, Export/import
- Cloud sync, User accounts, Authentication, Collaboration, Social features
- AI, Natural-language task creation
- Gamification, Pomodoro, Habit tracking
- Complex project management, Subtasks
- Widgets redesign
- Backend/server infrastructure

## 22. Recommended Implementation Order
1. Search Task (P0)
2. Sorting (P0)
3. Recurring Task UI Polish (P0)
4. Snooze Reminder (P1)
