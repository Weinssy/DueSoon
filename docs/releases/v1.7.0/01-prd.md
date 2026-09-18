# DueSoon v1.7.0 — Product Requirements Document (PRD)
## Recurring & Reminder Engine 2.0

## 1. Vision & Objective
Expand DueSoon's task recurrence and reminder engines to handle modern, realistic task routines (such as custom intervals, weekday filters, and multi-stage reminders). This release must enrich the scheduling power of the application without sacrificing minimalism, increasing the surface area of the Room database, or compromising local-first offline execution constraints.

## 2. Scope Matrix

### In-Scope
- **Extended Recurrence Rules:** Ability to specify an interval multiplier (e.g., every $X$ days, weeks, or months) and specific weekday selectors (e.g., Mon–Fri, Tue/Thu).
- **Polymorphic String Storage:** Encoding custom recurrence patterns inside the existing `recurrenceInterval: String?` database column, ensuring 100% backward compatibility for legacy string values (`"DAILY"`, `"WEEKLY"`, `"MONTHLY"`).
- **Advanced Advancement Rule:** Handling severely overdue completions so that newly spawned recurring tasks correctly land in the valid future, skipping obsolete occurrences.
- **Multi-Stage Reminders:** Up to 3 configurable reminder offset stages per task (e.g., At deadline, 1 hour before, 1 day before).
- **Backup & Restore Safeties:** Safe parsing and serialization fallback strategies for `PortableBackup.kt` to ensure export/import logic smoothly transitions legacy tasks.

### Out-of-Scope
- **Natural Language Parsing (NLP):** DueSoon will rely exclusively on deterministic pickers. No natural language processing will be used.
- **Location-Based Reminders:** Geo-fenced reminders are not supported.
- **Infinite Projection:** DueSoon does not instantiate infinite tasks in the Calendar view. Only the *next active instance* is spawned upon completion.
- **Room Database Schema Migrations:** Strictly prohibited.

## 3. Product Rules & Logic

### Recurrence String Protocol
To safely leverage the existing `String?` column without migrations, the system will use a simplified, token-based RRULE string protocol:
- **Legacy Fallback:** `DAILY`, `WEEKLY`, `MONTHLY`.
- **Custom Interval Syntax:** `INTERVAL:DAYS:X`, `INTERVAL:WEEKS:X`, `INTERVAL:MONTHS:X` (where X is an integer).
- **Weekdays Syntax:** `WEEKLY_DAYS:MO,TU,WE,TH,FR,SA,SU` (comma-separated days).

### Next Deadline Computation
The domain calculation engine must guarantee the following invariants:
1. **Time Preservation:** Local time (exact hour and minute) must be preserved perfectly across Daylight Savings Time (DST) and timezone transitions.
2. **Overdue Advancement Check:** If a recurring task is completed days or weeks late, the next instance's deadline must advance sequentially using the recurrence rule *until* it finds the first valid chronological instance that is `> System.currentTimeMillis()`. It must not spawn a new task that is already overdue.

### Multi-Stage Reminder Scheduling
1. **Dynamic Alarms:** The application will transition from static Smart Rules to configurable multi-stage reminders per task.
2. **Unique Request Codes:** The `AndroidNotificationScheduler` will map offsets to unique `AlarmManager` alarms utilizing `(taskId * 100 + index).toInt()`, isolating them per task safely.
3. **Stage Pruning:** Any user-defined reminder offset that already calculates to a time in the past when the task is saved must be silently ignored and omitted from scheduling without error.

## 4. Acceptance Criteria (AC)

- **AC-01 (Legacy Recurrence Compatibility):** Existing tasks and legacy backups utilizing `"DAILY"`, `"WEEKLY"`, or `"MONTHLY"` must continue to calculate identical next occurrences without parsing exceptions.
- **AC-02 (Custom Interval Accuracy):** Tasks set to repeat every $X$ days/weeks/months must advance accurately by exact calendar intervals while strictly preserving the original hour and minute.
- **AC-03 (Weekday Rule Accuracy):** A task configured for specific weekdays (e.g., Mon-Fri) correctly skips weekend days when advancing to spawn the next instance.
- **AC-04 (Overdue Advancement):** Completing a severely overdue recurring task computes and spawns the next valid chronological instance in the future, avoiding endless loops of overdue past tasks.
- **AC-05 (Multi-Stage Alarms):** A task configured with multiple user-selected reminder stages schedules distinct `AlarmManager` alarms with correct, non-colliding unique request codes.
- **AC-06 (Past Stage Pruning):** Any reminder offset stage that evaluates to a time strictly in the past is safely omitted and ignored at schedule time.
- **AC-07 (Zero Schema Migration):** The Room database must remain perfectly stable on schema version 3 (`3.json`). All data storage updates must be polymorphic strings inside existing schema columns.
- **AC-08 (Widget & Quick Complete Parity):** Completing a task with advanced custom recurrence via the Glance Widget produces the exact same next recurrence artifact as completing it inside the main application.
