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

enum class RecurrenceInterval(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan")
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
    val recurrenceInterval: RecurrenceInterval? = null,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
