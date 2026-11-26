package com.duesoon.app.domain.backup

import com.duesoon.app.domain.model.FocusSession
import com.duesoon.app.domain.model.Task

/**
 * Represents a backup that has passed all structural and business validations.
 * Tasks and focus sessions inside are normalized and mapped to domain models, ready for execution layer.
 */
data class ValidatedPortableBackup(
    val exportedAt: Long,
    val tasks: List<Task>,
    val focusSessions: List<FocusSession> = emptyList()
)
