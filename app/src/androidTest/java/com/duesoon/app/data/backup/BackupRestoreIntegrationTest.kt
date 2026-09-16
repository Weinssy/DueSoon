package com.duesoon.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duesoon.app.data.local.AppDatabase
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Integration tests for STEP 9.10 running on an actual Android environment.
 * Tests Export, Import, Restore, Validation, Notification edge cases, and Failure safety.
 */
@RunWith(AndroidJUnit4::class)
class BackupRestoreIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TaskRepository
    private lateinit var userPrefsRepo: UserPreferencesRepository
    private lateinit var coordinator: BackupRestoreCoordinator
    private lateinit var fakeStorage: FakeSafBackupStorage
    private lateinit var fakeScheduler: TrackingNotificationScheduler

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        userPrefsRepo = UserPreferencesRepository(context)
        
        fakeScheduler = TrackingNotificationScheduler()
        repository = TaskRepository(
            taskDao = db.taskDao(),
            notificationScheduler = fakeScheduler,
            userPreferencesRepository = userPrefsRepo
        )
        fakeStorage = FakeSafBackupStorage()

        coordinator = BackupRestoreCoordinator(
            taskRepository = repository,
            backupStorage = fakeStorage,
            appVersion = "1.3.0",
            notificationScheduler = fakeScheduler,
            userPreferencesRepository = userPrefsRepo
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    // --- 1. Export Tests ---
    @Test
    fun testExport_createsValidJsonAndSchemaVersion() = runBlocking {
        // Insert a task
        val task = Task(
            id = 0,
            title = "Export Me",
            priority = Priority.NORMAL,
            deadline = System.currentTimeMillis() + 100000,
            reminderType = ReminderType.SMART,
            isRecurring = false,
            recurrenceInterval = null
        )
        repository.insertTask(task)

        val uri = Uri.parse("content://fake/export")
        val result = coordinator.exportBackup(uri)

        assertTrue(result is BackupResult.Success)
        val jsonOutput = fakeStorage.outputStream.toString(Charsets.UTF_8.name())
        
        assertTrue("JSON should contain schemaVersion 1", jsonOutput.contains("\"schemaVersion\": 1"))
        assertTrue("JSON should contain the task title", jsonOutput.contains("\"title\": \"Export Me\""))
    }

    // --- 2. Import Tests ---
    @Test
    fun testImport_appendsTasksAndHandlesExternalIds() = runBlocking {
        val existingTask = Task(id = 0, title = "Existing Task")
        repository.insertTask(existingTask)

        val validBackupJson = """
            {
              "schemaVersion": 1,
              "appVersion": "1.3.0",
              "exportedAt": 123456789,
              "tasks": [
                {
                  "id": 101,
                  "title": "Imported Task",
                  "priority": "HIGH",
                  "deadline": ${System.currentTimeMillis() + 86400000},
                  "reminderType": "SMART",
                  "isRecurring": false,
                  "completed": false,
                  "createdAt": 100,
                  "updatedAt": 100
                }
              ]
            }
        """.trimIndent()
        fakeStorage.inputBytes = validBackupJson.toByteArray(Charsets.UTF_8)
        val uri = Uri.parse("content://fake/import")

        // Prepare
        val prepResult = coordinator.prepareImport(uri)
        assertTrue(prepResult is BackupResult.Success)
        val backup = (prepResult as BackupResult.Success).data

        // Execute
        fakeScheduler.clear()
        val execResult = coordinator.executeImport(backup)
        assertTrue(execResult is BackupResult.Success)

        val tasks = repository.observeTasks().first()
        assertEquals("Should have 2 tasks now", 2, tasks.size)
        assertTrue(tasks.any { it.title == "Existing Task" })
        assertTrue(tasks.any { it.title == "Imported Task" })
        
        // Reminder for new task should be created
        assertEquals(1, fakeScheduler.scheduledTasks.size)
    }

    // --- 3. Restore Tests ---
    @Test
    fun testRestore_isDestructiveAndCancelsOldAlarms() = runBlocking {
        val oldTask = Task(id = 0, title = "Old Task", deadline = System.currentTimeMillis() + 1000)
        repository.insertTask(oldTask)
        
        // Simulate alarm created for old task
        fakeScheduler.scheduledTasks.add(1L) // ID generated by Room is likely 1

        val validBackupJson = """
            {
              "schemaVersion": 1,
              "appVersion": "1.3.0",
              "exportedAt": 123456789,
              "tasks": [
                {
                  "id": 102,
                  "title": "Restored Task",
                  "priority": "LOW",
                  "deadline": ${System.currentTimeMillis() + 86400000},
                  "reminderType": "SMART",
                  "isRecurring": false,
                  "completed": false,
                  "createdAt": 100,
                  "updatedAt": 100
                }
              ]
            }
        """.trimIndent()
        fakeStorage.inputBytes = validBackupJson.toByteArray(Charsets.UTF_8)
        val uri = Uri.parse("content://fake/restore")

        // Prepare
        val prepResult = coordinator.prepareRestore(uri)
        assertTrue(prepResult is BackupResult.Success)
        val backup = (prepResult as BackupResult.Success).data

        // Execute
        val execResult = coordinator.executeRestore(backup)
        assertTrue(execResult is BackupResult.Success)

        val tasks = repository.observeTasks().first()
        assertEquals("Old task should be destroyed", 1, tasks.size)
        assertEquals("Restored Task", tasks[0].title)

        assertTrue("Old alarms must be cancelled during restore", fakeScheduler.cancelAllCalled)
        assertEquals("New alarm should be scheduled", 1, fakeScheduler.scheduledTasks.size)
    }

    // --- 4. Invalid Backup Rejection ---
    @Test
    fun testInvalidBackup_rejectedBeforeChanges() = runBlocking {
        val oldTask = Task(id = 0, title = "Untouched Task")
        repository.insertTask(oldTask)
        
        // Missing schemaVersion
        val invalidJson = """
            {
              "appVersion": "1.3.0",
              "tasks": []
            }
        """.trimIndent()
        fakeStorage.inputBytes = invalidJson.toByteArray(Charsets.UTF_8)
        val uri = Uri.parse("content://fake/import")

        val prepResult = coordinator.prepareImport(uri)
        assertTrue(prepResult is BackupResult.Error.InvalidFormat)

        val tasks = repository.observeTasks().first()
        assertEquals("Database must remain unchanged", 1, tasks.size)
    }

    // --- 5. Notification Edge Cases ---
    @Test
    fun testNotificationReconciliation_edgeCases() = runBlocking {
        val now = System.currentTimeMillis()
        val backupJson = """
            {
              "schemaVersion": 1,
              "appVersion": "1.3.0",
              "exportedAt": 123456789,
              "tasks": [
                { "id": 201, "title": "Completed", "completed": true, "deadline": ${now + 86400000}, "priority": "NORMAL", "reminderType": "SMART", "isRecurring": false, "createdAt": 100, "updatedAt": 100 },
                { "id": 202, "title": "No Deadline", "completed": false, "deadline": null, "priority": "NORMAL", "reminderType": "SMART", "isRecurring": false, "createdAt": 100, "updatedAt": 100 },
                { "id": 203, "title": "Overdue", "completed": false, "deadline": ${now - 86400000}, "priority": "NORMAL", "reminderType": "SMART", "isRecurring": false, "createdAt": 100, "updatedAt": 100 },
                { "id": 204, "title": "Future Snooze", "completed": false, "deadline": ${now + 86400000}, "snoozedUntil": ${now + 3600000}, "priority": "NORMAL", "reminderType": "SMART", "isRecurring": false, "createdAt": 100, "updatedAt": 100 },
                { "id": 205, "title": "Past Snooze", "completed": false, "deadline": ${now + 86400000}, "snoozedUntil": ${now - 3600000}, "priority": "NORMAL", "reminderType": "SMART", "isRecurring": false, "createdAt": 100, "updatedAt": 100 }
              ]
            }
        """.trimIndent()
        fakeStorage.inputBytes = backupJson.toByteArray(Charsets.UTF_8)
        val uri = Uri.parse("content://fake/import")
        
        val prepResult = coordinator.prepareImport(uri)
        val execResult = coordinator.executeImport((prepResult as BackupResult.Success).data)
        
        assertTrue(execResult is BackupResult.Success)

        // Verifications
        // t1 (completed) -> no schedule
        // t2 (no deadline) -> no schedule
        // t3 (overdue) -> no retroactive schedule
        // t4 (future snooze) -> scheduleSnooze called
        // t5 (past snooze) -> snooze cleared (no scheduleSnooze), normal schedule called
        
        val tasks = repository.observeTasks().first()
        val t1 = tasks.find { it.title == "Completed" }!!
        val t2 = tasks.find { it.title == "No Deadline" }!!
        val t3 = tasks.find { it.title == "Overdue" }!!
        val t4 = tasks.find { it.title == "Future Snooze" }!!
        val t5 = tasks.find { it.title == "Past Snooze" }!!

        assertFalse("Completed task should not be scheduled", fakeScheduler.scheduledTasks.contains(t1.id))
        assertFalse("No deadline should not be scheduled", fakeScheduler.scheduledTasks.contains(t2.id))
        assertFalse("Overdue should not be scheduled", fakeScheduler.scheduledTasks.contains(t3.id))
        assertTrue("Future snooze should be snoozed", fakeScheduler.snoozedTasks.contains(t4.id))
        
        assertTrue("Past snooze should be treated as normal schedule", fakeScheduler.scheduledTasks.contains(t5.id))
        assertFalse("Past snooze should NOT be snoozed", fakeScheduler.snoozedTasks.contains(t5.id))
    }
}

