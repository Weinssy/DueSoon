package com.duesoon.app.ui.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository
) : ViewModel() {
    private val taskId: Long = checkNotNull(savedStateHandle["taskId"])

    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task.asStateFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            _task.value = repository.getTask(taskId)
        }
    }

    fun completeTask() {
        val currentTask = _task.value ?: return
        viewModelScope.launch {
            repository.updateTask(currentTask.copy(completed = true, updatedAt = System.currentTimeMillis()))
            loadTask()
        }
    }
    
    fun undoComplete() {
        val currentTask = _task.value ?: return
        viewModelScope.launch {
            repository.updateTask(currentTask.copy(completed = false, updatedAt = System.currentTimeMillis()))
            loadTask()
        }
    }

    fun deleteTask(onDeleted: () -> Unit) {
        val currentTask = _task.value ?: return
        viewModelScope.launch {
            repository.deleteTask(currentTask)
            onDeleted()
        }
    }
}
