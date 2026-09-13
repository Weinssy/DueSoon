package com.duesoon.app.notification

import com.duesoon.app.domain.model.Task

interface NotificationScheduler {
    fun schedule(task: Task)
    fun cancel(taskId: Long)
    fun cancelAll(task: Task)
}
