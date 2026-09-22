package com.duesoon.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.RecurrenceRule
import com.duesoon.app.domain.util.RecurrenceRuleParser
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.SyncState
import com.duesoon.app.domain.model.Task

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String?,
    val deadline: Long?,
    val category: String?,
    val priority: String,
    val reminderType: String,
    val isRecurring: Boolean = false,
    val recurrenceInterval: String? = null,
    val completed: Boolean,
    val snoozedUntil: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false,
    val updatedAtUtc: Long = System.currentTimeMillis(),
    val revision: Long = 1L,
    val syncState: String = SyncState.DIRTY.name
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
        isRecurring = isRecurring,
        recurrenceRule = RecurrenceRuleParser.deserialize(recurrenceInterval),
        completed = completed,
        snoozedUntil = snoozedUntil,
        createdAt = createdAt,
        updatedAt = updatedAt,
        uuid = uuid,
        isDeleted = isDeleted,
        updatedAtUtc = updatedAtUtc,
        revision = revision,
        syncState = SyncState.valueOf(syncState)
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
        isRecurring = isRecurring,
        recurrenceInterval = recurrenceRule?.let { RecurrenceRuleParser.serialize(it) },
        completed = completed,
        snoozedUntil = snoozedUntil,
        createdAt = createdAt,
        updatedAt = updatedAt,
        uuid = uuid,
        isDeleted = isDeleted,
        updatedAtUtc = updatedAtUtc,
        revision = revision,
        syncState = syncState.name
    )
}
