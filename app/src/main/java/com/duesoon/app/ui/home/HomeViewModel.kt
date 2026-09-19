package com.duesoon.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.DeadlineState
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.util.DeadlineStateCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.duesoon.app.domain.util.CalendarDensityCalculator
import com.duesoon.app.ui.home.calendar.CalendarUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

import com.duesoon.app.core.sync.SyncScheduler

private data class FilterState(
    val status: TaskStatusFilter,
    val category: String?,
    val query: String,
    val sort: SortOrder,
    val date: LocalDate?
)

class HomeViewModel(
    private val repository: TaskRepository,
    private val syncScheduler: SyncScheduler? = null
) : ViewModel() {

    private val _statusFilter = MutableStateFlow(TaskStatusFilter.ALL)
    val statusFilter: StateFlow<TaskStatusFilter> = _statusFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DEADLINE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _currentDisplayedMonth = MutableStateFlow<YearMonth>(YearMonth.now(ZoneId.systemDefault()))
    private val _isCalendarExpanded = MutableStateFlow<Boolean>(false)

    val tasks: StateFlow<List<Task>> = repository.observeActiveTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        CalendarUiState(currentDisplayedMonth = YearMonth.now(ZoneId.systemDefault()))
    )

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private val filtersFlow = combine(
        _statusFilter,
        _categoryFilter,
        _searchQuery.debounce(300L),
        _sortOrder,
        _selectedDate
    ) { status, category, query, sort, date ->
        FilterState(status, category, query, sort, date)
    }

    val filteredTasks: StateFlow<List<Task>> = combine(
        tasks,
        filtersFlow
    ) { taskList, filters ->
        val currentTime = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        
        val dateFiltered = HomeFilterLogic.filterByDate(taskList, filters.date, zoneId)
        val filtered = HomeFilterLogic.filterTasks(dateFiltered, filters.status, filters.category, filters.query)
        HomeFilterLogic.sortTasks(filtered, filters.sort, currentTime)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Backward-compatibility
    val needsAttentionTasks: StateFlow<List<Task>> = filteredTasks

    val upcomingTasks: StateFlow<List<Task>> = tasks.map { list ->
        val currentTime = System.currentTimeMillis()
        list.filter { 
            !it.completed && DeadlineStateCalculator.calculate(it, currentTime) == DeadlineState.UPCOMING 
        }.sortedBy { it.deadline }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setStatusFilter(status: TaskStatusFilter) {
        _statusFilter.value = status
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = if (category.isNullOrBlank() || category.equals("Semua", ignoreCase = true)) null else category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(sort: SortOrder) {
        _sortOrder.value = sort
    }

    fun toggleTaskCompletion(task: Task, isComplete: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = isComplete, updatedAt = System.currentTimeMillis()))
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = if (_selectedDate.value == date) null else date
    }

    fun onClearDateFilter() {
        _selectedDate.value = null
    }

    fun onMonthChanged(month: YearMonth) {
        _currentDisplayedMonth.value = month
    }

    fun onToggleCalendarExpanded() {
        _isCalendarExpanded.value = !_isCalendarExpanded.value
    }

    fun onJumpToToday() {
        val today = LocalDate.now(ZoneId.systemDefault())
        _currentDisplayedMonth.value = YearMonth.from(today)
    }
    
    fun triggerSync() {
        syncScheduler?.triggerExpeditedSync()
    }
}
