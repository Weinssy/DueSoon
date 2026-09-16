package com.duesoon.app.data.backup

import com.duesoon.app.domain.backup.BackupValidationError

sealed class BackupResult<out T> {
    data class Success<T>(val data: T) : BackupResult<T>()
    
    sealed class Error : BackupResult<Nothing>() {
        object Cancelled : Error()
        data class StorageError(val message: String) : Error()
        data class InvalidFormat(val message: String) : Error()
        data class UnsupportedSchema(val message: String) : Error()
        data class DatabaseError(val message: String) : Error()
        data class ValidationErrors(val errors: List<BackupValidationError>) : Error()
    }
}
