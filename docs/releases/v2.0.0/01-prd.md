# Product Requirements Document: DueSoon v2.0.0 (Cloud & Multi-Device Sync)

## 1. Context & Objectives
DueSoon v2.0.0 introduces a major architectural evolution: opt-in cloud synchronization and multi-device support. The core objective is to deliver seamless cross-device synchronization without compromising DueSoon’s foundational offline-first, zero-knowledge, and privacy-centric philosophy.

### Hard Invariants
1. **Offline-First Strict Parity:** Zero mandatory accounts or logins. Sync is completely opt-in; the app remains 100% functional locally without any network access.
2. **Zero-Knowledge / E2EE:** Task titles, descriptions, and categories must be encrypted with AES-256-GCM prior to transport. The remote backend sees only opaque ciphertext.
3. **Safe Room Migration (Schema 3 -> 4):** A table recreation strategy must be employed during migration to guarantee no unique constraint collisions on UUID population.
4. **Soft Delete Leak Prevention:** All existing DAO queries must enforce `isDeleted = 0`. Tombstone lifecycle must include an automated 30-day purge threshold.
5. **Deterministic Conflict Resolution:** Last-Write-Wins (LWW) augmented with monotonic revision counters must be utilized to protect against clock drift and deadlocks.

## 2. Product Modes & User Journey

### Mode 1: Pure Local (Default)
This mode remains exactly identical to v1.9.0.
- No network calls are made.
- No sync overhead exists.
- Total privacy and offline isolation are maintained out-of-the-box.

### Mode 2: E2EE Synced (Opt-in)
For users requiring multi-device sync, an opt-in flow is introduced.
- **Enable Sync:** The user navigates to Settings -> Sync.
- **Configuration:** The user inputs a custom Server Endpoint URL and enters (or generates) a Sync Passphrase.
- **Activation:** Once enabled, the background `WorkManager` initializes the sync engine, derives the encryption keys, and begins negotiating state with the remote backend.

## 3. Room Schema 4 & Query Overhaul

To support distributed state and synchronization, the local SQLite database must be upgraded from Schema 3 to Schema 4.

### New Columns (`TaskEntity`)
- `uuid: String` (Unique, Non-null): The primary sync identifier. (Legacy `id: Long` remains for fast local lookups/foreign keys).
- `isDeleted: Boolean`: Tombstone marker for soft deletes.
- `updatedAtUtc: Long`: UTC timestamp for conflict resolution.
- `revision: Long`: Monotonically incrementing counter on local edits.
- `syncState: String`: Tracks sync status (`DIRTY`, `SYNCED`, `CONFLICT`).

### Migration Strategy (`MIGRATION_3_4`)
To prevent SQLite unique index collisions during UUID population, the migration will utilize a **table-recreation strategy**:
1. Create a temporary table matching Schema 4.
2. Insert data from the old `tasks` table into the temporary table, generating `UUID.randomUUID().toString()` for `uuid` and initializing new columns (`isDeleted = 0`, `syncState = 'DIRTY'`, `revision = 1`).
3. Drop the old `tasks` table.
4. Rename the temporary table to `tasks`.
5. Recreate necessary indices (e.g., unique index on `uuid`).

### DAO Contract Updates
- **Read Operations:** All user-facing `SELECT` queries in `TaskDao` must enforce `WHERE isDeleted = 0`.
- **Tombstone Purge:** A background routine must execute a hard-delete for rows meeting the condition: `isDeleted = 1 AND syncState = 'SYNCED' AND updatedAtUtc < (now - 30 days)`.

## 4. Key Management & Crypto Pipeline (E2EE)

The encryption pipeline guarantees Zero-Knowledge transport.

### Specifications
- **Master Key Derivation:** A 256-bit symmetric key is derived from the user's Sync Passphrase using `PBKDF2WithHmacSHA256` (120,000 iterations) with a locally generated salt.
- **Symmetric Cipher:** `AES-256-GCM` is used for authenticated encryption. A secure, random 12-byte IV is generated for every payload encryption.
- **Key Storage:** The derived Master Sync Key is persisted securely in the Android KeyStore (`EncryptedSharedPreferences`).

### Remote Payload Structure
The JSON payload sent to and retrieved from the server must contain only sync metadata and opaque ciphertext:
```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "updatedAtUtc": 1710428400,
  "revision": 2,
  "isDeleted": false,
  "ciphertext": "base64_encoded_aes_gcm_blob",
  "iv": "base64_encoded_iv"
}
```

## 5. Conflict Resolution & Clock Drift Guard

Conflicts across devices will be resolved locally using a deterministic Last-Write-Wins (LWW) algorithm augmented with a revision counter.

### Resolution Priority
When comparing an incoming remote task against a local task with the same `uuid`:
1. **Primary Sort (Revision):** The record with the higher `revision` counter wins. (This mitigates clock drift issues where a device has an incorrect system time).
2. **Secondary Sort (Timestamp):** If `revision` counters are equal, the record with the higher `updatedAtUtc` wins.
3. **Tie-breaker:** If both `revision` and `updatedAtUtc` are identical, fallback to a deterministic lexicographical sort on the `ciphertext` string.

## 6. Background Scheduling & Sync Lifecycle

Synchronization relies on the Android `WorkManager` API.

### Execution Constraints
- **Network Requirement:** `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)`.
- **Battery Safety:** Sync workers must respect Android's Doze mode and battery optimization restrictions.

### Sync Triggers
- **Immediate Push (Outbox):** Enqueued immediately upon a local task mutation (`syncState = DIRTY`).
- **Periodic Pull:** Scheduled to fetch updates periodically while the device is active and connected.

## 7. Acceptance Criteria (AC)

- **AC-01 (Offline Parity):** The app functions identically to v1.9.0 without sync enabled; zero network traffic is generated.
- **AC-02 (Zero-Knowledge Transport):** Network inspection reveals only encrypted base64 blobs; no plain text leaks (titles, descriptions, categories are fully encrypted).
- **AC-03 (Migration Integrity):** Upgrading from Schema 3 to Schema 4 preserves all existing tasks, successfully populating unique UUIDs and setting `isDeleted = 0` without SQLite constraint errors.
- **AC-04 (Soft Delete Isolation):** Tasks marked as `isDeleted = 1` immediately disappear from Home, Archive, Search, and Glance widget feeds.
- **AC-05 (Deterministic LWW):** Conflicts resolve predictably according to the resolution priority rules without data corruption or app crashes.
- **AC-06 (Clock Drift Protection):** Stale offline edits from a device with a severely incorrect system clock do not override newer revisions (protected by the `revision` counter).
- **AC-07 (Background Battery Safety):** Sync `WorkManager` tasks run only when the network is connected and strict battery optimization rules are respected.
- **AC-08 (Backup Interoperability):** The `PortableBackup` JSON export/import mechanism works seamlessly across v1.x configurations and the new v2.0 sync schema.
