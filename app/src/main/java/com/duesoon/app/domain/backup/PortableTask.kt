package com.duesoon.app.domain.backup

import kotlinx.serialization.Serializable

@Serializable
data class PortableTask(
    val id: Long,
    val title: String,
    val description: String? = null,
    val deadline: Long? = null,
    val category: String? = null,
    val priority: String,
    val reminderType: String,
    val isRecurring: Boolean,
    val recurrenceInterval: String? = null,
    val completed: Boolean,
    val snoozedUntil: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)
