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
    val updatedAt: Long = System.currentTimeMillis()
)
