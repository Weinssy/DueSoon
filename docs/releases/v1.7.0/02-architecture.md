# DueSoon v1.7.0: Architecture Design (Recurring & Reminder Engine 2.0)

## 1. Domain Recurrence Specification
To support advanced recurrence without breaking the database, the domain `RecurrenceInterval` enum is formally replaced/upgraded to a sealed interface in the domain layer.

```kotlin
sealed interface RecurrenceRule {
    data object Daily : RecurrenceRule
    data object Weekly : RecurrenceRule
    data object Monthly : RecurrenceRule
    data class CustomInterval(val count: Int, val unit: ChronoUnit) : RecurrenceRule
    data class SpecificWeekdays(val days: Set<DayOfWeek>) : RecurrenceRule
}
```

*Note: `ChronoUnit` (from `java.time.temporal`) and `DayOfWeek` (from `java.time`) are leveraged for precise Java Time calculations natively supported in Android API 26+.*

## 2. Polymorphic String Serialization Protocol
DueSoon v1.7.0 enforces **Zero Room Schema Migrations**. The database column `recurrenceInterval: String?` will persist string tokens representing the polymorphic state of `RecurrenceRule`.

### Encoding / Decoding Mapper (`TaskEntity.kt` / `TaskMapper.kt`)
- **Legacy Fallback:** 
  - `"DAILY"` -> `RecurrenceRule.Daily`
  - `"WEEKLY"` -> `RecurrenceRule.Weekly`
  - `"MONTHLY"` -> `RecurrenceRule.Monthly`
- **Custom Interval Protocol:** 
  - `"INTERVAL:DAYS:3"` -> `RecurrenceRule.CustomInterval(3, ChronoUnit.DAYS)`
  - `"INTERVAL:WEEKS:2"` -> `RecurrenceRule.CustomInterval(2, ChronoUnit.WEEKS)`
  - `"INTERVAL:MONTHS:1"` -> `RecurrenceRule.CustomInterval(1, ChronoUnit.MONTHS)`
- **Weekdays Protocol:**
  - `"WEEKLY_DAYS:MONDAY,WEDNESDAY,FRIDAY"` -> `RecurrenceRule.SpecificWeekdays(setOf(DayOfWeek.MONDAY, ...))`

*Error Handling:* If an unknown token is encountered (e.g., from a future backup), the mapper will safely fallback to `null` (no recurrence) to prevent crashes.

## 3. Overdue Advancement Algorithm
Currently, `TaskRepository` strictly adds 1 day/week/month to the original `deadline`. 
In v1.7.0, a new domain component `RecurrenceCalculator` will be introduced to calculate the next deadline.

### The Advancement Loop
If a recurring task is completed severely late (e.g., a Daily task completed 5 days late), spawning a task for "4 days ago" is useless. The algorithm will:
1. Start with the original `deadline` as `baseline`.
2. Apply the `RecurrenceRule` offset (e.g., `+1 WEEK` or `find next MONDAY`).
3. Check if the newly computed timestamp is strictly greater than `System.currentTimeMillis()`.
4. If **yes**, return this timestamp as the next `deadline`.
5. If **no**, apply the rule again to the new timestamp. Loop until condition #3 is met.

*Time Preservation:* The calculation will explicitly preserve the Local Time (Hour:Minute) by mapping the epoch epoch to `ZonedDateTime` using `ZoneId.systemDefault()`, applying offsets via `.plus()`, and mapping back to epoch milliseconds.

## 4. Multi-Stage Alarm Pipeline
The `SmartReminderCalculator` will be refactored to support configurable offsets. 

### Offset Storage 
Because we cannot migrate Room to add a `reminders: List<Long>` column, the system will utilize one of two non-breaking strategies:
1. **Dynamic Tiering:** Keep using `ReminderType.SMART`, but enhance the generator to read user-defined offsets from `UserPreferencesRepository` (DataStore) instead of hardcoding them.
2. **Polymorphic String (If required in future):** If per-task overrides are strictly needed, they could be encoded into a new schema version, but for v1.7.0, all multi-stage presets will be governed globally via `DataStore`.

### Request Code Safety
`AndroidNotificationScheduler` currently utilizes:
`val requestCode = (task.id * 100 + index).toInt()`
This gives us a safe sandbox of 0-9 index slots for exact alarms per task.
- `index 0`: Exact deadline.
- `index 1`: Stage 1 offset (e.g., 1 hour before).
- `index 2`: Stage 2 offset (e.g., 1 day before).

### Past Stage Pruning
Before calling `alarmManager.setExactAndAllowWhileIdle`, the pipeline will filter the generated offsets:
```kotlin
val validOffsets = generatedOffsets.filter { it > System.currentTimeMillis() }
```
This guarantees no alarms are scheduled in the past.

## 5. Data Integrity & PortableBackup
Since `PortableBackup` serializes the SQLite entity exactly as it is, the JSON will begin containing the new string tokens (e.g., `"INTERVAL:DAYS:3"`).
Because the domain layer handles deserialization transparently, older versions of DueSoon attempting to import a v1.7.0 backup will simply fail the Enum `valueOf()` map, resolving to a non-recurring task. This is acceptable gracefully degraded behavior for forward compatibility. Backward compatibility is 100% maintained via the legacy string mapping block.
