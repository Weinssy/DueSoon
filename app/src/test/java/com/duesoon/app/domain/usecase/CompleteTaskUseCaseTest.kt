package com.duesoon.app.domain.usecase

import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.RecurrenceInterval
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class CompleteTaskUseCaseTest {

    private lateinit var fakeTaskRepository: FakeTaskRepository
    private lateinit var completeTaskUseCase: CompleteTaskUseCase

    @Before
    fun setUp() {
        fakeTaskRepository = FakeTaskRepository()
        completeTaskUseCase = CompleteTaskUseCase(fakeTaskRepository)
    }

    @Test
    fun invoke_standardIncompleteTask_callsUpdateTask() = runBlocking {
        val task = Task(
            id = 1L,
            title = "Standard Task",
            priority = Priority.NORMAL,
            reminderType = ReminderType.SMART,
            isRecurring = false,
            completed = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        fakeTaskRepository.tasks[1L] = task

        val result = completeTaskUseCase(1L)

        assertTrue(result.isSuccess)
        val updatedTask = fakeTaskRepository.updatedTasks.last()
        assertEquals(1L, updatedTask.id)
        assertTrue(updatedTask.completed)
    }

    @Test
    fun invoke_recurringIncompleteTask_callsUpdateTask() = runBlocking {
        val task = Task(
            id = 2L,
            title = "Recurring Task",
            priority = Priority.NORMAL,
            reminderType = ReminderType.SMART,
            isRecurring = true,
            recurrenceInterval = RecurrenceInterval.DAILY,
            deadline = 2000L,
            completed = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        fakeTaskRepository.tasks[2L] = task

        val result = completeTaskUseCase(2L)

        assertTrue(result.isSuccess)
        val updatedTask = fakeTaskRepository.updatedTasks.last()
        assertEquals(2L, updatedTask.id)
        assertTrue(updatedTask.completed)
    }

    @Test
    fun invoke_alreadyCompletedTask_safeNoOp() = runBlocking {
        val task = Task(
            id = 3L,
            title = "Already Completed",
            priority = Priority.NORMAL,
            reminderType = ReminderType.SMART,
            isRecurring = false,
            completed = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        fakeTaskRepository.tasks[3L] = task

        val result = completeTaskUseCase(3L)

        assertTrue(result.isSuccess)
        // Must not call updateTask to prevent duplicate side effects
        assertTrue(fakeTaskRepository.updatedTasks.isEmpty())
    }

    @Test
    fun invoke_notFoundTask_safeNoOp() = runBlocking {
        val result = completeTaskUseCase(99L)

        assertTrue(result.isSuccess)
        // Must not call updateTask
        assertTrue(fakeTaskRepository.updatedTasks.isEmpty())
    }

    class FakeTaskRepository : TaskRepository(
        org.mockito.Mockito.mock(com.duesoon.app.data.local.TaskDao::class.java), 
        org.mockito.Mockito.mock(com.duesoon.app.notification.NotificationScheduler::class.java), 
        org.mockito.Mockito.mock(com.duesoon.app.data.repository.UserPreferencesRepository::class.java)
    ) {
        val tasks = mutableMapOf<Long, Task>()
        val updatedTasks = mutableListOf<Task>()

        override suspend fun getTask(id: Long): Task? {
            return tasks[id]
        }

        override suspend fun updateTask(task: Task) {
            updatedTasks.add(task)
            tasks[task.id] = task
        }
    }
}
