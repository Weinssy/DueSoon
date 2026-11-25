package com.duesoon.app.domain.focus

import com.duesoon.app.domain.model.FocusSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

data class DailyFocusBar(
    val dateKey: String,
    val dayLabel: String,
    val sessions: Int,
    val minutes: Int,
    val isToday: Boolean
)

data class CourseFocusStat(
    val name: String,
    val sessions: Int,
    val minutes: Int,
    val shareFraction: Float
)

data class TaskFocusSummary(
    val sessions: Int,
    val minutes: Int
)

data class FocusStatsSummary(
    val todaySessions: Int = 0,
    val todayMinutes: Int = 0,
    val weekSessions: Int = 0,
    val weekMinutes: Int = 0,
    val streakDays: Int = 0,
    val averageDailyMinutes: Int = 0,
    val last7Days: List<DailyFocusBar> = emptyList(),
    val courseStats: List<CourseFocusStat> = emptyList(),
    val taskTotals: Map<Long, TaskFocusSummary> = emptyMap(),
    val hasAnyHistory: Boolean = false
)

object FocusStatsCalculator {

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun dateKeyOf(date: LocalDate): String = date.format(DATE_FORMATTER)

    fun dayLabelId(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "Sen"
        DayOfWeek.TUESDAY -> "Sel"
        DayOfWeek.WEDNESDAY -> "Rab"
        DayOfWeek.THURSDAY -> "Kam"
        DayOfWeek.FRIDAY -> "Jum"
        DayOfWeek.SATURDAY -> "Sab"
        DayOfWeek.SUNDAY -> "Min"
    }

    fun calculate(
        sessions: List<FocusSession>,
        today: LocalDate = LocalDate.now()
    ): FocusStatsSummary {
        val dailyTotals = mutableMapOf<String, Pair<Int, Int>>() // dateKey -> (sessions, minutes)
        val courseBuckets = mutableMapOf<String, Pair<Int, Int>>()
        val taskBuckets = mutableMapOf<Long, Pair<Int, Int>>()

        for (session in sessions) {
            if (session.durationMinutes <= 0) continue
            val currentDay = dailyTotals[session.dateKey] ?: (0 to 0)
            dailyTotals[session.dateKey] = (currentDay.first + 1) to (currentDay.second + session.durationMinutes)

            val courseName = session.courseOrCategory?.trim().orEmpty()
            if (courseName.isNotEmpty()) {
                val currentCourse = courseBuckets[courseName] ?: (0 to 0)
                courseBuckets[courseName] = (currentCourse.first + 1) to (currentCourse.second + session.durationMinutes)
            }

            val tid = session.taskId
            if (tid != null) {
                val currentTask = taskBuckets[tid] ?: (0 to 0)
                taskBuckets[tid] = (currentTask.first + 1) to (currentTask.second + session.durationMinutes)
            }
        }

        val todayKey = dateKeyOf(today)
        val todayPair = dailyTotals[todayKey] ?: (0 to 0)

        // Weekly totals: Monday to Sunday of current week
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var weekSessions = 0
        var weekMinutes = 0
        for (i in 0 until 7) {
            val key = dateKeyOf(monday.plusDays(i.toLong()))
            val pair = dailyTotals[key]
            if (pair != null) {
                weekSessions += pair.first
                weekMinutes += pair.second
            }
        }

        // Last 7 days bar chart (oldest -> today)
        val last7Days = (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            val key = dateKeyOf(day)
            val pair = dailyTotals[key] ?: (0 to 0)
            DailyFocusBar(
                dateKey = key,
                dayLabel = dayLabelId(day.dayOfWeek),
                sessions = pair.first,
                minutes = pair.second,
                isToday = offset == 0
            )
        }

        val sum7Minutes = last7Days.sumOf { it.minutes }
        val avgDailyMinutes = (sum7Minutes / 7.0).roundToInt()

        // Streak calculation (same semantics as Fokus Kuliah)
        val streakDays = computeStreak(dailyTotals, today)

        // Course / category distribution (top 6 sorted by minutes desc)
        val sortedCourses = courseBuckets.entries
            .sortedWith(compareByDescending<Map.Entry<String, Pair<Int, Int>>> { it.value.second }.thenBy { it.key })
            .take(6)
        val maxCourseMinutes = sortedCourses.firstOrNull()?.value?.second ?: 0
        val courseStats = sortedCourses.map { (name, pair) ->
            CourseFocusStat(
                name = name,
                sessions = pair.first,
                minutes = pair.second,
                shareFraction = if (maxCourseMinutes > 0) {
                    (pair.second.toFloat() / maxCourseMinutes.toFloat()).coerceIn(0.05f, 1f)
                } else {
                    0f
                }
            )
        }

        val taskTotals = taskBuckets.mapValues { (_, pair) ->
            TaskFocusSummary(sessions = pair.first, minutes = pair.second)
        }

        return FocusStatsSummary(
            todaySessions = todayPair.first,
            todayMinutes = todayPair.second,
            weekSessions = weekSessions,
            weekMinutes = weekMinutes,
            streakDays = streakDays,
            averageDailyMinutes = avgDailyMinutes,
            last7Days = last7Days,
            courseStats = courseStats,
            taskTotals = taskTotals,
            hasAnyHistory = dailyTotals.values.any { it.first > 0 || it.second > 0 }
        )
    }

    private fun computeStreak(
        dailyTotals: Map<String, Pair<Int, Int>>,
        today: LocalDate
    ): Int {
        val todayHas = (dailyTotals[dateKeyOf(today)]?.first ?: 0) > 0
        var offset = if (todayHas) 0L else 1L
        var streak = 0
        while (offset < 3650L) {
            val key = dateKeyOf(today.minusDays(offset))
            if ((dailyTotals[key]?.first ?: 0) > 0) {
                streak += 1
                offset += 1L
            } else {
                break
            }
        }
        return streak
    }
}
