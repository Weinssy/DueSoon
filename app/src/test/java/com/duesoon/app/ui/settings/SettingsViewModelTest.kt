package com.duesoon.app.ui.settings

import android.net.Uri
import com.duesoon.app.data.backup.BackupRestoreCoordinator
import com.duesoon.app.data.backup.BackupResult
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.data.repository.UserPreferencesState
import com.duesoon.app.domain.model.AccentPalette
import com.duesoon.app.domain.model.VisualDensity
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private val userPreferencesRepository = mock(UserPreferencesRepository::class.java)
    private val taskRepository = mock(TaskRepository::class.java)
    private val notificationScheduler = mock(NotificationScheduler::class.java)
    private val backupRestoreCoordinator = mock(BackupRestoreCoordinator::class.java)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        `when`(userPreferencesRepository.userPreferencesFlow).thenReturn(flowOf(UserPreferencesState()))
        viewModel = SettingsViewModel(
            userPreferencesRepository,
            taskRepository,
            notificationScheduler,
            backupRestoreCoordinator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateAccentPalette_callsRepository() = runTest {
        viewModel.updateAccentPalette(AccentPalette.ROSE)
        advanceUntilIdle()
        verify(userPreferencesRepository).updateAccentPalette(AccentPalette.ROSE)
    }

    @Test
    fun updateVisualDensity_callsRepository() = runTest {
        viewModel.updateVisualDensity(VisualDensity.COMPACT)
        advanceUntilIdle()
        verify(userPreferencesRepository).updateVisualDensity(VisualDensity.COMPACT)
    }

    @Test
    fun updateHapticsEnabled_callsRepository() = runTest {
        viewModel.updateHapticsEnabled(false)
        advanceUntilIdle()
        verify(userPreferencesRepository).updateHapticsEnabled(false)
    }
}
