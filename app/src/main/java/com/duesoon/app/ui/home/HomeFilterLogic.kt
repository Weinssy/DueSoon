package com.duesoon.app.ui.home

import androidx.annotation.StringRes
import com.duesoon.app.R
import com.duesoon.app.domain.model.Task

enum class TaskStatusFilter(val label: String, @StringRes val labelResId: Int) {
    ALL("Semua", R.string.filter_status_all),
    ACTIVE("Aktif", R.string.filter_status_active),
    COMPLETED("Selesai", R.string.filter_status_completed)
}

object HomeFilterLogic {

    /**
     * Pure function that filters [tasks] based on [statusFilter] and [categoryFilter].
     *
     * - [statusFilter]: ALL (all tasks), ACTIVE (completed == false), COMPLETED (completed == true)
     * - [categoryFilter]: null or "Semua" (all categories), or a specific category name (case-insensitive)
     *
     * Returns a new filtered list without mutating [tasks].
     */
    fun filterTasks(
        tasks: List<Task>,
        statusFilter: TaskStatusFilter = TaskStatusFilter.ALL,
        categoryFilter: String? = null
    ): List<Task> {
        val normalizedCategory = if (categoryFilter.isNullOrBlank() || categoryFilter.equals("Semua", ignoreCase = true)) {
            null
        } else {
            categoryFilter
        }

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

            matchesStatus && matchesCategory
        }
    }
}
