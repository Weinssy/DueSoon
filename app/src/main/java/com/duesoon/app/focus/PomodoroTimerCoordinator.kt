package com.duesoon.app.focus

import com.duesoon.app.data.repository.FocusRepository
import com.duesoon.app.data.repository.PersistedPomodoroTimerState
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.domain.focus.FocusStatsCalculator
import com.duesoon.app.domain.focus.PomodoroCycleEngine
import com.duesoon.app.domain.model.FocusSession
import com.duesoon.app.domain.model.PomodoroMode
import com.duesoon.app.domain.model.PomodoroSettings
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil

interface PomodoroServiceBridge {
    fun startForegroundTimer()
    fun updateNotification()
    fun stopForegroundTimer(keepPausedNotification: Boolean)
    fun notifyPhaseCompleted(completedMode: PomodoroMode, nextMode: PomodoroMode, soundEnabled: Boolean)
}

object NoOpPomodoroServiceBridge : PomodoroServiceBridge {
    override fun startForegroundTimer() {}
    override fun updateNotification() {}
    override fun stopForegroundTimer(keepPausedNotification: Boolean) {}
    override fun notifyPhaseCompleted(completedMode: PomodoroMode, nextMode: PomodoroMode, soundEnabled: Boolean) {}
}

data class PomodoroTimerSnapshot(
    val mode: PomodoroMode = PomodoroMode.FOCUS,
    val cycleCount: Int = 0,
    val cycleTarget: Int = PomodoroSettings.CYCLE_TARGET,
    val pomodoroSettings: PomodoroSettings = PomodoroSettings(),
    val totalSeconds: Long = PomodoroSettings.DEFAULT_FOCUS_MINUTES * 60L,
    val remainingSeconds: Long = PomodoroSettings.DEFAULT_FOCUS_MINUTES * 60L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val lastCompletedMode: PomodoroMode? = null,
    val completionEventId: Long = 0L,
    val linkedTaskId: Long? = null,
    val linkedTaskTitle: String? = null,
    val linkedTaskCategory: String? = null
)

