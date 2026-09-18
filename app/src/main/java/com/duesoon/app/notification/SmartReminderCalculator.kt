package com.duesoon.app.notification

import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import java.util.concurrent.TimeUnit

object SmartReminderCalculator {
    
    fun calculateReminders(task: Task, currentTimeMillis: Long = System.currentTimeMillis()): List<Long> {
        val deadline = task.deadline ?: return emptyList()
        if (task.completed) return emptyList()
        if (task.reminderType == ReminderType.NONE) return emptyList()

        if (task.reminderType == ReminderType.CUSTOM) {
            // TODO: Extensible custom reminders in future phases
        }

        val scheduledTimes = mutableListOf<Long>()

        // 1. Exact Deadline
        scheduledTimes.add(deadline)

        // 2. Short-range (2 hours before)
        val twoHoursBefore = deadline - TimeUnit.HOURS.toMillis(2)
        scheduledTimes.add(twoHoursBefore)

        // 3. Medium-range (24 hours before) - Only for HIGH
        if (task.priority == com.duesoon.app.domain.model.Priority.HIGH) {
            val oneDayBefore = deadline - TimeUnit.DAYS.toMillis(1)
            scheduledTimes.add(oneDayBefore)
        }

        // Pruning Rule: Filter generated timestamps using triggerTime > currentTimeMillis
        return scheduledTimes.filter { it > currentTimeMillis }.sorted()
    }
}
