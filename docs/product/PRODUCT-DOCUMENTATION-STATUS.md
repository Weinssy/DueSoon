# DueSoon — Product Documentation Status

This document maps the authority and consistency of DueSoon's documentation. The source code is the final authority for actual implementation.

## Documentation Hierarchy

**Product vision / original intent**
↓
`docs/product/PRD-v1.0-original.md`
*Status: Historical. Records the original vision and pre-development requirements. Not authoritative for current implementation.*

**Current product behavior**
↓
`docs/product/CURRENT-SPEC.md`
*Status: Current Source of Truth. Authoritative for high-level product behavior, features, and capabilities based on actual repository state.*

**Release-specific scope and implementation records**
↓
`docs/releases/vX.Y.Z/`
*Status: Active/Historical records of specific version development cycles.*
- Authoritative for v1.2.0 features: `docs/releases/v1.2.0/`
- Authoritative for v1.3.0 features (Backup/Restore): `docs/releases/v1.3.0/`

**Technical architecture / implementation details**
↓
*Release architecture documents + repository source code*
- v1.2.0 Architecture: `docs/releases/v1.2.0/02-architecture.md`
- v1.3.0 Architecture: `docs/releases/v1.3.0/03-architecture.md`

## Specific Sources of Truth

- **Roadmap:** `docs/roadmap/duesoon-roadmap.md` and `README.md`
- **Data/Backup specification:** `docs/releases/v1.3.0/02-prd.md` and `docs/releases/v1.3.0/04-serialization-foundation.md`
- **Domain Model:** Actual source code in `app/src/main/java/com/duesoon/app/domain/model/`

## Known Documentation Inconsistencies

1. **Original PRD Scope:** The original PRD v1.0 references several features as "Future Roadmap" or "Could Have" (e.g., Snooze, Recurring tasks, Search). These are now implemented. `CURRENT-SPEC.md` is the authoritative source for their status.
2. **Reminder Behaviors:** The original PRD provided a hypothetical Smart Reminder algorithm. The actual implemented logic resides in `SmartReminderCalculator.kt`, which is documented in `CURRENT-SPEC.md`.

*Note: If contradictions exist between documents, the current repository implementation and `CURRENT-SPEC.md` supersede historical documents.*
