package com.duesoon.app.domain.focus

import com.duesoon.app.domain.model.FocusSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FocusStatsCalculatorTest {

    @Test
    fun `empty sessions returns zeroed summary and 7 days chart`() {
        val today = LocalDate.of(2026, 4, 15) // Wednesday
        val summary = FocusStatsCalculator.calculate(emptyList(), today)

        assertEquals(0, summary.todaySessions)
        assertEquals(0, summary.todayMinutes)
        assertEquals(0, summary.weekSessions)
        assertEquals(0, summary.weekMinutes)
        assertEquals(0, summary.streakDays)
        assertEquals(0, summary.averageDailyMinutes)
        assertEquals(7, summary.last7Days.size)
        assertTrue(summary.last7Days.last().isToday)
        assertEquals("Rab", summary.last7Days.last().dayLabel)
        assertFalse(summary.hasAnyHistory)
    }

    @Test
    fun `calculates weekly totals, streak, daily average, course and task stats accurately`() {
        val today = LocalDate.of(2026, 4, 15) // Wednesday
        val sessions = listOf(
            // Today (Wed)
            FocusSession(id = 1, taskId = 10L, courseOrCategory = "Basis Data", durationMinutes = 25, dateKey = "2026-04-15", completedAt = 1L),
            FocusSession(id = 2, taskId = 10L, courseOrCategory = "Basis Data", durationMinutes = 25, dateKey = "2026-04-15", completedAt = 2L),
            // Yesterday (Tue)
            FocusSession(id = 3, taskId = 11L, courseOrCategory = "Jaringan", durationMinutes = 30, dateKey = "2026-04-14", completedAt = 3L),
            // Monday (same week)
            FocusSession(id = 4, taskId = null, courseOrCategory = "Basis Data", durationMinutes = 20, dateKey = "2026-04-13", completedAt = 4L),
            // Previous Sunday (different week Mon-Sun, still within last 7 days and streak!)
            FocusSession(id = 5, taskId = null, courseOrCategory = "Kalkulus", durationMinutes = 40, dateKey = "2026-04-12", completedAt = 5L)
        )

        val summary = FocusStatsCalculator.calculate(sessions, today)

        assertTrue(summary.hasAnyHistory)
        assertEquals(2, summary.todaySessions)
        assertEquals(50, summary.todayMinutes)
        // Week Mon..Sun = Mon(20) + Tue(30) + Wed(50) = 100 min, 4 sessions
        assertEquals(4, summary.weekSessions)
        assertEquals(100, summary.weekMinutes)
        // Streak = Apr 15, 14, 13, 12 = 4 days
        assertEquals(4, summary.streakDays)
        // Sum of last 7 days = 140 -> round(140 / 7.0) = 20
        assertEquals(20, summary.averageDailyMinutes)

        // Course stats sorted by minutes desc: Basis Data (70), Kalkulus (40), Jaringan (30)
        assertEquals(3, summary.courseStats.size)
        assertEquals("Basis Data", summary.courseStats[0].name)
        assertEquals(70, summary.courseStats[0].minutes)
        assertEquals(3, summary.courseStats[0].sessions)
        assertEquals("Kalkulus", summary.courseStats[1].name)
        assertEquals("Jaringan", summary.courseStats[2].name)

        // Task totals
        assertEquals(2, summary.taskTotals[10L]?.sessions)
        assertEquals(50, summary.taskTotals[10L]?.minutes)
        assertEquals(1, summary.taskTotals[11L]?.sessions)
        assertEquals(30, summary.taskTotals[11L]?.minutes)
    }

    @Test
    fun `streak continues from yesterday if today has no sessions yet`() {
        val today = LocalDate.of(2026, 4, 15)
        val sessions = listOf(
            FocusSession(durationMinutes = 25, dateKey = "2026-04-14", completedAt = 1L),
            FocusSession(durationMinutes = 25, dateKey = "2026-04-13", completedAt = 2L)
        )
        val summary = FocusStatsCalculator.calculate(sessions, today)
        assertEquals(0, summary.todaySessions)
        assertEquals(2, summary.streakDays)
    }
}
