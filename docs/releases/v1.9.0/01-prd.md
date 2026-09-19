# Product Requirements Document: DueSoon v1.9.0 (Custom Themes & Visual Density)

## 1. Vision & Objective
Provide personalization and high-density productivity viewing options while honoring DueSoon's minimalist, distraction-free dark UI aesthetic. The goal is to allow users to make the app feel tailored to their preferences without compromising the core design philosophy.

## 2. Scope Matrix

### In-Scope
- **Visual Density Switcher:** Ability to toggle between `Comfortable` (spacious multi-line card) and `Compact` (condensed single-row item).
- **Curated Accent Palettes:** Four curated color themes: `Default (Indigo)`, `Emerald`, `Amber`, `Rose`, applied dynamically over the core dark scheme.
- **Haptic Feedback:** Subtle tactile feedback when completing tasks (both in-app and via Glance widget), with a dedicated toggle in settings.
- **Preferences Storage:** Leveraging `UserPreferencesRepository` (Jetpack DataStore) to store new visual and haptic settings.
- **Appearance Settings UI:** A dedicated section or sheet in the Settings screen to adjust density, accent color, and haptics.

### Out-of-Scope
- Arbitrary user-defined hex color pickers (only curated palettes are supported).
- Light mode architectural overhaul (only baseline dark theme accent adaptation is scoped).
- Custom font family loaders or typography customization.
- Any database schema alterations (Room schema stays strictly on v3).

## 3. Product Rules & Logic

### Visual Density Modes
- **Comfortable (Default):** Current 16dp padding, multi-row layout containing the task Title, Deadline, Category badge, Priority indicator, and Recurrence tag.
- **Compact:** Condensed layout with ~8-12dp vertical padding. Features a title + inline compact deadline/urgency pip. Secondary badges (category, priority tags) are collapsed or hidden to maximize vertical list efficiency.
- **Checkbox Hit Target:** Must rigidly maintain a `48.dp` minimum touch area (width and height) regardless of visual card height. The touch target must not bleed improperly into the card body action area.

### Accent Theming Architecture
- **Curated Options:**
  - Indigo (`0xFF6366F1`) - Default
  - Emerald (`0xFF10B981`)
  - Amber (`0xFFF59E0B`)
  - Rose (`0xFFF43F5E`)
- **Application:** Accent overrides `primary`, `onPrimary`, `primaryContainer`, and active control highlights within `MaterialTheme.colorScheme`.
- **Urgency Protection:** Semantic urgency colors provided by `AttentionRankingEngine` (Critical Red, Elevated Orange, Normal Gray) remain strictly immutable. Accent colors must NOT collide with or override these semantic indicators.

### Haptic Feedback
- **Triggers:** Triggers immediately on the task completion toggle inside `TaskCard.kt` and via the Glance `CompleteTaskActionCallback`.
- **Control:** Governed by a global `hapticsEnabled` boolean stored in DataStore.

## 4. Acceptance Criteria (AC)

- **AC-01 (Density Persistence):** Switching density between Comfortable and Compact updates the UI immediately and persists across app restarts via DataStore.
- **AC-02 (Compact Hitbox Safety):** In Compact mode, the task complete checkbox maintains a minimum 48dp touch hitbox without mis-clicks onto the card body.
- **AC-03 (Accent Palette Switching):** Selecting a new accent palette updates all primary controls, FAB, and active toggles instantaneously without restarting the Activity.
- **AC-04 (Urgency Color Protection):** Changing accent colors does NOT alter the semantic urgency colors determined by `AttentionRankingEngine`.
- **AC-05 (In-App Haptics):** Toggling a task complete in the app triggers a tactile haptic response if haptics are enabled in settings, and remains silent if disabled.
- **AC-06 (Widget Haptics):** Completing a task via Glance widget triggers a device vibration if enabled.
- **AC-07 (Zero Schema Migration):** Room database stays on schema version 3 (`3.json`).
- **AC-08 (Zero Backup Regression):** JSON backups remain 100% interoperable with older versions.
