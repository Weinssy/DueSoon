package com.duesoon.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.backup.BackupRestoreCoordinator
import com.duesoon.app.data.backup.BackupResult
import com.duesoon.app.data.backup.FokusKuliahImportCoordinator
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.data.repository.UserPreferencesState
import com.duesoon.app.domain.backup.ParsedFokusKuliahBackup
import com.duesoon.app.domain.backup.ValidatedPortableBackup
import com.duesoon.app.domain.model.AccentPalette
import com.duesoon.app.domain.model.VisualDensity
import com.duesoon.app.domain.update.ApkDownloadState
import com.duesoon.app.domain.update.AppReleaseInfo
import com.duesoon.app.domain.update.AppUpdateRepository
import com.duesoon.app.domain.update.SemanticVersion
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BackupUiState {
    object Idle : BackupUiState()
    object Loading : BackupUiState()
    data class AwaitingImportConfirmation(val backup: ValidatedPortableBackup) : BackupUiState()
    data class AwaitingRestoreConfirmation(val backup: ValidatedPortableBackup) : BackupUiState()
    data class AwaitingFokusKuliahImportConfirmation(val parsed: ParsedFokusKuliahBackup) : BackupUiState()
    data class Success(val message: String) : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpToDate(val currentVersion: String) : UpdateUiState()
    data class UpdateAvailable(val release: AppReleaseInfo) : UpdateUiState()
    data class Downloading(
        val release: AppReleaseInfo,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Int?
    ) : UpdateUiState()
    data class ReadyToInstall(val release: AppReleaseInfo, val apkFilePath: String) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository,
    private val notificationScheduler: NotificationScheduler,
    private val backupRestoreCoordinator: BackupRestoreCoordinator,
    private val fokusKuliahImportCoordinator: FokusKuliahImportCoordinator? = null,
    private val appUpdateRepository: AppUpdateRepository? = null,
    private val currentVersionName: String = "1.9.5"
) : ViewModel() {

    private val _backupUiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

    private val _updateUiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateUiState: StateFlow<UpdateUiState> = _updateUiState.asStateFlow()

    private var downloadJob: Job? = null

    val userPreferences: StateFlow<UserPreferencesState> = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesState())

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateTheme(theme)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateNotificationsEnabled(enabled)

            val tasks = taskRepository.observeTasks().first()
            tasks.forEach { task ->
                if (!task.completed && task.deadline != null) {
                    if (enabled) {
                        notificationScheduler.schedule(task)
                    } else {
                        notificationScheduler.cancelAll(task)
                    }
                }
            }
        }
    }

    fun updateAlarmReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateAlarmReminderEnabled(enabled)
        }
    }

    fun updateAccentPalette(palette: AccentPalette) {
        viewModelScope.launch {
            userPreferencesRepository.updateAccentPalette(palette)
        }
    }

    fun updateVisualDensity(density: VisualDensity) {
        viewModelScope.launch {
            userPreferencesRepository.updateVisualDensity(density)
        }
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateHapticsEnabled(enabled)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            val tasks = taskRepository.observeTasks().first()
            val completedTasks = tasks.filter { it.completed }
            completedTasks.forEach { task ->
                taskRepository.deleteTask(task)
            }
        }
    }

    // --- Backup / Import / Restore ---

    fun resetBackupState() {
        _backupUiState.value = BackupUiState.Idle
    }

    fun exportBackup(uri: Uri?) {
        if (uri == null) return
        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            when (val result = backupRestoreCoordinator.exportBackup(uri)) {
                is BackupResult.Success -> _backupUiState.value = BackupUiState.Success("Backup berhasil diekspor")
                is BackupResult.Error -> _backupUiState.value = BackupUiState.Error(mapErrorToMessage(result))
            }
        }
    }

    fun prepareImport(uri: Uri?) {
        if (uri == null) return
        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            when (val result = backupRestoreCoordinator.prepareImport(uri)) {
                is BackupResult.Success -> _backupUiState.value = BackupUiState.AwaitingImportConfirmation(result.data)
                is BackupResult.Error -> _backupUiState.value = BackupUiState.Error(mapErrorToMessage(result))
            }
        }
    }

    fun executeImport() {
        val state = _backupUiState.value
        if (state !is BackupUiState.AwaitingImportConfirmation) return

        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            when (val result = backupRestoreCoordinator.executeImport(state.backup)) {
                is BackupResult.Success -> _backupUiState.value = BackupUiState.Success("Data berhasil diimpor")
                is BackupResult.Error -> _backupUiState.value = BackupUiState.Error(mapErrorToMessage(result))
            }
        }
    }

    fun prepareRestore(uri: Uri?) {
        if (uri == null) return
        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            when (val result = backupRestoreCoordinator.prepareRestore(uri)) {
                is BackupResult.Success -> _backupUiState.value = BackupUiState.AwaitingRestoreConfirmation(result.data)
                is BackupResult.Error -> _backupUiState.value = BackupUiState.Error(mapErrorToMessage(result))
            }
        }
    }

    fun executeRestore() {
        val state = _backupUiState.value
        if (state !is BackupUiState.AwaitingRestoreConfirmation) return

        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            when (val result = backupRestoreCoordinator.executeRestore(state.backup)) {
                is BackupResult.Success -> _backupUiState.value = BackupUiState.Success("Data berhasil dipulihkan")
                is BackupResult.Error -> _backupUiState.value = BackupUiState.Error(mapErrorToMessage(result))
            }
        }
    }

    fun prepareFokusKuliahImport(uri: Uri?) {
        if (uri == null) return
        val coordinator = fokusKuliahImportCoordinator
            ?: run {
                _backupUiState.value = BackupUiState.Error("Fitur impor Fokus Kuliah tidak tersedia")
                return
            }
        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            when (val result = coordinator.prepareImport(uri)) {
                is BackupResult.Success -> _backupUiState.value =
                    BackupUiState.AwaitingFokusKuliahImportConfirmation(result.data)
                is BackupResult.Error -> _backupUiState.value =
                    BackupUiState.Error(mapErrorToMessage(result))
            }
        }
    }

    fun executeFokusKuliahImport() {
        val state = _backupUiState.value
        if (state !is BackupUiState.AwaitingFokusKuliahImportConfirmation) return
        val coordinator = fokusKuliahImportCoordinator ?: return

        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            when (val result = coordinator.executeImport(state.parsed)) {
                is BackupResult.Success -> _backupUiState.value = BackupUiState.Success(
                    "Impor Fokus Kuliah selesai (${state.parsed.tasks.size} tugas, ${state.parsed.focusSessions.size} sesi fokus)"
                )
                is BackupResult.Error -> _backupUiState.value =
                    BackupUiState.Error(mapErrorToMessage(result))
            }
        }
    }

    fun cancelConfirmation() {
        _backupUiState.value = BackupUiState.Idle
    }

    // --- In-App Update (GitHub Releases) ---

    fun checkForUpdates() {
        val updater = appUpdateRepository
            ?: run {
                _updateUiState.value = UpdateUiState.Error("Layanan pembaruan tidak tersedia.")
                return
            }
        if (_updateUiState.value is UpdateUiState.Checking || _updateUiState.value is UpdateUiState.Downloading) {
            return
        }

        _updateUiState.value = UpdateUiState.Checking
        viewModelScope.launch {
            val result = updater.fetchLatestRelease()
            result.fold(
                onSuccess = { release ->
                    if (release == null || !SemanticVersion.isNewer(release.versionName, currentVersionName)) {
                        _updateUiState.value = UpdateUiState.UpToDate(currentVersionName)
                    } else {
                        _updateUiState.value = UpdateUiState.UpdateAvailable(release)
                    }
                },
                onFailure = { error ->
                    _updateUiState.value = UpdateUiState.Error(
                        error.message ?: "Gagal memeriksa pembaruan."
                    )
                }
            )
        }
    }

    fun startUpdateDownload() {
        val updater = appUpdateRepository ?: return
        val release = when (val current = _updateUiState.value) {
            is UpdateUiState.UpdateAvailable -> current.release
            is UpdateUiState.ReadyToInstall -> current.release
            else -> return
        }

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _updateUiState.value = UpdateUiState.Downloading(
                release = release,
                bytesDownloaded = 0L,
                totalBytes = release.apkSizeBytes ?: -1L,
                progressPercent = 0
            )
            updater.downloadApk(release).collect { progress ->
                when (progress) {
                    is ApkDownloadState.Downloading -> {
                        _updateUiState.value = UpdateUiState.Downloading(
                            release = release,
                            bytesDownloaded = progress.bytesDownloaded,
                            totalBytes = progress.totalBytes,
                            progressPercent = progress.progressPercent
                        )
                    }
                    is ApkDownloadState.Completed -> {
                        _updateUiState.value = UpdateUiState.ReadyToInstall(
                            release = release,
                            apkFilePath = progress.filePath
                        )
                    }
                    is ApkDownloadState.Failed -> {
                        _updateUiState.value = UpdateUiState.Error(progress.message)
                    }
                }
            }
        }
    }

    fun cancelUpdateDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _updateUiState.value = UpdateUiState.Idle
    }

    fun resetUpdateState() {
        if (_updateUiState.value !is UpdateUiState.Downloading) {
            _updateUiState.value = UpdateUiState.Idle
        }
    }

    private fun mapErrorToMessage(error: BackupResult.Error): String {
        return when (error) {
            is BackupResult.Error.Cancelled -> "Operasi dibatalkan"
            is BackupResult.Error.DatabaseError -> error.message
            is BackupResult.Error.InvalidFormat -> error.message.ifBlank { "Format file backup tidak valid" }
            is BackupResult.Error.StorageError -> error.message.ifBlank { "Terjadi kesalahan penyimpanan" }
            is BackupResult.Error.UnsupportedSchema -> error.message
            is BackupResult.Error.ValidationErrors -> "Validasi backup gagal"
        }
    }

    override fun onCleared() {
        super.onCleared()
        downloadJob?.cancel()
    }
}
