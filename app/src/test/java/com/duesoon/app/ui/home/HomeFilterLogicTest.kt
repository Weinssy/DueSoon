package com.duesoon.app.ui.home

import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeFilterLogicTest {

    private lateinit var sampleTasks: List<Task>

    @Before
    fun setUp() {
        sampleTasks = listOf(
            Task(id = 1, title = "Tugas Kuliah 1", completed = false, category = "Kuliah", priority = Priority.HIGH),
            Task(id = 2, title = "Tugas Kuliah 2", completed = true, category = "Kuliah", priority = Priority.NORMAL),
            Task(id = 3, title = "Proyek Kerja 1", completed = false, category = "Kerja", priority = Priority.HIGH),
            Task(id = 4, title = "Proyek Kerja 2", completed = true, category = "Kerja", priority = Priority.LOW),
            Task(id = 5, title = "Urusan Pribadi 1", completed = false, category = "Pribadi", priority = Priority.NORMAL),
            Task(id = 6, title = "Urusan Pribadi 2", completed = true, category = "Pribadi", priority = Priority.NORMAL),
            Task(id = 7, title = "Hal Lainnya 1", completed = false, category = "Lainnya", priority = Priority.LOW),
            Task(id = 8, title = "Hal Lainnya 2", completed = true, category = "Lainnya", priority = Priority.LOW),
            Task(id = 9, title = "Tanpa Kategori Aktif", completed = false, category = null, priority = Priority.NORMAL),
            Task(id = 10, title = "Tanpa Kategori Selesai", completed = true, category = null, priority = Priority.LOW)
        )
    }

    // ==================================================
    // A. STATUS FILTER TESTS
    // ==================================================

    @Test
    fun filterTasks_statusAll_returnsAllTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, statusFilter = TaskStatusFilter.ALL)
        assertEquals(10, result.size)
        assertEquals(sampleTasks, result)
    }

    @Test
    fun filterTasks_statusActive_returnsOnlyIncompleteTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, statusFilter = TaskStatusFilter.ACTIVE)
        assertEquals(5, result.size)
        assertTrue(result.all { !it.completed })
    }

    @Test
    fun filterTasks_statusCompleted_returnsOnlyCompletedTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, statusFilter = TaskStatusFilter.COMPLETED)
        assertEquals(5, result.size)
        assertTrue(result.all { it.completed })
    }

    // ==================================================
    // B. CATEGORY FILTER TESTS
    // ==================================================

    @Test
    fun filterTasks_categoryAll_returnsAllTasks() {
        val resultNull = HomeFilterLogic.filterTasks(sampleTasks, categoryFilter = null)
        val resultSemua = HomeFilterLogic.filterTasks(sampleTasks, categoryFilter = "Semua")

        assertEquals(10, resultNull.size)
        assertEquals(10, resultSemua.size)
    }

    @Test
    fun filterTasks_categoryKuliah_returnsOnlyKuliahTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, categoryFilter = "Kuliah")
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "Kuliah" })
    }

    @Test
    fun filterTasks_categoryKerja_returnsOnlyKerjaTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, categoryFilter = "Kerja")
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "Kerja" })
    }

    @Test
    fun filterTasks_categoryPribadi_returnsOnlyPribadiTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, categoryFilter = "Pribadi")
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "Pribadi" })
    }

    @Test
    fun filterTasks_categoryLainnya_returnsOnlyLainnyaTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, categoryFilter = "Lainnya")
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "Lainnya" })
    }

    // ==================================================
    // C. COMBINED AND LOGIC TESTS
    // ==================================================

    @Test
    fun filterTasks_activeAndKuliah_returnsOnlyActiveKuliahTasks() {
        val result = HomeFilterLogic.filterTasks(
            sampleTasks,
            statusFilter = TaskStatusFilter.ACTIVE,
            categoryFilter = "Kuliah"
        )
        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
        assertFalse(result.first().completed)
        assertEquals("Kuliah", result.first().category)
    }

    @Test
    fun filterTasks_completedAndKuliah_returnsOnlyCompletedKuliahTasks() {
        val result = HomeFilterLogic.filterTasks(
            sampleTasks,
            statusFilter = TaskStatusFilter.COMPLETED,
            categoryFilter = "Kuliah"
        )
        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
        assertTrue(result.first().completed)
        assertEquals("Kuliah", result.first().category)
    }

    @Test
    fun filterTasks_activeAndKerja_returnsOnlyActiveKerjaTasks() {
        val result = HomeFilterLogic.filterTasks(
            sampleTasks,
            statusFilter = TaskStatusFilter.ACTIVE,
            categoryFilter = "Kerja"
        )
        assertEquals(1, result.size)
        assertEquals(3L, result.first().id)
        assertFalse(result.first().completed)
        assertEquals("Kerja", result.first().category)
    }

    @Test
    fun filterTasks_completedAndPribadi_returnsOnlyCompletedPribadiTasks() {
        val result = HomeFilterLogic.filterTasks(
            sampleTasks,
            statusFilter = TaskStatusFilter.COMPLETED,
            categoryFilter = "Pribadi"
        )
        assertEquals(1, result.size)
        assertEquals(6L, result.first().id)
        assertTrue(result.first().completed)
        assertEquals("Pribadi", result.first().category)
    }

    @Test
    fun filterTasks_completedAndAllCategory_returnsAllCompletedTasks() {
        val result = HomeFilterLogic.filterTasks(
            sampleTasks,
            statusFilter = TaskStatusFilter.COMPLETED,
            categoryFilter = "Semua"
        )
        assertEquals(5, result.size)
        assertTrue(result.all { it.completed })
    }

    @Test
    fun filterTasks_allStatusAndSpecificCategory_returnsAllTasksInThatCategory() {
        val result = HomeFilterLogic.filterTasks(
            sampleTasks,
            statusFilter = TaskStatusFilter.ALL,
            categoryFilter = "Kerja"
        )
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "Kerja" })
        assertTrue(result.any { it.completed })
        assertTrue(result.any { !it.completed })
    }

    // ==================================================
    // D. NULL CATEGORY HANDLING TESTS
    // ==================================================

    @Test
    fun filterTasks_nullCategoryTask_doesNotMatchSpecificCategory() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, categoryFilter = "Kuliah")
        assertTrue(result.none { it.category == null })
    }

    @Test
    fun filterTasks_nullCategoryTask_matchesAllCategory() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, categoryFilter = "Semua")
        val nullCategoryTasks = result.filter { it.category == null }
        assertEquals(2, nullCategoryTasks.size)
    }

    // ==================================================
    // E. NO MUTATION TEST
    // ==================================================

    @Test
    fun filterTasks_doesNotMutateSourceTaskList() {
        val originalSnapshot = ArrayList(sampleTasks)

        HomeFilterLogic.filterTasks(
            sampleTasks,
            statusFilter = TaskStatusFilter.ACTIVE,
            categoryFilter = "Kuliah"
        )

        assertEquals(originalSnapshot.size, sampleTasks.size)
        assertEquals(originalSnapshot, sampleTasks)
    }
}
