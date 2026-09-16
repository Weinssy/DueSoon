package com.duesoon.app.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableBackupValidatorTest {

    private fun createValidBackup(schemaVersion: Int = 1, tasks: List<PortableTask> = listOf(createValidTask())): PortableBackup {
        return PortableBackup(
            schemaVersion = schemaVersion,
            appVersion = "1.3.0",
            exportedAt = 1000L,
            tasks = tasks
        )
    }

    private fun createValidTask(id: Long = 1L): PortableTask {
        return PortableTask(
            id = id,
            title = "Valid Task",
            description = null,
            deadline = null,
            category = null,
            priority = "NORMAL",
            reminderType = "SMART",
            isRecurring = false,
            recurrenceInterval = null,
            completed = false,
            snoozedUntil = null,
            createdAt = 1000L,
            updatedAt = 2000L
        )
    }

    @Test
    fun `schemaVersion == 1 returns Success`() {
        val backup = createValidBackup()
        val result = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `schemaVersion greater than 1 returns UnsupportedSchema`() {
        val backup = createValidBackup(schemaVersion = 2)
        val result = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        assertTrue(result is ValidationResult.Failure)
        assertTrue((result as ValidationResult.Failure).errors.contains(BackupValidationError.UnsupportedSchema))
    }

    @Test
    fun `schemaVersion 0 or negative returns UnsupportedSchema`() {
        val backup0 = createValidBackup(schemaVersion = 0)
        val result0 = PortableBackupValidator.validate(backup0, ValidationMode.IMPORT)
        assertTrue((result0 as ValidationResult.Failure).errors.contains(BackupValidationError.UnsupportedSchema))

        val backupNeg = createValidBackup(schemaVersion = -1)
        val resultNeg = PortableBackupValidator.validate(backupNeg, ValidationMode.IMPORT)
        assertTrue((resultNeg as ValidationResult.Failure).errors.contains(BackupValidationError.UnsupportedSchema))
    }

    @Test
    fun `blank title returns InvalidField`() {
        val backup = createValidBackup(tasks = listOf(createValidTask().copy(title = "   ")))
        val result = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        val failure = result as ValidationResult.Failure
        assertTrue(failure.errors.any { it is BackupValidationError.InvalidField && it.fieldName == "title" })
    }

    @Test
    fun `valid enums pass validation`() {
        val backup = createValidBackup(tasks = listOf(
            createValidTask().copy(priority = "LOW", reminderType = "NONE"),
            createValidTask().copy(priority = "HIGH", reminderType = "CUSTOM", isRecurring = true, recurrenceInterval = "MONTHLY")
        ))
        val result = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `unknown enums return InvalidEnum`() {
        val backup = createValidBackup(tasks = listOf(createValidTask().copy(priority = "high")))
        val result = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        assertTrue((result as ValidationResult.Failure).errors.any { it is BackupValidationError.InvalidEnum && it.fieldName == "priority" })
    }

    @Test
    fun `valid timestamps pass`() {
        val backup = createValidBackup()
        val result = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `negative timestamps return InvalidTimestamp`() {
        val backup = createValidBackup(tasks = listOf(createValidTask().copy(createdAt = -1L)))
        val result = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        assertTrue((result as ValidationResult.Failure).errors.any { it is BackupValidationError.InvalidTimestamp && it.fieldName == "createdAt" })
    }

    @Test
    fun `updatedAt older than createdAt returns InvalidTimestamp`() {
        val backup = createValidBackup(tasks = listOf(createValidTask().copy(createdAt = 2000L, updatedAt = 1000L)))
        val result = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        assertTrue((result as ValidationResult.Failure).errors.any { it is BackupValidationError.InvalidTimestamp && it.fieldName == "updatedAt" })
    }

    @Test
    fun `past deadline is VALID`() {
        val backup = createValidBackup(tasks = listOf(createValidTask().copy(deadline = 100L))) // small epoch = past
        val result = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `recurring states combinations`() {
        // false + null = valid
        assertTrue(PortableBackupValidator.validate(createValidBackup(tasks = listOf(createValidTask().copy(isRecurring = false, recurrenceInterval = null))), ValidationMode.IMPORT) is ValidationResult.Success)
        
        // false + interval = invalid
        assertTrue(PortableBackupValidator.validate(createValidBackup(tasks = listOf(createValidTask().copy(isRecurring = false, recurrenceInterval = "DAILY"))), ValidationMode.IMPORT) is ValidationResult.Failure)

        // true + DAILY = valid
        assertTrue(PortableBackupValidator.validate(createValidBackup(tasks = listOf(createValidTask().copy(isRecurring = true, recurrenceInterval = "DAILY"))), ValidationMode.IMPORT) is ValidationResult.Success)

        // true + null = invalid
        assertTrue(PortableBackupValidator.validate(createValidBackup(tasks = listOf(createValidTask().copy(isRecurring = true, recurrenceInterval = null))), ValidationMode.IMPORT) is ValidationResult.Failure)
    }

    @Test
    fun `completed states valid regardless of deadline`() {
        assertTrue(PortableBackupValidator.validate(createValidBackup(tasks = listOf(createValidTask().copy(completed = true, deadline = 100L))), ValidationMode.IMPORT) is ValidationResult.Success)
        assertTrue(PortableBackupValidator.validate(createValidBackup(tasks = listOf(createValidTask().copy(completed = true, deadline = null))), ValidationMode.IMPORT) is ValidationResult.Success)
    }

    @Test
    fun `import allows duplicate and zero IDs`() {
        val backup = createValidBackup(tasks = listOf(createValidTask(id = 1), createValidTask(id = 1), createValidTask(id = 0)))
        val result = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `restore requires unique and positive IDs`() {
        val backupPositive = createValidBackup(tasks = listOf(createValidTask(id = 1), createValidTask(id = 2)))
        assertTrue(PortableBackupValidator.validate(backupPositive, ValidationMode.RESTORE) is ValidationResult.Success)

        val backupDuplicate = createValidBackup(tasks = listOf(createValidTask(id = 1), createValidTask(id = 1)))
        val resultDup = PortableBackupValidator.validate(backupDuplicate, ValidationMode.RESTORE)
        assertTrue((resultDup as ValidationResult.Failure).errors.any { it is BackupValidationError.DuplicateId })

        val backupZero = createValidBackup(tasks = listOf(createValidTask(id = 0)))
        val resultZero = PortableBackupValidator.validate(backupZero, ValidationMode.RESTORE)
        assertTrue((resultZero as ValidationResult.Failure).errors.any { it is BackupValidationError.InvalidId })
    }
    
    @Test
    fun `empty backup returns EmptyBackup failure`() {
        val backup = createValidBackup(tasks = emptyList())
        val resultImport = PortableBackupValidator.validate(backup, ValidationMode.IMPORT)
        assertTrue((resultImport as ValidationResult.Failure).errors.contains(BackupValidationError.EmptyBackup))

        val resultRestore = PortableBackupValidator.validate(backup, ValidationMode.RESTORE)
        assertTrue((resultRestore as ValidationResult.Failure).errors.contains(BackupValidationError.EmptyBackup))
    }
}
