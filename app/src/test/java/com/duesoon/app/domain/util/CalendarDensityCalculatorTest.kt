package com.duesoon.app.domain.util

import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.Task
import com.duesoon.app.ui.home.calendar.DayDensityDot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarDensityCalculatorTest {

    private val utcZone = ZoneId.of("UTC")
    private val jstZone = ZoneId.of("Asia/Tokyo") // UTC+9
    
    // A fixed current time for tests (e.g. 2026-09-18T10:00:00Z)
    private val currentMillis = Instant.parse("2026-09-18T10:00:00Z").toEpochMilli()

    private fun createBaseTask(
        id: Long = 1,
        deadline: Long? = null,
        completed: Boolean = false,
        priority: Priority = Priority.NORMAL
    ): Task {
        return Task(
            id = id,
            title = "Test Task $id",
            deadline = deadline,
            completed = completed,
            priority = priority
        )
    }

    @Test
    fun `completed tasks do not generate a dot`() {
        val deadlineMillis = currentMillis + 86400000 // Tomorrow
        val task = createBaseTask(deadline = deadlineMillis, completed = true)

        val result = CalendarDensityCalculator.calculateDayDots(
            tasks = listOf(task),
            zoneId = utcZone,
            currentTimeMillis = currentMillis
        )

        assertEquals(0, result.size)
    }

    @Test
    fun `null deadline tasks do not generate a dot`() {
        val task = createBaseTask(deadline = null)

        val result = CalendarDensityCalculator.calculateDayDots(
            tasks = listOf(task),
            zoneId = utcZone,
            currentTimeMillis = currentMillis
        )

        assertEquals(0, result.size)
    }

    @Test
    fun `multiple tasks on same date resolve to highest severity dot`() {
        // Today in UTC
        val todayLocalDate = Instant.ofEpochMilli(currentMillis).atZone(utcZone).toLocalDate()

        // Task 1: Due in 5 days (NORMAL) -> UPCOMING -> MUTED
        val normalDeadline = currentMillis + (86400000 * 5)
        val task1 = createBaseTask(id = 1, deadline = normalDeadline, priority = Priority.NORMAL)
        
        // Task 2: Due today (OVERDUE if earlier than current, or TODAY if later)
        // Let's make it overdue
        val overdueDeadline = currentMillis - 3600000 // 1 hour ago
        val task2 = createBaseTask(id = 2, deadline = overdueDeadline, priority = Priority.HIGH)

        val result = CalendarDensityCalculator.calculateDayDots(
            tasks = listOf(task1, task2),
            zoneId = utcZone,
            currentTimeMillis = currentMillis
        )

        val todayDot = result[todayLocalDate]
        assertEquals(DayDensityDot.CRITICAL, todayDot)

        val normalDate = Instant.ofEpochMilli(normalDeadline).atZone(utcZone).toLocalDate()
        val normalDot = result[normalDate]
        assertEquals(DayDensityDot.MUTED, normalDot)
    }

    @Test
    fun `timezone shift mapping is correct`() {
        // Suppose a deadline is 2026-09-18T22:00:00Z
        val deadlineMillis = Instant.parse("2026-09-18T22:00:00Z").toEpochMilli()
        val task = createBaseTask(deadline = deadlineMillis)

        // In UTC, this is September 18
        val resultUtc = CalendarDensityCalculator.calculateDayDots(
            tasks = listOf(task),
            zoneId = utcZone,
            currentTimeMillis = currentMillis
        )
        assertEquals(DayDensityDot.WARNING, resultUtc[LocalDate.of(2026, 9, 18)]) // Elevated (today) -> Warning

        // In Asia/Tokyo (UTC+9), 22:00 UTC is 07:00 on the NEXT DAY (September 19)
        val resultJst = CalendarDensityCalculator.calculateDayDots(
            tasks = listOf(task),
            zoneId = jstZone,
            currentTimeMillis = currentMillis
        )
        assertEquals(DayDensityDot.WARNING, resultJst[LocalDate.of(2026, 9, 19)]) 
        assertNull(resultJst[LocalDate.of(2026, 9, 18)])
    }
}
