package com.duesoon.app.ui.settings.sync

import com.duesoon.app.core.crypto.CryptoManager
import com.duesoon.app.core.crypto.SecureStorage
import com.duesoon.app.core.sync.SyncScheduler
import com.duesoon.app.data.local.TaskDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.crypto.spec.SecretKeySpec

@OptIn(ExperimentalCoroutinesApi::class)
class SyncSettingsViewModelTest {

    private lateinit var viewModel: SyncSettingsViewModel
    private val secureStorage: SecureStorage = mock()
    private val cryptoManager: CryptoManager = mock()
    private val taskDao: TaskDao = mock()
    private val syncScheduler: SyncScheduler = mock()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        runTest {
            whenever(taskDao.getDirtyTasks()).thenReturn(emptyList())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init checks sync status and updates ui state`() = runTest {
        val masterKey = SecretKeySpec(ByteArray(32), "AES")
        whenever(secureStorage.getDerivedMasterKey()).thenReturn(masterKey)
        whenever(secureStorage.getAuthToken()).thenReturn("token")
        
        viewModel = SyncSettingsViewModel(secureStorage, cryptoManager, taskDao, syncScheduler, testDispatcher)
        
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.isSyncEnabled)
    }

    @Test
    fun `init checks sync status disabled if keys missing`() = runTest {
        whenever(secureStorage.getDerivedMasterKey()).thenReturn(null)
        whenever(secureStorage.getAuthToken()).thenReturn(null)
        
        viewModel = SyncSettingsViewModel(secureStorage, cryptoManager, taskDao, syncScheduler, testDispatcher)
        
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isSyncEnabled)
    }

    @Test
    fun `onSetupSubmit derives keys and schedules sync`() = runTest {
        whenever(secureStorage.getDerivedMasterKey()).thenReturn(null)
        whenever(secureStorage.getAuthToken()).thenReturn(null)
        
        val masterKey = SecretKeySpec(ByteArray(32) { 1 }, "AES")
        val authToken = "mockToken123"
        
        whenever(cryptoManager.deriveMasterKey(any(), any())).thenReturn(masterKey)
        whenever(cryptoManager.deriveAccountAuthToken(any())).thenReturn(authToken)
        
        viewModel = SyncSettingsViewModel(secureStorage, cryptoManager, taskDao, syncScheduler, testDispatcher)
        
        viewModel.onSetupSubmit("https://sync.duesoon.com", "password123".toCharArray())
        
        advanceUntilIdle()
        
        verify(secureStorage).saveDerivedMasterKey(any())
        verify(secureStorage).saveAuthToken(any())
        verify(syncScheduler).schedulePeriodicSync()
        verify(syncScheduler).triggerExpeditedSync()
        
        assertTrue(viewModel.uiState.value.isSyncEnabled)
        assertFalse(viewModel.uiState.value.isConfiguring)
    }

    @Test
    fun `onDisconnect clears secure storage`() = runTest {
        whenever(secureStorage.getDerivedMasterKey()).thenReturn(null)
        whenever(secureStorage.getAuthToken()).thenReturn(null)
        
        viewModel = SyncSettingsViewModel(secureStorage, cryptoManager, taskDao, syncScheduler, testDispatcher)
        
        viewModel.onDisconnect()
        
        verify(secureStorage).clear()
        assertFalse(viewModel.uiState.value.isSyncEnabled)
    }
}
