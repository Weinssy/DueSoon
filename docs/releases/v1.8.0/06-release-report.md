# Release Report (DueSoon v1.8.0)

## Overview
DueSoon v1.8.0 (Search & Archive Hardening) has been successfully packaged and prepared for release. The build meets all standards for production deployment.

## Version Metadata
- **Version Name:** `1.8.0`
- **Version Code:** `9`
- **Git Tag:** `v1.8.0` (Pending Execution)

## Release Artifacts Verified
1. **Unit Tests:** `testDebugUnitTest` - **PASSED** (0 failures).
2. **Release APK Build:** `assembleRelease` - **PASSED**.
   - Minification (`minifyReleaseWithR8`) completed successfully without obfuscation errors.
   - Resource shrinking and dex compilation succeeded.
3. **Spec Alignment:** All requirements verified and documented in `05-verification-report.md`.

## Execution Checklists Completed
- [x] Version bump in `app/build.gradle.kts`.
- [x] Release notes drafted at `release-notes-v1.8.0.md`.
- [x] Spec update mapped to `CURRENT-SPEC.md`.
- [x] Zero regressions caught in unit testing.
- [x] Release assembly compilation successful.

## Next Steps (Manual Execution)
The release is ready for GitHub deployment. The user must run the following exact commands to stage, commit, tag, and finalize the v1.8.0 release:

```bash
git add .
git commit -m "Release v1.8.0: Search & Archive Hardening"
git tag v1.8.0
```
