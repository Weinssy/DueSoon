package com.duesoon.app.data.backup

import android.net.Uri
import com.duesoon.app.data.repository.FocusRepository
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.domain.backup.BackupSerializer
import com.duesoon.app.domain.backup.BackupValidationError
import com.duesoon.app.domain.backup.PortableBackup
import com.duesoon.app.domain.backup.PortableBackupValidator
import com.duesoon.app.domain.backup.PortableFocusSessionMapper
import com.duesoon.app.domain.backup.PortableTaskMapper
import com.duesoon.app.domain.backup.ValidatedPortableBackup
import com.duesoon.app.domain.backup.ValidationMode
import com.duesoon.app.domain.backup.ValidationResult
import com.duesoon.app.domain.model.Task
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class BackupRestoreCoordinator(
    private val taskRepository: TaskRepository,
    private val backupStorage: BackupStorage,
    private val appVersion: String,
    private val notificationScheduler: NotificationScheduler,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val focusRepository: FocusRepository? = null,
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit = { block -> block() }
) {
    private val mutex = Mutex()

    suspend fun exportBackup(uri: Uri?): BackupResult<Unit> = mutex.withLock {
        if (uri == null) return BackupResult.Error.Cancelled
        try {
            val tasks = taskRepository.observeTasks().first()
            val portableTasks = tasks.map { PortableTaskMapper.toPortableTask(it) }
            val portableFocusSessions = focusRepository
                ?.getAllSessions()
                .orEmpty()
                .map { PortableFocusSessionMapper.toPortable(it) }

            val backup = PortableBackup(
                schemaVersion = 1,
                appVersion = appVersion,
                exportedAt = System.currentTimeMillis(),
                tasks = portableTasks,
                focusSessions = portableFocusSessions
            )

            val json = BackupSerializer.json.encodeToString(backup)
            val outputStream = backupStorage.openOutputStream(uri)
                ?: return BackupResult.Error.StorageError("Failed to open output stream")

            outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            return BackupResult.Success(Unit)
        } catch (e: Exception) {
            return BackupResult.Error.StorageError(e.message ?: "Unknown error during export")
        }
    }

    suspend fun prepareImport(uri: Uri?): BackupResult<ValidatedPortableBackup> = mutex.withLock {
        if (uri == null) return BackupResult.Error.Cancelled
        return readAndValidate(uri, ValidationMode.IMPORT)
    }

    suspend fun prepareRestore(uri: Uri?): BackupResult<ValidatedPortableBackup> = mutex.withLock {
        if (uri == null) return BackupResult.Error.Cancelled
        return readAndValidate(uri, ValidationMode.RESTORE)
    }

    private suspend fun readAndValidate(uri: Uri, mode: ValidationMode): BackupResult<ValidatedPortableBackup> {
        try {
            val inputStream = backupStorage.openInputStream(uri)
                ?: return BackupResult.Error.StorageError("Failed to open input stream")

            val json = inputStream.use { it.readBytes().toString(Charsets.UTF_8) }

            val backup = try {
                BackupSerializer.json.decodeFromString<PortableBackup>(json)
            } catch (e: Exception) {
                return BackupResult.Error.InvalidFormat(e.message ?: "Invalid JSON")
            }

            val validationResult = PortableBackupValidator.validate(backup, mode)
            return when (validationResult) {
                is ValidationResult.Success -> BackupResult.Success(validationResult.validatedBackup)
                is ValidationResult.Failure -> {
                    if (validationResult.errors.contains(BackupValidationError.UnsupportedSchema)) {
                        return BackupResult.Error.UnsupportedSchema("Schema version ${backup.schemaVersion} is not supported")
                    }
                    if (validationResult.errors.contains(BackupValidationError.EmptyBackup)) {
                        return BackupResult.Error.InvalidFormat("Backup file contains no tasks or focus sessions")
                    }
                    BackupResult.Error.ValidationErrors(validationResult.errors)
                }
            }
        } catch (e: Exception) {
            return BackupResult.Error.StorageError(e.message ?: "Unknown error during file read")
        }
    }

    suspend fun executeImport(validatedBackup: ValidatedPortableBackup): BackupResult<Unit> = mutex.withLock {
        return try {
            val beforeTasks = taskRepository.observeTasks().first().map { it.id }.toSet()
            runInTransaction {
                if (validatedBackup.tasks.isNotEmpty()) {
                    taskRepository.importTasks(validatedBackup.tasks)
                }
                if (validatedBackup.focusSessions.isNotEmpty()) {
                    focusRepository?.importSessions(
                        validatedBackup.focusSessions.map { it.copy(id = 0L, taskId = null) }
                    )
                }
            }
            val afterTasks = taskRepository.observeTasks().first()
            val importedTasks = afterTasks.filter { !beforeTasks.contains(it.id) }

            reconcileNotifications(importedTasks)
            BackupResult.Success(Unit)
        } catch (e: Exception) {
            BackupResult.Error.DatabaseError("Failed to import tasks due to a database error.")
        }
    }

    suspend fun executeRestore(validatedBackup: ValidatedPortableBackup): BackupResult<Unit> = mutex.withLock {
        return try {
            val oldTasks = taskRepository.observeTasks().first()
            runInTransaction {
                taskRepository.restoreTasks(validatedBackup.tasks)
                focusRepository?.replaceAllSessions(validatedBackup.focusSessions)
            }

            // Restore succeeded. Database is now the source of truth.
            oldTasks.forEach { notificationScheduler.cancelAll(it) }

            val restoredTasks = taskRepository.observeTasks().first()
            reconcileNotifications(restoredTasks)

            BackupResult.Success(Unit)
        } catch (e: Exception) {
            BackupResult.Error.DatabaseError("Failed to restore tasks due to a database error.")
        }
    }

    private suspend fun reconcileNotifications(tasks: List<Task>) {
        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        if (!prefs.notificationsEnabled) return

        val now = System.currentTimeMillis()
        tasks.forEach { task ->
            if (!task.completed) {
                if (task.deadline != null) {
                    notificationScheduler.schedule(task)
                }
                if (task.snoozedUntil != null) {
                    if (task.snoozedUntil > now) {
                        notificationScheduler.scheduleSnooze(task, task.snoozedUntil)
                    } else {
                        // Past snooze: silently clear it, do not fire immediate alarm.
                        taskRepository.clearSnooze(task.id)
                    }
                }
            }
        }
    }
}
