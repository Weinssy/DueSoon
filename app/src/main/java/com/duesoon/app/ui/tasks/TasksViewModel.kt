package com.duesoon.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.model.DeadlineState
import com.duesoon.app.domain.util.DeadlineStateCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class TasksViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    val categories: StateFlow<List<String>> = repository.observeTasks()
        .combine(_selectedCategory) { tasks, _ -> 
            tasks.mapNotNull { it.category }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedTasks: StateFlow<Map<String, List<Task>>> = repository.observeTasks()
        .combine(_selectedCategory) { tasks, category ->
            val filtered = if (category == null) tasks else tasks.filter { it.category == category }
            
            val needsAttention = filtered.filter { !it.completed && (DeadlineStateCalculator.calculate(it) == DeadlineState.OVERDUE || DeadlineStateCalculator.calculate(it) == DeadlineState.DUE_TODAY || DeadlineStateCalculator.calculate(it) == DeadlineState.DUE_SOON) }.sortedBy { it.deadline ?: Long.MAX_VALUE }
            
            val upcoming = filtered.filter { !it.completed && (DeadlineStateCalculator.calculate(it) == DeadlineState.UPCOMING || DeadlineStateCalculator.calculate(it) == DeadlineState.NO_DEADLINE) }.sortedBy { it.deadline ?: Long.MAX_VALUE }
            val completed = filtered.filter { it.completed }.sortedByDescending { it.updatedAt }
            
            mapOf(
                "Needs Attention" to needsAttention,
                "Upcoming" to upcoming,
                "Completed" to completed
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }
}
