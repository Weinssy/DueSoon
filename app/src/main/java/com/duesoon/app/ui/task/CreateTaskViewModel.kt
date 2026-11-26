package com.duesoon.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.RecurrenceRule
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val deadline: Long? = null,
    val category: String = "",
    val categorySuggestions: List<String> = emptyList(),
    val priority: Priority = Priority.NORMAL,
    val reminderType: ReminderType = ReminderType.SMART,
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule = RecurrenceRule.Daily,
    val isSaved: Boolean = false,
    val titleError: String? = null
)

class CreateTaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    init {
        observeCategorySuggestions()
    }

    private fun observeCategorySuggestions() {
        viewModelScope.launch {
            @Suppress("USELESS_ELVIS")
            val flow = repository.observeTasks() ?: return@launch
            flow.catch { }.collect { tasks ->
                val suggestions = tasks
                    .mapNotNull { it.category?.trim()?.takeIf { c -> c.isNotEmpty() } }
                    .distinctBy { it.lowercase() }
                    .sortedBy { it.lowercase() }
                _uiState.update { it.copy(categorySuggestions = suggestions) }
            }
        }
    }

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title, titleError = null) } }
    fun updateDescription(desc: String) { _uiState.update { it.copy(description = desc) } }
    fun updateDeadline(deadline: Long?) {
        _uiState.update {
            it.copy(
                deadline = deadline,
                isRecurring = if (deadline == null) false else it.isRecurring
            )
        }
    }
    fun updateCategory(category: String) { _uiState.update { it.copy(category = category) } }
    fun updatePriority(priority: Priority) { _uiState.update { it.copy(priority = priority) } }
    fun updateReminderType(type: ReminderType) { _uiState.update { it.copy(reminderType = type) } }
    fun updateIsRecurring(isRecurring: Boolean) { _uiState.update { it.copy(isRecurring = isRecurring) } }
    fun updateRecurrenceRule(rule: RecurrenceRule) { _uiState.update { it.copy(recurrenceRule = rule) } }

    fun saveTask() {
        val currentState = _uiState.value
        if (currentState.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title can't be empty.") }
            return
        }

        val task = Task(
            title = currentState.title.trim(),
            description = currentState.description.trim().ifBlank { null },
            deadline = currentState.deadline,
            category = currentState.category.trim().ifBlank { null },
            priority = currentState.priority,
            reminderType = currentState.reminderType,
            isRecurring = currentState.isRecurring,
            recurrenceRule = if (currentState.isRecurring) currentState.recurrenceRule else null
        )

        viewModelScope.launch {
            repository.insertTask(task)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
