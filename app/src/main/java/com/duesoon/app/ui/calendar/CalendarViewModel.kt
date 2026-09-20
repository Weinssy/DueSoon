package com.duesoon.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.util.CalendarDensityCalculator
import com.duesoon.app.ui.home.calendar.CalendarUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now(ZoneId.systemDefault()))
    private val _currentDisplayedMonth = MutableStateFlow<YearMonth>(YearMonth.now(ZoneId.systemDefault()))
    private val _isCalendarExpanded = MutableStateFlow<Boolean>(true)

    val tasks: StateFlow<List<Task>> = repository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarUiState: StateFlow<CalendarUiState> = combine(
        _selectedDate,
        _currentDisplayedMonth,
        _isCalendarExpanded,
        tasks
    ) { date, month, expanded, taskList ->
        val currentTime = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val densityMap = CalendarDensityCalculator.calculateDayDots(taskList, zoneId, currentTime)
        CalendarUiState(date, month, expanded, densityMap)
    }.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        CalendarUiState(
            selectedDate = LocalDate.now(ZoneId.systemDefault()),
            currentDisplayedMonth = YearMonth.now(ZoneId.systemDefault()),
            isExpanded = true
        )
    )

    val tasksOnSelectedDate: StateFlow<List<Task>> = combine(
        tasks,
        _selectedDate
    ) { taskList, date ->
        if (date == null) emptyList()
        else {
            val zoneId = ZoneId.systemDefault()
            taskList.filter { task ->
                if (task.deadline == null) false
                else {
                    val taskDate = java.time.Instant.ofEpochMilli(task.deadline).atZone(zoneId).toLocalDate()
                    taskDate == date
                }
            }.sortedBy { it.deadline }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onMonthChanged(month: YearMonth) {
        _currentDisplayedMonth.value = month
    }

    fun onJumpToToday() {
        val today = LocalDate.now(ZoneId.systemDefault())
        _currentDisplayedMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }
    
    fun onToggleCalendarExpanded() {
        _isCalendarExpanded.value = !_isCalendarExpanded.value
    }

    fun toggleTaskCompletion(task: Task, isComplete: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = isComplete, updatedAt = System.currentTimeMillis()))
        }
    }
}
