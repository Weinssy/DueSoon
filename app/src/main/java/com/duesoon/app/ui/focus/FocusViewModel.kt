package com.duesoon.app.ui.focus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.FocusRepository
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.domain.focus.FocusStatsCalculator
import com.duesoon.app.domain.focus.FocusStatsSummary
import com.duesoon.app.domain.model.PomodoroMode
import com.duesoon.app.domain.model.PomodoroSettings
import com.duesoon.app.domain.model.Task
import com.duesoon.app.focus.PomodoroTimerCoordinator
import com.duesoon.app.focus.PomodoroTimerSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FocusUiState(
    val task: Task? = null,
    val activeTasks: List<Task> = emptyList(),
    val mode: PomodoroMode = PomodoroMode.FOCUS,
    val cycleCount: Int = 0,
    val cycleTarget: Int = PomodoroSettings.CYCLE_TARGET,
    val pomodoroSettings: PomodoroSettings = PomodoroSettings(),
    val totalSeconds: Long = PomodoroSettings.DEFAULT_FOCUS_MINUTES * 60L,
    val remainingSeconds: Long = PomodoroSettings.DEFAULT_FOCUS_MINUTES * 60L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val lastCompletedMode: PomodoroMode? = null,
    val completionEventId: Long = 0L,
    val stats: FocusStatsSummary = FocusStatsSummary(),
    val isTaskScopedMode: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds > 0) {
            (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val formattedTime: String
        get() {
            val safe = remainingSeconds.coerceAtLeast(0L)
            val minutes = safe / 60
            val seconds = safe % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}

class FocusViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository,
    private val focusRepository: FocusRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    sharedCoordinator: PomodoroTimerCoordinator? = null
) : ViewModel() {

    private val initialTaskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0L }

    private val coordinator: PomodoroTimerCoordinator = sharedCoordinator ?: PomodoroTimerCoordinator(
        scope = viewModelScope,
        taskRepository = repository,
        focusRepository = focusRepository,
        userPreferencesRepository = userPreferencesRepository,
        nowMillisProvider = nowMillisProvider,
        todayProvider = todayProvider
    )

    private val _uiState = MutableStateFlow(
        FocusUiState(isTaskScopedMode = initialTaskId != null)
    )
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    init {
        syncFromCoordinator(coordinator.state.value)
        observeCoordinator()
        if (initialTaskId != null) {
            loadTask(initialTaskId)
        }
        observeActiveTasks()
        observeFocusStats()
    }

    private fun observeCoordinator() {
        viewModelScope.launch {
            coordinator.state.collect { snapshot ->
                syncFromCoordinator(snapshot)
            }
        }
    }

    private fun syncFromCoordinator(snapshot: PomodoroTimerSnapshot) {
        _uiState.update { current ->
            val resolvedTask = when {
                snapshot.linkedTaskId == null -> null
                current.task?.id == snapshot.linkedTaskId -> current.task
                else -> current.activeTasks.firstOrNull { it.id == snapshot.linkedTaskId }
                    ?: snapshot.linkedTaskTitle?.let { title ->
                        Task(
                            id = snapshot.linkedTaskId,
                            title = title,
                            category = snapshot.linkedTaskCategory
                        )
                    }
            }
            current.copy(
                task = resolvedTask,
                mode = snapshot.mode,
                cycleCount = snapshot.cycleCount,
                cycleTarget = snapshot.cycleTarget,
                pomodoroSettings = snapshot.pomodoroSettings,
                totalSeconds = snapshot.totalSeconds,
                remainingSeconds = snapshot.remainingSeconds,
                isRunning = snapshot.isRunning,
                isFinished = snapshot.isFinished,
                lastCompletedMode = snapshot.lastCompletedMode,
                completionEventId = snapshot.completionEventId
            )
        }
    }

    private fun loadTask(taskId: Long) {
        viewModelScope.launch {
            val task = repository.getTask(taskId)
            val snapshot = coordinator.state.value
            if (task != null && (!snapshot.isRunning && !snapshot.isPaused || snapshot.linkedTaskId == taskId)) {
                coordinator.selectTask(task)
                _uiState.update { it.copy(task = task) }
            }
        }
    }

    private fun observeActiveTasks() {
        viewModelScope.launch {
            @Suppress("USELESS_ELVIS")
            val flow = repository.observeActiveTasks() ?: return@launch
            flow.catch { }.collect { tasks ->
                val sorted = tasks.sortedWith(
                    compareBy<Task> { it.deadline == null }
                        .thenBy { it.deadline ?: Long.MAX_VALUE }
                        .thenBy { it.title.lowercase() }
                )
                _uiState.update { state ->
                    val linkedId = coordinator.state.value.linkedTaskId
                    val refreshedTask = if (linkedId != null) {
                        sorted.firstOrNull { it.id == linkedId } ?: state.task
                    } else {
                        null
                    }
                    state.copy(
                        activeTasks = sorted,
                        task = refreshedTask
                    )
                }
            }
        }
    }

    private fun observeFocusStats() {
        val focusRepo = focusRepository ?: return
        viewModelScope.launch {
            @Suppress("USELESS_ELVIS")
            val flow = focusRepo.observeAllSessions() ?: return@launch
            flow.catch { }.collect { sessions ->
                val summary = FocusStatsCalculator.calculate(
                    sessions = sessions,
                    today = todayProvider()
                )
                _uiState.update { it.copy(stats = summary) }
            }
        }
    }

    fun selectTask(taskId: Long?) {
        if (taskId == null) {
            coordinator.selectTask(null)
            _uiState.update { it.copy(task = null) }
            return
        }
        val fromList = _uiState.value.activeTasks.firstOrNull { it.id == taskId }
        if (fromList != null) {
            coordinator.selectTask(fromList)
            _uiState.update { it.copy(task = fromList) }
        } else {
            viewModelScope.launch {
                val task = repository.getTask(taskId)
                coordinator.selectTask(task)
                _uiState.update { it.copy(task = task) }
            }
        }
    }

    fun switchMode(mode: PomodoroMode) {
        coordinator.switchMode(mode)
        syncFromCoordinator(coordinator.state.value)
    }

    fun startTimer() {
        coordinator.startTimer()
        syncFromCoordinator(coordinator.state.value)
    }

    fun skipSession() {
        coordinator.skipSession()
        syncFromCoordinator(coordinator.state.value)
    }

    fun pauseTimer() {
        coordinator.pauseTimer()
        syncFromCoordinator(coordinator.state.value)
    }

    fun toggleTimer() {
        coordinator.toggleTimer()
        syncFromCoordinator(coordinator.state.value)
    }

    fun resetTimer() {
        coordinator.resetTimer()
        syncFromCoordinator(coordinator.state.value)
    }

    fun setDuration(minutes: Long) {
        coordinator.setDuration(minutes)
        syncFromCoordinator(coordinator.state.value)
    }

    fun addMinutes(minutes: Long) {
        coordinator.addMinutes(minutes)
        syncFromCoordinator(coordinator.state.value)
    }

    fun updatePomodoroSettings(settings: PomodoroSettings) {
        coordinator.updatePomodoroSettings(settings)
        syncFromCoordinator(coordinator.state.value)
    }

    fun completeTask(onCompleted: () -> Unit) {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            repository.updateTask(
                currentTask.copy(
                    completed = true,
                    updatedAt = nowMillisProvider()
                )
            )
            if (!_uiState.value.isTaskScopedMode) {
                coordinator.clearLinkedTaskIfMatches(currentTask.id)
                _uiState.update { it.copy(task = null) }
            }
            onCompleted()
        }
    }
}
