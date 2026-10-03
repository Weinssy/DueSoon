package com.duesoon.app.ui.focus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusUiState(
    val task: Task? = null,
    val totalSeconds: Long = 25 * 60,
    val remainingSeconds: Long = 25 * 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f

    val formattedTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}

class FocusViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository
) : ViewModel() {

    private val taskId: Long = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            val task = repository.getTask(taskId)
            _uiState.update { it.copy(task = task) }
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning || _uiState.value.remainingSeconds <= 0) return

        _uiState.update { it.copy(isRunning = true, isFinished = false) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 && _uiState.value.isRunning) {
                delay(1000L)
                _uiState.update { state ->
                    val newRemaining = state.remainingSeconds - 1
                    if (newRemaining <= 0) {
                        state.copy(remainingSeconds = 0, isRunning = false, isFinished = true)
                    } else {
                        state.copy(remainingSeconds = newRemaining)
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                remainingSeconds = it.totalSeconds,
                isRunning = false,
                isFinished = false
            )
        }
    }

    fun setDuration(minutes: Long) {
        timerJob?.cancel()
        val seconds = minutes * 60
        _uiState.update {
            it.copy(
                totalSeconds = seconds,
                remainingSeconds = seconds,
                isRunning = false,
                isFinished = false
            )
        }
    }

    fun addMinutes(minutes: Long) {
        val additionalSeconds = minutes * 60
        _uiState.update {
            val newTotal = it.totalSeconds + additionalSeconds
            val newRemaining = it.remainingSeconds + additionalSeconds
            it.copy(
                totalSeconds = newTotal,
                remainingSeconds = newRemaining,
                isFinished = false
            )
        }
        if (!_uiState.value.isRunning) {
            startTimer()
        }
    }

    fun completeTask(onCompleted: () -> Unit) {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            repository.updateTask(currentTask.copy(completed = true, updatedAt = System.currentTimeMillis()))
            onCompleted()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
