package com.duesoon.app.domain.util

import com.duesoon.app.domain.model.DeadlineState
import com.duesoon.app.domain.model.Task
import java.util.Calendar

object DeadlineStateCalculator {
    fun calculate(task: Task, currentTimeMillis: Long = System.currentTimeMillis()): DeadlineState {
        if (task.completed) return DeadlineState.COMPLETED
        if (task.deadline == null) return DeadlineState.NO_DEADLINE
        
        val deadline = task.deadline
        if (deadline < currentTimeMillis) {
            return DeadlineState.OVERDUE
        }

        val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val deadlineCalendar = Calendar.getInstance().apply { timeInMillis = deadline }

        val isSameDay = currentCalendar.get(Calendar.YEAR) == deadlineCalendar.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.DAY_OF_YEAR) == deadlineCalendar.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) return DeadlineState.DUE_TODAY

        val diff = deadline - currentTimeMillis
        val daysDiff = diff / (1000 * 60 * 60 * 24)
        if (daysDiff <= 3) {
            return DeadlineState.DUE_SOON
        }

        return DeadlineState.UPCOMING
    }
}
