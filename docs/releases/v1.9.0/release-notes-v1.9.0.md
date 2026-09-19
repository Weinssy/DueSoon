# DueSoon v1.9.0 - Release Notes

**Release Date:** September 2026
**Version:** 1.9.0 (Version Code 10)

Welcome to DueSoon v1.9.0! This release focuses on deep personalization and tactile responsiveness. We've introduced a dynamic theming engine, customizable visual densities, and hardware haptic integration to ensure DueSoon adapts to your unique aesthetic and physical workflow, all while retaining our core offline-first minimalism.

## What's New

### 🎨 Custom Accent Palettes
Make DueSoon yours without sacrificing readability. You can now select from four distinct, hand-tuned accent palettes that dynamically tint the entire application:
- **Indigo** (Default)
- **Emerald**
- **Amber**
- **Rose**
*These palettes intelligently coexist with our strict `#0F0F10` dark mode foundation, ensuring maximum contrast and battery efficiency.*

### 📏 Visual Density Controls
Adapting to different screen sizes and workflow preferences, you can now toggle between two layout densities:
- **Comfortable**: Expansive cards with 16dp padding and multi-row metadata for maximum readability.
- **Compact**: Condensed 8dp paddings with single-line focus, perfect for power users scanning high volumes of tasks.

### 📳 Tactile Haptics & Widget Feedback
Interacting with DueSoon is now physical. 
- Completing a task from the app now triggers a subtle, satisfying physical vibration. 
- **Widget Parity**: Tapping the complete button directly from your Home Screen Glance widget will also trigger a localized haptic response, giving you instant physical confirmation without opening the app.
*(This can be toggled off at any time in Settings).*

### 🛡️ Semantic Alert Protection
Regardless of your chosen accent palette, critical deadlines remain visually protected. Overdue and high-priority alerts permanently utilize our dedicated `AttentionUrgencyColors` (Red/Orange/Gray), ensuring you never confuse an aesthetic choice with an urgent deadline.

## Technical Notes
- **Zero Migrations**: DueSoon's local SQLite database remains safely on schema 3, with zero migrations required for this update.
- **DataStore Isolation**: All new personalization preferences (theme, density, haptics) are strictly persisted via Jetpack Preferences DataStore, preserving the pristine state of your portable JSON backups.
- **Accessibility Guaranteed**: The core task completion button maintains a strict 48dp minimum hardware touch target, even in the tightest Compact density mode.

---
*Stay focused. Know what needs your attention next.*
*— The DueSoon Team*
