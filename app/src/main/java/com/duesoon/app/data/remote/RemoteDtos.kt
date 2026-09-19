package com.duesoon.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class TaskPayload(
    val title: String,
    val description: String?,
    val deadline: Long?,
    val category: String?,
    val priority: String,
    val reminderType: String,
    val isRecurring: Boolean,
    val recurrenceInterval: String?,
    val completed: Boolean,
    val snoozedUntil: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SyncPayloadDto(
    val uuid: String,
    val ciphertext: String, // Base64 encoded AES-GCM ciphertext (IV prepended)
    val revision: Long,
    val updatedAtUtc: Long,
    val isDeleted: Boolean
)

@Serializable
data class SyncPushRequest(
    val payloads: List<SyncPayloadDto>
)

@Serializable
data class SyncPullResponse(
    val payloads: List<SyncPayloadDto>
)

@Serializable
data class SaltResponse(
    val salt: String // Base64 encoded salt
)

@Serializable
data class SyncPushResponse(
    val success: Boolean,
    val serverRevision: Long
)
