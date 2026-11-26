# Implementation Report — DueSoon v1.9.7

## Scope Completed (Points 1–9)

1. **Pomodoro Foreground Service & Persistent Notification**
   - Added `PomodoroTimerCoordinator` (`app/src/main/java/com/duesoon/app/focus/PomodoroTimerCoordinator.kt`) and `PomodoroTimerService` / `AndroidPomodoroServiceBridge` (`app/src/main/java/com/duesoon/app/focus/PomodoroTimerService.kt`).
   - Added persisted timer keys in `UserPreferencesRepository.kt`.
   - Declared `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` permissions and `specialUse` service entry in `AndroidManifest.xml`.
   - Wired `ACTION_OPEN_FOCUS` in `MainActivity.kt` and `AppNavigation.kt`.

2. **Course Autocomplete & Home Filter**
   - Enhanced `CategorySelector.kt` with free-text course input, clear action, and previous-course suggestion chips backed by `CreateTaskViewModel.kt` and `EditTaskViewModel.kt`.
   - Updated `CategoryChip.kt` to render custom courses with Material `secondaryContainer`/`onSecondaryContainer`.
   - Added `categories` & `selectedCategory` filtering plus title/category/description search in `HomeViewModel.kt` and `HomeScreen.kt`.

3. **"Kertas & Tinta Kampus" Theme Palette**
   - Added `AccentPalette.CAMPUS_PAPER` in `ThemePreferences.kt`, `CampusPaperColors` in `Color.kt`, light/dark schemes in `Theme.kt`, and UI selection in `SettingsScreen.kt` & `strings.xml`.

4. **Portable Backup/Restore for `focus_sessions`**
   - Added `PortableFocusSession.kt`, `PortableFocusSessionMapper.kt`, and optional `focusSessions` field on `PortableBackup.kt` and `ValidatedPortableBackup.kt` while keeping `schemaVersion = 1`.
   - Added validation rules in `PortableBackupValidator.kt`, `replaceAllSessions` in `FocusSessionDao.kt` & `FocusRepository.kt`, and transactional import/restore coordination in `BackupRestoreCoordinator.kt` + `AppContainer.kt`.

5. **AndroidTest Compilation Fix & Integration Coverage**
   - Fixed `recurrenceInterval` -> `recurrenceRule` in `BackupRestoreIntegrationTest.kt` and extended export assertion with `focusSessions`.

6. **Instrumented Test Verification Status**
   - `adb devices` and `emulator -list-avds` confirmed no connected Android device, AVD, or SDK `system-images` exist in the current environment.
   - `compileDebugAndroidTestKotlin` compiled cleanly with zero errors; runtime `connectedDebugAndroidTest` remains unexecuted.

7. **Dead Code Removal**
   - Removed unused `TasksScreen.kt` and `TasksViewModel.kt` and removed their factory entry from `AppViewModelProvider.kt`.

8. **Documentation Synchronization**
   - Updated `README.md`, `AGENTS.md`, `docs/product/CURRENT-SPEC.md`, and `docs/roadmap/duesoon-roadmap.md` to reflect `v1.9.7` (`versionCode = 17`, Room `v5`, Portable Schema `v1`). Left immutable git tags untouched.

9. **GitHub Actions CI/CD**
   - Added `.github/workflows/ci.yml` (runs `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `assembleDebug` on `main` pushes/PRs).
   - Added `.github/workflows/release.yml` (runs verification, decodes signing keystore from repository secrets `KEYSTORE_BASE64`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD`, builds signed release APK, and publishes GitHub Release on `v*` tags).

## Verification Results

- Command executed: `.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug --console=plain`
- Exit code: `0` (`BUILD SUCCESSFUL`)
- `testDebugUnitTest`: 110 tests across 21 test suites (`0 failures`, `0 errors`, `0 skipped`), including new tests in `PomodoroTimerCoordinatorTest`, `CreateTaskViewModelTest`, `HomeViewModelTest`, `PortableBackupValidatorTest`, and `BackupRestoreCoordinatorTest`.
- `compileDebugAndroidTestKotlin`: passed cleanly.
- `assembleDebug`: passed cleanly.
- `connectedDebugAndroidTest` (device/emulator runtime): not executed (no device or emulator image available).
