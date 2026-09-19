package com.duesoon.app.domain.usecase

import android.content.Context
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.notification.NotificationScheduler
import com.duesoon.app.widget.DueSoonWidgetUpdater
import com.duesoon.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class RestoreTaskUseCase(
    private val repository: TaskRepository,
    private val notificationScheduler: NotificationScheduler,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context? = null
) {
    suspend operator fun invoke(taskId: Long) {
        val task = repository.getTask(taskId) ?: return
        if (!task.completed) return // Already active
        
        val updatedTask = task.copy(
            completed = false,
            updatedAt = System.currentTimeMillis()
        )
        
        // Update in database. The repository.updateTask() already schedules notifications if it's not complete.
        // However, we want to be safe and use repository.updateTask which handles it internally.
        // Let's call repository.updateTask directly.
        repository.updateTask(updatedTask)
        
        // Force widget update
        DueSoonWidgetUpdater.update(context)
    }
}
