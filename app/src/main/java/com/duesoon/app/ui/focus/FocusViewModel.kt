package com.duesoon.app.ui.focus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.FocusRepository
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.domain.focus.FocusStatsCalculator
import com.duesoon.app.domain.focus.FocusStatsSummary
import com.duesoon.app.domain.focus.PomodoroCycleEngine
import com.duesoon.app.domain.model.FocusSession
import com.duesoon.app.domain.model.PomodoroMode
import com.duesoon.app.domain.model.PomodoroSettings
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil

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
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val initialTaskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0L }

    private val _uiState = MutableStateFlow(
        FocusUiState(isTaskScopedMode = initialTaskId != null)
    )
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var endTimestampMs: Long? = null
    private var activeSessionMinutes: Int = PomodoroSettings.DEFAULT_FOCUS_MINUTES
    private var hasCustomDurationOverride: Boolean = false

    init {
        if (initialTaskId != null) {
            loadTask(initialTaskId)
        }
        observeActiveTasks()
        observePomodoroSettings()
        observeFocusStats()
    }

    private fun loadTask(taskId: Long) {
        viewModelScope.launch {
            val task = repository.getTask(taskId)
            _uiState.update { it.copy(task = task) }
        }
    }

    private fun observeActiveTasks() {
        viewModelScope.launch {
            @Suppress("USELESS_ELVIS")
            val flow = repository.observeActiveTasks() ?: return@launch
            flow.catch { /* keep existing state on error */ }.collect { tasks ->
                val sorted = tasks.sortedWith(
                    compareBy<Task> { it.deadline == null }
                        .thenBy { it.deadline ?: Long.MAX_VALUE }
                        .thenBy { it.title.lowercase() }
                )
                _uiState.update { state ->
                    val currentId = state.task?.id
                    val refreshedTask = if (currentId != null) {
                        sorted.firstOrNull { it.id == currentId } ?: state.task
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

    private fun observePomodoroSettings() {
        val prefsRepo = userPreferencesRepository ?: return
        viewModelScope.launch {
            prefsRepo.userPreferencesFlow.catch { }.collect { prefs ->
                val newSettings = prefs.pomodoroSettings
                _uiState.update { state ->
                    val shouldRefreshIdleTimer =
                        !state.isRunning &&
                            !hasCustomDurationOverride &&
                            state.remainingSeconds == state.totalSeconds
                    if (shouldRefreshIdleTimer) {
                        val minutes = newSettings.durationMinutesFor(state.mode)
                        val seconds = minutes * 60L
                        activeSessionMinutes = minutes
                        state.copy(
                            pomodoroSettings = newSettings,
                            totalSeconds = seconds,
                            remainingSeconds = seconds
                        )
                    } else {
                        state.copy(pomodoroSettings = newSettings)
                    }
                }
            }
        }
    }

    private fun observeFocusStats() {
        val focusRepo = focusRepository ?: return
        viewModelScope.launch {
            focusRepo.observeAllSessions().catch { }.collect { sessions ->
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
            _uiState.update { it.copy(task = null) }
            return
        }
        val fromList = _uiState.value.activeTasks.firstOrNull { it.id == taskId }
        if (fromList != null) {
            _uiState.update { it.copy(task = fromList) }
        } else {
            loadTask(taskId)
        }
    }

    fun switchMode(mode: PomodoroMode) {
        timerJob?.cancel()
        endTimestampMs = null
        hasCustomDurationOverride = false
        val minutes = _uiState.value.pomodoroSettings.durationMinutesFor(mode)
        activeSessionMinutes = minutes
        val seconds = minutes * 60L
        _uiState.update {
            it.copy(
                mode = mode,
                totalSeconds = seconds,
                remainingSeconds = seconds,
                isRunning = false,
                isFinished = false
            )
        }
    }

    fun startTimer() {
        val current = _uiState.value
        if (current.isRunning || current.remainingSeconds <= 0) return

        val now = nowMillisProvider()
        endTimestampMs = now + current.remainingSeconds * 1000L
        if (current.remainingSeconds == current.totalSeconds) {
            activeSessionMinutes = (current.totalSeconds / 60L).toInt().coerceAtLeast(1)
        }

        _uiState.update { it.copy(isRunning = true, isFinished = false) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(250L)
                val targetEnd = endTimestampMs ?: break
                val remainingMs = targetEnd - nowMillisProvider()
                if (remainingMs <= 0L) {
                    onPhaseCompleted()
                    break
                } else {
                    val newRemainingSec = ceil(remainingMs / 1000.0).toLong().coerceAtLeast(1L)
                    if (newRemainingSec != _uiState.value.remainingSeconds) {
                        _uiState.update { it.copy(remainingSeconds = newRemainingSec) }
                    }
                }
            }
        }
    }

    private fun onPhaseCompleted() {
        timerJob?.cancel()
        endTimestampMs = null
        hasCustomDurationOverride = false

        val snapshot = _uiState.value
        val transition = PomodoroCycleEngine.advanceAfterCompletion(
            currentMode = snapshot.mode,
            currentCycleCount = snapshot.cycleCount,
            cycleTarget = snapshot.cycleTarget
        )

        if (transition.shouldRecordFocusSession) {
            val completedTask = snapshot.task
            val durationMinutes = activeSessionMinutes.coerceAtLeast(1)
            val completedAt = nowMillisProvider()
            val dateKey = FocusStatsCalculator.dateKeyOf(todayProvider())
            viewModelScope.launch {
                focusRepository?.recordSession(
                    FocusSession(
                        taskId = completedTask?.id,
                        taskTitle = completedTask?.title,
                        courseOrCategory = completedTask?.category?.trim()?.takeIf { it.isNotEmpty() },
                        durationMinutes = durationMinutes,
                        dateKey = dateKey,
                        completedAt = completedAt
                    )
                )
            }
        }

        val nextMinutes = snapshot.pomodoroSettings.durationMinutesFor(transition.nextMode)
        val nextSeconds = nextMinutes * 60L
        activeSessionMinutes = nextMinutes

        val autoStart = snapshot.pomodoroSettings.autoStartNext
        _uiState.update {
            it.copy(
                mode = transition.nextMode,
                cycleCount = transition.nextCycleCount,
                totalSeconds = nextSeconds,
                remainingSeconds = nextSeconds,
                isRunning = false,
                isFinished = !autoStart,
                lastCompletedMode = snapshot.mode,
                completionEventId = it.completionEventId + 1L
            )
        }

        if (autoStart) {
            startTimer()
        }
    }

    fun skipSession() {
        timerJob?.cancel()
        endTimestampMs = null
        hasCustomDurationOverride = false

        val snapshot = _uiState.value
        val transition = PomodoroCycleEngine.advanceOnSkip(
            currentMode = snapshot.mode,
            currentCycleCount = snapshot.cycleCount,
            cycleTarget = snapshot.cycleTarget
        )
        val nextMinutes = snapshot.pomodoroSettings.durationMinutesFor(transition.nextMode)
        val nextSeconds = nextMinutes * 60L
        activeSessionMinutes = nextMinutes

        _uiState.update {
            it.copy(
                mode = transition.nextMode,
                cycleCount = transition.nextCycleCount,
                totalSeconds = nextSeconds,
                remainingSeconds = nextSeconds,
                isRunning = false,
                isFinished = false
            )
        }
    }

    fun pauseTimer() {
        val targetEnd = endTimestampMs
        if (targetEnd != null && _uiState.value.isRunning) {
            val remainingMs = (targetEnd - nowMillisProvider()).coerceAtLeast(0L)
            val remainingSec = ceil(remainingMs / 1000.0).toLong()
            _uiState.update { it.copy(remainingSeconds = remainingSec) }
        }
        timerJob?.cancel()
        endTimestampMs = null
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
        endTimestampMs = null
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
        endTimestampMs = null
        hasCustomDurationOverride = true
        val clampedMinutes = minutes.toInt().coerceIn(PomodoroSettings.MIN_MINUTES, PomodoroSettings.MAX_FOCUS_MINUTES)
        val seconds = clampedMinutes * 60L
        activeSessionMinutes = clampedMinutes
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
        val additionalSeconds = minutes * 60L
        val additionalMs = additionalSeconds * 1000L
        if (endTimestampMs != null) {
            endTimestampMs = endTimestampMs!! + additionalMs
        }
        activeSessionMinutes += minutes.toInt()
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

    fun updatePomodoroSettings(settings: PomodoroSettings) {
        val sanitized = PomodoroSettings(
            focusMinutes = PomodoroSettings.sanitizeFocusMinutes(settings.focusMinutes),
            shortBreakMinutes = PomodoroSettings.sanitizeBreakMinutes(settings.shortBreakMinutes),
            longBreakMinutes = PomodoroSettings.sanitizeBreakMinutes(settings.longBreakMinutes),
            autoStartNext = settings.autoStartNext,
            soundEnabled = settings.soundEnabled
        )
        hasCustomDurationOverride = false

        _uiState.update { state ->
            if (!state.isRunning) {
                val minutes = sanitized.durationMinutesFor(state.mode)
                val seconds = minutes * 60L
                activeSessionMinutes = minutes
                state.copy(
                    pomodoroSettings = sanitized,
                    totalSeconds = seconds,
                    remainingSeconds = seconds,
                    isFinished = false
                )
            } else {
                state.copy(pomodoroSettings = sanitized)
            }
        }

        viewModelScope.launch {
            userPreferencesRepository?.updatePomodoroSettings(sanitized)
        }
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
                _uiState.update { it.copy(task = null) }
            }
            onCompleted()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
