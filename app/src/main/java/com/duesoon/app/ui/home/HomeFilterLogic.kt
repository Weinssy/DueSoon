package com.duesoon.app.ui.home

import androidx.annotation.StringRes
import com.duesoon.app.R
import com.duesoon.app.domain.model.DeadlineState
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.util.DeadlineStateCalculator

enum class TaskStatusFilter(val label: String, @StringRes val labelResId: Int) {
    ALL("Semua", R.string.filter_status_all),
    ACTIVE("Aktif", R.string.filter_status_active),
    COMPLETED("Selesai", R.string.filter_status_completed)
}

enum class SortOrder(@StringRes val labelResId: Int) {
    DEADLINE(R.string.sort_deadline),
    PRIORITY(R.string.sort_priority),
    TITLE(R.string.sort_title),
    CREATED(R.string.sort_created)
}

object HomeFilterLogic {

    fun filterTasks(
        tasks: List<Task>,
        statusFilter: TaskStatusFilter = TaskStatusFilter.ALL,
        categoryFilter: String? = null,
        searchQuery: String = ""
    ): List<Task> {
        val normalizedCategory = if (categoryFilter.isNullOrBlank() || categoryFilter.equals("Semua", ignoreCase = true)) {
            null
        } else {
            categoryFilter
        }
        val normalizedQuery = searchQuery.trim()

        return tasks.filter { task ->
            val matchesStatus = when (statusFilter) {
                TaskStatusFilter.ALL -> true
                TaskStatusFilter.ACTIVE -> !task.completed
                TaskStatusFilter.COMPLETED -> task.completed
            }

            val matchesCategory = if (normalizedCategory == null) {
                true
            } else {
                task.category?.equals(normalizedCategory, ignoreCase = true) == true
            }

            val matchesSearch = if (normalizedQuery.isBlank()) {
                true
            } else {
                task.title.contains(normalizedQuery, ignoreCase = true)
            }

            matchesStatus && matchesCategory && matchesSearch
        }
    }

    fun sortTasks(
        tasks: List<Task>,
        sortOrder: SortOrder = SortOrder.DEADLINE,
        currentTime: Long = System.currentTimeMillis()
    ): List<Task> {
        return when (sortOrder) {
            SortOrder.DEADLINE -> {
                tasks.sortedWith(compareBy<Task> {
                    val state = DeadlineStateCalculator.calculate(it, currentTime)
                    when (state) {
                        DeadlineState.OVERDUE -> 0
                        DeadlineState.DUE_TODAY -> 1
                        DeadlineState.DUE_SOON -> 2
                        DeadlineState.UPCOMING -> 3
                        DeadlineState.NO_DEADLINE -> 4
                        DeadlineState.COMPLETED -> 5
                    }
                }.thenBy(nullsLast()) { it.deadline }
                    .thenByDescending { it.priority.ordinal }
                    .thenBy { it.createdAt }
                )
            }
            SortOrder.PRIORITY -> {
                tasks.sortedWith(
                    compareByDescending<Task> { it.priority.ordinal }
                        .thenBy(nullsLast()) { it.deadline }
                        .thenBy { it.createdAt }
                )
            }
            SortOrder.TITLE -> {
                tasks.sortedWith(
                    compareBy<Task, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
                        .thenByDescending { it.priority.ordinal }
                        .thenBy { it.createdAt }
                )
            }
            SortOrder.CREATED -> {
                tasks.sortedWith(
                    compareByDescending<Task> { it.createdAt }
                        .thenBy(nullsLast()) { it.deadline }
                        .thenByDescending { it.priority.ordinal }
                )
            }
        }
    }
}
