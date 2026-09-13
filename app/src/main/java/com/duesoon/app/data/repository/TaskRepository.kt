package com.duesoon.app.data.repository

import com.duesoon.app.data.local.TaskDao
import com.duesoon.app.data.local.toDomainModel
import com.duesoon.app.data.local.toEntity
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val taskDao: TaskDao) {

    fun observeTasks(): Flow<List<Task>> {
        return taskDao.observeTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getTask(id: Long): Task? {
        return taskDao.getTask(id)?.toDomainModel()
    }

    suspend fun insertTask(task: Task): Long {
        return taskDao.insert(task.toEntity())
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity())
    }

    suspend fun deleteTask(task: Task) {
        taskDao.delete(task.toEntity())
    }
}
