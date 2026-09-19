package com.duesoon.app.ui.home

import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val mockRepository = mock(TaskRepository::class.java)
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val dummyTasks = listOf(
            Task(1, "Active 1", null, null, null, Priority.NORMAL, ReminderType.SMART, false, null, false, null, 0L, 0L)
        )
        // HomeViewModel now observes observeActiveTasks
        `when`(mockRepository.observeActiveTasks()).thenReturn(flowOf(dummyTasks))
        viewModel = HomeViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls observeActiveTasks`() {
        // Initialization in setup should have triggered observeActiveTasks because tasks uses stateIn
        // Wait, stateIn is lazy unless collected if we use WhileSubscribed(5000), but tasks is public.
        // Just verify it's called once we access tasks or initially.
        val flow = viewModel.tasks
        verify(mockRepository).observeActiveTasks()
    }
}
