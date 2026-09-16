package com.duesoon.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duesoon.app.data.local.AppDatabase
import com.duesoon.app.data.local.TaskEntity
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskRepositorySnoozeTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TaskRepository
    private lateinit var fakeScheduler: FakeNotificationScheduler

    class FakeNotificationScheduler : NotificationScheduler {
        var scheduleSnoozeCalled = false
        var cancelSnoozeCalled = false
        var lastSnoozedUntil = -1L

        override fun schedule(task: Task) {}
        override fun cancel(taskId: Long) {}
        override fun cancelAll(task: Task) {}
        override fun scheduleSnooze(task: Task, snoozedUntil: Long) {
            scheduleSnoozeCalled = true
            lastSnoozedUntil = snoozedUntil
        }
        override fun cancelSnooze(task: Task) {
            cancelSnoozeCalled = true
        }
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        fakeScheduler = FakeNotificationScheduler()
        repo = TaskRepository(
            db.taskDao(),
            fakeScheduler,
            UserPreferencesRepository(context),
            context
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testSnoozeStateTransitions() = runBlocking {
        // 1. Valid snooze
        val task = Task(
            title = "Test",
            priority = Priority.NORMAL,
            reminderType = ReminderType.SMART,
            completed = false
        )
        val id = repo.insertTask(task)
        
        val snoozedUntil = System.currentTimeMillis() + 60000
        repo.snoozeTask(id, snoozedUntil)
        
        var currentTask = repo.getTask(id)
        assertNotNull(currentTask)
        assertEquals(snoozedUntil, currentTask?.snoozedUntil)
        assertTrue(fakeScheduler.scheduleSnoozeCalled)
        assertEquals(snoozedUntil, fakeScheduler.lastSnoozedUntil)
        
        // 2. Clear snooze
        repo.clearSnooze(id)
        currentTask = repo.getTask(id)
        assertNull(currentTask?.snoozedUntil)
        assertTrue(fakeScheduler.cancelSnoozeCalled)
        
        // 3. Completed task does not snooze
        repo.updateTask(currentTask!!.copy(completed = true))
        fakeScheduler.scheduleSnoozeCalled = false
        repo.snoozeTask(id, snoozedUntil)
        currentTask = repo.getTask(id)
        assertNull(currentTask?.snoozedUntil)
        assertFalse(fakeScheduler.scheduleSnoozeCalled)
        
        // 4. Missing task does not crash or schedule
        fakeScheduler.scheduleSnoozeCalled = false
        repo.snoozeTask(999L, snoozedUntil)
        assertFalse(fakeScheduler.scheduleSnoozeCalled)
    }
}
