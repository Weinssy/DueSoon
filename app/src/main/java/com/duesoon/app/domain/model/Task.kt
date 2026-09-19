package com.duesoon.app.domain.model

enum class Priority {
    LOW,
    NORMAL,
    HIGH
}

enum class ReminderType {
    SMART,
    CUSTOM,
    NONE
}
enum class SyncState {
    DIRTY,
    SYNCING,
    SYNCED
}


data class Task(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val deadline: Long? = null,
    val category: String? = null,
    val priority: Priority = Priority.NORMAL,
    val reminderType: ReminderType = ReminderType.SMART,
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule? = null,
    val completed: Boolean = false,
    val snoozedUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val uuid: String = "",
    val isDeleted: Boolean = false,
    val updatedAtUtc: Long = 0L,
    val revision: Long = 1L,
    val syncState: SyncState = SyncState.DIRTY
)
