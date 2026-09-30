package com.duesoon.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY completed ASC, deadline ASC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 0 AND isDeleted = 0 ORDER BY deadline ASC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>


    @Query("UPDATE tasks SET isDeleted = 1, syncState = 'DIRTY', updatedAtUtc = :timestamp, revision = revision + 1 WHERE completed = 1")
    suspend fun deleteCompletedTasks(timestamp: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM tasks WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getTask(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE uuid = :uuid LIMIT 1")
    suspend fun getTaskByUuid(uuid: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Insert
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET isDeleted = 1, syncState = 'DIRTY', updatedAtUtc = :timestamp, revision = revision + 1")
    suspend fun deleteAllTasks(timestamp: Long = System.currentTimeMillis())

    @androidx.room.Transaction
    suspend fun replaceAllTasks(tasks: List<TaskEntity>) {
        deleteAllTasks()
        insertTasks(tasks)
    }

}
