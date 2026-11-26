package com.duesoon.app.ui.home

import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            Task(1, "Active 1", null, null, "Basis Data", Priority.NORMAL, ReminderType.SMART, false, null, false, null, 0L, 0L),
            Task(2, "Active 2", null, null, "Algoritma", Priority.HIGH, ReminderType.SMART, false, null, false, null, 0L, 0L),
            Task(3, "Active 3", null, null, "basis data", Priority.LOW, ReminderType.SMART, false, null, false, null, 0L, 0L)
        )
        `when`(mockRepository.observeTasks()).thenReturn(flowOf(dummyTasks))
        viewModel = HomeViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls observeTasks`() {
        viewModel.tasks
        verify(mockRepository).observeTasks()
    }

    @Test
    fun `categories emits sorted distinct course names and selectCategory updates filter`() = runTest(testDispatcher) {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect()
        }
        advanceUntilIdle()

        assertEquals(listOf("Algoritma", "Basis Data"), viewModel.categories.value)
        assertNull(viewModel.selectedCategory.value)

        viewModel.selectCategory("Basis Data")
        assertEquals("Basis Data", viewModel.selectedCategory.value)

        viewModel.selectCategory(null)
        assertNull(viewModel.selectedCategory.value)
        job.cancel()
    }
}
