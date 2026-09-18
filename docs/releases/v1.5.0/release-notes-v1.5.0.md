# DueSoon v1.5.0 Release Notes

**Version Code:** 6
**Version Name:** 1.5.0
**Target SDK:** 34

## 🌟 What's New: Calendar & Time UX
DueSoon v1.5.0 brings a brand new way to visualize and plan your upcoming tasks with a fast, built-in calendar view!

### 📅 Interactive Calendar Grid
- Tap the **DateRange icon** on the top right of your Home Screen to open the new Calendar View.
- Select any date to instantly filter your task list and see exactly what's due on that day.
- A quick **Clear** button lets you return to your regular task list instantly.

### 🔴 Workload Density Dots
See how busy your days are at a glance! 
The calendar displays colored dots on days that have deadlines:
- **Red:** You have Critical or Overdue tasks.
- **Orange:** You have High or Elevated priority tasks.
- **Gray:** You have Normal or Optional tasks.
- *Completed tasks are automatically excluded from the density dots so you can focus on what's left.*

### ⚡ Blazing Fast Performance
- The calendar filters tasks instantly (*in-memory*) without ever freezing or loading.
- We built the calendar entirely from scratch to keep DueSoon lightweight and completely local. No cloud sync, no tracking, and no bloatware.

---

## 🛠️ Technical Details & Invariants Preserved
For developers and advanced users, v1.5.0 strictly adheres to the following architectural invariants:
- **Zero Database Migrations:** The application remains securely on Room schema `3.json`. Zero local data mutations were required.
- **Portable JSON Backup Compatibility:** Backups generated in v1.3.0 and v1.4.0 remain 100% compatible.
- **100% Widget Isolation:** Interacting with the new calendar UI on the Home Screen does not mutate or affect the `DueSoonWidget` or the background `NotificationScheduler` in any way.
- **Safe Timezone Boundaries:** Day boundaries and visual indicators are evaluated securely against `ZoneId.systemDefault()` to ensure accurate display regardless of the user's physical timezone.
