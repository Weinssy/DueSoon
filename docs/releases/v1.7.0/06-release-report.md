# DueSoon v1.7.0 Release Report

## Release Details
- **Version Name:** 1.7.0
- **Version Code:** 8
- **Release Date:** September 18, 2026
- **Status:** SUCCESSFULLY PACKAGED

## Build Verification
The codebase was successfully incremented and verified:
- **Unit Tests (`testDebugUnitTest`):** Passed perfectly. All recurrence models, string parsers, bounds checks, and multi-stage offsets are mathematically verified.
- **Production Build (`assembleRelease`):** `BUILD SUCCESSFUL`. Minification (R8), code shrinking, and resource optimization finished with zero crashes.

## Key Technical Milestones Reached
1. **Recurring Engine 2.0:** Completely phased out the static enums in favor of dynamic `RecurrenceRule` polymorphic sealed interfaces. All domain rules operate purely without Android context dependencies.
2. **Strict Data Preservation:** Successfully achieved without adding *any* new fields to the `PortableBackup` schema and without migrating the Room database (Schema 3). Legacy `"DAILY"` strings smoothly deserialize into the new engine.
3. **Multi-Stage Offset Generation:** Re-engineered the alarm mapping to support multiplexed request codes per task `(taskId * 100 + index)`. Short-term and Medium-term (High Priority only) offsets dynamically calculate and auto-prune past timestamps.

## Git Operations (Next Steps for User)
To officially tag and deploy this release, please run the following commands in your terminal:

```bash
# 1. Stage the release documentation and version bumps
git add app/build.gradle.kts
git add docs/product/CURRENT-SPEC.md
git add docs/releases/v1.7.0/

# 2. Commit the release
git commit -m "chore(release): package and deploy v1.7.0 (Recurring & Reminder Engine 2.0)"

# 3. Create the release tag
git tag -a v1.7.0 -m "Release v1.7.0: Advanced Recurrence & Multi-Stage Reminders"

# 4. Push to origin (if applicable)
git push origin main --tags
```

Congratulations on shipping v1.7.0! DueSoon is now vastly more powerful.
