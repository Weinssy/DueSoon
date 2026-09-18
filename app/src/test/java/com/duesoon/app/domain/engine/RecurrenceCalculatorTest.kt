package com.duesoon.app.domain.engine

import com.duesoon.app.domain.model.RecurrenceRule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class RecurrenceCalculatorTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun calculateNextDeadline_daily_advancesOneDay() {
        // Monday, 12:00 PM
        val currentDeadline = ZonedDateTime.of(2026, 9, 14, 12, 0, 0, 0, zoneId)
        val currentTime = currentDeadline.minusHours(1).toInstant().toEpochMilli()

        val nextMillis = RecurrenceCalculator.calculateNextDeadline(
            currentDeadline = currentDeadline.toInstant().toEpochMilli(),
            rule = RecurrenceRule.Daily,
            currentTimeMillis = currentTime,
            zoneId = zoneId
        )

        val nextZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextMillis), zoneId)
        
        // Tuesday, 12:00 PM
        assertEquals(15, nextZdt.dayOfMonth)
        assertEquals(12, nextZdt.hour)
    }

    @Test
    fun calculateNextDeadline_overdue_advancesUntilFuture() {
        // Task was due 5 days ago!
        val currentDeadline = ZonedDateTime.of(2026, 9, 10, 10, 0, 0, 0, zoneId)
        // Today is 9/15
        val currentTime = ZonedDateTime.of(2026, 9, 15, 9, 0, 0, 0, zoneId).toInstant().toEpochMilli()

        val nextMillis = RecurrenceCalculator.calculateNextDeadline(
            currentDeadline = currentDeadline.toInstant().toEpochMilli(),
            rule = RecurrenceRule.Daily,
            currentTimeMillis = currentTime,
            zoneId = zoneId
        )

        val nextZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextMillis), zoneId)
        
        // Next valid instance should be 9/15 at 10:00 AM (in the future compared to 9:00 AM)
        assertEquals(15, nextZdt.dayOfMonth)
        assertEquals(10, nextZdt.hour)
    }

    @Test
    fun calculateNextDeadline_specificWeekdays_jumpsToNextValidDay() {
        // Friday, 12:00 PM
        val currentDeadline = ZonedDateTime.of(2026, 9, 18, 12, 0, 0, 0, zoneId)
        val currentTime = currentDeadline.minusHours(1).toInstant().toEpochMilli()

        val rule = RecurrenceRule.SpecificWeekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

        val nextMillis = RecurrenceCalculator.calculateNextDeadline(
            currentDeadline = currentDeadline.toInstant().toEpochMilli(),
            rule = rule,
            currentTimeMillis = currentTime,
            zoneId = zoneId
        )

        val nextZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextMillis), zoneId)
        
        // Should skip Sat, Sun and jump to Monday 9/21
        assertEquals(DayOfWeek.MONDAY, nextZdt.dayOfWeek)
        assertEquals(21, nextZdt.dayOfMonth)
        assertEquals(12, nextZdt.hour)
    }

    @Test
    fun calculateNextDeadline_customInterval_months() {
        // Jan 31st
        val currentDeadline = ZonedDateTime.of(2026, 1, 31, 12, 0, 0, 0, zoneId)
        val currentTime = currentDeadline.minusHours(1).toInstant().toEpochMilli()

        val rule = RecurrenceRule.CustomInterval(1, ChronoUnit.MONTHS)

        val nextMillis = RecurrenceCalculator.calculateNextDeadline(
            currentDeadline = currentDeadline.toInstant().toEpochMilli(),
            rule = rule,
            currentTimeMillis = currentTime,
            zoneId = zoneId
        )

        val nextZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextMillis), zoneId)
        
        // Should roll over to Feb 28th
        assertEquals(2, nextZdt.monthValue)
        assertEquals(28, nextZdt.dayOfMonth)
        assertEquals(12, nextZdt.hour)
    }
}
