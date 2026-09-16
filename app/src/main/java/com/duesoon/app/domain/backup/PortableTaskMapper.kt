package com.duesoon.app.domain.backup

import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.RecurrenceInterval
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task

object PortableTaskMapper {
    /**
     * Converts a domain Task to a PortableTask for serialization.
     */
    fun toPortableTask(task: Task): PortableTask {
        return PortableTask(
            id = task.id,
            title = task.title,
            description = task.description,
            deadline = task.deadline,
            category = task.category,
            priority = task.priority.name,
            reminderType = task.reminderType.name,
            isRecurring = task.isRecurring,
            recurrenceInterval = task.recurrenceInterval?.name,
            completed = task.completed,
            snoozedUntil = task.snoozedUntil,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt
        )
    }

    /**
     * Converts a PortableTask to a domain Task.
     * Note: This does NOT apply ID manipulation (stripping or preserving).
     * ID orchestration is handled by the Import/Restore logic layer.
     */
    fun toDomainTask(portableTask: PortableTask): Task {
        return Task(
            id = portableTask.id,
            title = portableTask.title,
            description = portableTask.description,
            deadline = portableTask.deadline,
            category = portableTask.category,
            priority = Priority.valueOf(portableTask.priority),
            reminderType = ReminderType.valueOf(portableTask.reminderType),
            isRecurring = portableTask.isRecurring,
            recurrenceInterval = portableTask.recurrenceInterval?.let { RecurrenceInterval.valueOf(it) },
            completed = portableTask.completed,
            snoozedUntil = portableTask.snoozedUntil,
            createdAt = portableTask.createdAt,
            updatedAt = portableTask.updatedAt
        )
    }
}
