package com.duesoon.app.ui.focus

import androidx.lifecycle.SavedStateHandle
import com.duesoon.app.data.repository.FocusRepository
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.data.repository.UserPreferencesState
import com.duesoon.app.domain.model.FocusSession
import com.duesoon.app.domain.model.PomodoroMode
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockRepository: TaskRepository = mock()
    private val mockFocusRepository: FocusRepository = mock()
    private val mockPrefsRepository: UserPreferencesRepository = mock()

    private val dummyTask = Task(
        id = 10L,
        title = "Important Essay",
        category = "Algoritma",
        priority = Priority.HIGH,
        reminderType = ReminderType.SMART,
        completed = false,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private lateinit var viewModel: FocusViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        whenever(mockRepository.getTask(10L)).thenReturn(dummyTask)
        whenever(mockRepository.observeActiveTasks()).thenReturn(flowOf(listOf(dummyTask)))
        whenever(mockFocusRepository.observeAllSessions()).thenReturn(flowOf(emptyList()))
        whenever(mockPrefsRepository.userPreferencesFlow).thenReturn(flowOf(UserPreferencesState()))

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to 10L))
        viewModel = FocusViewModel(
            savedStateHandle = savedStateHandle,
            repository = mockRepository,
            focusRepository = mockFocusRepository,
            userPreferencesRepository = mockPrefsRepository,
            nowMillisProvider = { testDispatcher.scheduler.currentTime },
            todayProvider = { LocalDate.of(2026, 4, 15) }
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads task and default 25 min duration`() {
        val state = viewModel.uiState.value
        assertEquals("Important Essay", state.task?.title)
        assertEquals(PomodoroMode.FOCUS, state.mode)
        assertEquals(25 * 60L, state.totalSeconds)
        assertEquals(25 * 60L, state.remainingSeconds)
        assertEquals("25:00", state.formattedTime)
        assertFalse(state.isRunning)
        assertFalse(state.isFinished)
    }

    @Test
    fun `startTimer ticks down using timestamp and pauseTimer pauses countdown`() = runTest(testDispatcher) {
        viewModel.startTimer()
        assertTrue(viewModel.uiState.value.isRunning)

        advanceTimeBy(3000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(25 * 60L - 3L, viewModel.uiState.value.remainingSeconds)

        viewModel.pauseTimer()
        assertFalse(viewModel.uiState.value.isRunning)

        advanceTimeBy(2000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(25 * 60L - 3L, viewModel.uiState.value.remainingSeconds)
    }

    @Test
    fun `resetTimer resets remaining seconds to total duration`() = runTest(testDispatcher) {
        viewModel.startTimer()
        advanceTimeBy(5000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(25 * 60L - 5L, viewModel.uiState.value.remainingSeconds)

        viewModel.resetTimer()
        assertFalse(viewModel.uiState.value.isRunning)
        assertEquals(25 * 60L, viewModel.uiState.value.remainingSeconds)
        assertEquals("25:00", viewModel.uiState.value.formattedTime)
    }

    @Test
    fun `setDuration updates duration and resets countdown`() {
        viewModel.setDuration(50)
        val state = viewModel.uiState.value
        assertEquals(50 * 60L, state.totalSeconds)
        assertEquals(50 * 60L, state.remainingSeconds)
        assertEquals("50:00", state.formattedTime)
        assertFalse(state.isRunning)
    }

    @Test
    fun `addMinutes extends timer duration`() {
        viewModel.addMinutes(5)
        val state = viewModel.uiState.value
        assertEquals(30 * 60L, state.totalSeconds)
        assertEquals(30 * 60L, state.remainingSeconds)
        assertEquals("30:00", state.formattedTime)
    }

    @Test
    fun `completing focus session records session in repository and transitions to short break`() = runTest(testDispatcher) {
        viewModel.setDuration(1) // 1 minute = 60 seconds
        viewModel.startTimer()

        advanceTimeBy(61_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.SHORT_BREAK, state.mode)
        assertEquals(1, state.cycleCount)
        assertTrue(state.isFinished)
        assertEquals(PomodoroMode.FOCUS, state.lastCompletedMode)
        assertEquals(1L, state.completionEventId)

        val sessionCaptor = argumentCaptor<FocusSession>()
        verify(mockFocusRepository).recordSession(sessionCaptor.capture())
        assertEquals(10L, sessionCaptor.firstValue.taskId)
        assertEquals("Algoritma", sessionCaptor.firstValue.courseOrCategory)
        assertEquals(1, sessionCaptor.firstValue.durationMinutes)
        assertEquals("2026-04-15", sessionCaptor.firstValue.dateKey)
    }

    @Test
    fun `skipSession transitions mode without recording session`() = runTest(testDispatcher) {
        viewModel.skipSession()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PomodoroMode.SHORT_BREAK, viewModel.uiState.value.mode)
        verify(mockFocusRepository, never()).recordSession(any())
    }

    @Test
    fun `standalone focus tab allows selecting and unlinking target task`() = runTest(testDispatcher) {
        val standaloneVm = FocusViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = mockRepository,
            focusRepository = mockFocusRepository,
            userPreferencesRepository = mockPrefsRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(standaloneVm.uiState.value.isTaskScopedMode)
        assertNull(standaloneVm.uiState.value.task)

        standaloneVm.selectTask(10L)
        assertEquals("Important Essay", standaloneVm.uiState.value.task?.title)

        standaloneVm.selectTask(null)
        assertNull(standaloneVm.uiState.value.task)
    }

    @Test
    fun `completeTask updates task in repository to completed`() = runTest(testDispatcher) {
        var callbackCalled = false
        viewModel.completeTask(onCompleted = { callbackCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        val taskCaptor = argumentCaptor<Task>()
        verify(mockRepository).updateTask(taskCaptor.capture())
        assertTrue(taskCaptor.firstValue.completed)
        assertEquals(10L, taskCaptor.firstValue.id)
        assertTrue(callbackCalled)
    }
}
