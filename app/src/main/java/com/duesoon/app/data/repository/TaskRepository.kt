package com.duesoon.app.data.repository

import android.content.Context
import com.duesoon.app.data.local.TaskDao
import com.duesoon.app.data.local.toDomainModel
import com.duesoon.app.data.local.toEntity
import com.duesoon.app.domain.model.RecurrenceInterval
import com.duesoon.app.domain.model.Task
import com.duesoon.app.notification.NotificationScheduler
import com.duesoon.app.widget.DueSoonWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val notificationScheduler: NotificationScheduler,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context? = null
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
        DueSoonWidgetUpdater.update(context)
        return id
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity())
        val prefs = userPreferencesRepository.userPreferencesFlow.first()

        // If task is being marked complete and is recurring, create the next instance
        if (task.completed && task.isRecurring && task.recurrenceInterval != null && task.deadline != null) {
            val nextDeadline = calculateNextDeadline(task.deadline, task.recurrenceInterval)
            val nextTask = task.copy(
                id = 0,
                completed = false,
                deadline = nextDeadline,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            insertTask(nextTask)
        }

        if (task.completed || task.deadline == null || !prefs.notificationsEnabled) {
            notificationScheduler.cancelAll(task)
        } else {
            notificationScheduler.schedule(task)
        }
        DueSoonWidgetUpdater.update(context)
    }

    suspend fun snoozeTask(taskId: Long, snoozedUntil: Long) {
        val task = getTask(taskId) ?: return
        if (task.completed) return
        val updatedTask = task.copy(snoozedUntil = snoozedUntil, updatedAt = System.currentTimeMillis())
        taskDao.update(updatedTask.toEntity())
        notificationScheduler.scheduleSnooze(updatedTask, snoozedUntil)
    }

    suspend fun clearSnooze(taskId: Long) {
        val task = getTask(taskId) ?: return
        val updatedTask = task.copy(snoozedUntil = null, updatedAt = System.currentTimeMillis())
        taskDao.update(updatedTask.toEntity())
        notificationScheduler.cancelSnooze(updatedTask)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.delete(task.toEntity())
        notificationScheduler.cancelAll(task)
        DueSoonWidgetUpdater.update(context)
    }

    private fun calculateNextDeadline(currentDeadline: Long, interval: RecurrenceInterval): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDeadline }
        when (interval) {
            RecurrenceInterval.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceInterval.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceInterval.MONTHLY -> calendar.add(Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }

    suspend fun importTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) return
        val entities = tasks.map { it.copy(id = 0).toEntity() }
        taskDao.insertTasks(entities)
        DueSoonWidgetUpdater.update(context)
    }

    suspend fun restoreTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) return
        val entities = tasks.map { it.toEntity() }
        taskDao.replaceAllTasks(entities)
        DueSoonWidgetUpdater.update(context)
    }
}

