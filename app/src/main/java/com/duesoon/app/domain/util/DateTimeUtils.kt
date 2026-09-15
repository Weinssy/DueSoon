package com.duesoon.app.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    val INDONESIAN_LOCALE: Locale = Locale("id", "ID")

    private val DEADLINE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", INDONESIAN_LOCALE)

    /**
     * Converts a local date and local time in a given [zoneId] into epoch milliseconds.
     */
    fun toEpochMillis(
        date: LocalDate,
        time: LocalTime,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return LocalDateTime.of(date, time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Converts epoch milliseconds to a [LocalDateTime] in the given [zoneId].
     */
    fun toLocalDateTime(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalDateTime {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalDateTime()
    }

    /**
     * Converts epoch milliseconds to [LocalDate] in the given [zoneId].
     */
    fun toLocalDate(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalDate {
        return toLocalDateTime(epochMillis, zoneId).toLocalDate()
    }

    /**
     * Converts epoch milliseconds to [LocalTime] in the given [zoneId].
     */
    fun toLocalTime(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalTime {
        return toLocalDateTime(epochMillis, zoneId).toLocalTime()
    }

    /**
     * Material 3 DatePicker returns epoch millis at UTC midnight for the selected calendar day.
     * This safely extracts the [LocalDate] represented by that UTC date without local time shifting.
     */
    fun fromDatePickerUtcMillis(utcDateMillis: Long): LocalDate {
        return Instant.ofEpochMilli(utcDateMillis)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
    }

    /**
     * Formats an epoch millisecond deadline into "dd MMM yyyy, HH:mm" (e.g., "15 Sep 2026, 14:30")
     * using Indonesian locale and 24-hour format.
     * Returns null if [epochMillis] is null.
     */
    fun formatDeadline(
        epochMillis: Long?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String? {
        if (epochMillis == null) return null
        val localDateTime = toLocalDateTime(epochMillis, zoneId)
        return localDateTime.format(DEADLINE_FORMATTER)
    }
}
