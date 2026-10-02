package com.duesoon.app.ui.focus

import androidx.lifecycle.SavedStateHandle
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockRepository: TaskRepository = mock()
    private val dummyTask = Task(
        id = 10L,
        title = "Important Essay",
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

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to 10L))
        viewModel = FocusViewModel(savedStateHandle, mockRepository)
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
        assertEquals(25 * 60L, state.totalSeconds)
        assertEquals(25 * 60L, state.remainingSeconds)
        assertEquals("25:00", state.formattedTime)
        assertFalse(state.isRunning)
        assertFalse(state.isFinished)
    }

    @Test
    fun `startTimer ticks down and pauseTimer pauses countdown`() = runTest(testDispatcher) {
        viewModel.startTimer()
        assertTrue(viewModel.uiState.value.isRunning)

        advanceTimeBy(3100L)
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
        advanceTimeBy(5100L)
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
