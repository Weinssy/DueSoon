# Architecture Design: DueSoon v2.0.0 (Cloud & Multi-Device Sync Architecture)

## 1. Database & Migration Architecture (`TaskEntity` Schema 4)

### Entity Specification
The `TaskEntity` will be expanded to incorporate the distributed state model.
```kotlin
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String?,
    val deadline: Long?,
    val category: String?,
    val priority: String,
    val reminderType: String,
    val isRecurring: Boolean,
    val recurrenceInterval: String?,
    val completed: Boolean,
    val snoozedUntil: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    
    // v2.0.0 Sync Columns
    val uuid: String,
    val isDeleted: Boolean = false,
    val updatedAtUtc: Long = 0,
    val revision: Long = 0,
    val syncState: String = "DIRTY" // DIRTY, SYNCING, SYNCED
)
```

### `MIGRATION_3_4` Implementation
A complex table-recreation migration is required to populate `uuid` without triggering unique constraint violations in standard SQL alters.

**Steps:**
1. Execute `CREATE TABLE tasks_temp (...)` with the Schema 4 definition.
2. Open a Kotlin Cursor on the old `tasks` table. Iterate through every existing row.
3. For each row, generate `UUID.randomUUID().toString()`, map the legacy columns, and set `isDeleted = 0`, `syncState = 'DIRTY'`, `revision = 1`, and `updatedAtUtc = System.currentTimeMillis()`.
4. Insert the augmented row into `tasks_temp`.
5. Execute `DROP TABLE tasks`.
6. Execute `ALTER TABLE tasks_temp RENAME TO tasks`.
7. Execute `CREATE UNIQUE INDEX IF NOT EXISTS index_tasks_uuid ON tasks (uuid)`.

### DAO Query Overhaul
All user-facing read operations must enforce soft-delete isolation.
- `getTasksByCompletionStatus(completed)` -> Add `AND isDeleted = 0`
- `getTaskById(id)` -> Add `AND isDeleted = 0`
- `searchTasks(query)` -> Add `AND isDeleted = 0`

**Tombstone Cleanup Routine:**
```sql
@Query("DELETE FROM tasks WHERE isDeleted = 1 AND syncState = 'SYNCED' AND updatedAtUtc < :cutoffTimestamp")
suspend fun purgeTombstones(cutoffTimestamp: Long)
```

## 2. End-to-End Encryption Architecture

### Salt Synchronization Protocol
To ensure Device A and Device B generate the exact same Master Key from the Sync Passphrase, the system requires a deterministic Salt.
1. When a user first configures Sync (Device A), a random 16-byte salt is generated.
2. This salt is transmitted to the server unencrypted during the initial account handshake and persisted on the remote backend.
3. When Device B connects using the exact same server endpoint and account identifier, it pulls the registered salt before executing the KDF (Key Derivation Function).

### Cipher Suite
- **KDF:** `PBKDF2WithHmacSHA256` using the synchronized remote salt, with 120,000 iterations to derive a 256-bit symmetric Master Key.
- **Cipher:** `AES-256-GCM` with a cryptographically secure random 12-byte IV for every payload.
- **Key Storage:** The derived Master Sync Key is securely housed locally within the Android KeyStore (`EncryptedSharedPreferences`), never transmitted.

### Payload Transformation
Only structural routing metadata is sent in plaintext.
- **Plaintext Envelope:** `uuid`, `updatedAtUtc`, `revision`, `isDeleted`.
- **Ciphertext Blob:** Contains a JSON representation of `title`, `description`, `category`, `priority`, `reminderType`, `deadline`, `isRecurring`, `recurrenceInterval`, `completed`, and `snoozedUntil`.

## 3. Outbox Pattern & Sync Engine (`SyncEngine.kt`)

### Local Mutation Interceptor
All repository modification methods (Insert, Update, Soft-Delete) enforce the Outbox pattern:
1. Apply the user's change.
2. Increment `revision` by 1.
3. Update `updatedAtUtc` to current UTC timestamp.
4. Set `syncState = 'DIRTY'`.

### Sync Protocol Cycle
The `SyncEngine.kt` orchestrates the REST protocol.
- **Phase 1 (Push):** Retrieve all `TaskEntity` where `syncState == 'DIRTY'`. Encrypt the sensitive fields into the payload envelope. POST `/sync/push`. On a successful 200 OK, update local DB setting `syncState = 'SYNCED'`.
- **Phase 2 (Pull):** Execute GET `/sync/pull?since={lastSyncTimestamp}`. The server returns all payloads modified since the timestamp.
- **Phase 3 (Reconcile):** Decrypt incoming payloads. Apply the Conflict Resolution algorithm (LWW). Persist winning mutations to the local Room database, marking them immediately as `SYNCED`.
- **Phase 4 (Cascade):** The SyncEngine triggers the side-effect cascade:
  - Invokes `AndroidNotificationScheduler.reconcileAlarms()` to configure or cancel hardware alarms for incoming/deleted tasks.
  - Invokes `DueSoonWidgetUpdater.updateAllWidgets()` to refresh the Glance widget UI.

## 4. Conflict Resolution Engine Specification
Deterministic LWW (Last-Write-Wins) guarantees consistency without CRDT overhead.

**Resolution Algorithm:**
When comparing an incoming Remote Task vs Local Task with the same `uuid`:
1. **Primary Sort:** Compare `revision` counters. The highest revision strictly wins.
2. **Secondary Sort:** If revisions are exactly identical, compare `updatedAtUtc`. The highest timestamp wins.
3. **Tie-Breaker:** If both are identical (exceedingly rare edge case), perform a string comparison (`compareTo`) on the encrypted `ciphertext` blobs. The lexicographically larger string wins.

If Remote wins, update local Room database. If Local wins, discard the remote payload and wait for the local `DIRTY` task to push.

## 5. Background Job Scheduling (`SyncWorker.kt`)
The sync pipeline runs invisibly in the background leveraging `WorkManager`.

- **Periodic Work Request:** Scheduled for 1-hour intervals. Constraints: `NetworkType.CONNECTED` and `BatteryNotLow`.
- **Expedited One-Time Request:** Triggered immediately when the user mutates a task locally, providing real-time propagation if the network is active.
- **Manual Pull-to-Refresh:** Exposed within the `HomeScreen` UI (via SwipeRefresh) and a "Sync Now" button in Settings, bypassing the 1-hour interval.

## 6. Verification & Test Strategy
Comprehensive unit testing is required before integrating the UI.
- `Migration3To4Test`: Verifies SQLite table-recreation migration utilizing Room's `MigrationTestHelper`. Proves UUID population and data retention.
- `CryptoManagerTest`: Validates `AES-256-GCM` encryption/decryption parity, string encoding robustness, and bad passphrase rejection.
- `ConflictResolverTest`: Matrix testing of Remote Wins (newer revision), Local Wins (newer timestamp), Tie-breakers, and Tombstone override scenarios.
- `SyncEngineTest`: Mocks the REST transport layer and verifies the Phase 1-4 Push/Pull orchestration cycle.
