package com.duesoon.app.domain.backup

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    private val json = BackupSerializer.json

    @Test
    fun `portable backup serialize to JSON and back to object successfully`() {
        val originalBackup = PortableBackup(
            schemaVersion = 1,
            appVersion = "1.3.0",
            exportedAt = 1729012345678L,
            tasks = listOf(
                PortableTask(
                    id = 1L,
                    title = "Buy groceries",
                    description = "Milk, Bread, Eggs",
                    deadline = 1729050000000L,
                    category = "Personal",
                    priority = "HIGH",
                    reminderType = "SMART",
                    isRecurring = false,
                    recurrenceInterval = null,
                    completed = false,
                    snoozedUntil = null,
                    createdAt = 1729000000000L,
                    updatedAt = 1729000000000L
                )
            )
        )

        val jsonString = json.encodeToString(originalBackup)
        
        // Assert json contains required strings based on encodeDefaults and enums
        assertTrue(jsonString.contains("\"HIGH\""))
        assertTrue(jsonString.contains("\"SMART\""))
        assertTrue(jsonString.contains("\"schemaVersion\": 1"))
        
        val decodedBackup = json.decodeFromString<PortableBackup>(jsonString)
        
        assertEquals(1, decodedBackup.schemaVersion)
        assertEquals(1, decodedBackup.tasks.size)
        
        val task = decodedBackup.tasks.first()
        assertEquals(1L, task.id)
        assertEquals("HIGH", task.priority)
        assertEquals(1729050000000L, task.deadline)
        assertEquals(false, task.completed)
        assertNull(task.snoozedUntil)
    }
    
    @Test
    fun `ignore unknown fields keeps parsing intact`() {
        val unknownFieldJson = """
        {
          "schemaVersion": 1,
          "appVersion": "1.3.0",
          "exportedAt": 1729012345678,
          "futureUnknownField": "Should be ignored",
          "tasks": []
        }
        """.trimIndent()
        
        val decoded = json.decodeFromString<PortableBackup>(unknownFieldJson)
        assertEquals(1, decoded.schemaVersion)
    }

    @Test
    fun `missing optional fields fall back to default nulls`() {
        val minimalTaskJson = """
        {
          "schemaVersion": 1,
          "appVersion": "1.3.0",
          "exportedAt": 1729012345678,
          "tasks": [
            {
              "id": 100,
              "title": "Minimal Task",
              "priority": "LOW",
              "reminderType": "NONE",
              "isRecurring": false,
              "completed": true,
              "createdAt": 1000,
              "updatedAt": 2000
            }
          ]
        }
        """.trimIndent()

        val decoded = json.decodeFromString<PortableBackup>(minimalTaskJson)
        val task = decoded.tasks.first()
        
        assertEquals(100L, task.id)
        assertNull(task.description)
        assertNull(task.deadline)
        assertNull(task.category)
        assertNull(task.recurrenceInterval)
        assertNull(task.snoozedUntil)
        assertEquals(true, task.completed)
    }
}
