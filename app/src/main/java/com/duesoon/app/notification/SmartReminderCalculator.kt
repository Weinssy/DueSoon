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

        val diff = deadline - currentTimeMillis
        val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)
        val scheduledTimes = mutableListOf<Long>()

        val threeDaysBefore = deadline - TimeUnit.DAYS.toMillis(3)
        val oneDayBefore = deadline - TimeUnit.DAYS.toMillis(1)
        val threeHoursBefore = deadline - TimeUnit.HOURS.toMillis(3)
        val thirtyMinsBefore = deadline - TimeUnit.MINUTES.toMillis(30)

        if (daysDiff > 7) {
            scheduledTimes.add(threeDaysBefore)
            scheduledTimes.add(oneDayBefore)
            scheduledTimes.add(threeHoursBefore)
        } else if (daysDiff in 1..7) {
            scheduledTimes.add(oneDayBefore)
            scheduledTimes.add(threeHoursBefore)
        } else {
            scheduledTimes.add(threeHoursBefore)
            scheduledTimes.add(thirtyMinsBefore)
        }

        return scheduledTimes.filter { it > currentTimeMillis }
    }
}
