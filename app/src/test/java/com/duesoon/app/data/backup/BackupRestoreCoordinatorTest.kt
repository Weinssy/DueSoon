package com.duesoon.app.data.backup

import android.net.Uri
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.backup.PortableBackup
import com.duesoon.app.domain.backup.BackupSerializer
import com.duesoon.app.domain.backup.ValidationMode
import com.duesoon.app.domain.backup.ValidatedPortableBackup
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.encodeToString

import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BackupRestoreCoordinatorTest {

    private lateinit var coordinator: BackupRestoreCoordinator
    private lateinit var fakeStorage: FakeBackupStorage
    private lateinit var mockTaskRepo: TaskRepository
    private lateinit var mockScheduler: com.duesoon.app.notification.NotificationScheduler
    private lateinit var mockPrefsRepo: com.duesoon.app.data.repository.UserPreferencesRepository

    class FakeBackupStorage : BackupStorage {
        var inputBytes: ByteArray = ByteArray(0)
        var outputStream = ByteArrayOutputStream()

        override fun openOutputStream(uri: Uri): OutputStream? = outputStream
        override fun openInputStream(uri: Uri): InputStream? = ByteArrayInputStream(inputBytes)
    }

    @Before
    fun setup() {
        fakeStorage = FakeBackupStorage()
        mockTaskRepo = mock(TaskRepository::class.java)
        
        mockScheduler = mock(com.duesoon.app.notification.NotificationScheduler::class.java)
        mockPrefsRepo = mock(com.duesoon.app.data.repository.UserPreferencesRepository::class.java)
        `when`(mockPrefsRepo.userPreferencesFlow).thenReturn(
            flowOf(com.duesoon.app.data.repository.UserPreferencesState(notificationsEnabled = true))
        )
        `when`(mockTaskRepo.observeTasks()).thenReturn(flowOf(emptyList()))

        coordinator = BackupRestoreCoordinator(
            taskRepository = mockTaskRepo,
            backupStorage = fakeStorage,
            appVersion = "1.2.0",
            notificationScheduler = mockScheduler,
            userPreferencesRepository = mockPrefsRepo
        )
    }

    @Test
    fun testExportBackup_success() = runBlocking {
        val tasksToReturn = listOf(
            Task(
                id = 1L,
                title = "Test Task",
                priority = Priority.HIGH,
                deadline = 1000L,
                reminderType = ReminderType.SMART,
                isRecurring = false,
                recurrenceRule = null,
                completed = false,
                createdAt = 500L,
                updatedAt = 500L,
                snoozedUntil = null
            )
        )
        `when`(mockTaskRepo.observeTasks()).thenReturn(flowOf(tasksToReturn))

        val uri = mock(Uri::class.java)
        val result = coordinator.exportBackup(uri)

        assertTrue(result is BackupResult.Success)
        
        val jsonWritten = fakeStorage.outputStream.toString(Charsets.UTF_8.name())
        val decoded = BackupSerializer.json.decodeFromString<PortableBackup>(jsonWritten)
        
        assertEquals(1, decoded.schemaVersion)
        assertEquals("1.2.0", decoded.appVersion)
        assertEquals(1, decoded.tasks.size)
        assertEquals("Test Task", decoded.tasks[0].title)
    }

    @Test
    fun testPrepareRestore_success() = runBlocking {
        val backup = PortableBackup(
            schemaVersion = 1,
            appVersion = "1.2.0",
            exportedAt = 12345L,
            tasks = listOf(
                com.duesoon.app.domain.backup.PortableTask(
                    id = 1L,
                    title = "Test Task",
                    priority = "HIGH",
                    deadline = 1000L,
                    reminderType = "SMART",
                    isRecurring = false,
                    recurrenceInterval = null,
                    completed = false,
                    createdAt = 500L,
                    updatedAt = 500L,
                    snoozedUntil = null
                )
            )
        )
        fakeStorage.inputBytes = BackupSerializer.json.encodeToString(backup).toByteArray(Charsets.UTF_8)
        
        val uri = mock(Uri::class.java)
        val result = coordinator.prepareRestore(uri)

        assertTrue(result is BackupResult.Success)
        val data = (result as BackupResult.Success).data
        assertEquals(1, data.tasks.size)
        assertEquals("Test Task", data.tasks[0].title)
    }

    @Test
    fun testPrepareRestore_unsupportedSchema() = runBlocking {
        val json = """
            {
              "schemaVersion": 2,
              "appVersion": "1.2.0",
              "exportedAt": 12345,
              "tasks": []
            }
        """.trimIndent()
        
        fakeStorage.inputBytes = json.toByteArray(Charsets.UTF_8)
        
        val uri = mock(Uri::class.java)
        val result = coordinator.prepareRestore(uri)

        assertTrue(result is BackupResult.Error.UnsupportedSchema)
    }
    
    @Test
    fun testPrepareImport_emptyTasks() = runBlocking {
        val json = """
            {
              "schemaVersion": 1,
              "appVersion": "1.2.0",
              "exportedAt": 12345,
              "tasks": []
            }
        """.trimIndent()
        
        fakeStorage.inputBytes = json.toByteArray(Charsets.UTF_8)
        
        val uri = mock(Uri::class.java)
        val result = coordinator.prepareImport(uri)

        // Validation for Empty Backup should fail with InvalidFormat (as handled in Coordinator logic)
        assertTrue(result is BackupResult.Error.InvalidFormat)
    }

    @Test
    fun testExecuteImport_success() = runBlocking {
        val validatedBackup = ValidatedPortableBackup(
            exportedAt = 12345L,
            tasks = listOf(
                Task(
                    id = 1L,
                    title = "Test Task",
                    priority = Priority.HIGH,
                    deadline = 1000L,
                    reminderType = ReminderType.SMART,
                    isRecurring = false,
                    recurrenceRule = null,
                    completed = false,
                    createdAt = 500L,
                    updatedAt = 500L,
                    snoozedUntil = null
                )
            )
        )
        
        `when`(mockTaskRepo.observeTasks())
            .thenReturn(flowOf(emptyList())) // before
            .thenReturn(flowOf(listOf(
                Task(
                    id = 1L,
                    title = "Test Task",
                    priority = Priority.HIGH,
                    deadline = 1000L,
                    reminderType = ReminderType.SMART,
                    isRecurring = false,
                    recurrenceRule = null,
                    completed = false,
                    createdAt = 500L,
                    updatedAt = 500L,
                    snoozedUntil = null
                )
            ))) // after

        `when`(mockTaskRepo.importTasks(org.mockito.ArgumentMatchers.anyList())).thenReturn(Unit)
        
        val result = coordinator.executeImport(validatedBackup)
        assertTrue(result is BackupResult.Success)
    }

    @Test
    fun testExecuteImport_databaseError() = runBlocking {
        val validatedBackup = ValidatedPortableBackup(
            exportedAt = 12345L,
            tasks = listOf(
                Task(
                    id = 1L,
                    title = "Test Task",
                    priority = Priority.HIGH,
                    deadline = 1000L,
                    reminderType = ReminderType.SMART,
                    isRecurring = false,
                    recurrenceRule = null,
                    completed = false,
                    createdAt = 500L,
                    updatedAt = 500L,
                    snoozedUntil = null
                )
            )
        )
        
        `when`(mockTaskRepo.importTasks(org.mockito.ArgumentMatchers.anyList())).thenThrow(RuntimeException("SQLite constraint failed"))
        
        val result = coordinator.executeImport(validatedBackup)
        assertTrue(result is BackupResult.Error.DatabaseError)
        assertEquals("Failed to import tasks due to a database error.", (result as BackupResult.Error.DatabaseError).message)
    }

    @Test
    fun testExecuteRestore_success() = runBlocking {
        val validatedBackup = ValidatedPortableBackup(
            exportedAt = 12345L,
            tasks = listOf(
                Task(
                    id = 1L,
                    title = "Test Task",
                    priority = Priority.HIGH,
                    deadline = 1000L,
                    reminderType = ReminderType.SMART,
                    isRecurring = false,
                    recurrenceRule = null,
                    completed = false,
                    createdAt = 500L,
                    updatedAt = 500L,
                    snoozedUntil = null
                )
            )
        )
        
        `when`(mockTaskRepo.restoreTasks(org.mockito.ArgumentMatchers.anyList())).thenReturn(Unit)
        
        val result = coordinator.executeRestore(validatedBackup)
        assertTrue(result is BackupResult.Success)
    }

    @Test
    fun testExecuteRestore_databaseError() = runBlocking {
        val validatedBackup = ValidatedPortableBackup(
            exportedAt = 12345L,
            tasks = listOf(
                Task(
                    id = 1L,
                    title = "Test Task",
                    priority = Priority.HIGH,
                    deadline = 1000L,
                    reminderType = ReminderType.SMART,
                    isRecurring = false,
                    recurrenceRule = null,
                    completed = false,
                    createdAt = 500L,
                    updatedAt = 500L,
                    snoozedUntil = null
                )
            )
        )
        
        `when`(mockTaskRepo.restoreTasks(org.mockito.ArgumentMatchers.anyList())).thenThrow(RuntimeException("Transaction failed"))
        
        val result = coordinator.executeRestore(validatedBackup)
        assertTrue(result is BackupResult.Error.DatabaseError)
        assertEquals("Failed to restore tasks due to a database error.", (result as BackupResult.Error.DatabaseError).message)
    }
}
