package com.duesoon.app.domain.backup

import com.duesoon.app.domain.focus.FocusStatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FokusKuliahBackupParserTest {

    @Test
    fun `parses valid fokus-kuliah backup with tasks, course history, remainder history, and settings`() {
        val raw = """
            {
              "app": "fokus-kuliah",
              "schemaVersion": 1,
              "exportedAt": "2026-04-15T08:00:00.000Z",
              "data": {
                "tasks": [
                  {
                    "id": "task-1",
                    "name": "Makalah Kecerdasan Buatan",
                    "course": "Kecerdasan Buatan",
                    "date": "2026-04-20",
                    "time": "23:59",
                    "done": false,
                    "focusSessions": 2,
                    "focusMinutes": 50
                  },
                  {
                    "id": "task-2",
                    "name": "Latihan SQL",
                    "course": "Basis Data",
                    "date": "2026-04-18",
                    "time": "",
                    "done": true,
                    "focusSessions": 0,
                    "focusMinutes": 0
                  }
                ],
                "history": {
                  "2026-04-15": { "sessions": 3, "minutes": 75 },
                  "2026-04-14": { "sessions": 1, "minutes": 25 }
                },
                "courseHistory": {
                  "2026-04-15": {
                    "Kecerdasan Buatan": { "sessions": 2, "minutes": 50 }
                  }
                },
                "settings": {
                  "focus": 30,
                  "break": 10,
                  "longBreak": 20,
                  "sound": false,
                  "autoStart": true
                }
              }
            }
        """.trimIndent()

        val result = FokusKuliahBackupParser.parse(raw, zoneId = ZoneId.of("UTC"), nowMillis = 1_700_000_000_000L)
        assertTrue(result is FokusKuliahParseResult.Success)
        val parsed = (result as FokusKuliahParseResult.Success).data

        assertEquals(2, parsed.tasks.size)
        assertEquals("Makalah Kecerdasan Buatan", parsed.tasks[0].title)
        assertEquals("Kecerdasan Buatan", parsed.tasks[0].category)
        assertNotNull(parsed.tasks[0].deadline)
        assertEquals(false, parsed.tasks[0].completed)
        assertEquals(true, parsed.tasks[1].completed)

        // Total sessions: 2 (courseHistory on 15th) + 1 (unlinked remainder on 15th) + 1 (unlinked on 14th) = 4
        assertEquals(4, parsed.focusSessions.size)
        val stats = FocusStatsCalculator.calculate(parsed.focusSessions, LocalDate.of(2026, 4, 15))
        assertEquals(3, stats.todaySessions)
        assertEquals(75, stats.todayMinutes)
        assertEquals(2, stats.streakDays)
        assertEquals(1, stats.courseStats.size)
        assertEquals("Kecerdasan Buatan", stats.courseStats[0].name)
        assertEquals(50, stats.courseStats[0].minutes)

        assertNotNull(parsed.pomodoroSettings)
        assertEquals(30, parsed.pomodoroSettings?.focusMinutes)
        assertEquals(10, parsed.pomodoroSettings?.shortBreakMinutes)
        assertEquals(20, parsed.pomodoroSettings?.longBreakMinutes)
        assertEquals(false, parsed.pomodoroSettings?.soundEnabled)
        assertEquals(true, parsed.pomodoroSettings?.autoStartNext)
    }

    @Test
    fun `rejects non fokus-kuliah backup payload`() {
        val raw = """{"schemaVersion":1,"appVersion":"1.9.5","exportedAt":123,"tasks":[]}"""
        val result = FokusKuliahBackupParser.parse(raw)
        assertTrue(result is FokusKuliahParseResult.Error)
    }

    @Test
    fun `rejects unsupported schema version`() {
        val raw = """{"app":"fokus-kuliah","schemaVersion":99,"data":{"tasks":[]}}"""
        val result = FokusKuliahBackupParser.parse(raw)
        assertTrue(result is FokusKuliahParseResult.Error)
    }
}
