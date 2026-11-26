package com.duesoon.app.ui.task

import androidx.lifecycle.SavedStateHandle
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

class EditTaskViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository
) : ViewModel() {
    private val taskId: Long = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    private var originalTask: Task? = null

    init {
        viewModelScope.launch {
            originalTask = repository.getTask(taskId)
            originalTask?.let { task ->
                _uiState.update {
                    it.copy(
                        title = task.title,
                        description = task.description ?: "",
                        deadline = task.deadline,
                        category = task.category ?: "",
                        priority = task.priority,
                        reminderType = task.reminderType,
                        isRecurring = task.isRecurring,
                        recurrenceRule = task.recurrenceRule ?: RecurrenceRule.Daily
                    )
                }
            }
        }
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

    fun updateTask() {
        val currentState = _uiState.value
        if (currentState.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title can't be empty.") }
            return
        }

        val currentOriginal = originalTask ?: return

        val task = currentOriginal.copy(
            title = currentState.title.trim(),
            description = currentState.description.trim().ifBlank { null },
            deadline = currentState.deadline,
            category = currentState.category.trim().ifBlank { null },
            priority = currentState.priority,
            reminderType = currentState.reminderType,
            isRecurring = currentState.isRecurring,
            recurrenceRule = if (currentState.isRecurring) currentState.recurrenceRule else null,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.updateTask(task)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
