package com.duesoon.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferences
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.net.Uri
import com.duesoon.app.data.backup.BackupRestoreCoordinator
import com.duesoon.app.data.backup.BackupResult
import com.duesoon.app.domain.backup.ValidatedPortableBackup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class BackupUiState {
    object Idle : BackupUiState()
    object Loading : BackupUiState()
    data class AwaitingImportConfirmation(val backup: ValidatedPortableBackup) : BackupUiState()
    data class AwaitingRestoreConfirmation(val backup: ValidatedPortableBackup) : BackupUiState()
    data class Success(val message: String) : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository,
    private val notificationScheduler: NotificationScheduler,
    private val backupRestoreCoordinator: BackupRestoreCoordinator
) : ViewModel() {

    private val _backupUiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

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

    fun clearCompletedTasks() {
        viewModelScope.launch {
            val tasks = taskRepository.observeTasks().first()
            val completedTasks = tasks.filter { it.completed }
            completedTasks.forEach { task ->
                taskRepository.deleteTask(task)
            }
        }
    }

    fun resetBackupState() {
        _backupUiState.value = BackupUiState.Idle
    }

    fun exportBackup(uri: Uri?) {
        if (uri == null) return
        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            when (val result = backupRestoreCoordinator.exportBackup(uri)) {
                is BackupResult.Success -> _backupUiState.value = BackupUiState.Success("Backup exported successfully")
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
                is BackupResult.Success -> _backupUiState.value = BackupUiState.Success("Data imported successfully")
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
                is BackupResult.Success -> _backupUiState.value = BackupUiState.Success("Data restored successfully")
                is BackupResult.Error -> _backupUiState.value = BackupUiState.Error(mapErrorToMessage(result))
            }
        }
    }

    fun cancelConfirmation() {
        _backupUiState.value = BackupUiState.Idle
    }

    private fun mapErrorToMessage(error: BackupResult.Error): String {
        return when (error) {
            is BackupResult.Error.Cancelled -> "Operation cancelled"
            is BackupResult.Error.DatabaseError -> error.message
            is BackupResult.Error.InvalidFormat -> "Invalid backup file format"
            is BackupResult.Error.StorageError -> "Storage error occurred"
            is BackupResult.Error.UnsupportedSchema -> error.message
            is BackupResult.Error.ValidationErrors -> "Backup validation failed"
        }
    }
}
