package com.duesoon.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String?,
    val deadline: Long?,
    val category: String?,
    val priority: String,
    val reminderType: String,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

fun TaskEntity.toDomainModel(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        deadline = deadline,
        category = category,
        priority = Priority.valueOf(priority),
        reminderType = ReminderType.valueOf(reminderType),
        completed = completed,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        deadline = deadline,
        category = category,
        priority = priority.name,
        reminderType = reminderType.name,
        completed = completed,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
