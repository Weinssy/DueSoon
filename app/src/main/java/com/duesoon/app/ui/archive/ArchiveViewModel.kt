package com.duesoon.app.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.usecase.RestoreTaskUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ArchiveViewModel(
    private val repository: TaskRepository,
    private val restoreTaskUseCase: RestoreTaskUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val archivedTasks: StateFlow<List<Task>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.observeArchivedTasks()
            } else {
                repository.searchArchivedTasks(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun restoreTask(taskId: Long) {
        viewModelScope.launch {
            restoreTaskUseCase(taskId)
        }
    }

    fun clearArchive() {
        viewModelScope.launch {
            repository.clearArchive()
        }
    }
}
