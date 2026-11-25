package com.duesoon.app.domain.backup

import com.duesoon.app.domain.model.FocusSession
import com.duesoon.app.domain.model.PomodoroSettings
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

@Serializable
data class FokusKuliahBackupDto(
    val app: String? = null,
    val schemaVersion: Int? = null,
    val exportedAt: String? = null,
    val data: FokusKuliahDataDto? = null
)

@Serializable
data class FokusKuliahDataDto(
    val tasks: List<FokusKuliahTaskDto> = emptyList(),
    val history: Map<String, FokusKuliahHistoryEntryDto> = emptyMap(),
    val courseHistory: Map<String, Map<String, FokusKuliahHistoryEntryDto>> = emptyMap(),
    val settings: FokusKuliahSettingsDto? = null
)

@Serializable
data class FokusKuliahTaskDto(
    val id: String? = null,
    val name: String? = null,
    val course: String? = null,
    val date: String? = null,
    val time: String? = null,
    val done: Boolean = false,
    val focusSessions: Int = 0,
    val focusMinutes: Int = 0
)

@Serializable
data class FokusKuliahHistoryEntryDto(
    val sessions: Int = 0,
    val minutes: Int = 0
)

@Serializable
data class FokusKuliahSettingsDto(
    val focus: Int? = null,
    @SerialName("break")
    val shortBreak: Int? = null,
    val longBreak: Int? = null,
    val sound: Boolean? = null,
    val autoStart: Boolean? = null
)

data class ParsedFokusKuliahBackup(
    val tasks: List<Task>,
    val focusSessions: List<FocusSession>,
    val pomodoroSettings: PomodoroSettings?
)

sealed class FokusKuliahParseResult {
    data class Success(val data: ParsedFokusKuliahBackup) : FokusKuliahParseResult()
    data class Error(val message: String) : FokusKuliahParseResult()
}

object FokusKuliahBackupParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(
        rawJson: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis()
    ): FokusKuliahParseResult {
        val dto = try {
            json.decodeFromString<FokusKuliahBackupDto>(rawJson)
        } catch (e: Exception) {
            return FokusKuliahParseResult.Error("Format JSON Fokus Kuliah tidak valid.")
        }

        if (dto.app != "fokus-kuliah" || dto.data == null) {
            return FokusKuliahParseResult.Error("Berkas ini bukan cadangan Fokus Kuliah yang valid.")
        }

        if (dto.schemaVersion != null && dto.schemaVersion != 1) {
            return FokusKuliahParseResult.Error("Versi skema Fokus Kuliah (${dto.schemaVersion}) tidak didukung.")
        }

        val data = dto.data

        val tasks = data.tasks.mapNotNull { item ->
            mapTask(item, zoneId, nowMillis)
        }

        val focusSessions = mapFocusHistory(
            history = data.history,
            courseHistory = data.courseHistory,
            zoneId = zoneId
        )

        val settings = data.settings?.let { s ->
            PomodoroSettings(
                focusMinutes = PomodoroSettings.sanitizeFocusMinutes(
                    s.focus ?: PomodoroSettings.DEFAULT_FOCUS_MINUTES
                ),
                shortBreakMinutes = PomodoroSettings.sanitizeBreakMinutes(
                    s.shortBreak ?: PomodoroSettings.DEFAULT_SHORT_BREAK_MINUTES
                ),
                longBreakMinutes = PomodoroSettings.sanitizeBreakMinutes(
                    s.longBreak ?: PomodoroSettings.DEFAULT_LONG_BREAK_MINUTES
                ),
                autoStartNext = s.autoStart ?: false,
                soundEnabled = s.sound ?: true
            )
        }

        if (tasks.isEmpty() && focusSessions.isEmpty()) {
            return FokusKuliahParseResult.Error("Berkas cadangan Fokus Kuliah tidak memiliki tugas maupun riwayat fokus.")
        }

        return FokusKuliahParseResult.Success(
            ParsedFokusKuliahBackup(
                tasks = tasks,
                focusSessions = focusSessions,
                pomodoroSettings = settings
            )
        )
    }

    private fun mapTask(
        item: FokusKuliahTaskDto,
        zoneId: ZoneId,
        nowMillis: Long
    ): Task? {
        val title = item.name?.trim().orEmpty()
        if (title.isEmpty()) return null

        val course = item.course?.trim()?.takeIf { it.isNotEmpty() }
        val deadlineMillis = parseDeadlineMillis(item.date, item.time, zoneId)
        val note = if (item.focusSessions > 0 || item.focusMinutes > 0) {
            "Diimpor dari Fokus Kuliah (${item.focusSessions} sesi · ${item.focusMinutes} menit)"
        } else {
            null
        }

        return Task(
            id = 0L,
            title = title,
            description = note,
            deadline = deadlineMillis,
            category = course,
            priority = Priority.NORMAL,
            reminderType = if (deadlineMillis != null) ReminderType.SMART else ReminderType.NONE,
            isRecurring = false,
            recurrenceRule = null,
            completed = item.done,
            snoozedUntil = null,
            createdAt = nowMillis,
            updatedAt = nowMillis
        )
    }

    private fun parseDeadlineMillis(
        dateStr: String?,
        timeStr: String?,
        zoneId: ZoneId
    ): Long? {
        val trimmedDate = dateStr?.trim().orEmpty()
        if (trimmedDate.isEmpty()) return null
        val localDate = try {
            LocalDate.parse(trimmedDate)
        } catch (e: DateTimeParseException) {
            return null
        }

        val localTime = parseLocalTimeOrNull(timeStr) ?: LocalTime.of(23, 59)
        return localDate.atTime(localTime).atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun parseLocalTimeOrNull(timeStr: String?): LocalTime? {
        val trimmed = timeStr?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return try {
            LocalTime.parse(trimmed)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private fun mapFocusHistory(
        history: Map<String, FokusKuliahHistoryEntryDto>,
        courseHistory: Map<String, Map<String, FokusKuliahHistoryEntryDto>>,
        zoneId: ZoneId
    ): List<FocusSession> {
        val result = mutableListOf<FocusSession>()
        val coveredByDate = mutableMapOf<String, Pair<Int, Int>>() // dateKey -> (sessions, minutes)

        // 1. Expand per-course history first so course distribution stays intact
        for ((dateKey, coursesMap) in courseHistory) {
            val parsedDate = parseLocalDateOrNull(dateKey) ?: continue
            val baseMillis = parsedDate.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
            var dateSessions = 0
            var dateMinutes = 0

            for ((rawCourseName, entry) in coursesMap) {
                val courseName = rawCourseName.trim()
                if (courseName.isEmpty()) continue
                val count = entry.sessions.coerceAtLeast(0)
                val minutes = entry.minutes.coerceAtLeast(0)
                if (count == 0 || minutes == 0) continue

                val expanded = expandAggregateIntoSessions(
                    dateKey = dateKey,
                    baseCompletedAt = baseMillis,
                    sessionCount = count,
                    totalMinutes = minutes,
                    courseOrCategory = courseName
                )
                result.addAll(expanded)
                dateSessions += count
                dateMinutes += minutes
            }

            if (dateSessions > 0 || dateMinutes > 0) {
                coveredByDate[dateKey] = dateSessions to dateMinutes
            }
        }

        // 2. Add any unlinked/remaining daily history not covered by courseHistory
        for ((dateKey, entry) in history) {
            val parsedDate = parseLocalDateOrNull(dateKey) ?: continue
            val totalSessions = entry.sessions.coerceAtLeast(0)
            val totalMinutes = entry.minutes.coerceAtLeast(0)
            val covered = coveredByDate[dateKey] ?: (0 to 0)

            val remainingSessions = (totalSessions - covered.first).coerceAtLeast(0)
            val remainingMinutes = (totalMinutes - covered.second).coerceAtLeast(0)
            if (remainingSessions > 0 && remainingMinutes > 0) {
                val baseMillis = parsedDate.atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli()
                result.addAll(
                    expandAggregateIntoSessions(
                        dateKey = dateKey,
                        baseCompletedAt = baseMillis,
                        sessionCount = remainingSessions,
                        totalMinutes = remainingMinutes,
                        courseOrCategory = null
                    )
                )
            }
        }

        return result
    }

    private fun expandAggregateIntoSessions(
        dateKey: String,
        baseCompletedAt: Long,
        sessionCount: Int,
        totalMinutes: Int,
        courseOrCategory: String?
    ): List<FocusSession> {
        val safeCount = sessionCount.coerceAtLeast(1)
        val baseMinutes = (totalMinutes / safeCount).coerceAtLeast(1)
        var remainder = (totalMinutes - baseMinutes * safeCount).coerceAtLeast(0)

        return List(safeCount) { idx ->
            val extra = if (remainder > 0) {
                remainder -= 1
                1
            } else {
                0
            }
            FocusSession(
                id = 0L,
                taskId = null,
                taskTitle = null,
                courseOrCategory = courseOrCategory,
                durationMinutes = baseMinutes + extra,
                dateKey = dateKey,
                completedAt = baseCompletedAt + (idx * 60_000L)
            )
        }
    }

    private fun parseLocalDateOrNull(dateKey: String): LocalDate? {
        return try {
            LocalDate.parse(dateKey)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
