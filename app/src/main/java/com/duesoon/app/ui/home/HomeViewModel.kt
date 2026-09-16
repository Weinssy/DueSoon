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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DEADLINE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val tasks: StateFlow<List<Task>> = repository.observeTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredTasks: StateFlow<List<Task>> = combine(
        tasks,
        _statusFilter,
        _categoryFilter,
        _searchQuery,
        _sortOrder
    ) { taskList, status, category, query, sort ->
        val filtered = HomeFilterLogic.filterTasks(taskList, status, category, query)
        HomeFilterLogic.sortTasks(filtered, sort)
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(sort: SortOrder) {
        _sortOrder.value = sort
    }

    fun toggleTaskCompletion(task: Task, isComplete: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = isComplete, updatedAt = System.currentTimeMillis()))
        }
    }
}
