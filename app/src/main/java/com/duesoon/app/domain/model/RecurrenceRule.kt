package com.duesoon.app.domain.model

import java.time.DayOfWeek
import java.time.temporal.ChronoUnit

sealed interface RecurrenceRule {
    data object Daily : RecurrenceRule
    data object Weekly : RecurrenceRule
    data object Monthly : RecurrenceRule
    data class CustomInterval(val count: Int, val unit: ChronoUnit) : RecurrenceRule
    data class SpecificWeekdays(val days: Set<DayOfWeek>) : RecurrenceRule
}
