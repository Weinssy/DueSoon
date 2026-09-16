package com.duesoon.app.domain.backup

sealed interface BackupValidationError {
    data object UnsupportedSchema : BackupValidationError
    data object EmptyBackup : BackupValidationError // For both import and restore empty cases
    
    data class MissingRequiredField(val taskIndex: Int, val fieldName: String) : BackupValidationError
    data class InvalidField(val taskIndex: Int, val fieldName: String, val reason: String) : BackupValidationError
    data class InvalidEnum(val taskIndex: Int, val fieldName: String, val invalidValue: String) : BackupValidationError
    data class InvalidTimestamp(val taskIndex: Int, val fieldName: String, val reason: String) : BackupValidationError
    data class InvalidRecurringState(val taskIndex: Int, val reason: String) : BackupValidationError
    data class DuplicateId(val duplicateId: Long) : BackupValidationError // Only for RESTORE
    data class InvalidId(val taskIndex: Int, val reason: String) : BackupValidationError // Only for RESTORE
}
