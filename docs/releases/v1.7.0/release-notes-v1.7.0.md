# DueSoon v1.7.0 Release Notes

**Release Date:** September 18, 2026
**Version:** 1.7.0
**Target SDK:** 34

We are thrilled to announce DueSoon v1.7.0, introducing the highly anticipated **Recurring & Reminder Engine 2.0**. This update makes task management smarter, safer, and more tailored to your busy schedule, without sacrificing the minimal, local-first experience you love.

## What's New

### Advanced Recurrence System
- **Custom Intervals:** You can now create custom recurrence intervals (e.g., Every 3 Days, Every 2 Months).
- **Specific Weekdays:** Schedule tasks to repeat only on specific days of the week (e.g., Every Monday, Wednesday, and Friday).
- **Infinite Loop Safeguards:** Advanced computational safeguards ensure your battery and phone performance remain optimal, even if you fall months behind on a task.

### Multi-Stage Smart Reminders
- DueSoon now calculates smart offsets dynamically depending on the task's priority:
  - **All Tasks:** Receive a notification exactly at the deadline and an early heads-up 2 hours prior.
  - **High & Critical Tasks:** Gain an extra advantage with a comprehensive 24-hour advance warning.
- **Smart Pruning:** If a deadline is scheduled very soon (e.g., in 1 hour), DueSoon is smart enough to skip the 24-hour and 2-hour reminders, keeping your notification tray clean.

### Under the Hood
- **Zero Schema Migrations:** All of these advanced engine capabilities were shipped with exactly zero changes to the underlying database structure.
- **Flawless Backups:** Your portable JSON backups (from v1.3.0 and newer) remain 100% interoperable. You can safely restore old data into v1.7.0, and DueSoon will seamlessly map legacy logic to the new engine.

Thank you for choosing DueSoon to keep track of what needs your attention next. Stay calm, and conquer your deadlines!
