# Architecture Design: DueSoon v1.9.0 (Custom Themes & Visual Density)

## 1. DataStore Preferences Architecture
The central hub for visual preferences will reside within the expanded `UserPreferencesRepository.kt`. All settings will be securely isolated from the SQLite Room database.

### New Preference Keys
```kotlin
private object PreferencesKeys {
    val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
    val VISUAL_DENSITY = stringPreferencesKey("visual_density")
    val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
}
```

### Domain Enums
To enforce type safety across the repository and UI layer, preferences map to the following domain models:

```kotlin
enum class AccentPalette(val primaryHex: Long, val displayName: String) {
    INDIGO(0xFF6366F1, "Default (Indigo)"),
    EMERALD(0xFF10B981, "Emerald"),
    AMBER(0xFFF59E0B, "Amber"),
    ROSE(0xFFF43F5E, "Rose")
}

enum class VisualDensity {
    COMFORTABLE,
    COMPACT
}
```

### State Exposure
The repository will expose a consolidated `UserPreferencesState` data class via a single unified `Flow` ensuring UI consistency without multiple fragmented emissions.

## 2. Theming Architecture & Color Token Hierarchy
The Jetpack Compose theming layer (`Theme.kt` and `Color.kt`) will be refactored to support dynamic accent injection while locking the dark background baseline.

### Theme Pipeline Re-design
The root `DueSoonTheme` composable will accept the `AccentPalette` parameter:
```kotlin
@Composable
fun DueSoonTheme(
    accentPalette: AccentPalette = AccentPalette.INDIGO,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
)
```
The `ColorScheme` will be dynamically built, overriding the `primary`, `onPrimary`, `primaryContainer`, and `secondary` values based on the passed `AccentPalette.primaryHex`.

### Semantic Urgency Protection
To avoid clashes (e.g., Amber accent vs Elevated urgency), urgency colors will be completely decoupled from `MaterialTheme.colorScheme`. `AttentionUrgencyColors` will act as immutable semantic constants:
- Critical: `#EF4444`
- Elevated: `#F97316`
- Normal: `#71717A`

## 3. TaskCard Layout & Density Composition
The visual density of tasks will be managed dynamically, injected either via parameters or `CompositionLocal`.

### Layout Differentiation
- **Comfortable**: `16.dp` padding, multi-row internal grid structure. Displays category badges, urgency tags, and recurrence indicators explicitly.
- **Compact**: `8.dp` or `10.dp` vertical padding. Condenses title and deadline into a tight horizontal flow. Sub-badges are collapsed into minimalist urgency dot indicators.

### Hitbox Isolation
Accessibility constraints dictate a rigid `48.dp` minimum touch target for the completion checkbox. In `Compact` mode, the checkbox will be wrapped in a dedicated `Modifier.size(48.dp)` container structurally isolated from the row's click listener. This guarantees zero click collisions, preventing accidental task detail navigation when attempting to toggle completion.

## 4. Haptic Feedback Pipeline (Dual-Surface)
Haptics must fire contextually across both standard application surfaces and remote views.

### Compose Layer (In-App)
When the checkbox is toggled in `TaskCard.kt`:
```kotlin
val haptic = LocalHapticFeedback.current
if (hapticsEnabled) {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}
```

### Glance Layer (Widget)
Glance AppWidgets execute remotely and lack access to Compose `LocalHapticFeedback`.
- Inside `CompleteTaskActionCallback.kt`, read `hapticsEnabled` from DataStore.
- Ensure `<uses-permission android:name="android.permission.VIBRATE"/>` is active.
- Utilize system `Vibrator` or `VibratorManager` to execute a short 50ms pulse asynchronously upon task completion.

## 5. Settings UI Design
The Settings screen will be updated with a new "Appearance" section:
- **Accent Palette**: Displayed as a horizontal row of clickable color circular chips.
- **Visual Density**: Displayed as a Segmented Button or Radio Group (Comfortable vs Compact).
- **Haptic Feedback**: Standard `Switch` component.

## 6. Verification & Test Strategy
The release will be hardened with the following verification boundaries:
- **`UserPreferencesRepositoryTest`**: Validate coroutine emission and disk persistence of `AccentPalette`, `VisualDensity`, and `Boolean` types.
- **`SettingsViewModelTest`**: Verify state transitions and repository updates.
- **Invariants**: Final audit to manually verify `3.json` Room schema status and run full backup/restore tests against `PortableBackup` structures to ensure absolute zero regressions.
