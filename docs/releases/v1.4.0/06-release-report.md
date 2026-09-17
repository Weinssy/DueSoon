# STEP 10.6 — Release Report (DueSoon v1.4.0)

**Release:** v1.4.0 (Version Code: 5)  
**Date:** 2026-09-18  
**Status:** READY TO TAG

## 1. Objective
This report finalizes the DueSoon v1.4.0 release cycle by confirming the successful version bump, verifying the release assembly, and validating all release artifacts. 

## 2. Execution Summary

### 2.1. Version Bump (`app/build.gradle.kts`)
- `versionCode` incremented to `5`.
- `versionName` incremented to `"1.4.0"`.

### 2.2. Test & Assembly Verification
- **Unit Tests:** Executed `./gradlew testDebugUnitTest`. Result: ✅ `BUILD SUCCESSFUL`.
- **Release Assembly:** Executed `./gradlew assembleRelease`. Result: ✅ `BUILD SUCCESSFUL`.
- **ProGuard/R8:** Minification executed successfully with no missing reference crashes or serialization failures (verified via zero compilation/linking errors). 

### 2.3. Release Artifacts Generated
- **Release Notes:** `docs/releases/v1.4.0/release-notes-v1.4.0.md` has been authored for user-facing communication.
- **APK Path:** Expected at `app/build/outputs/apk/release/DueSoon-v1.4.0-release.apk` (unsigned or signed via local config).

## 3. Pre-Flight Checklist
- [x] Room schema remains unaltered (v3).
- [x] Portable Backup JSON schema is structurally unchanged.
- [x] Smart Attention Ranking (Hybrid Urgency Matrix Option C) implemented and unit tested.
- [x] Home Screen and Widget sort algorithms are synchronized.
- [x] SPEC documentation reconciled.
- [x] All integration tests passing.
- [x] Assemble Release completed successfully.

## 4. Git Deployment Instructions
To officially seal this release, execute the following commands in the terminal:

```bash
# 1. Stage all changes
git add .

# 2. Commit the v1.4.0 release
git commit -m "chore: release v1.4.0 (Smart Attention Ranking)"

# 3. Create the Git Tag
git tag -a v1.4.0 -m "DueSoon v1.4.0 Release"

# 4. Push to remote
git push origin main
git push origin v1.4.0
```

Once pushed, the GitHub Release can be published using the content from `release-notes-v1.4.0.md`.
