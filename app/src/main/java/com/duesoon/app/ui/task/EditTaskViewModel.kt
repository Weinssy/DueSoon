package com.duesoon.app.ui.task

import androidx.lifecycle.SavedStateHandle
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
                _uiState.value = CreateTaskUiState(
                    title = task.title,
                    description = task.description ?: "",
                    deadline = task.deadline,
                    category = task.category ?: "",
                    priority = task.priority,
                    reminderType = task.reminderType,
                    isRecurring = task.isRecurring,
                    recurrenceInterval = task.recurrenceInterval ?: RecurrenceInterval.DAILY
                )
            }
        }
    }

    fun updateTitle(title: String) { _uiState.value = _uiState.value.copy(title = title, titleError = null) }
    fun updateDescription(desc: String) { _uiState.value = _uiState.value.copy(description = desc) }
    fun updateDeadline(deadline: Long?) { _uiState.value = _uiState.value.copy(deadline = deadline, isRecurring = if (deadline == null) false else _uiState.value.isRecurring) }
    fun updateCategory(category: String) { _uiState.value = _uiState.value.copy(category = category) }
    fun updatePriority(priority: Priority) { _uiState.value = _uiState.value.copy(priority = priority) }
    fun updateReminderType(type: ReminderType) { _uiState.value = _uiState.value.copy(reminderType = type) }
    fun updateIsRecurring(isRecurring: Boolean) { _uiState.value = _uiState.value.copy(isRecurring = isRecurring) }
    fun updateRecurrenceInterval(interval: RecurrenceInterval) { _uiState.value = _uiState.value.copy(recurrenceInterval = interval) }

    fun updateTask() {
        val currentState = _uiState.value
        if (currentState.title.isBlank()) {
            _uiState.value = currentState.copy(titleError = "Title can't be empty.")
            return
        }
        
        val currentOriginal = originalTask ?: return

        val task = currentOriginal.copy(
            title = currentState.title,
            description = currentState.description.ifBlank { null },
            deadline = currentState.deadline,
            category = currentState.category.ifBlank { null },
            priority = currentState.priority,
            reminderType = currentState.reminderType,
            isRecurring = currentState.isRecurring,
            recurrenceInterval = if (currentState.isRecurring) currentState.recurrenceInterval else null,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.updateTask(task)
            _uiState.value = currentState.copy(isSaved = true)
        }
    }
}


