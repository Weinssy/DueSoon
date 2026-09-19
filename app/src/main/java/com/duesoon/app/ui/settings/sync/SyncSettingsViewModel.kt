package com.duesoon.app.ui.settings.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.core.crypto.CryptoManager
import com.duesoon.app.core.crypto.SecureStorage
import com.duesoon.app.core.sync.SyncScheduler
import com.duesoon.app.data.local.TaskDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.spec.SecretKeySpec

data class SyncUiState(
    val isSyncEnabled: Boolean = false,
    val serverUrl: String = "",
    val isConfiguring: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val dirtyTaskCount: Int = 0,
    val syncErrorMessage: String? = null,
    val showDisconnectDialog: Boolean = false
)

class SyncSettingsViewModel(
    private val secureStorage: SecureStorage,
    private val cryptoManager: CryptoManager,
    private val taskDao: TaskDao,
    private val syncScheduler: SyncScheduler,
    private val defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        checkSyncStatus()
        observeDirtyTasks()
    }

    private fun checkSyncStatus() {
        val masterKey = secureStorage.getDerivedMasterKey()
        val authToken = secureStorage.getAuthToken()
        
        _uiState.update { it.copy(isSyncEnabled = masterKey != null && authToken != null) }
    }

    private fun observeDirtyTasks() {
        viewModelScope.launch {
            val count = taskDao.getDirtyTasks().size
            _uiState.update { it.copy(dirtyTaskCount = count) }
        }
    }

    fun onSetupSubmit(url: String, passphrase: CharArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConfiguring = true, syncErrorMessage = null, serverUrl = url) }
            
            val success = withContext(defaultDispatcher) {
                try {
                    // Derive master key (Using a dummy salt for the scope of the local app logic)
                    val masterKey = cryptoManager.deriveMasterKey(passphrase, "placeholder_salt".toByteArray())
                    // Derive auth token
                    val authToken = cryptoManager.deriveAccountAuthToken(masterKey)
                    
                    secureStorage.saveDerivedMasterKey(masterKey)
                    secureStorage.saveAuthToken(authToken)
                    
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            if (success) {
                _uiState.update { it.copy(isConfiguring = false, isSyncEnabled = true) }
                syncScheduler.schedulePeriodicSync()
                triggerManualSync()
            } else {
                _uiState.update { it.copy(isConfiguring = false, syncErrorMessage = "Failed to setup sync. Try again.") }
            }
        }
    }

    fun triggerManualSync() {
        if (!_uiState.value.isSyncEnabled) return
        _uiState.update { it.copy(isSyncing = true) }
        syncScheduler.triggerExpeditedSync()
        // Here we could observe WorkManager status. For simplicity, we just stop loading after a delay.
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(isSyncing = false, lastSyncTimestamp = System.currentTimeMillis()) }
        }
    }

    fun showDisconnectDialog(show: Boolean) {
        _uiState.update { it.copy(showDisconnectDialog = show) }
    }

    fun onDisconnect() {
        secureStorage.clear()
        _uiState.update { it.copy(isSyncEnabled = false, showDisconnectDialog = false) }
    }
}
