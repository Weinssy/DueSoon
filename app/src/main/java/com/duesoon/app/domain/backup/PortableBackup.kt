package com.duesoon.app.domain.backup

import kotlinx.serialization.Serializable

@Serializable
data class PortableBackup(
    val schemaVersion: Int = 1,
    val appVersion: String,
    val exportedAt: Long,
    val tasks: List<PortableTask>
)
