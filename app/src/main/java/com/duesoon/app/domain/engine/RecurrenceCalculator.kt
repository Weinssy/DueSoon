package com.duesoon.app.domain.engine

import com.duesoon.app.domain.model.RecurrenceRule
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object RecurrenceCalculator {

    private const val MAX_ADVANCE_ITERATIONS = 365

    /**
     * Calculates the next deadline based on the recurrence rule.
     * Preserves local time (hour and minute) across daylight savings and timezone transitions.
     * Handles severely overdue tasks by looping until the next deadline is > currentTimeMillis.
     */
    fun calculateNextDeadline(
        currentDeadline: Long,
        rule: RecurrenceRule,
        currentTimeMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        var nextZdt = Instant.ofEpochMilli(currentDeadline).atZone(zoneId)
        var iterations = 0

        while (iterations < MAX_ADVANCE_ITERATIONS) {
            nextZdt = advanceOnce(nextZdt, rule)
            
            if (nextZdt.toInstant().toEpochMilli() > currentTimeMillis) {
                break
            }
            iterations++
        }

        return nextZdt.toInstant().toEpochMilli()
    }

    private fun advanceOnce(zdt: ZonedDateTime, rule: RecurrenceRule): ZonedDateTime {
        return when (rule) {
            is RecurrenceRule.Daily -> zdt.plusDays(1)
            is RecurrenceRule.Weekly -> zdt.plusWeeks(1)
            is RecurrenceRule.Monthly -> zdt.plusMonths(1)
            is RecurrenceRule.CustomInterval -> {
                when (rule.unit) {
                    ChronoUnit.DAYS -> zdt.plusDays(rule.count.toLong())
                    ChronoUnit.WEEKS -> zdt.plusWeeks(rule.count.toLong())
                    ChronoUnit.MONTHS -> zdt.plusMonths(rule.count.toLong())
                    else -> zdt.plusDays(1) // fallback
                }
            }
            is RecurrenceRule.SpecificWeekdays -> {
                var current = zdt.plusDays(1)
                // Search for the next valid day within 7 days
                for (i in 0 until 7) {
                    if (rule.days.contains(current.dayOfWeek)) {
                        return current
                    }
                    current = current.plusDays(1)
                }
                // Fallback if set is empty or weird
                zdt.plusDays(1)
            }
        }
    }
}
