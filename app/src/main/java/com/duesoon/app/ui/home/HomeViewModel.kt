package com.duesoon.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.observeTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleTaskCompletion(task: Task, isComplete: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = isComplete, updatedAt = System.currentTimeMillis()))
        }
    }
}
