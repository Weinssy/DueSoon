package com.duesoon.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.duesoon.app.domain.model.FocusSession

@Entity(
    tableName = "focus_sessions",
    indices = [
        Index(value = ["dateKey"]),
        Index(value = ["taskId"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val courseOrCategory: String? = null,
    val durationMinutes: Int,
    val dateKey: String,
    val completedAt: Long
)

fun FocusSessionEntity.toDomainModel(): FocusSession {
    return FocusSession(
        id = id,
        taskId = taskId,
        taskTitle = taskTitle,
        courseOrCategory = courseOrCategory,
        durationMinutes = durationMinutes,
        dateKey = dateKey,
        completedAt = completedAt
    )
}

fun FocusSession.toEntity(): FocusSessionEntity {
    return FocusSessionEntity(
        id = id,
        taskId = taskId,
        taskTitle = taskTitle,
        courseOrCategory = courseOrCategory,
        durationMinutes = durationMinutes,
        dateKey = dateKey,
        completedAt = completedAt
    )
}
