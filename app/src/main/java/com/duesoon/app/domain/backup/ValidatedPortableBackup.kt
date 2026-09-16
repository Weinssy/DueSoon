package com.duesoon.app.domain.backup

import com.duesoon.app.domain.model.Task

/**
 * Represents a backup that has passed all structural and business validations.
 * Tasks inside are normalized and mapped to Domain Tasks, ready for execution layer.
 */
data class ValidatedPortableBackup(
    val exportedAt: Long,
    val tasks: List<Task>
)
