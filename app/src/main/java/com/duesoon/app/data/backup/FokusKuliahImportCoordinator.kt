package com.duesoon.app.data.backup

import android.net.Uri
import com.duesoon.app.data.repository.FocusRepository
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.domain.backup.FokusKuliahBackupParser
import com.duesoon.app.domain.backup.FokusKuliahParseResult
import com.duesoon.app.domain.backup.ParsedFokusKuliahBackup
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class FokusKuliahImportCoordinator(
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository,
    private val backupStorage: BackupStorage,
    private val notificationScheduler: NotificationScheduler,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val mutex = Mutex()

    open suspend fun prepareImport(uri: Uri?): BackupResult<ParsedFokusKuliahBackup> = mutex.withLock {
        if (uri == null) return BackupResult.Error.Cancelled
        return try {
            val inputStream = backupStorage.openInputStream(uri)
                ?: return BackupResult.Error.StorageError("Gagal membuka berkas cadangan.")
            val rawJson = inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            when (val parsed = FokusKuliahBackupParser.parse(rawJson)) {
                is FokusKuliahParseResult.Success -> BackupResult.Success(parsed.data)
                is FokusKuliahParseResult.Error -> BackupResult.Error.InvalidFormat(parsed.message)
            }
        } catch (e: Exception) {
            BackupResult.Error.StorageError(e.message ?: "Gagal membaca berkas cadangan.")
        }
    }

    open suspend fun executeImport(parsed: ParsedFokusKuliahBackup): BackupResult<Unit> = mutex.withLock {
        return try {
            if (parsed.tasks.isNotEmpty()) {
                val beforeIds = taskRepository.observeTasks().first().map { it.id }.toSet()
                taskRepository.importTasks(parsed.tasks)
                val afterTasks = taskRepository.observeTasks().first()
                val importedTasks = afterTasks.filter { !beforeIds.contains(it.id) }

                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                if (prefs.notificationsEnabled) {
                    importedTasks.forEach { task ->
                        if (!task.completed && task.deadline != null) {
                            notificationScheduler.schedule(task)
                        }
                    }
                }
            }

            if (parsed.focusSessions.isNotEmpty()) {
                focusRepository.importSessions(parsed.focusSessions)
            }

            if (parsed.pomodoroSettings != null) {
                userPreferencesRepository.updatePomodoroSettings(parsed.pomodoroSettings)
            }

            BackupResult.Success(Unit)
        } catch (e: Exception) {
            BackupResult.Error.DatabaseError("Gagal menyimpan data Fokus Kuliah ke database lokal.")
        }
    }
}
