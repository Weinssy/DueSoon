package com.duesoon.app.domain.backup

import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.util.RecurrenceRuleParser
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task

enum class ValidationMode {
    IMPORT,
    RESTORE
}

sealed class ValidationResult {
    data class Success(val validatedBackup: ValidatedPortableBackup) : ValidationResult()
    data class Failure(val errors: List<BackupValidationError>) : ValidationResult()
}

object PortableBackupValidator {

    private const val SUPPORTED_PORTABLE_SCHEMA_VERSION = 1

    /**
     * Validates and normalizes the given backup according to the mode (IMPORT or RESTORE).
     * Does NOT have side effects on Database or NotificationScheduler.
     */
    fun validate(
        backup: PortableBackup,
        mode: ValidationMode
    ): ValidationResult {
        val errors = mutableListOf<BackupValidationError>()

        // 1. Schema Validation
        if (backup.schemaVersion != SUPPORTED_PORTABLE_SCHEMA_VERSION) {
            errors.add(BackupValidationError.UnsupportedSchema)
            return ValidationResult.Failure(errors) // Fast fail for schema
        }

        // 2. ExportedAt Validation
        if (backup.exportedAt < 0) {
            errors.add(BackupValidationError.InvalidTimestamp(-1, "exportedAt", "Timestamp cannot be negative"))
        }

        // 3. Empty List Decision
        // PRD explicitly rejects empty import. For restore, we also reject empty for safety (OPEN DECISION).
        if (backup.tasks.isEmpty()) {
            errors.add(BackupValidationError.EmptyBackup)
            return ValidationResult.Failure(errors) // Fast fail if empty
        }

        val validatedTasks = mutableListOf<Task>()
        val seenIds = mutableSetOf<Long>()

        // 4. Task Validation
        for ((index, portableTask) in backup.tasks.withIndex()) {
            
            // Title Validation
            val normalizedTitle = portableTask.title.trim()
            if (normalizedTitle.isEmpty()) {
                errors.add(BackupValidationError.InvalidField(index, "title", "Title cannot be empty or blank"))
            }

            // Enum Validation
            if (!isValidPriority(portableTask.priority)) {
                errors.add(BackupValidationError.InvalidEnum(index, "priority", portableTask.priority))
            }
            if (!isValidReminderType(portableTask.reminderType)) {
                errors.add(BackupValidationError.InvalidEnum(index, "reminderType", portableTask.reminderType))
            }

            // Timestamp Validation
            if (portableTask.createdAt < 0) {
                errors.add(BackupValidationError.InvalidTimestamp(index, "createdAt", "Timestamp cannot be negative"))
            }
            if (portableTask.updatedAt < 0) {
                errors.add(BackupValidationError.InvalidTimestamp(index, "updatedAt", "Timestamp cannot be negative"))
            }
            if (portableTask.updatedAt < portableTask.createdAt) {
                errors.add(BackupValidationError.InvalidTimestamp(index, "updatedAt", "updatedAt cannot be before createdAt"))
            }
            if (portableTask.deadline != null && portableTask.deadline < 0) {
                errors.add(BackupValidationError.InvalidTimestamp(index, "deadline", "Timestamp cannot be negative"))
            }
            if (portableTask.snoozedUntil != null && portableTask.snoozedUntil < 0) {
                errors.add(BackupValidationError.InvalidTimestamp(index, "snoozedUntil", "Timestamp cannot be negative"))
            }

            // Recurring Validation
            if (!portableTask.isRecurring) {
                if (portableTask.recurrenceInterval != null) {
                    errors.add(BackupValidationError.InvalidRecurringState(index, "recurrenceInterval must be null if isRecurring is false"))
                }
            } else {
                if (portableTask.recurrenceInterval == null) {
                    errors.add(BackupValidationError.InvalidRecurringState(index, "recurrenceInterval must not be null if isRecurring is true"))
                } else if (!isValidRecurrenceInterval(portableTask.recurrenceInterval)) {
                    errors.add(BackupValidationError.InvalidEnum(index, "recurrenceInterval", portableTask.recurrenceInterval))
                }
            }

            // Mode-specific ID Validation
            if (mode == ValidationMode.RESTORE) {
                if (portableTask.id <= 0) {
                    errors.add(BackupValidationError.InvalidId(index, "ID must be positive for restore"))
                }
                if (!seenIds.add(portableTask.id)) {
                    errors.add(BackupValidationError.DuplicateId(portableTask.id))
                }
            }

            // If no errors so far for this task (meaning enum parsing won't throw), map to Domain
            if (errors.isEmpty()) {
                validatedTasks.add(
                    PortableTaskMapper.toDomainTask(
                        portableTask.copy(title = normalizedTitle) // Normalize title
                    )
                )
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success(
                ValidatedPortableBackup(
                    exportedAt = backup.exportedAt,
                    tasks = validatedTasks
                )
            )
        } else {
            ValidationResult.Failure(errors)
        }
    }

    private fun isValidPriority(value: String): Boolean {
        return try {
            Priority.valueOf(value)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun isValidReminderType(value: String): Boolean {
        return try {
            ReminderType.valueOf(value)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun isValidRecurrenceInterval(value: String): Boolean {
        return RecurrenceRuleParser.deserialize(value) != null
    }
}
