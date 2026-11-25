package com.duesoon.app.ui.settings

import android.net.Uri
import com.duesoon.app.data.backup.BackupRestoreCoordinator
import com.duesoon.app.data.backup.BackupResult
import com.duesoon.app.data.backup.FokusKuliahImportCoordinator
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.data.repository.UserPreferencesState
import com.duesoon.app.domain.backup.ParsedFokusKuliahBackup
import com.duesoon.app.domain.model.AccentPalette
import com.duesoon.app.domain.model.FocusSession
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.model.VisualDensity
import com.duesoon.app.domain.update.ApkDownloadState
import com.duesoon.app.domain.update.AppReleaseInfo
import com.duesoon.app.domain.update.AppUpdateRepository
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private val userPreferencesRepository = mock(UserPreferencesRepository::class.java)
    private val taskRepository = mock(TaskRepository::class.java)
    private val notificationScheduler = mock(NotificationScheduler::class.java)
    private val backupRestoreCoordinator = mock(BackupRestoreCoordinator::class.java)
    private val fokusKuliahImportCoordinator = mock(FokusKuliahImportCoordinator::class.java)
    private val appUpdateRepository = mock(AppUpdateRepository::class.java)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        `when`(userPreferencesRepository.userPreferencesFlow).thenReturn(flowOf(UserPreferencesState()))
        viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            taskRepository = taskRepository,
            notificationScheduler = notificationScheduler,
            backupRestoreCoordinator = backupRestoreCoordinator,
            fokusKuliahImportCoordinator = fokusKuliahImportCoordinator,
            appUpdateRepository = appUpdateRepository,
            currentVersionName = "1.9.5"
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

    @Test
    fun checkForUpdates_whenNewerReleaseExists_setsUpdateAvailable() = runTest {
        val newerRelease = AppReleaseInfo(
            tagName = "v2.0.0",
            versionName = "2.0.0",
            releaseName = "DueSoon 2.0.0",
            releaseNotes = "Rilis terbaru",
            htmlUrl = "https://github.com/Weinssy/DueSoon/releases/tag/v2.0.0",
            apkDownloadUrl = "https://example.com/DueSoon-v2.0.0.apk",
            apkFileName = "DueSoon-v2.0.0.apk",
            apkSizeBytes = 4_000_000L,
            publishedAt = "2026-04-15T00:00:00Z"
        )
        whenever(appUpdateRepository.fetchLatestRelease()).thenReturn(Result.success(newerRelease))

        viewModel.checkForUpdates()
        advanceUntilIdle()

        val state = viewModel.updateUiState.value
        assertTrue(state is UpdateUiState.UpdateAvailable)
        assertEquals("2.0.0", (state as UpdateUiState.UpdateAvailable).release.versionName)
    }

    @Test
    fun checkForUpdates_whenSameVersion_setsUpToDate() = runTest {
        val sameRelease = AppReleaseInfo(
            tagName = "v1.9.5",
            versionName = "1.9.5",
            releaseName = "DueSoon 1.9.5",
            releaseNotes = "",
            htmlUrl = "https://github.com/Weinssy/DueSoon/releases/tag/v1.9.5",
            apkDownloadUrl = null,
            apkFileName = null,
            apkSizeBytes = null,
            publishedAt = null
        )
        whenever(appUpdateRepository.fetchLatestRelease()).thenReturn(Result.success(sameRelease))

        viewModel.checkForUpdates()
        advanceUntilIdle()

        assertTrue(viewModel.updateUiState.value is UpdateUiState.UpToDate)
    }

    @Test
    fun startUpdateDownload_emitsReadyToInstallOnCompletion() = runTest {
        val release = AppReleaseInfo(
            tagName = "v2.0.0",
            versionName = "2.0.0",
            releaseName = "DueSoon 2.0.0",
            releaseNotes = "Changelog",
            htmlUrl = "https://github.com/Weinssy/DueSoon/releases/tag/v2.0.0",
            apkDownloadUrl = "https://example.com/DueSoon-v2.0.0.apk",
            apkFileName = "DueSoon-v2.0.0.apk",
            apkSizeBytes = 1000L,
            publishedAt = null
        )
        whenever(appUpdateRepository.fetchLatestRelease()).thenReturn(Result.success(release))
        whenever(appUpdateRepository.downloadApk(release)).thenReturn(
            flowOf(
                ApkDownloadState.Downloading(500L, 1000L, 50),
                ApkDownloadState.Completed("/cache/updates/DueSoon-v2.0.0.apk")
            )
        )

        viewModel.checkForUpdates()
        advanceUntilIdle()
        viewModel.startUpdateDownload()
        advanceUntilIdle()

        val state = viewModel.updateUiState.value
        assertTrue(state is UpdateUiState.ReadyToInstall)
        assertEquals("/cache/updates/DueSoon-v2.0.0.apk", (state as UpdateUiState.ReadyToInstall).apkFilePath)
    }

    @Test
    fun prepareAndExecuteFokusKuliahImport_transitionsToSuccess() = runTest {
        val fakeUri = mock(Uri::class.java)
        val parsed = ParsedFokusKuliahBackup(
            tasks = listOf(Task(title = "Makalah AI", category = "AI")),
            focusSessions = listOf(FocusSession(durationMinutes = 25, dateKey = "2026-04-15")),
            pomodoroSettings = null
        )
        whenever(fokusKuliahImportCoordinator.prepareImport(fakeUri)).thenReturn(BackupResult.Success(parsed))
        whenever(fokusKuliahImportCoordinator.executeImport(parsed)).thenReturn(BackupResult.Success(Unit))

        viewModel.prepareFokusKuliahImport(fakeUri)
        advanceUntilIdle()
        assertTrue(viewModel.backupUiState.value is BackupUiState.AwaitingFokusKuliahImportConfirmation)

        viewModel.executeFokusKuliahImport()
        advanceUntilIdle()
        assertTrue(viewModel.backupUiState.value is BackupUiState.Success)
    }
}
