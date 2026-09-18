package com.duesoon.app.domain.util

import com.duesoon.app.domain.model.RecurrenceRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit

class RecurrenceRuleParserTest {

    @Test
    fun serialize_legacyValues() {
        assertEquals("DAILY", RecurrenceRuleParser.serialize(RecurrenceRule.Daily))
        assertEquals("WEEKLY", RecurrenceRuleParser.serialize(RecurrenceRule.Weekly))
        assertEquals("MONTHLY", RecurrenceRuleParser.serialize(RecurrenceRule.Monthly))
    }

    @Test
    fun serialize_customIntervals() {
        assertEquals(
            "INTERVAL:DAYS:3",
            RecurrenceRuleParser.serialize(RecurrenceRule.CustomInterval(3, ChronoUnit.DAYS))
        )
        assertEquals(
            "INTERVAL:WEEKS:2",
            RecurrenceRuleParser.serialize(RecurrenceRule.CustomInterval(2, ChronoUnit.WEEKS))
        )
        assertEquals(
            "INTERVAL:MONTHS:6",
            RecurrenceRuleParser.serialize(RecurrenceRule.CustomInterval(6, ChronoUnit.MONTHS))
        )
    }

    @Test
    fun serialize_specificWeekdays() {
        assertEquals(
            "WEEKLY_DAYS:MO,WE,FR",
            RecurrenceRuleParser.serialize(
                RecurrenceRule.SpecificWeekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
            )
        )
    }

    @Test
    fun deserialize_legacyValues() {
        assertEquals(RecurrenceRule.Daily, RecurrenceRuleParser.deserialize("DAILY"))
        assertEquals(RecurrenceRule.Weekly, RecurrenceRuleParser.deserialize("WEEKLY"))
        assertEquals(RecurrenceRule.Monthly, RecurrenceRuleParser.deserialize("MONTHLY"))
    }

    @Test
    fun deserialize_customIntervals() {
        assertEquals(
            RecurrenceRule.CustomInterval(5, ChronoUnit.DAYS),
            RecurrenceRuleParser.deserialize("INTERVAL:DAYS:5")
        )
        assertEquals(
            RecurrenceRule.CustomInterval(1, ChronoUnit.WEEKS),
            RecurrenceRuleParser.deserialize("INTERVAL:WEEKS:1")
        )
        assertEquals(
            RecurrenceRule.CustomInterval(12, ChronoUnit.MONTHS),
            RecurrenceRuleParser.deserialize("INTERVAL:MONTHS:12")
        )
    }

    @Test
    fun deserialize_specificWeekdays() {
        val rule = RecurrenceRuleParser.deserialize("WEEKLY_DAYS:MO,WE,FR") as RecurrenceRule.SpecificWeekdays
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), rule.days)
    }

    @Test
    fun deserialize_invalidStrings_returnsNull() {
        assertNull(RecurrenceRuleParser.deserialize(null))
        assertNull(RecurrenceRuleParser.deserialize(""))
        assertNull(RecurrenceRuleParser.deserialize("INVALID_STRING"))
        assertNull(RecurrenceRuleParser.deserialize("INTERVAL:SECONDS:5")) // unsupported unit
        assertNull(RecurrenceRuleParser.deserialize("INTERVAL:DAYS:NOT_A_NUMBER"))
        assertNull(RecurrenceRuleParser.deserialize("WEEKLY_DAYS:INVALID_DAY"))
    }
}
