package com.duesoon.app.domain.util

import com.duesoon.app.domain.model.RecurrenceRule
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit

object RecurrenceRuleParser {

    fun serialize(rule: RecurrenceRule): String {
        return when (rule) {
            is RecurrenceRule.Daily -> "DAILY"
            is RecurrenceRule.Weekly -> "WEEKLY"
            is RecurrenceRule.Monthly -> "MONTHLY"
            is RecurrenceRule.CustomInterval -> {
                val unitStr = when (rule.unit) {
                    ChronoUnit.DAYS -> "DAYS"
                    ChronoUnit.WEEKS -> "WEEKS"
                    ChronoUnit.MONTHS -> "MONTHS"
                    else -> "DAYS" // fallback
                }
                "INTERVAL:$unitStr:${rule.count}"
            }
            is RecurrenceRule.SpecificWeekdays -> {
                val daysStr = rule.days.joinToString(",") {
                    it.name.take(2) // e.g., MONDAY -> MO
                }
                "WEEKLY_DAYS:$daysStr"
            }
        }
    }

    fun deserialize(raw: String?): RecurrenceRule? {
        if (raw.isNullOrBlank()) return null
        
        return try {
            when (raw) {
                "DAILY" -> RecurrenceRule.Daily
                "WEEKLY" -> RecurrenceRule.Weekly
                "MONTHLY" -> RecurrenceRule.Monthly
                else -> {
                    if (raw.startsWith("INTERVAL:")) {
                        val parts = raw.split(":")
                        if (parts.size == 3) {
                            val unitStr = parts[1]
                            val count = parts[2].toIntOrNull() ?: return null
                            val unit = when (unitStr) {
                                "DAYS" -> ChronoUnit.DAYS
                                "WEEKS" -> ChronoUnit.WEEKS
                                "MONTHS" -> ChronoUnit.MONTHS
                                else -> return null
                            }
                            RecurrenceRule.CustomInterval(count, unit)
                        } else null
                    } else if (raw.startsWith("WEEKLY_DAYS:")) {
                        val parts = raw.split(":")
                        if (parts.size == 2) {
                            val daysStr = parts[1].split(",")
                            val days = daysStr.mapNotNull { dayCode ->
                                DayOfWeek.values().find { it.name.startsWith(dayCode) }
                            }.toSet()
                            if (days.isNotEmpty()) {
                                RecurrenceRule.SpecificWeekdays(days)
                            } else null
                        } else null
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
