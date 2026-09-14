package com.duesoon.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _selectedDateMillis = MutableStateFlow<Long>(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis

    val tasksOnSelectedDate: StateFlow<List<Task>> = repository.observeTasks()
        .combine(_selectedDateMillis) { tasks, selectedDate ->
            val selectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selectedDate
            }
            val selYear = selectedCalendar.get(Calendar.YEAR)
            val selMonth = selectedCalendar.get(Calendar.MONTH)
            val selDay = selectedCalendar.get(Calendar.DAY_OF_MONTH)

            tasks.filter { task ->
                if (task.deadline == null) false
                else {
                    val taskCal = Calendar.getInstance().apply { timeInMillis = task.deadline }
                    taskCal.get(Calendar.YEAR) == selYear &&
                    taskCal.get(Calendar.MONTH) == selMonth &&
                    taskCal.get(Calendar.DAY_OF_MONTH) == selDay
                }
            }.sortedBy { it.deadline }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(millis: Long?) {
        if (millis != null) {
            _selectedDateMillis.value = millis
        }
    }

    fun toggleTaskCompletion(task: Task, isComplete: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = isComplete, updatedAt = System.currentTimeMillis()))
        }
    }
}
