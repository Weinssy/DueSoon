# DueSoon — Agent Instructions

## Project

DueSoon is a minimal, local-first Android deadline reminder app.

Product principle:

> Know what needs your attention next.

The app is deadline-focused, calm, clear, fast, reliable, and minimal.

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Coroutines
- Flow
- StateFlow
- Navigation Compose
- DataStore
- Android Notification APIs
- Gradle Kotlin DSL

## Architecture

Primary architecture:

UI → ViewModel → Repository → Room

Supporting components may include:

- Domain models
- NotificationScheduler
- Backup/Restore Coordinator
- Storage Access Framework wrapper
- Serialization/Validation services

Keep responsibilities separated.

## Core Engineering Rules

1. Do not introduce unnecessary dependencies.
2. Do not add cloud/backend functionality unless explicitly requested.
3. Do not add authentication unless explicitly requested.
4. Do not add AI features to the current MVP unless explicitly requested.
5. Keep the UI minimal and deadline-focused.
6. Follow Material 3.
7. Keep business logic out of Composables.
8. Use ViewModel for UI state and user interaction state.
9. Use Repository between UI/domain and Room.
10. Build and test after significant changes. Report the exact commands and results.
11. Do not modify unrelated files.
12. Preserve existing functionality when implementing new features.
13. Do not mass-upgrade dependencies during feature implementation.
14. Preserve existing public behavior unless the task explicitly requires a change.

## Room / Database Rules

15. Use Room as the application persistence boundary.
16. Do not manipulate the raw SQLite database files directly.
17. Never replace or copy `.db`, `.db-wal`, or `.db-shm` files during normal application operation.
18. Use Room transactions for multi-step database operations that must be atomic.
19. Do not change the Room database version unless explicitly required.
20. Do not create a Room migration unless the implementation actually changes the Room schema.
21. Preserve existing migrations.
22. Historical Room schema export files must not be deleted or rewritten unnecessarily.

## Backup / Import / Restore Rules

23. Portable backup schema is separate from Room database schema.
24. Do not use TaskEntity as the public JSON backup format.
25. Use dedicated portable DTOs for external backup data.
26. Serialization must remain independent from UI and Room.
27. Validation must remain independent from UI, Room, and NotificationScheduler.
28. Validate the complete backup before any destructive restore operation.
29. Invalid backup data must never partially modify the database.
30. Import is MERGE:
    - external task IDs are ignored
    - Room generates new IDs
31. Restore is REPLACE:
    - valid task IDs are preserved
    - duplicate IDs are rejected
    - invalid IDs are rejected
32. Import must be all-or-nothing.
33. Restore database replacement must use Room transactions.
34. Do not assume database transactions and notification scheduling are one atomic operation.
35. Notification reconciliation must happen explicitly after successful database operations.
36. Do not schedule or cancel notifications from serialization or validation code.

## Portable Schema Rules

37. Portable schema version is independent from Room schema version.
38. Current portable schema version is 1.
39. Current schema validation must explicitly accept schemaVersion == 1.
40. Future unsupported portable schema versions must be rejected.
41. Do not use enum ordinals in portable JSON.
42. Portable enum values must remain stable external strings.
43. Do not silently change the meaning of existing portable fields.
44. Portable DTO changes require explicit consideration of backward/forward compatibility.

## Storage Access Framework

45. User-selected backup files must use Android Storage Access Framework.
46. Do not request broad filesystem access when SAF is sufficient.
47. Close InputStream/OutputStream using structured resource handling.
48. User cancellation of the file picker is not a fatal application error.
49. Do not log complete backup JSON or task contents.

## Notification Rules

50. Keep notification scheduling behind NotificationScheduler.
51. Do not put notification scheduling logic in Composables.
52. Do not put notification scheduling logic in serialization/validation.
53. Preserve existing Smart Reminder behavior unless explicitly changed.
54. Completed tasks must not receive normal deadline reminders.
55. Overdue tasks must not receive retroactive deadline reminders.
56. Snooze semantics must remain compatible with the existing implementation.
57. Restore/import notification reconciliation must not modify task deadlines.

## UI Rules

58. Keep business logic out of Composables.
59. Use ViewModel for loading, processing, success, and error state.
60. Use reusable Material 3 components where appropriate.
61. Preserve existing navigation behavior.
62. Maintain accessibility:
    - adequate touch targets
    - content descriptions where needed
    - text scaling
    - sufficient contrast
    - do not rely on color alone

## Testing Rules

63. Add unit tests for new business logic.
64. Add database/instrumented tests when changing Room behavior.
65. Add integration tests where notification or persistence behavior requires them.
66. Clearly distinguish:
    - build/compilation success
    - unit test success
    - instrumented test compilation
    - instrumented test runtime
67. Never report AndroidTest runtime as passed unless it was actually executed on a device/emulator.
68. Run relevant existing tests to prevent regressions.

## Privacy / Security

69. DueSoon remains local-first.
70. Do not introduce network synchronization unless explicitly requested.
71. Do not log task titles, descriptions, or complete backup JSON.
72. Do not add analytics/tracking SDKs unless explicitly requested.
73. Backup encryption is out of scope unless explicitly requested.
74. Do not expose task data through unnecessary external services.

## Documentation / Release Workflow

75. Follow the implementation step specified by the current task.
76. Every implementation step must create or update its designated Markdown report under:
    `docs/releases/<version>/`
77. The report must reflect the actual implementation and verified commands/results.
78. Verify that the report file exists and read it back before finishing the step.
79. Do not invent test results, build results, file changes, or runtime verification.
80. Do not commit, tag, or push unless explicitly requested by the user.
81. Do not modify or recreate immutable release tags.
82. Do not automatically continue to the next implementation step.

## Change Discipline

83. Inspect the existing implementation before modifying it.
84. Prefer the smallest change that correctly satisfies the task.
85. Reuse existing abstractions when they are appropriate.
86. Do not introduce duplicate abstractions without a clear reason.
87. Do not refactor unrelated code during feature implementation.
88. If an existing architecture conflicts with the new requirement, document the conflict before making a broad architectural change.
89. Preserve existing user data and behavior.
90. When a requirement is ambiguous, document the ambiguity rather than silently inventing behavior.

## Current Product Scope

Current core scope includes:

- task CRUD
- task completion
- title/description
- deadline
- category
- priority
- Smart Reminder
- Custom Reminder
- local notifications
- Home
- Tasks
- Calendar
- Settings
- search
- filtering
- sorting
- recurring tasks
- snooze
- local backup/export/import/restore work for v1.3

Do not introduce unrelated product areas such as:

- cloud sync
- accounts
- collaboration
- social features
- gamification
- AI
- analytics dashboards
- habit tracking
- Pomodoro
- statistics

unless explicitly requested.

## Current Release Context

Current development target:

DueSoon v1.3.0

Primary v1.3 focus:

Backup & Data Safety

Planned areas:

- portable JSON export
- backup
- import
- restore
- validation
- safe Room transactions
- notification reconciliation

Room schema and portable backup schema are separate concepts.

Never assume:

Room schema version == portable schema version.