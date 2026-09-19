package com.duesoon.app.core.sync

import android.content.Context
import android.util.Base64
import com.duesoon.app.core.crypto.CryptoManager
import com.duesoon.app.core.crypto.SecureStorage
import com.duesoon.app.data.local.TaskDao
import com.duesoon.app.data.local.TaskEntity
import com.duesoon.app.data.remote.SyncApiService
import com.duesoon.app.data.remote.SyncPayloadDto
import com.duesoon.app.data.remote.SyncPushRequest
import com.duesoon.app.data.remote.TaskPayload
import com.duesoon.app.domain.model.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.crypto.AEADBadTagException

class SyncEngine(
    private val context: Context,
    private val taskDao: TaskDao,
    private val syncApiService: SyncApiService,
    private val secureStorage: SecureStorage,
    private val cryptoManager: CryptoManager
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        val masterKey = secureStorage.getDerivedMasterKey() ?: return@withContext false
        val authToken = secureStorage.getAuthToken() ?: return@withContext false

        var syncSuccess = true

        try {
            // --- 1. Push Phase ---
            val dirtyTasks = taskDao.getDirtyTasks()
            if (dirtyTasks.isNotEmpty()) {
                val payloads = dirtyTasks.map { task ->
                    val payloadObj = TaskPayload(
                        title = task.title,
                        description = task.description,
                        deadline = task.deadline,
                        category = task.category,
                        priority = task.priority,
                        reminderType = task.reminderType,
                        isRecurring = task.isRecurring,
                        recurrenceInterval = task.recurrenceInterval,
                        completed = task.completed,
                        snoozedUntil = task.snoozedUntil,
                        createdAt = task.createdAt,
                        updatedAt = task.updatedAt
                    )
                    val plaintext = json.encodeToString(payloadObj).toByteArray(Charsets.UTF_8)
                    val ciphertext = cryptoManager.encrypt(plaintext, masterKey)
                    val base64Cipher = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

                    SyncPayloadDto(
                        uuid = task.uuid,
                        ciphertext = base64Cipher,
                        revision = task.revision,
                        updatedAtUtc = task.updatedAtUtc,
                        isDeleted = task.isDeleted
                    )
                }

                val pushResponse = syncApiService.push(SyncPushRequest(payloads))
                if (pushResponse.success) {
                    for (task in dirtyTasks) {
                        taskDao.markTaskSynced(task.uuid, task.revision)
                    }
                } else {
                    syncSuccess = false
                }
            }

            // --- 2. Pull Phase ---
            val pullResponse = syncApiService.pull(0L)
            
            // --- 3. Reconcile Phase ---
            for (remoteDto in pullResponse.payloads) {
                try {
                    val local = taskDao.getTaskByUuid(remoteDto.uuid)
                    
                    val verdict = ConflictResolver.resolve(
                        local = local,
                        remoteUuid = remoteDto.uuid,
                        remoteUpdatedAtUtc = remoteDto.updatedAtUtc,
                        remoteRevision = remoteDto.revision,
                        remoteIsDeleted = remoteDto.isDeleted,
                        remoteCiphertext = remoteDto.ciphertext,
                        localCiphertext = null
                    )

                    if (verdict == ResolutionVerdict.APPLY_REMOTE) {
                        if (remoteDto.isDeleted) {
                            if (local != null) {
                                val deletedLocal = local.copy(
                                    isDeleted = true,
                                    updatedAtUtc = remoteDto.updatedAtUtc,
                                    revision = remoteDto.revision,
                                    syncState = SyncState.SYNCED.name
                                )
                                taskDao.update(deletedLocal)
                            }
                        } else {
                            val decodedCipher = Base64.decode(remoteDto.ciphertext, Base64.NO_WRAP)
                            val plaintextBytes = cryptoManager.decrypt(decodedCipher, masterKey)
                            val taskPayload = json.decodeFromString<TaskPayload>(String(plaintextBytes, Charsets.UTF_8))
                            
                            val newLocal = TaskEntity(
                                id = local?.id ?: 0L,
                                title = taskPayload.title,
                                description = taskPayload.description,
                                deadline = taskPayload.deadline,
                                category = taskPayload.category,
                                priority = taskPayload.priority,
                                reminderType = taskPayload.reminderType,
                                isRecurring = taskPayload.isRecurring,
                                recurrenceInterval = taskPayload.recurrenceInterval,
                                completed = taskPayload.completed,
                                snoozedUntil = taskPayload.snoozedUntil,
                                createdAt = taskPayload.createdAt,
                                updatedAt = taskPayload.updatedAt,
                                uuid = remoteDto.uuid,
                                isDeleted = false,
                                updatedAtUtc = remoteDto.updatedAtUtc,
                                revision = remoteDto.revision,
                                syncState = SyncState.SYNCED.name
                            )
                            if (local == null) {
                                taskDao.insert(newLocal)
                            } else {
                                taskDao.update(newLocal)
                            }
                        }
                    }
                } catch (e: AEADBadTagException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                    syncSuccess = false
                }
            }

            // --- 4. Cascade ---
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            taskDao.purgeTombstones(thirtyDaysAgo)

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }

        return@withContext syncSuccess
    }
}
