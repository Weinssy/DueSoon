package com.duesoon.app.data.repository

import com.duesoon.app.data.local.TaskDao
import com.duesoon.app.data.local.toDomainModel
import com.duesoon.app.data.local.toEntity
import com.duesoon.app.domain.model.Task
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TaskRepository(
    private val taskDao: TaskDao,
    private val notificationScheduler: NotificationScheduler,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    fun observeTasks(): Flow<List<Task>> {
        return taskDao.observeTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getTask(id: Long): Task? {
        return taskDao.getTask(id)?.toDomainModel()
    }

    suspend fun insertTask(task: Task): Long {
        val id = taskDao.insert(task.toEntity())
        val savedTask = task.copy(id = id)
        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        if (prefs.notificationsEnabled && savedTask.deadline != null && !savedTask.completed) {
            notificationScheduler.schedule(savedTask)
        }
        return id
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity())
        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        if (task.completed || task.deadline == null || !prefs.notificationsEnabled) {
            notificationScheduler.cancelAll(task)
        } else {
            notificationScheduler.schedule(task)
        }
    }

    suspend fun deleteTask(task: Task) {
        taskDao.delete(task.toEntity())
        notificationScheduler.cancelAll(task)
    }
}
