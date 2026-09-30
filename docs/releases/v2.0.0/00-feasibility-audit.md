# Architectural Feasibility Audit: DueSoon v2.0.0 (Cloud & Multi-Device Sync)

## 1. Executive Summary & Migration Decision
The primary objective of DueSoon v2.0.0 is to introduce opt-in cloud synchronization and multi-device support while strictly adhering to our local-first, zero-knowledge philosophy. The sync mechanism must remain transparent, battery-efficient, and robust against poor network conditions.

### Recommended Sync Protocol: Custom Lightweight E2EE Sync Server (Option B)
After evaluating self-hosted WebDAV, CRDTs, and a custom REST polling/push server, **Option B (Lightweight Custom E2EE Sync Server with REST + Outbox)** is the recommended path forward.
- **Why not WebDAV/Nextcloud?** While fully decentralized, WebDAV lacks granular conflict resolution semantics tailored for specific JSON entities. It often relies on complete file overwrites, which is brittle for multi-device rapid edits.
- **Why not CRDTs?** True Conflict-free Replicated Data Types require significantly bloated storage footprints (storing all operational histories and tombstones indefinitely), which contradicts our minimalist, offline-first SQLite approach. 
- **The Choice (State-based LWW):** A custom backend holding encrypted JSON payloads (opaque to the server) combined with state-based Last-Write-Wins (LWW) resolution driven by UTC timestamps provides the best balance of minimalism, conflict determinism, and privacy. The server acts purely as a dumb relay for encrypted blobs.

## 2. Schema 4 Proposal & Migration Plan

### Current Schema 3 Assessment
`TaskEntity` currently defines `id` (auto-increment Long), `title`, `description`, `deadline`, `category`, `priority`, `reminderType`, `isRecurring`, `recurrenceInterval`, `completed`, `snoozedUntil`, `createdAt`, `updatedAt`. 

### Required Additions for Schema 4
To support robust synchronization, the database must transition from a purely local state to a distributed state model. We need to introduce the following columns:
- `uuid: String` (Primary Sync Key): The auto-incremented local `id: Long` will remain for legacy foreign keys and fast local lookups, but `uuid` becomes the immutable, globally unique identifier across all devices.
- `isDeleted: Boolean` (default `false`): Enables soft deletes (tombstones). When a user deletes a task, it is marked as `isDeleted = true` so the deletion event can propagate to other devices instead of silently vanishing.
- `updatedAtUtc: Long`: A rigorous UTC timestamp used for LWW conflict resolution.
- `syncState: String`: Tracks the synchronization status (`SYNCED`, `DIRTY`, `CONFLICT`) to orchestrate background uploads via the Outbox pattern.

### SQL Migration (MIGRATION_3_4)
```sql
-- Step 1: Add new columns with default values
ALTER TABLE `tasks` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT '';
ALTER TABLE `tasks` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `tasks` ADD COLUMN `updatedAtUtc` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `tasks` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'DIRTY';

-- Step 2: Create Index on UUID for fast remote lookups
CREATE UNIQUE INDEX IF NOT EXISTS `index_tasks_uuid` ON `tasks` (`uuid`);
```
*(Note: A post-migration hook in Room will be required to iterate over all existing rows and populate `uuid` with `UUID.randomUUID().toString()` and set `updatedAtUtc` to current time.)*

## 3. Conflict Resolution Specification
We will utilize a **Deterministic State-Based Last-Write-Wins (LWW)** algorithm.

### Conflict Rules
1. **Clock Synchronization:** All devices must record mutations using strict `System.currentTimeMillis()` mapped to UTC (`updatedAtUtc`).
2. **Resolution Algorithm:** When Device A and Device B both mutate the same `uuid`:
   - The device with the highest `updatedAtUtc` wins.
   - If `updatedAtUtc` is exactly identical (rare), we fall back to lexicographical sorting of the encrypted payloads to ensure deterministic resolution without deadlocks.
3. **Edge Case 1 - Simultaneous Offline Edits:** Device A edits the title, Device B marks as completed. When both connect, the payload with the highest `updatedAtUtc` completely overwrites the other. (We accept full state overwrites over granular field-level CRDT merging to preserve architecture simplicity).
4. **Edge Case 2 - Deleted vs Edited:** If Device A soft-deletes (`isDeleted = true`) a task, and Device B edits it later offline, Device B's edit wins (resurrecting the task) because its `updatedAtUtc` is higher. This prevents accidental data loss from stale offline deletions.
5. **Recurrence Protection:** Recurrence advancement (`recurrenceInterval`) is calculated strictly based on the baseline `deadline`. If two devices advance the same task, the resulting next deadline will be mathematically identical, merging safely under LWW without duplicating alarms.

## 4. End-to-End Encryption (E2EE) Pipeline
The backend must operate with Zero-Knowledge. It will only store and relay opaque encrypted blobs.

### Encryption Suite
- **Key Derivation:** `Argon2id` or `PBKDF2` (using `androidx.security.crypto` or Tink) to derive a 256-bit symmetric Master Sync Key from a user-provided Sync Passphrase.
- **Cipher:** `AES-256-GCM` with a random 96-bit Initialization Vector (IV) for every payload.
- **Payload Format (Sent to Server):** 
  ```json
  {
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "updatedAtUtc": 1710428400,
    "isDeleted": false,
    "ciphertext": "base64_encoded_aes_gcm_blob",
    "iv": "base64_encoded_iv"
  }
  ```
- **Local Storage:** The derived Master Sync Key is securely stored in the Android KeyStore (`EncryptedSharedPreferences`).

## 5. Background Scheduling & Sync Engine
The sync engine will rely on Android's **WorkManager** to handle the Outbox pattern.

- **Constraints:** `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()`.
- **Triggers:**
  - *Immediate Push:* Enqueued immediately when a task is mutated locally (`syncState = DIRTY`).
  - *Periodic Pull:* A periodic worker running every X hours to catch mutations from other devices.
  - *Manual Pull-to-Refresh:* Exposed in the UI for instant forced synchronization.

## 6. Proposed Roadmap & Scope Matrix (v2.0.0)

### In-Scope (MVP for v2.0.0)
- Room Schema 4 Migration (Soft deletes, UUIDs, sync states).
- In-app Sync Settings UI (Setup passphrase, enable/disable sync).
- Android KeyStore E2EE implementation (`AES-256-GCM`).
- `WorkManager` Background Sync Engine (Periodic sync + Immediate push on mutation).
- Deterministic LWW Conflict Resolution locally.
- Backward compatibility for `PortableBackup` JSON schemas.

### Out of Scope (Deferred to v2.1+)
- Sharing tasks or collaborating with other users (Multi-user sync).
- Web client or iOS client.
- Conflict Resolution UI (Silent deterministic resolution only).
- Granular field-level CRDT merging.

---
**Status:** STEP 16.0 Feasibility Audit is Complete. The architecture is sound and the invariants are protected. Ready for PRD authoring.
