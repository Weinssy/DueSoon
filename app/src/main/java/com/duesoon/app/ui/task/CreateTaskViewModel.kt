package com.duesoon.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.RecurrenceInterval
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val deadline: Long? = null,
    val category: String = "",
    val priority: Priority = Priority.NORMAL,
    val reminderType: ReminderType = ReminderType.SMART,
    val isRecurring: Boolean = false,
    val recurrenceInterval: RecurrenceInterval = RecurrenceInterval.DAILY,
    val isSaved: Boolean = false,
    val titleError: String? = null
)

class CreateTaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) { _uiState.value = _uiState.value.copy(title = title, titleError = null) }
    fun updateDescription(desc: String) { _uiState.value = _uiState.value.copy(description = desc) }
    fun updateDeadline(deadline: Long?) { _uiState.value = _uiState.value.copy(deadline = deadline) }
    fun updateCategory(category: String) { _uiState.value = _uiState.value.copy(category = category) }
    fun updatePriority(priority: Priority) { _uiState.value = _uiState.value.copy(priority = priority) }
    fun updateReminderType(type: ReminderType) { _uiState.value = _uiState.value.copy(reminderType = type) }
    fun updateIsRecurring(isRecurring: Boolean) { _uiState.value = _uiState.value.copy(isRecurring = isRecurring) }
    fun updateRecurrenceInterval(interval: RecurrenceInterval) { _uiState.value = _uiState.value.copy(recurrenceInterval = interval) }

    fun saveTask() {
        val currentState = _uiState.value
        if (currentState.title.isBlank()) {
            _uiState.value = currentState.copy(titleError = "Title can't be empty.")
            return
        }

        val task = Task(
            title = currentState.title,
            description = currentState.description.ifBlank { null },
            deadline = currentState.deadline,
            category = currentState.category.ifBlank { null },
            priority = currentState.priority,
            reminderType = currentState.reminderType,
            isRecurring = currentState.isRecurring,
            recurrenceInterval = if (currentState.isRecurring) currentState.recurrenceInterval else null
        )

        viewModelScope.launch {
            repository.insertTask(task)
            _uiState.value = currentState.copy(isSaved = true)
        }
    }
}

