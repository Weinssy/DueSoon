# STEP 9.12 — Final Release Verification

**Target Release:** v1.3.0
**Date:** 2026-09-16

This document contains the final release verification matrix for DueSoon v1.3.0.

## 1. Repository State
- **Branch:** `main`
- **Commit:** `4a6b5ea74c4256cc73cd3a82aa594bdb34100abf`
- **Git Status:** Clean (all files committed). Untracked artifact: `DueSoon-v1.3.0-release.apk`
- **.idea state:** `git rm -r .idea` applied, fully tracked as deleted.
- **Schemas:** `app/schemas/com.duesoon.app.data.local.AppDatabase/3.json` is tracked.
- **Secrets:** No keys or `keystore.properties` are committed.

## 2. Version Identity
`app/build.gradle.kts` configuration:
- `versionCode` = 4
- `versionName` = "1.3.0"
- `archivesBaseName` dynamically generates the correct output file names based on `versionName`.

## 3. Full Test Matrix
| Command | Result |
|---|---|
| `./gradlew testDebugUnitTest` | PASS |
| `./gradlew assembleDebug` | PASS |
| `./gradlew assembleRelease` | PASS |
| `./gradlew assembleAndroidTest` | PASS |
| `./gradlew connectedDebugAndroidTest` | BLOCKED (No ADB device attached to local environment) |

*Note: The user previously reported `connectedDebugAndroidTest` as passing after Step 9.10 fixes. However, the final pipeline gate mandates a re-run here, which could not execute due to lack of a connected device/emulator.*

## 4. Release APK Verification
- **Generated File:** `app/build/outputs/apk/release/DueSoon-v1.3.0-release.apk`
- **Copied File:** `DueSoon-v1.3.0-release.apk`
- **APK Size:** 2,805,291 bytes (approx. 2.8 MB)
- **Build Variant:** Release

## 5. SHA-256
Checksum of `DueSoon-v1.3.0-release.apk`:
`C91FE2B71CDFC3972B7DA45936AC199B81631B9B3C3F7002FBDB0DEFE5B2E075`

## 6. Release Configuration
- **R8/ProGuard:** Enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).
- **Keys:** Release build successfully skipped explicit local signing because `keystore.properties` safely omitted missing keys, producing an unsigned release APK suitable for Play Store App Bundle conversion or manual signing.
- **Cloud/Account:** None introduced. Fully offline.

## 7. Git Diff Audit
The v1.3.0 changes exclusively implement:
- SAF export/import foundation (`BackupStorage`, `SafBackupStorage`)
- JSON serialization (`PortableBackup`, `PortableTask`, `PortableTaskMapper`)
- Validation (`PortableBackupValidator`)
- Database integration (`TaskRepository`, `TaskDao`)
- Execution & Notification Reconciliation (`BackupRestoreCoordinator`, `AndroidNotificationScheduler`)
- UI Settings (`SettingsScreen`, `SettingsViewModel`)
- Strict Documentation

No feature regressions or unrelated features were found.

## 8. Release Documentation
All documents in `docs/releases/v1.3.0/` align consistently with the codebase. The `CURRENT-SPEC.md` and `PRD-v1.0-original.md` accurately reflect the structural evolution. 

## 9. Final Release Gate
- An annotated git tag `v1.3.0` was successfully created pointing to commit `4a6b5ea7`.

## 10. GitHub Release
- **Status:** BLOCKED
- **Reason:** The GitHub CLI (`gh`) is not installed or available in the local execution path. Furthermore, the `connectedDebugAndroidTest` was technically BLOCKED. Following protocol, the GitHub release is aborted.

## 11. Post-Release Verification
- Local git tag `v1.3.0` exists.
- `DueSoon-v1.3.0-release.apk` exists in the repository root.

---

### STEP 9.12 FINAL STATUS

Status: BLOCKED
Release: v1.3.0
Commit: 4a6b5ea74c4256cc73cd3a82aa594bdb34100abf
Tag: v1.3.0
APK: DueSoon-v1.3.0-release.apk
SHA-256: C91FE2B71CDFC3972B7DA45936AC199B81631B9B3C3F7002FBDB0DEFE5B2E075
Unit Tests: PASS
Instrumentation Build: PASS
Instrumentation Runtime: BLOCKED
GitHub Release: BLOCKED
Working Tree: CLEAN (except untracked release APK)
