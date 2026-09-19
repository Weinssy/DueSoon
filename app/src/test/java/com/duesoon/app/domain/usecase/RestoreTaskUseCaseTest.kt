package com.duesoon.app.domain.usecase

import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock

class RestoreTaskUseCaseTest {

    private val mockScheduler = mock(NotificationScheduler::class.java)
    private val mockPrefsRepo = mock(UserPreferencesRepository::class.java)
    
    private val fakeTasks = mutableListOf<Task>()
    private var updatedTaskResult: Task? = null

    private val fakeRepository = object : TaskRepository(mock(com.duesoon.app.data.local.TaskDao::class.java), mockScheduler, mockPrefsRepo, null) {
        override suspend fun getTask(id: Long): Task? {
            return fakeTasks.find { it.id == id }
        }

        override suspend fun updateTask(task: Task) {
            updatedTaskResult = task
        }
    }
    
    private val useCase = RestoreTaskUseCase(
        repository = fakeRepository,
        notificationScheduler = mockScheduler,
        userPreferencesRepository = mockPrefsRepo,
        context = null
    )

    @Test
    fun `invoke with active task does nothing`() = runBlocking {
        val activeTask = Task(1, "Active", null, null, null, Priority.NORMAL, ReminderType.SMART, false, null, false, null, 0L, 0L)
        fakeTasks.add(activeTask)
        updatedTaskResult = null

        useCase(1)

        assertNull(updatedTaskResult)
    }

    @Test
    fun `invoke with completed task restores it`() = runBlocking {
        val completedTask = Task(2, "Completed", null, null, null, Priority.NORMAL, ReminderType.SMART, false, null, true, null, 0L, 0L)
        fakeTasks.add(completedTask)
        updatedTaskResult = null

        useCase(2)

        assertEquals(false, updatedTaskResult?.completed)
        assertEquals(2L, updatedTaskResult?.id)
    }
}
