package com.duesoon.app.ui.task

import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Task
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CreateTaskViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockRepository: TaskRepository = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `collects distinct sorted course suggestions and trims category on save`() = runTest(testDispatcher) {
        whenever(mockRepository.observeTasks()).thenReturn(
            flowOf(
                listOf(
                    Task(id = 1L, title = "T1", category = "  Kalkulus "),
                    Task(id = 2L, title = "T2", category = "Algoritma"),
                    Task(id = 3L, title = "T3", category = "algoritma"),
                    Task(id = 4L, title = "T4", category = "")
                )
            )
        )
        whenever(mockRepository.insertTask(org.mockito.kotlin.any())).thenReturn(5L)

        val viewModel = CreateTaskViewModel(mockRepository)
        advanceUntilIdle()

        assertEquals(listOf("Algoritma", "Kalkulus"), viewModel.uiState.value.categorySuggestions)

        viewModel.updateTitle("  Tugas Besar ")
        viewModel.updateCategory("  Basis Data ")
        viewModel.saveTask()
        advanceUntilIdle()

        val captor = argumentCaptor<Task>()
        verify(mockRepository).insertTask(captor.capture())
        assertEquals("Tugas Besar", captor.firstValue.title)
        assertEquals("Basis Data", captor.firstValue.category)
        assertTrue(viewModel.uiState.value.isSaved)
    }
}
