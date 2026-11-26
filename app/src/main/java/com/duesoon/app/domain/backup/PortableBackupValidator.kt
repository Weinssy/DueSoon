package com.duesoon.app.domain.backup

import com.duesoon.app.domain.model.FocusSession
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.util.RecurrenceRuleParser
import java.time.LocalDate
import java.time.format.DateTimeParseException

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
        if (backup.tasks.isEmpty() && backup.focusSessions.isEmpty()) {
            errors.add(BackupValidationError.EmptyBackup)
            return ValidationResult.Failure(errors) // Fast fail if empty
        }

        val validatedTasks = mutableListOf<Task>()
        val seenIds = mutableSetOf<Long>()

        // 4. Task Validation
        for ((index, portableTask) in backup.tasks.withIndex()) {
            val taskErrorsStart = errors.size

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

            if (errors.size == taskErrorsStart) {
                validatedTasks.add(
                    PortableTaskMapper.toDomainTask(
                        portableTask.copy(title = normalizedTitle)
                    )
                )
            }
        }

        // 5. Focus Session Validation
        val validatedSessions = mutableListOf<FocusSession>()
        val seenSessionIds = mutableSetOf<Long>()

        for ((index, portableSession) in backup.focusSessions.withIndex()) {
            val sessionErrorsStart = errors.size

            if (portableSession.durationMinutes <= 0) {
                errors.add(
                    BackupValidationError.InvalidField(
                        index,
                        "focusSessions.durationMinutes",
                        "Duration minutes must be positive"
                    )
                )
            }

            val trimmedDateKey = portableSession.dateKey.trim()
            if (!isValidIsoDateKey(trimmedDateKey)) {
                errors.add(
                    BackupValidationError.InvalidField(
                        index,
                        "focusSessions.dateKey",
                        "dateKey must be a valid YYYY-MM-DD date"
                    )
                )
            }

            if (portableSession.completedAt < 0) {
                errors.add(
                    BackupValidationError.InvalidTimestamp(
                        index,
                        "focusSessions.completedAt",
                        "Timestamp cannot be negative"
                    )
                )
            }

            if (mode == ValidationMode.RESTORE && portableSession.id > 0L) {
                if (!seenSessionIds.add(portableSession.id)) {
                    errors.add(BackupValidationError.DuplicateId(portableSession.id))
                }
            } else if (mode == ValidationMode.RESTORE && portableSession.id <= 0L) {
                errors.add(BackupValidationError.InvalidId(index, "Focus session ID must be positive for restore"))
            }

            if (errors.size == sessionErrorsStart) {
                validatedSessions.add(
                    PortableFocusSessionMapper.toDomain(
                        portableSession.copy(dateKey = trimmedDateKey),
                        mode
                    )
                )
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success(
                ValidatedPortableBackup(
                    exportedAt = backup.exportedAt,
                    tasks = validatedTasks,
                    focusSessions = validatedSessions
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

    private fun isValidIsoDateKey(value: String): Boolean {
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(value)) return false
        return try {
            LocalDate.parse(value)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }
}
