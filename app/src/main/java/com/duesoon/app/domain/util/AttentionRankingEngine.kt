package com.duesoon.app.domain.util

import com.duesoon.app.domain.model.AttentionTier
import com.duesoon.app.domain.model.DeadlineState
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.Task

object AttentionRankingEngine {

    /**
     * Calculates the AttentionTier for a given task based on its DeadlineState and Priority.
     */
    fun calculateTier(task: Task, currentTimeMillis: Long = System.currentTimeMillis()): AttentionTier {
        if (task.completed) return AttentionTier.COMPLETED

        val deadlineState = DeadlineStateCalculator.calculate(task, currentTimeMillis)

        if (deadlineState == DeadlineState.OVERDUE) return AttentionTier.OVERDUE
        if (deadlineState == DeadlineState.NO_DEADLINE) return AttentionTier.OPTIONAL

        return when (deadlineState) {
            DeadlineState.DUE_TODAY -> {
                when (task.priority) {
                    Priority.HIGH, Priority.NORMAL -> AttentionTier.CRITICAL
                    Priority.LOW -> AttentionTier.HIGH
                }
            }
            DeadlineState.DUE_SOON -> {
                when (task.priority) {
                    Priority.HIGH -> AttentionTier.HIGH
                    Priority.NORMAL, Priority.LOW -> AttentionTier.ELEVATED
                }
            }
            DeadlineState.UPCOMING -> {
                when (task.priority) {
                    Priority.HIGH -> AttentionTier.ELEVATED
                    Priority.NORMAL, Priority.LOW -> AttentionTier.NORMAL
                }
            }
            else -> AttentionTier.NORMAL // Fallback, though conceptually unreachable
        }
    }

    /**
     * Provides a deterministic comparator for ranking tasks.
     * Order of evaluation:
     * 1. AttentionTier
     * 2. Deadline (nulls last)
     * 3. Priority (High -> Normal -> Low)
     * 4. CreatedAt
     * 5. Task ID
     */
    fun getComparator(currentTimeMillis: Long = System.currentTimeMillis()): Comparator<Task> {
        return compareBy<Task> { calculateTier(it, currentTimeMillis).ordinal }
            .thenBy(nullsLast()) { it.deadline }
            .thenByDescending { it.priority.ordinal }
            .thenBy { it.createdAt }
            .thenBy { it.id }
    }
}
