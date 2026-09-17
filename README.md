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
- No account required
- Offline-first

## Installation

Download the latest `DueSoon-v1.4.0-release.apk` from the [GitHub Releases](https://github.com/Weinssy/DueSoon/releases) page and install it on your Android device (Android 8.0 Oreo or higher, API 26+).

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

**Latest stable release: v1.4.0**

DueSoon is a minimal, deadline-first reminder app designed to answer one question:

> What needs my attention next?

### v1.4.0 highlights
- **Smart Attention Ranking:** A deterministic hybrid matrix that blends deadline and priority.
- **100% Widget Parity:** The Glance widget perfectly matches the Home Screen's attention ranking.
- **Explainable Badges:** Task badges accurately reflect attention tiers.

### v1.3.0 highlights
- **JSON Backup & Restore:** Portable data exports with full offline safety.
- **Merge/Import:** Safely import tasks from JSON files.

### v1.2.0 highlights
- Home Search & Sorting
- Recurring tasks UI polish
- Snooze notifications (10m, 1h, Tomorrow)

## Roadmap

Please refer to our detailed [Roadmap](docs/roadmap/duesoon-roadmap.md) for upcoming features in v1.5 and beyond.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
