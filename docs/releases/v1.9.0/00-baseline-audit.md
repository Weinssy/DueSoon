# Baseline Audit: DueSoon v1.9.0 (Custom Themes & Visual Density)

## 1. Release & Repository State
- **Git Status**: Working tree is clean on `main`, tagged at `v1.8.0`.
- **Version**: Confirmed `versionCode = 9`, `versionName = "1.8.0"`.
- **Database Schema**: Room schema is stable at version 3 (`schemas/3.json`). It remains untampered.
- **Verification Tests**: `./gradlew testDebugUnitTest` executed with 0 failures, preserving the v1.8.0 baseline integrity.

## 2. Theming Architecture & Color Tokens
- **Current Setup**: `Theme.kt` implements a manual `DarkColorScheme` and `LightColorScheme`. Dynamic color (Material You) is currently unsupported.
- **Color Definitions**: Hardcoded colors reside in `Color.kt`. The dark mode retains a minimalist, deeply dark aesthetic (`DarkBackground = 0xFF0F0F10`).
- **Accent Palettes Assessment**: Introducing accent palettes (e.g., Default/Indigo, Emerald, Amber, Rose) is highly feasible. We can dynamically override the `primary` and `onPrimary` color tokens in the active `ColorScheme` without altering the core `#0F0F10` background or surface structure, ensuring high-contrast regressions are avoided.

## 3. Task Item Visual Structure & Density Assessment
- **Current Structure**: `TaskCard.kt` uses a `Card` wrapping a `Column` with 16dp padding and 8dp spacing. It presents a title, deadline, and a secondary row containing a category chip, priority indicator, and recurrence tag.
- **DensityMode Abstraction**:
  - `Comfortable` (Default): Retains the current 16dp padding and multi-row metadata.
  - `Compact`: Feasible by reducing vertical padding (e.g., to 8dp or 12dp) and either hiding the secondary metadata row or condensing it into a single-line string next to the deadline.
- **Touch Targets**: The completion toggle relies on `IconButton`, which inherently guarantees a 48dp minimum touch target. This compliance must be strictly preserved if the height of the card is reduced in Compact mode.

## 4. DataStore Preferences Infrastructure
- **Current Setup**: `UserPreferencesRepository.kt` utilizes Jetpack Preferences DataStore (`preferencesDataStore`). It currently tracks a general `theme` string (SYSTEM/LIGHT/DARK) and a `notificationsEnabled` boolean.
- **Expansion Readiness**: 
  - The repository is perfectly positioned to absorb new visual preferences without touching Room. 
  - New preference keys to add:
    - `ACCENT_THEME` (String enum).
    - `VISUAL_DENSITY` (String enum: `COMFORTABLE`, `COMPACT`).
    - `HAPTIC_FEEDBACK_ENABLED` (Boolean).

## 5. Haptic Feedback Pipeline
- **Compose UI**: Haptics can be easily implemented in `TaskCard.kt` using `LocalHapticFeedback.current.performHapticFeedback()` when `onCompleteToggle` is triggered.
- **Glance Widgets**: Glance does not have direct access to `LocalHapticFeedback`. Haptics must be executed natively via the system `Vibrator` or `VibratorManager` within the `ActionCallback` (e.g., `CompleteTaskActionCallback.kt`) execution scope.

## 6. Scope Boundaries & Invariants
- **Non-Goals**: 
  - Freeform user-supplied hex color inputs (we will provide curated palettes only).
  - Heavy structural overhauls to Light Mode.
  - Custom font loading.
- **Strict Invariants**:
  - Zero Room migrations.
  - Zero changes to `PortableBackup` JSON structure.
  - Settings must persist locally in `UserPreferencesRepository`.
