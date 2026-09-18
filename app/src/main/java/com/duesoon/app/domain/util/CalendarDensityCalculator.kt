package com.duesoon.app.domain.util

import com.duesoon.app.domain.model.AttentionTier
import com.duesoon.app.domain.model.Task
import com.duesoon.app.ui.home.calendar.DayDensityDot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object CalendarDensityCalculator {
    fun calculateDayDots(
        tasks: List<Task>,
        zoneId: ZoneId,
        currentTimeMillis: Long
    ): Map<LocalDate, DayDensityDot> {
        return tasks
            .filter { !it.completed && it.deadline != null }
            .groupBy {
                Instant.ofEpochMilli(it.deadline!!).atZone(zoneId).toLocalDate()
            }
            .mapValues { (_, dayTasks) ->
                val highestTier = dayTasks.minOfOrNull {
                    AttentionRankingEngine.calculateTier(it, currentTimeMillis)
                } ?: AttentionTier.OPTIONAL

                when (highestTier) {
                    AttentionTier.OVERDUE, AttentionTier.CRITICAL -> DayDensityDot.CRITICAL
                    AttentionTier.HIGH, AttentionTier.ELEVATED -> DayDensityDot.WARNING
                    AttentionTier.NORMAL, AttentionTier.OPTIONAL, AttentionTier.COMPLETED -> DayDensityDot.MUTED
                }
            }
    }
}
