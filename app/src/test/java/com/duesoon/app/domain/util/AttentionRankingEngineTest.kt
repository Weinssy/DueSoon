package com.duesoon.app.domain.util

import com.duesoon.app.domain.model.AttentionTier
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AttentionRankingEngineTest {

    private val currentTime = 1000000000000L // arbitrary epoch
    private val oneHour = TimeUnit.HOURS.toMillis(1)
    private val oneDay = TimeUnit.DAYS.toMillis(1)
    private val twoDays = TimeUnit.DAYS.toMillis(2)
    private val fourDays = TimeUnit.DAYS.toMillis(4)
    private val overdueDeadline = currentTime - oneHour

    @Test
    fun `overdue tasks are always OVERDUE tier regardless of priority`() {
        val lowTask = Task(title = "Low", deadline = overdueDeadline, priority = Priority.LOW)
        val highTask = Task(title = "High", deadline = overdueDeadline, priority = Priority.HIGH)

        assertEquals(AttentionTier.OVERDUE, AttentionRankingEngine.calculateTier(lowTask, currentTime))
        assertEquals(AttentionTier.OVERDUE, AttentionRankingEngine.calculateTier(highTask, currentTime))
    }

    @Test
    fun `tasks due today rank based on priority`() {
        // Due in 1 hour (same day)
        val highTask = Task(title = "High", deadline = currentTime + oneHour, priority = Priority.HIGH)
        val normalTask = Task(title = "Normal", deadline = currentTime + oneHour, priority = Priority.NORMAL)
        val lowTask = Task(title = "Low", deadline = currentTime + oneHour, priority = Priority.LOW)

        assertEquals(AttentionTier.CRITICAL, AttentionRankingEngine.calculateTier(highTask, currentTime))
        assertEquals(AttentionTier.CRITICAL, AttentionRankingEngine.calculateTier(normalTask, currentTime))
        assertEquals(AttentionTier.HIGH, AttentionRankingEngine.calculateTier(lowTask, currentTime))
    }

    @Test
    fun `tasks due soon rank based on priority`() {
        // Due in 2 days (due soon)
        val highTask = Task(title = "High", deadline = currentTime + twoDays, priority = Priority.HIGH)
        val normalTask = Task(title = "Normal", deadline = currentTime + twoDays, priority = Priority.NORMAL)
        val lowTask = Task(title = "Low", deadline = currentTime + twoDays, priority = Priority.LOW)

        assertEquals(AttentionTier.HIGH, AttentionRankingEngine.calculateTier(highTask, currentTime))
        assertEquals(AttentionTier.ELEVATED, AttentionRankingEngine.calculateTier(normalTask, currentTime))
        assertEquals(AttentionTier.ELEVATED, AttentionRankingEngine.calculateTier(lowTask, currentTime))
    }

    @Test
    fun `upcoming tasks rank based on priority`() {
        // Due in 4 days (upcoming)
        val highTask = Task(title = "High", deadline = currentTime + fourDays, priority = Priority.HIGH)
        val normalTask = Task(title = "Normal", deadline = currentTime + fourDays, priority = Priority.NORMAL)
        val lowTask = Task(title = "Low", deadline = currentTime + fourDays, priority = Priority.LOW)

        assertEquals(AttentionTier.ELEVATED, AttentionRankingEngine.calculateTier(highTask, currentTime))
        assertEquals(AttentionTier.NORMAL, AttentionRankingEngine.calculateTier(normalTask, currentTime))
        assertEquals(AttentionTier.NORMAL, AttentionRankingEngine.calculateTier(lowTask, currentTime))
    }

    @Test
    fun `tasks with no deadline are OPTIONAL tier`() {
        val noDeadlineTask = Task(title = "No Deadline", deadline = null, priority = Priority.HIGH)
        assertEquals(AttentionTier.OPTIONAL, AttentionRankingEngine.calculateTier(noDeadlineTask, currentTime))
    }

    @Test
    fun `completed tasks are COMPLETED tier regardless of deadline`() {
        val completedTask = Task(title = "Completed", deadline = overdueDeadline, priority = Priority.HIGH, completed = true)
        assertEquals(AttentionTier.COMPLETED, AttentionRankingEngine.calculateTier(completedTask, currentTime))
    }

    @Test
    fun `deterministic sorting correctly applies tie breakers`() {
        // Two tasks with same exact tier (HIGH), same deadline
        val deadline = currentTime + oneHour
        val task1 = Task(id = 2, title = "Task 2", deadline = deadline, priority = Priority.LOW, createdAt = 200)
        val task2 = Task(id = 1, title = "Task 1", deadline = deadline, priority = Priority.LOW, createdAt = 100)

        // Tier -> Deadline -> Priority -> CreatedAt -> Id
        // Tier is HIGH for both.
        // Deadline is same.
        // Priority is same.
        // CreatedAt is 100 vs 200 (task2 should be first).
        
        val comparator = AttentionRankingEngine.getComparator(currentTime)
        val sortedList = listOf(task1, task2).sortedWith(comparator)
        
        assertEquals(task2, sortedList[0])
        assertEquals(task1, sortedList[1])
    }

    @Test
    fun `deterministic sorting correctly ranks different priorities in same tier`() {
        val deadline = currentTime + oneHour
        // DUE_TODAY + HIGH = CRITICAL
        val highTask = Task(id = 1, title = "High", deadline = deadline, priority = Priority.HIGH)
        // DUE_TODAY + NORMAL = CRITICAL
        val normalTask = Task(id = 2, title = "Normal", deadline = deadline, priority = Priority.NORMAL)

        val comparator = AttentionRankingEngine.getComparator(currentTime)
        val sortedList = listOf(normalTask, highTask).sortedWith(comparator)
        
        // Priority descending, so HIGH should come before NORMAL
        assertEquals(highTask, sortedList[0])
        assertEquals(normalTask, sortedList[1])
    }
}
