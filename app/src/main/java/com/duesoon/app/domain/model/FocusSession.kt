package com.duesoon.app.domain.model

enum class PomodoroMode {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK
}

data class PomodoroSettings(
    val focusMinutes: Int = DEFAULT_FOCUS_MINUTES,
    val shortBreakMinutes: Int = DEFAULT_SHORT_BREAK_MINUTES,
    val longBreakMinutes: Int = DEFAULT_LONG_BREAK_MINUTES,
    val autoStartNext: Boolean = false,
    val soundEnabled: Boolean = true
) {
    fun durationMinutesFor(mode: PomodoroMode): Int = when (mode) {
        PomodoroMode.FOCUS -> focusMinutes
        PomodoroMode.SHORT_BREAK -> shortBreakMinutes
        PomodoroMode.LONG_BREAK -> longBreakMinutes
    }

    companion object {
        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val MIN_MINUTES = 1
        const val MAX_FOCUS_MINUTES = 180
        const val MAX_BREAK_MINUTES = 90
        const val CYCLE_TARGET = 4

        fun sanitizeFocusMinutes(value: Int): Int = value.coerceIn(MIN_MINUTES, MAX_FOCUS_MINUTES)
        fun sanitizeBreakMinutes(value: Int): Int = value.coerceIn(MIN_MINUTES, MAX_BREAK_MINUTES)
    }
}

data class FocusSession(
    val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val courseOrCategory: String? = null,
    val durationMinutes: Int,
    val dateKey: String,
    val completedAt: Long = System.currentTimeMillis()
)