// Fakes

class TrackingNotificationScheduler : NotificationScheduler {
    val scheduledTasks = mutableSetOf<Long>()
    val snoozedTasks = mutableSetOf<Long>()
    var cancelAllCalled = false

    override fun schedule(task: Task) {
        val reminders = com.duesoon.app.notification.SmartReminderCalculator.calculateReminders(task)
        if (reminders.isNotEmpty()) {
            scheduledTasks.add(task.id)
        }
    }

    override fun cancel(taskId: Long) {
        scheduledTasks.remove(taskId)
    }

    override fun cancelAll(task: Task) {
        cancelAllCalled = true
        scheduledTasks.remove(task.id)
    }

    fun clear() {
        scheduledTasks.clear()
        snoozedTasks.clear()
        cancelAllCalled = false
    }

    override fun scheduleSnooze(task: Task, snoozedUntil: Long) {
        snoozedTasks.add(task.id)
    }

    override fun cancelSnooze(task: Task) {
        snoozedTasks.remove(task.id)
    }
}

class FakeSafBackupStorage : BackupStorage {
    var inputBytes: ByteArray = ByteArray(0)
    var outputStream = ByteArrayOutputStream()

    override fun openInputStream(uri: Uri): InputStream? {
        return ByteArrayInputStream(inputBytes)
    }

    override fun openOutputStream(uri: Uri): OutputStream? {
        outputStream = ByteArrayOutputStream()
        return outputStream
    }
}
