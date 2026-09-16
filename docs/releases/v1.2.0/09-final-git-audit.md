# DueSoon v1.2.0 — Final Git Audit

## 1. Audit Date
2026-09-16 07:18:00 WIB

## 2. Git State
- **Branch:** main
- **Commit before release:** e41a516 (Merge branch 'main')
- **Commit after release:** `af6ebe1` (Release v1.2.0: Search, Sorting, Recurring Polish, and Snooze)
- **Working tree status:** Cleaned and staged properly.
- **Remote status:** Up to date with origin/main.
- **Push status:** NOT PUSHED.

## 3. Version
- **versionName:** 1.2.0
- **versionCode:** 3
- **applicationId:** com.duesoon.app
- **app name:** DueSoon

## 4. Release Scope
- **Search:** Title-only, case-insensitive, AND filtering.
- **Sorting:** Deadline, Priority, Title, Created.
- **Recurring UI:** Polish completed, native intervals supported.
- **Snooze:** 10m, 1h, Tomorrow, persisted state, stale-alarm protection.
- **Localization:** Indonesian strings implemented.
- **Release identity polish:** BuildConfig integration, app name, and APK filename configured.

## 5. Security / Repository Hygiene
- **secrets:** None staged. Keystore is securely ignored.
- **credentials:** None staged.
- **APK/AAB:** None staged. Properly ignored under `/build`.
- **build artifacts:** Safely ignored.
- **local.properties:** Safely ignored.
- **IDE files:** Safely ignored.
- **generated files:** Safely ignored.

## 6. Test / Build Result
- **unit tests:** PASS (JVM execution successful).
- **AndroidTest compilation:** PASS.
- **AndroidTest runtime status:** PENDING (No physical emulator environment for runtime verification).
- **debug build:** PASS.
- **release build:** PASS.
- **manual runtime verification status:** PASS (Executed and confirmed externally by user).

## 7. Documentation
- `docs/releases/v1.2.0/00-baseline-audit.md`
- `docs/releases/v1.2.0/01-prd.md`
- `docs/releases/v1.2.0/02-architecture.md`
- `docs/releases/v1.2.0/03-search.md`
- `docs/releases/v1.2.0/04-sorting.md`
- `docs/releases/v1.2.0/05-recurring.md`
- `docs/releases/v1.2.0/06-snooze.md`
- `docs/releases/v1.2.0/07-testing.md`
- `docs/releases/v1.2.0/08-release-verification.md`
- `docs/releases/v1.2.0/09-final-git-audit.md`
*(All documentation confirmed present and accurate)*

## 8. Commit
- **Commit Hash:** `af6ebe151347666fe92966dab27d0d1870e94da5`
- **Commit Message:** Release v1.2.0: Search, Sorting, Recurring Polish, and Snooze

## 9. Tag
- **tag:** v1.2.0
- **annotated:** yes
- **target commit hash:** `af6ebe151347666fe92966dab27d0d1870e94da5`

## 10. Push
NOT PUSHED.

## 11. Final Status
PASS — v1.2.0 committed and tagged locally.
