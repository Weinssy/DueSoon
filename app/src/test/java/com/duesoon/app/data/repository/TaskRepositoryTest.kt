package com.duesoon.app.data.repository

import com.duesoon.app.data.local.TaskDao
import com.duesoon.app.data.local.TaskEntity
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.RecurrenceRule
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TaskRepositoryTest {

    private val mockDao: TaskDao = mock()
    private val mockNotificationScheduler: NotificationScheduler = mock()
    private val mockPrefsRepo: UserPreferencesRepository = mock()
    private val repository = TaskRepository(mockDao, mockNotificationScheduler, mockPrefsRepo, null)

    @Before
    fun setup() {
        whenever(mockPrefsRepo.userPreferencesFlow).thenReturn(flowOf(UserPreferencesState()))
    }

    @Test
    fun `observeActiveTasks maps entities correctly`() = runBlocking {
        val entities = listOf(
            TaskEntity(1, "Test Active", null, null, null, Priority.NORMAL.name, ReminderType.SMART.name, false, null, false, null, 0L, 0L)
        )
        whenever(mockDao.observeActiveTasks()).thenReturn(flowOf(entities))

        val result = repository.observeActiveTasks().first()
        assertEquals(1, result.size)
        assertEquals("Test Active", result[0].title)
        assertEquals(false, result[0].completed)
    }

    @Test
    fun `observeTasks maps entities correctly`() = runBlocking {
        val entities = listOf(
            TaskEntity(1, "Task 1", null, null, null, Priority.NORMAL.name, ReminderType.SMART.name, false, null, false, null, 0L, 0L),
            TaskEntity(2, "Task 2", null, null, null, Priority.HIGH.name, ReminderType.SMART.name, false, null, true, null, 0L, 0L)
        )
        whenever(mockDao.observeTasks()).thenReturn(flowOf(entities))

        val result = repository.observeTasks().first()
        assertEquals(2, result.size)
        assertEquals("Task 1", result[0].title)
        assertEquals("Task 2", result[1].title)
        assertTrue(result[1].completed)
    }

    @Test
    fun `updateTask with recurring completed task creates next instance with new uuid and advanced deadline`() = runBlocking {
        val now = System.currentTimeMillis()
        val originalUuid = "test-uuid-12345"
        val recurringTask = Task(
            id = 42,
            uuid = originalUuid,
            title = "Daily Standup",
            deadline = now,
            completed = true,
            isRecurring = true,
            recurrenceRule = RecurrenceRule.Daily,
            createdAt = now - 1000,
            updatedAt = now
        )

        whenever(mockDao.insert(any())).thenReturn(43L)

        repository.updateTask(recurringTask)

        // Verify update was called on the completed task
        val updateCaptor = argumentCaptor<TaskEntity>()
        verify(mockDao).update(updateCaptor.capture())
        assertEquals(42L, updateCaptor.firstValue.id)
        assertTrue(updateCaptor.firstValue.completed)

        // Verify insert was called for the new recurring task instance
        val insertCaptor = argumentCaptor<TaskEntity>()
        verify(mockDao).insert(insertCaptor.capture())
        val nextEntity = insertCaptor.firstValue
        assertEquals("Daily Standup", nextEntity.title)
        assertFalse(nextEntity.completed)
        assertTrue(nextEntity.isRecurring)
        assertTrue(nextEntity.deadline!! > now)
        assertNotEquals(originalUuid, nextEntity.uuid)
    }
}
