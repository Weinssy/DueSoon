package com.duesoon.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant

class DateTimeUtilsTest {

    private val jakartaZone: ZoneId = ZoneId.of("Asia/Jakarta")
    private val newYorkZone: ZoneId = ZoneId.of("America/New_York")
    private val utcZone: ZoneId = ZoneId.of("UTC")

    // ==================================================
    // 1. ROUND-TRIP CORRECTNESS TESTS
    // ==================================================

    @Test
    fun roundTrip_standardAfternoon_preservesLocalDateTime() {
        val date = LocalDate.of(2026, 9, 15)
        val time = LocalTime.of(14, 30)

        val epochMillis = DateTimeUtils.toEpochMillis(date, time, jakartaZone)
        val resultDateTime = DateTimeUtils.toLocalDateTime(epochMillis, jakartaZone)
        val resultDate = DateTimeUtils.toLocalDate(epochMillis, jakartaZone)
        val resultTime = DateTimeUtils.toLocalTime(epochMillis, jakartaZone)

        assertEquals(date, resultDate)
        assertEquals(time, resultTime)
        assertEquals(LocalDateTime.of(date, time), resultDateTime)
    }

    @Test
    fun roundTrip_startOfDayBoundary_preservesLocalDateTime() {
        val date = LocalDate.of(2026, 9, 15)
        val time = LocalTime.of(0, 5)

        val epochMillis = DateTimeUtils.toEpochMillis(date, time, jakartaZone)
        val resultDateTime = DateTimeUtils.toLocalDateTime(epochMillis, jakartaZone)

        assertEquals(date, resultDateTime.toLocalDate())
        assertEquals(time, resultDateTime.toLocalTime())
    }

    @Test
    fun roundTrip_endOfDayBoundary_preservesLocalDateTime() {
        val date = LocalDate.of(2026, 9, 15)
        val time = LocalTime.of(23, 55)

        val epochMillis = DateTimeUtils.toEpochMillis(date, time, jakartaZone)
        val resultDateTime = DateTimeUtils.toLocalDateTime(epochMillis, jakartaZone)

        assertEquals(date, resultDateTime.toLocalDate())
        assertEquals(time, resultDateTime.toLocalTime())
    }

    @Test
    fun roundTrip_exactMidnight_preservesLocalDateTime() {
        val date = LocalDate.of(2026, 9, 15)
        val time = LocalTime.of(0, 0)

        val epochMillis = DateTimeUtils.toEpochMillis(date, time, jakartaZone)
        val resultDateTime = DateTimeUtils.toLocalDateTime(epochMillis, jakartaZone)

        assertEquals(date, resultDateTime.toLocalDate())
        assertEquals(time, resultDateTime.toLocalTime())
    }

    @Test
    fun roundTrip_lastMinuteOfDay_preservesLocalDateTime() {
        val date = LocalDate.of(2026, 9, 15)
        val time = LocalTime.of(23, 59)

        val epochMillis = DateTimeUtils.toEpochMillis(date, time, jakartaZone)
        val resultDateTime = DateTimeUtils.toLocalDateTime(epochMillis, jakartaZone)

        assertEquals(date, resultDateTime.toLocalDate())
        assertEquals(time, resultDateTime.toLocalTime())
    }

    // ==================================================
    // 2. TIMEZONE INDEPENDENCE TESTS
    // ==================================================

    @Test
    fun timezoneIndependence_sameLocalDateTimeProducesDifferentEpochInDifferentZones() {
        val date = LocalDate.of(2026, 9, 15)
        val time = LocalTime.of(14, 30)

        val jakartaMillis = DateTimeUtils.toEpochMillis(date, time, jakartaZone)
        val newYorkMillis = DateTimeUtils.toEpochMillis(date, time, newYorkZone)
        val utcMillis = DateTimeUtils.toEpochMillis(date, time, utcZone)

        // Jakarta (UTC+7) is earlier in UTC than New York (UTC-4)
        assert(jakartaMillis != newYorkMillis)
        assert(jakartaMillis != utcMillis)

        // Both round-trip correctly within their respective timezones
        assertEquals(LocalDateTime.of(date, time), DateTimeUtils.toLocalDateTime(jakartaMillis, jakartaZone))
        assertEquals(LocalDateTime.of(date, time), DateTimeUtils.toLocalDateTime(newYorkMillis, newYorkZone))
        assertEquals(LocalDateTime.of(date, time), DateTimeUtils.toLocalDateTime(utcMillis, utcZone))
    }

    // ==================================================
    // 3. LOCALE & FORMATTING TESTS
    // ==================================================

    @Test
    fun formatDeadline_septemberAfternoon_formatsIndonesian24Hour() {
        val date = LocalDate.of(2026, 9, 15)
        val time = LocalTime.of(14, 30)
        val epochMillis = DateTimeUtils.toEpochMillis(date, time, jakartaZone)

        val formatted = DateTimeUtils.formatDeadline(epochMillis, jakartaZone)

        assertEquals("15 Sep 2026, 14:30", formatted)
    }

    @Test
    fun formatDeadline_midnightBoundary_formatsWithLeadingZeros() {
        val date = LocalDate.of(2026, 9, 15)
        val time = LocalTime.of(0, 5)
        val epochMillis = DateTimeUtils.toEpochMillis(date, time, jakartaZone)

        val formatted = DateTimeUtils.formatDeadline(epochMillis, jakartaZone)

        assertEquals("15 Sep 2026, 00:05", formatted)
    }

    @Test
    fun formatDeadline_endOfDay_formats24HourCorrectly() {
        val date = LocalDate.of(2026, 9, 15)
        val time = LocalTime.of(23, 55)
        val epochMillis = DateTimeUtils.toEpochMillis(date, time, jakartaZone)

        val formatted = DateTimeUtils.formatDeadline(epochMillis, jakartaZone)

        assertEquals("15 Sep 2026, 23:55", formatted)
    }

    // ==================================================
    // 4. NULL AND DATEPICKER UTC TESTS
    // ==================================================

    @Test
    fun formatDeadline_nullEpoch_returnsNull() {
        val formatted = DateTimeUtils.formatDeadline(null, jakartaZone)
        assertNull(formatted)
    }

    @Test
    fun fromDatePickerUtcMillis_convertsUtcMidnightToLocalDateWithoutShifting() {
        // Suppose Material 3 DatePicker selected 2026-09-15 (UTC midnight: 1789430400000L)
        val expectedDate = LocalDate.of(2026, 9, 15)
        val utcMidnightMillis = expectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

        val resolvedDate = DateTimeUtils.fromDatePickerUtcMillis(utcMidnightMillis)

        assertEquals(expectedDate, resolvedDate)
    }
    @Test
    fun `getTomorrowSnoozeTime returns next day at 9 AM local time`() {
        val snoozeTimeMillis = DateTimeUtils.getTomorrowSnoozeTime()
        val localDateTime = Instant.ofEpochMilli(snoozeTimeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        val expectedDate = LocalDateTime.now().plusDays(1).toLocalDate()
        assertEquals(expectedDate, localDateTime.toLocalDate())
        assertEquals(9, localDateTime.hour)
        assertEquals(0, localDateTime.minute)
        assertEquals(0, localDateTime.second)
        assertEquals(0, localDateTime.nano)
    }
}

