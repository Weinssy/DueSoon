# DueSoon

> Know what needs your attention next.

DueSoon is a minimal, local-first Android deadline reminder app designed
to help users stay aware of upcoming deadlines without the complexity
of traditional project management tools.

## Features

- Create, edit, complete, and delete tasks
- Optional deadlines
- Upcoming / Due Soon / Due Today / Overdue states
- Smart reminders
- Custom reminders
- Local notifications
- Home screen widget (Jetpack Glance)
- Calendar view
- Search and task filtering
- Dark mode
- Local data persistence
- No account required (Offline-first)
- **[NEW]** Opt-in Cloud & Multi-Device Sync (E2EE)

## Installation

Download the latest `DueSoon-v2.0.0-release.apk` from the [GitHub Releases](https://github.com/Weinssy/DueSoon/releases) page and install it on your Android device (Android 8.0 Oreo or higher, API 26+).

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Room
- KSP
- Coroutines
- Flow / StateFlow
- Navigation Compose
- DataStore
- Glance (App Widgets)
- Android Notification APIs
- WorkManager (Background Sync)
- Tink (E2EE Cryptography)
- Gradle Kotlin DSL

## Architecture

UI → ViewModel → Repository → Room

Notification scheduling is handled through a dedicated
NotificationScheduler abstraction.

## Product Philosophy

DueSoon follows a simple principle:

"Deadline should be obvious. Everything else should stay quiet."

The app intentionally avoids unnecessary complexity such as
cloud accounts, collaboration, AI, gamification, and advanced
project management features in the MVP.


## Status

**Latest stable release: v2.0.0**

DueSoon is a minimal, deadline-first reminder app designed to answer one question:

> What needs my attention next?

### v2.0.0 highlights
- **Cloud & Multi-Device Sync:** Safely sync your tasks across devices using an optional, non-custodial cloud backend.
- **End-to-End Encryption (E2EE):** All tasks are encrypted locally via AES-256-GCM before leaving the device. The server only sees ciphertext.
- **Strict Offline-First:** Sync is 100% opt-in. The app continues to function perfectly without an account or internet connection.
- **Deterministic Conflict Resolution:** Last-Writer-Wins (LWW) conflict engine seamlessly merges offline edits using UTC timestamps and revisions.

### v1.8.0 highlights
- **Archive Screen:** Completed tasks are now siloed into a dedicated memory-efficient Archive screen.
- **Search Hardening:** Debounced reactive text search that filters both active tasks and archives instantly.
- **Bulk Management:** Permanently clear all completed tasks safely with one tap.

### v1.7.0 highlights
- **Advanced Recurrence:** Custom intervals & weekdays mapped seamlessly to local databases.
- **Multi-Stage Reminders:** Intelligent stage-based offsets providing staggered notifications for High-priority tasks.

### v1.6.0 highlights
- **Interactive Widgets:** Complete tasks directly from the home screen widget without opening the app.
- **Quick Add:** Added a `(+)` launcher shortcut on the widget to instantly open the task creation sheet.
- **Idempotency:** Widget interactions are strictly guarded against rapid double-taps to ensure database stability.

### v1.5.0 highlights
- **Calendar & Time UX:** In-memory date filtering grid with workload density dots.
- **Visual Workload Indicators:** Calendar days display colored dots based on the highest Attention Tier due.

### v1.4.0 highlights
- **Smart Attention Ranking:** A deterministic hybrid matrix that blends deadline and priority.
- **100% Widget Parity:** The Glance widget perfectly matches the Home Screen's attention ranking.

### v1.3.0 highlights
- **JSON Backup & Restore:** Portable data exports with full offline safety.
- **Merge/Import:** Safely import tasks from JSON files.

## Roadmap

Please refer to our detailed [Roadmap](docs/roadmap/duesoon-roadmap.md) for upcoming features in v2.0 and beyond.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
