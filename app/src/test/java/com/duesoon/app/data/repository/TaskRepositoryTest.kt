package com.duesoon.app.data.repository

import com.duesoon.app.data.local.TaskDao
import com.duesoon.app.data.local.TaskEntity
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TaskRepositoryTest {

    private val mockDao = mock(TaskDao::class.java)
    private val mockNotificationScheduler = mock(NotificationScheduler::class.java)
    private val mockPrefsRepo = mock(UserPreferencesRepository::class.java)
    private val repository = TaskRepository(mockDao, mockNotificationScheduler, mockPrefsRepo, null)

    @Test
    fun `observeActiveTasks maps entities correctly`() = runBlocking {
        val entities = listOf(TaskEntity(1, "Test Active", null, null, null, Priority.NORMAL.name, ReminderType.SMART.name, false, null, false, null, 0L, 0L))
        `when`(mockDao.observeActiveTasks()).thenReturn(flowOf(entities))

        val result = repository.observeActiveTasks().first()
        assertEquals(1, result.size)
        assertEquals("Test Active", result[0].title)
        assertEquals(false, result[0].completed)
    }

    @Test
    fun `observeArchivedTasks maps entities correctly`() = runBlocking {
        val entities = listOf(TaskEntity(2, "Test Archived", null, null, null, Priority.NORMAL.name, ReminderType.SMART.name, false, null, true, null, 0L, 0L))
        `when`(mockDao.observeArchivedTasks()).thenReturn(flowOf(entities))

        val result = repository.observeArchivedTasks().first()
        assertEquals(1, result.size)
        assertEquals("Test Archived", result[0].title)
        assertEquals(true, result[0].completed)
    }
    
    @Test
    fun `searchArchivedTasks passes query and maps entities correctly`() = runBlocking {
        val entities = listOf(TaskEntity(3, "Search Result", null, null, null, Priority.NORMAL.name, ReminderType.SMART.name, false, null, true, null, 0L, 0L))
        `when`(mockDao.searchArchivedTasks("Result")).thenReturn(flowOf(entities))

        val result = repository.searchArchivedTasks("Result").first()
        assertEquals(1, result.size)
        assertEquals("Search Result", result[0].title)
    }
}
