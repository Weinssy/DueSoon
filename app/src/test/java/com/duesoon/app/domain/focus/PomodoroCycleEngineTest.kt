package com.duesoon.app.domain.focus

import com.duesoon.app.domain.model.PomodoroMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PomodoroCycleEngineTest {

    @Test
    fun `completing focus 1 through 3 transitions to short break and increments cycle`() {
        val t1 = PomodoroCycleEngine.advanceAfterCompletion(PomodoroMode.FOCUS, 0)
        assertEquals(PomodoroMode.SHORT_BREAK, t1.nextMode)
        assertEquals(1, t1.nextCycleCount)
        assertTrue(t1.shouldRecordFocusSession)

        val t3 = PomodoroCycleEngine.advanceAfterCompletion(PomodoroMode.FOCUS, 2)
        assertEquals(PomodoroMode.SHORT_BREAK, t3.nextMode)
        assertEquals(3, t3.nextCycleCount)
        assertTrue(t3.shouldRecordFocusSession)
    }

    @Test
    fun `completing 4th focus session triggers long break and resets cycle`() {
        val t4 = PomodoroCycleEngine.advanceAfterCompletion(PomodoroMode.FOCUS, 3)
        assertEquals(PomodoroMode.LONG_BREAK, t4.nextMode)
        assertEquals(0, t4.nextCycleCount)
        assertTrue(t4.shouldRecordFocusSession)
    }

    @Test
    fun `completing break returns to focus without recording session`() {
        val shortDone = PomodoroCycleEngine.advanceAfterCompletion(PomodoroMode.SHORT_BREAK, 2)
        assertEquals(PomodoroMode.FOCUS, shortDone.nextMode)
        assertEquals(2, shortDone.nextCycleCount)
        assertFalse(shortDone.shouldRecordFocusSession)

        val longDone = PomodoroCycleEngine.advanceAfterCompletion(PomodoroMode.LONG_BREAK, 0)
        assertEquals(PomodoroMode.FOCUS, longDone.nextMode)
        assertEquals(0, longDone.nextCycleCount)
        assertFalse(longDone.shouldRecordFocusSession)
    }

    @Test
    fun `skipping focus does not record session or increment cycle`() {
        val skipped = PomodoroCycleEngine.advanceOnSkip(PomodoroMode.FOCUS, 1)
        assertEquals(PomodoroMode.SHORT_BREAK, skipped.nextMode)
        assertEquals(1, skipped.nextCycleCount)
        assertFalse(skipped.shouldRecordFocusSession)
    }
}
