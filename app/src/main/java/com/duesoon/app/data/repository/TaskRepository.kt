package com.duesoon.app.data.repository

import android.content.Context
import com.duesoon.app.data.local.TaskDao
import com.duesoon.app.data.local.toDomainModel
import com.duesoon.app.data.local.toEntity
import com.duesoon.app.domain.engine.RecurrenceCalculator
import com.duesoon.app.domain.model.Task
import com.duesoon.app.notification.NotificationScheduler
import com.duesoon.app.widget.DueSoonWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


open class TaskRepository(
    private val taskDao: TaskDao,
    private val notificationScheduler: NotificationScheduler,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context? = null
) {

    open fun observeTasks(): Flow<List<Task>> {
        return taskDao.observeTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    open fun observeActiveTasks(): Flow<List<Task>> {
        return taskDao.observeActiveTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    open fun observeArchivedTasks(): Flow<List<Task>> {
        return taskDao.observeArchivedTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    open fun searchArchivedTasks(query: String): Flow<List<Task>> {
        return taskDao.searchArchivedTasks(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    open suspend fun clearArchive(): Int {
        val count = taskDao.deleteCompletedTasks()
        DueSoonWidgetUpdater.update(context)
        return count
    }

    open suspend fun getTask(id: Long): Task? {
        return taskDao.getTask(id)?.toDomainModel()
    }

    open suspend fun insertTask(task: Task): Long {
        val id = taskDao.insert(task.toEntity())
        val savedTask = task.copy(id = id)
        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        if (prefs.notificationsEnabled && savedTask.deadline != null && !savedTask.completed) {
            notificationScheduler.schedule(savedTask)
        }
        DueSoonWidgetUpdater.update(context)
        return id
    }

    open suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity())
        val prefs = userPreferencesRepository.userPreferencesFlow.first()

        // If task is being marked complete and is recurring, create the next instance
        if (task.completed && task.isRecurring && task.recurrenceRule != null && task.deadline != null) {
            val nextDeadline = RecurrenceCalculator.calculateNextDeadline(task.deadline, task.recurrenceRule)
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

