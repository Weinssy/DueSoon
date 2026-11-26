package com.duesoon.app.focus

import com.duesoon.app.data.repository.FocusRepository
import com.duesoon.app.data.repository.PersistedPomodoroTimerState
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.data.repository.UserPreferencesState
import com.duesoon.app.domain.model.FocusSession
import com.duesoon.app.domain.model.PomodoroMode
import com.duesoon.app.domain.model.PomodoroSettings
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroTimerCoordinatorTest {

    private class RecordingBridge : PomodoroServiceBridge {
        var startedCount = 0
        var updatedCount = 0
        var stoppedCount = 0
        var lastKeepPaused = false
        var completedNotifiedCount = 0
        var lastCompletedMode: PomodoroMode? = null

        override fun startForegroundTimer() {
            startedCount += 1
        }

        override fun updateNotification() {
            updatedCount += 1
        }

        override fun stopForegroundTimer(keepPausedNotification: Boolean) {
            stoppedCount += 1
            lastKeepPaused = keepPausedNotification
        }

        override fun notifyPhaseCompleted(
            completedMode: PomodoroMode,
            nextMode: PomodoroMode,
            soundEnabled: Boolean
        ) {
            completedNotifiedCount += 1
            lastCompletedMode = completedMode
        }
    }

    @Test
    fun `start pause reset notify service bridge and persist timer state`() = runTest(StandardTestDispatcher()) {
        val mockTaskRepo: TaskRepository = mock()
        val mockFocusRepo: FocusRepository = mock()
        val mockPrefsRepo: UserPreferencesRepository = mock()
        whenever(mockPrefsRepo.userPreferencesFlow).thenReturn(flowOf(UserPreferencesState()))
        whenever(mockPrefsRepo.persistedPomodoroTimerFlow).thenReturn(flowOf(PersistedPomodoroTimerState()))

        val bridge = RecordingBridge()
        val coordinator = PomodoroTimerCoordinator(
            scope = backgroundScope,
            taskRepository = mockTaskRepo,
            focusRepository = mockFocusRepo,
            userPreferencesRepository = mockPrefsRepo,
            serviceBridge = bridge,
            nowMillisProvider = { testScheduler.currentTime },
            todayProvider = { LocalDate.of(2026, 11, 26) }
        )
        testScheduler.advanceUntilIdle()

        val task = Task(id = 5L, title = "Skripsi Bab 3", category = "Kuliah")
        coordinator.selectTask(task)
        coordinator.startTimer()

        assertTrue(coordinator.state.value.isRunning)
        assertEquals(1, bridge.startedCount)

        advanceTimeBy(3_000L)
        runCurrent()
        assertEquals(25 * 60L - 3L, coordinator.state.value.remainingSeconds)
        assertTrue(bridge.updatedCount >= 1)

        coordinator.pauseTimer()
        assertFalse(coordinator.state.value.isRunning)
        assertTrue(coordinator.state.value.isPaused)
        assertEquals(1, bridge.stoppedCount)
        assertTrue(bridge.lastKeepPaused)

        coordinator.resetTimer()
        assertFalse(coordinator.state.value.isPaused)
        assertEquals(25 * 60L, coordinator.state.value.remainingSeconds)
        assertFalse(bridge.lastKeepPaused)

        verify(mockPrefsRepo, atLeastOnce()).savePersistedPomodoroTimerState(any())
    }

    @Test
    fun `restoring expired running timer completes session and records focus history`() {
        val dispatcher = StandardTestDispatcher()
        runTest(dispatcher) {
            val mockTaskRepo: TaskRepository = mock()
            val mockFocusRepo: FocusRepository = mock()
            val mockPrefsRepo: UserPreferencesRepository = mock()

            val task = Task(id = 7L, title = "Praktikum Jaringan", category = "Jaringan")
            whenever(mockTaskRepo.getTask(7L)).thenReturn(task)
            whenever(mockPrefsRepo.userPreferencesFlow).thenReturn(flowOf(UserPreferencesState()))
            whenever(mockPrefsRepo.persistedPomodoroTimerFlow).thenReturn(
                flowOf(
                    PersistedPomodoroTimerState(
                        mode = PomodoroMode.FOCUS,
                        cycleCount = 0,
                        totalSeconds = 25 * 60L,
                        remainingSeconds = 120L,
                        endTimestampMs = 1_000L, // expired relative to current time 10_000L
                        isRunning = true,
                        linkedTaskId = 7L,
                        activeSessionMinutes = 25
                    )
                )
            )

            val bridge = RecordingBridge()
            val coordinator = PomodoroTimerCoordinator(
                scope = this,
                taskRepository = mockTaskRepo,
                focusRepository = mockFocusRepo,
                userPreferencesRepository = mockPrefsRepo,
                serviceBridge = bridge,
                nowMillisProvider = { 10_000L },
                todayProvider = { LocalDate.of(2026, 11, 26) }
            )
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(PomodoroMode.SHORT_BREAK, coordinator.state.value.mode)
            assertEquals(1, coordinator.state.value.cycleCount)
            assertFalse(coordinator.state.value.isRunning)
            assertEquals(1, bridge.completedNotifiedCount)
            assertEquals(PomodoroMode.FOCUS, bridge.lastCompletedMode)

            val captor = argumentCaptor<FocusSession>()
            verify(mockFocusRepo).recordSession(captor.capture())
            assertEquals(7L, captor.firstValue.taskId)
            assertEquals("Jaringan", captor.firstValue.courseOrCategory)
            assertEquals(25, captor.firstValue.durationMinutes)
            assertEquals("2026-11-26", captor.firstValue.dateKey)

            coordinator.clearLinkedTaskIfMatches(7L)
            assertNull(coordinator.state.value.linkedTaskId)
            coordinator.cancelTicker()
        }
    }
}
