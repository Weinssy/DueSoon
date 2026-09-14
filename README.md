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
- Calendar view
- Search and task filtering
- Dark mode
- Local data persistence
- No account required
- Offline-first

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

MVP v1.0 — Release Candidate

The current MVP has completed static verification, build verification,
final UX polish, and manual runtime testing.

## Roadmap

### v1.1
- Snooze
- Recurring tasks
- Subtasks
- Improved notification actions

### v1.2
- Home screen widget
- Natural language input
- Improved smart reminders

### v2
- AI assistance
- Calendar integration
- Cloud sync
- Multi-device support

## License

[Choose a license]
