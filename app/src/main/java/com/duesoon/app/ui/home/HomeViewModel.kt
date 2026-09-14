package com.duesoon.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.DeadlineState
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.util.DeadlineStateCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    val needsAttentionTasks: StateFlow<List<Task>> = tasks.map { list ->
        val currentTime = System.currentTimeMillis()
        list.filter { !it.completed && it.deadline != null }
            .sortedWith(compareBy<Task> { 
                val state = DeadlineStateCalculator.calculate(it, currentTime)
                when(state) {
                    DeadlineState.OVERDUE -> 0
                    DeadlineState.DUE_TODAY -> 1
                    DeadlineState.DUE_SOON -> 2
                    DeadlineState.UPCOMING -> 3
                    else -> 4
                }
            }.thenBy { it.deadline }
             .thenByDescending { it.priority.ordinal }
             .thenBy { it.createdAt }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingTasks: StateFlow<List<Task>> = tasks.map { list ->
        val currentTime = System.currentTimeMillis()
        list.filter { 
            !it.completed && DeadlineStateCalculator.calculate(it, currentTime) == DeadlineState.UPCOMING 
        }.sortedBy { it.deadline }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun toggleTaskCompletion(task: Task, isComplete: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = isComplete, updatedAt = System.currentTimeMillis()))
            // TODO: Cancel remaining notifications (Phase 6)
        }
    }
}
