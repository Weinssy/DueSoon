package com.duesoon.app.domain.backup

import com.duesoon.app.domain.model.FocusSession

object PortableFocusSessionMapper {

    fun toPortable(session: FocusSession): PortableFocusSession {
        return PortableFocusSession(
            id = session.id,
            taskId = session.taskId,
            taskTitle = session.taskTitle,
            courseOrCategory = session.courseOrCategory,
            durationMinutes = session.durationMinutes,
            dateKey = session.dateKey,
            completedAt = session.completedAt
        )
    }

    fun toDomain(portable: PortableFocusSession, mode: ValidationMode): FocusSession {
        return FocusSession(
            id = if (mode == ValidationMode.IMPORT) 0L else portable.id,
            taskId = if (mode == ValidationMode.IMPORT) null else portable.taskId?.takeIf { it > 0L },
            taskTitle = portable.taskTitle?.trim()?.takeIf { it.isNotEmpty() },
            courseOrCategory = portable.courseOrCategory?.trim()?.takeIf { it.isNotEmpty() },
            durationMinutes = portable.durationMinutes,
            dateKey = portable.dateKey.trim(),
            completedAt = portable.completedAt
        )
    }
}
