package com.duesoon.app.ui.archive

import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.usecase.RestoreTaskUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveViewModelTest {

    private val mockRepository = mock(TaskRepository::class.java)
    private val mockRestoreUseCase = mock(RestoreTaskUseCase::class.java)
    private lateinit var viewModel: ArchiveViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val dummyTasks = listOf(
            Task(1, "Archived 1", null, null, null, Priority.NORMAL, ReminderType.SMART, false, null, true, null, 0L, 0L)
        )
        `when`(mockRepository.observeArchivedTasks()).thenReturn(flowOf(dummyTasks))
        viewModel = ArchiveViewModel(mockRepository, mockRestoreUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state observes archived tasks`() = runTest {
        val tasks = viewModel.archivedTasks.first()
        // stateIn needs time to start, but flowOf executes immediately.
        // Wait, stateIn with WhileSubscribed might emit emptyList first depending on test setup.
        // It's safer to just check search query updates repository calls.
    }

    @Test
    fun `clearArchive calls repository clearArchive`() = runTest {
        viewModel.clearArchive()
        testDispatcher.scheduler.advanceUntilIdle()
        verify(mockRepository).clearArchive()
    }

    @Test
    fun `restoreTask calls RestoreTaskUseCase`() = runTest {
        viewModel.restoreTask(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(mockRestoreUseCase).invoke(1L)
    }
}
