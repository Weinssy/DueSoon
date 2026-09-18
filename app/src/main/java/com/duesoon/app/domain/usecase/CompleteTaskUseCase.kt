package com.duesoon.app.domain.usecase

import com.duesoon.app.data.repository.TaskRepository

class CompleteTaskUseCase(
    private val taskRepository: TaskRepository
) {
    /**
     * Completes a task idempotently. If the task is already completed or does not exist,
     * this returns a safe success Result without triggering database mutations or side-effects.
     * 
     * TaskRepository handles the recurrence logic, notification cancellation, and widget refresh internally.
     */
    suspend operator fun invoke(taskId: Long): Result<Unit> {
        return try {
            val task = taskRepository.getTask(taskId)
            
            if (task == null) {
                return Result.success(Unit)
            }
            
            if (task.completed) {
                return Result.success(Unit) // Safe no-op
            }

            // Update task; TaskRepository intrinsically handles recurrence generation,
            // alarm cancellation, and widget refresh triggers upon completion.
            val completedTask = task.copy(
                completed = true, 
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.updateTask(completedTask)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
