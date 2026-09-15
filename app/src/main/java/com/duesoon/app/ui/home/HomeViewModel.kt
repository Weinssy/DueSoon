package com.duesoon.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.DeadlineState
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.util.DeadlineStateCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _statusFilter = MutableStateFlow(TaskStatusFilter.ALL)
    val statusFilter: StateFlow<TaskStatusFilter> = _statusFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    val tasks: StateFlow<List<Task>> = repository.observeTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredTasks: StateFlow<List<Task>> = combine(
        tasks,
        _statusFilter,
        _categoryFilter
    ) { taskList, status, category ->
        val filtered = HomeFilterLogic.filterTasks(taskList, status, category)
        val currentTime = System.currentTimeMillis()
        filtered.sortedWith(compareBy<Task> {
            val state = DeadlineStateCalculator.calculate(it, currentTime)
            when (state) {
                DeadlineState.OVERDUE -> 0
                DeadlineState.DUE_TODAY -> 1
                DeadlineState.DUE_SOON -> 2
                DeadlineState.UPCOMING -> 3
                DeadlineState.NO_DEADLINE -> 4
                DeadlineState.COMPLETED -> 5
            }
        }.thenBy(nullsLast()) { it.deadline }
            .thenByDescending { it.priority.ordinal }
            .thenBy { it.createdAt }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Backward-compatibility
    val needsAttentionTasks: StateFlow<List<Task>> = filteredTasks

    val upcomingTasks: StateFlow<List<Task>> = tasks.map { list ->
        val currentTime = System.currentTimeMillis()
        list.filter { 
            !it.completed && DeadlineStateCalculator.calculate(it, currentTime) == DeadlineState.UPCOMING 
        }.sortedBy { it.deadline }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setStatusFilter(status: TaskStatusFilter) {
        _statusFilter.value = status
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = if (category.isNullOrBlank() || category.equals("Semua", ignoreCase = true)) null else category
    }

    fun toggleTaskCompletion(task: Task, isComplete: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = isComplete, updatedAt = System.currentTimeMillis()))
        }
    }
}

