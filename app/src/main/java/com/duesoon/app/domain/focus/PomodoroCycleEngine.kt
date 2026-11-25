package com.duesoon.app.domain.focus

import com.duesoon.app.domain.model.PomodoroMode
import com.duesoon.app.domain.model.PomodoroSettings

data class PomodoroTransition(
    val nextMode: PomodoroMode,
    val nextCycleCount: Int,
    val shouldRecordFocusSession: Boolean
)

object PomodoroCycleEngine {

    fun advanceAfterCompletion(
        currentMode: PomodoroMode,
        currentCycleCount: Int,
        cycleTarget: Int = PomodoroSettings.CYCLE_TARGET
    ): PomodoroTransition {
        return when (currentMode) {
            PomodoroMode.FOCUS -> {
                val incremented = currentCycleCount + 1
                if (incremented >= cycleTarget) {
                    PomodoroTransition(
                        nextMode = PomodoroMode.LONG_BREAK,
                        nextCycleCount = 0,
                        shouldRecordFocusSession = true
                    )
                } else {
                    PomodoroTransition(
                        nextMode = PomodoroMode.SHORT_BREAK,
                        nextCycleCount = incremented,
                        shouldRecordFocusSession = true
                    )
                }
            }
            PomodoroMode.SHORT_BREAK,
            PomodoroMode.LONG_BREAK -> {
                PomodoroTransition(
                    nextMode = PomodoroMode.FOCUS,
                    nextCycleCount = currentCycleCount.coerceIn(0, (cycleTarget - 1).coerceAtLeast(0)),
                    shouldRecordFocusSession = false
                )
            }
        }
    }

    fun advanceOnSkip(
        currentMode: PomodoroMode,
        currentCycleCount: Int,
        cycleTarget: Int = PomodoroSettings.CYCLE_TARGET
    ): PomodoroTransition {
        return when (currentMode) {
            PomodoroMode.FOCUS -> {
                val nextMode = if (currentCycleCount + 1 >= cycleTarget) {
                    PomodoroMode.LONG_BREAK
                } else {
                    PomodoroMode.SHORT_BREAK
                }
                PomodoroTransition(
                    nextMode = nextMode,
                    nextCycleCount = currentCycleCount.coerceIn(0, (cycleTarget - 1).coerceAtLeast(0)),
                    shouldRecordFocusSession = false
                )
            }
            PomodoroMode.SHORT_BREAK,
            PomodoroMode.LONG_BREAK -> {
                PomodoroTransition(
                    nextMode = PomodoroMode.FOCUS,
                    nextCycleCount = currentCycleCount.coerceIn(0, (cycleTarget - 1).coerceAtLeast(0)),
                    shouldRecordFocusSession = false
                )
            }
        }
    }
}
