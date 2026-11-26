package com.duesoon.app.domain.backup

import kotlinx.serialization.Serializable

@Serializable
data class PortableFocusSession(
    val id: Long = 0L,
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val courseOrCategory: String? = null,
    val durationMinutes: Int,
    val dateKey: String,
    val completedAt: Long
)