class PomodoroTimerCoordinator(
    private val scope: CoroutineScope,
    private val taskRepository: TaskRepository? = null,
    private val focusRepository: FocusRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val serviceBridge: PomodoroServiceBridge = NoOpPomodoroServiceBridge,
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    private val _state = MutableStateFlow(PomodoroTimerSnapshot())
    val state: StateFlow<PomodoroTimerSnapshot> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var endTimestampMs: Long? = null
    private var activeSessionMinutes: Int = PomodoroSettings.DEFAULT_FOCUS_MINUTES
    private var hasCustomDurationOverride: Boolean = false

    init {
        observeSettings()
        restorePersistedTimerIfNeeded()
    }

    private fun observeSettings() {
        val prefsRepo = userPreferencesRepository ?: return
        scope.launch {
            @Suppress("USELESS_ELVIS")
            val flow = prefsRepo.userPreferencesFlow ?: return@launch
            flow.catch { }.collect { prefs ->
                val newSettings = prefs.pomodoroSettings
                _state.update { current ->
                    val shouldRefreshIdle =
                        !current.isRunning &&
                            !current.isPaused &&
                            !hasCustomDurationOverride &&
                            current.remainingSeconds == current.totalSeconds
                    if (shouldRefreshIdle) {
                        val minutes = newSettings.durationMinutesFor(current.mode)
                        val seconds = minutes * 60L
                        activeSessionMinutes = minutes
                        current.copy(
                            pomodoroSettings = newSettings,
                            totalSeconds = seconds,
                            remainingSeconds = seconds
                        )
                    } else {
                        current.copy(pomodoroSettings = newSettings)
                    }
                }
            }
        }
    }

    private fun restorePersistedTimerIfNeeded() {
        val prefsRepo = userPreferencesRepository ?: return
        scope.launch {
            @Suppress("USELESS_ELVIS")
            val persistedFlow = prefsRepo.persistedPomodoroTimerFlow ?: return@launch
            val saved = persistedFlow.catch { }.first()
            if (!saved.isRunning && saved.endTimestampMs == null && saved.remainingSeconds == saved.totalSeconds) {
                return@launch
            }
            // Do not clobber if user already started a timer before restoration finished
            if (_state.value.isRunning || endTimestampMs != null) return@launch

            val linkedTask = saved.linkedTaskId?.let { taskRepository?.getTask(it) }
            activeSessionMinutes = saved.activeSessionMinutes
            hasCustomDurationOverride = saved.hasCustomDurationOverride

            if (saved.isRunning && saved.endTimestampMs != null) {
                val now = nowMillisProvider()
                val remainingMs = saved.endTimestampMs - now
                if (remainingMs <= 0L) {
                    _state.update {
                        it.copy(
                            mode = saved.mode,
                            cycleCount = saved.cycleCount,
                            totalSeconds = saved.totalSeconds,
                            remainingSeconds = 0L,
                            isRunning = false,
                            isPaused = false,
                            linkedTaskId = linkedTask?.id ?: saved.linkedTaskId,
                            linkedTaskTitle = linkedTask?.title,
                            linkedTaskCategory = linkedTask?.category
                        )
                    }
                    onPhaseCompleted(fromBackgroundRestore = true)
                } else {
                    val remainingSec = ceil(remainingMs / 1000.0).toLong().coerceAtLeast(1L)
                    endTimestampMs = saved.endTimestampMs
                    _state.update {
                        it.copy(
                            mode = saved.mode,
                            cycleCount = saved.cycleCount,
                            totalSeconds = saved.totalSeconds,
                            remainingSeconds = remainingSec,
                            isRunning = true,
                            isPaused = false,
                            isFinished = false,
                            linkedTaskId = linkedTask?.id ?: saved.linkedTaskId,
                            linkedTaskTitle = linkedTask?.title,
                            linkedTaskCategory = linkedTask?.category
                        )
                    }
                    serviceBridge.startForegroundTimer()
                    launchTicker()
                }
            } else if (saved.remainingSeconds in 1 until saved.totalSeconds) {
                _state.update {
                    it.copy(
                        mode = saved.mode,
                        cycleCount = saved.cycleCount,
                        totalSeconds = saved.totalSeconds,
                        remainingSeconds = saved.remainingSeconds,
                        isRunning = false,
                        isPaused = true,
                        isFinished = false,
                        linkedTaskId = linkedTask?.id ?: saved.linkedTaskId,
                        linkedTaskTitle = linkedTask?.title,
                        linkedTaskCategory = linkedTask?.category
                    )
                }
            }
        }
    }

    fun selectTask(task: Task?) {
        _state.update {
            it.copy(
                linkedTaskId = task?.id,
                linkedTaskTitle = task?.title,
                linkedTaskCategory = task?.category
            )
        }
        if (_state.value.isRunning || _state.value.isPaused) {
            serviceBridge.updateNotification()
        }
        persistTimerState()
    }

    fun switchMode(mode: PomodoroMode) {
        timerJob?.cancel()
        endTimestampMs = null
        hasCustomDurationOverride = false
        val minutes = _state.value.pomodoroSettings.durationMinutesFor(mode)
        activeSessionMinutes = minutes
        val seconds = minutes * 60L
        _state.update {
            it.copy(
                mode = mode,
                totalSeconds = seconds,
                remainingSeconds = seconds,
                isRunning = false,
                isPaused = false,
                isFinished = false
            )
        }
        serviceBridge.stopForegroundTimer(keepPausedNotification = false)
        persistTimerState()
    }

    fun startTimer() {
        val current = _state.value
        if (current.isRunning || current.remainingSeconds <= 0) return

        val now = nowMillisProvider()
        endTimestampMs = now + current.remainingSeconds * 1000L
        if (current.remainingSeconds == current.totalSeconds) {
            activeSessionMinutes = (current.totalSeconds / 60L).toInt().coerceAtLeast(1)
        }

        _state.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                isFinished = false
            )
        }
        serviceBridge.startForegroundTimer()
        persistTimerState()
        launchTicker()
    }

    private fun launchTicker() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_state.value.isRunning) {
                delay(250L)
                val targetEnd = endTimestampMs ?: break
                val remainingMs = targetEnd - nowMillisProvider()
                if (remainingMs <= 0L) {
                    onPhaseCompleted(fromBackgroundRestore = false)
                    break
                } else {
                    val newRemainingSec = ceil(remainingMs / 1000.0).toLong().coerceAtLeast(1L)
                    if (newRemainingSec != _state.value.remainingSeconds) {
                        _state.update { it.copy(remainingSeconds = newRemainingSec) }
                        serviceBridge.updateNotification()
                    }
                }
            }
        }
    }

    private fun onPhaseCompleted(fromBackgroundRestore: Boolean) {
        timerJob?.cancel()
        endTimestampMs = null
        hasCustomDurationOverride = false

        val snapshot = _state.value
        val transition = PomodoroCycleEngine.advanceAfterCompletion(
            currentMode = snapshot.mode,
            currentCycleCount = snapshot.cycleCount,
            cycleTarget = snapshot.cycleTarget
        )

        if (transition.shouldRecordFocusSession) {
            val durationMinutes = activeSessionMinutes.coerceAtLeast(1)
            val completedAt = nowMillisProvider()
            val dateKey = FocusStatsCalculator.dateKeyOf(todayProvider())
            scope.launch {
                focusRepository?.recordSession(
                    FocusSession(
                        taskId = snapshot.linkedTaskId,
                        taskTitle = snapshot.linkedTaskTitle,
                        courseOrCategory = snapshot.linkedTaskCategory?.trim()?.takeIf { it.isNotEmpty() },
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

        val autoStart = snapshot.pomodoroSettings.autoStartNext && !fromBackgroundRestore
        _state.update {
            it.copy(
                mode = transition.nextMode,
                cycleCount = transition.nextCycleCount,
                totalSeconds = nextSeconds,
                remainingSeconds = nextSeconds,
                isRunning = false,
                isPaused = false,
                isFinished = !autoStart,
                lastCompletedMode = snapshot.mode,
                completionEventId = it.completionEventId + 1L
            )
        }

        serviceBridge.notifyPhaseCompleted(
            completedMode = snapshot.mode,
            nextMode = transition.nextMode,
            soundEnabled = snapshot.pomodoroSettings.soundEnabled
        )

        if (autoStart) {
            startTimer()
        } else {
            serviceBridge.stopForegroundTimer(keepPausedNotification = false)
            persistTimerState()
        }
    }

    fun skipSession() {
        timerJob?.cancel()
        endTimestampMs = null
        hasCustomDurationOverride = false

        val snapshot = _state.value
        val transition = PomodoroCycleEngine.advanceOnSkip(
            currentMode = snapshot.mode,
            currentCycleCount = snapshot.cycleCount,
            cycleTarget = snapshot.cycleTarget
        )
        val nextMinutes = snapshot.pomodoroSettings.durationMinutesFor(transition.nextMode)
        val nextSeconds = nextMinutes * 60L
        activeSessionMinutes = nextMinutes

        _state.update {
            it.copy(
                mode = transition.nextMode,
                cycleCount = transition.nextCycleCount,
                totalSeconds = nextSeconds,
                remainingSeconds = nextSeconds,
                isRunning = false,
                isPaused = false,
                isFinished = false
            )
        }
        serviceBridge.stopForegroundTimer(keepPausedNotification = false)
        persistTimerState()
    }

    fun pauseTimer() {
        val targetEnd = endTimestampMs
        if (targetEnd != null && _state.value.isRunning) {
            val remainingMs = (targetEnd - nowMillisProvider()).coerceAtLeast(0L)
            val remainingSec = ceil(remainingMs / 1000.0).toLong()
            _state.update { it.copy(remainingSeconds = remainingSec) }
        }
        timerJob?.cancel()
        endTimestampMs = null
        _state.update { it.copy(isRunning = false, isPaused = true) }
        serviceBridge.stopForegroundTimer(keepPausedNotification = true)
        persistTimerState()
    }

    fun toggleTimer() {
        if (_state.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        endTimestampMs = null
        _state.update {
            it.copy(
                remainingSeconds = it.totalSeconds,
                isRunning = false,
                isPaused = false,
                isFinished = false
            )
        }
        serviceBridge.stopForegroundTimer(keepPausedNotification = false)
        persistTimerState()
    }

    fun setDuration(minutes: Long) {
        timerJob?.cancel()
        endTimestampMs = null
        hasCustomDurationOverride = true
        val clampedMinutes = minutes.toInt().coerceIn(PomodoroSettings.MIN_MINUTES, PomodoroSettings.MAX_FOCUS_MINUTES)
        val seconds = clampedMinutes * 60L
        activeSessionMinutes = clampedMinutes
        _state.update {
            it.copy(
                totalSeconds = seconds,
                remainingSeconds = seconds,
                isRunning = false,
                isPaused = false,
                isFinished = false
            )
        }
        serviceBridge.stopForegroundTimer(keepPausedNotification = false)
        persistTimerState()
    }

    fun addMinutes(minutes: Long) {
        val additionalSeconds = minutes * 60L
        val additionalMs = additionalSeconds * 1000L
        if (endTimestampMs != null) {
            endTimestampMs = endTimestampMs!! + additionalMs
        }
        activeSessionMinutes += minutes.toInt()
        _state.update {
            val newTotal = it.totalSeconds + additionalSeconds
            val newRemaining = it.remainingSeconds + additionalSeconds
            it.copy(
                totalSeconds = newTotal,
                remainingSeconds = newRemaining,
                isFinished = false
            )
        }
        if (!_state.value.isRunning) {
            startTimer()
        } else {
            serviceBridge.updateNotification()
            persistTimerState()
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

        _state.update { state ->
            if (!state.isRunning && !state.isPaused) {
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

        scope.launch {
            userPreferencesRepository?.updatePomodoroSettings(sanitized)
        }
    }

    fun clearLinkedTaskIfMatches(taskId: Long) {
        if (_state.value.linkedTaskId == taskId) {
            selectTask(null)
        }
    }

    private fun persistTimerState() {
        val prefsRepo = userPreferencesRepository ?: return
        val snapshot = _state.value
        scope.launch {
            prefsRepo.savePersistedPomodoroTimerState(
                PersistedPomodoroTimerState(
                    mode = snapshot.mode,
                    cycleCount = snapshot.cycleCount,
                    totalSeconds = snapshot.totalSeconds,
                    remainingSeconds = snapshot.remainingSeconds,
                    endTimestampMs = endTimestampMs,
                    isRunning = snapshot.isRunning,
                    linkedTaskId = snapshot.linkedTaskId,
                    activeSessionMinutes = activeSessionMinutes,
                    hasCustomDurationOverride = hasCustomDurationOverride
                )
            )
        }
    }

    fun cancelTicker() {
        timerJob?.cancel()
    }
}
