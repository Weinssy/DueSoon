package com.duesoon.app.domain.reminder

import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import com.duesoon.app.notification.SmartReminderCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class SmartReminderCalculatorTest {

    @Test
    fun calculateReminders_normalPriority_generatesTwoOffsets() {
        val currentTime = 1000L
        val deadline = currentTime + TimeUnit.DAYS.toMillis(2)
        val task = Task(
            id = 1L,
            title = "Test",
            priority = Priority.NORMAL,
            reminderType = ReminderType.SMART,
            deadline = deadline
        )

        val reminders = SmartReminderCalculator.calculateReminders(task, currentTime)

        assertEquals(2, reminders.size)
        // Short range (2 hours before)
        assertEquals(deadline - TimeUnit.HOURS.toMillis(2), reminders[0])
        // Exact deadline
        assertEquals(deadline, reminders[1])
    }

    @Test
    fun calculateReminders_highPriority_generatesThreeOffsets() {
        val currentTime = 1000L
        val deadline = currentTime + TimeUnit.DAYS.toMillis(2)
        val task = Task(
            id = 1L,
            title = "Test",
            priority = Priority.HIGH,
            reminderType = ReminderType.SMART,
            deadline = deadline
        )

        val reminders = SmartReminderCalculator.calculateReminders(task, currentTime)

        assertEquals(3, reminders.size)
        // Mid range (24 hours before)
        assertEquals(deadline - TimeUnit.DAYS.toMillis(1), reminders[0])
        // Short range (2 hours before)
        assertEquals(deadline - TimeUnit.HOURS.toMillis(2), reminders[1])
        // Exact deadline
        assertEquals(deadline, reminders[2])
    }

    @Test
    fun calculateReminders_prunesPastOffsets() {
        val currentTime = 1000L
        // Deadline is only 1 hour from now. 
        // 24hr offset and 2hr offset are both in the past!
        val deadline = currentTime + TimeUnit.HOURS.toMillis(1)
        val task = Task(
            id = 1L,
            title = "Test",
            priority = Priority.HIGH,
            reminderType = ReminderType.SMART,
            deadline = deadline
        )

        val reminders = SmartReminderCalculator.calculateReminders(task, currentTime)

        // Should only contain the exact deadline because the others are <= currentTime
        assertEquals(1, reminders.size)
        assertEquals(deadline, reminders[0])
    }
}
