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
    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY deadline ASC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY deadline DESC")
    fun observeArchivedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 1 AND title LIKE '%' || :query || '%' ORDER BY deadline DESC")
    fun searchArchivedTasks(query: String): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks WHERE completed = 1")
    suspend fun deleteCompletedTasks(): Int

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTask(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Insert
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @androidx.room.Transaction
    suspend fun replaceAllTasks(tasks: List<TaskEntity>) {
        deleteAllTasks()
        insertTasks(tasks)
    }
}
