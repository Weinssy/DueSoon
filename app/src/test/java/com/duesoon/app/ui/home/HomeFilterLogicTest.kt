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
    private lateinit var sortTasksList: List<Task>

    @Before
    fun setUp() {
        sampleTasks = listOf(
            Task(id = 1, title = "Tugas Kuliah 1", completed = false, category = "Kuliah", priority = Priority.HIGH),
            Task(id = 2, title = "Tugas Kuliah 2", completed = true, category = "Kuliah", priority = Priority.NORMAL),
            Task(id = 3, title = "Proyek Kerja 1", completed = false, category = "Kerja", priority = Priority.HIGH),
            Task(id = 4, title = "Proyek Kerja 2", completed = true, category = "Kerja", priority = Priority.LOW),
            Task(id = 5, title = "Urusan Pribadi 1 !@#", completed = false, category = "Pribadi", priority = Priority.NORMAL),
            Task(id = 6, title = "Urusan Pribadi 2", completed = true, category = "Pribadi", priority = Priority.NORMAL),
            Task(id = 7, title = "Hal Lainnya 1", completed = false, category = "Lainnya", priority = Priority.LOW),
            Task(id = 8, title = "Hal Lainnya 2", completed = true, category = "Lainnya", priority = Priority.LOW),
            Task(id = 9, title = "Tanpa Kategori Aktif dengan judul yang sangat panjang sekali", completed = false, category = null, priority = Priority.NORMAL),
            Task(id = 10, title = "Tanpa Kategori Selesai", completed = true, category = null, priority = Priority.LOW)
        )

        sortTasksList = listOf(
            Task(id = 1, title = "C Task", priority = Priority.LOW, createdAt = 100, deadline = 1000, completed = false),
            Task(id = 2, title = "A Task", priority = Priority.HIGH, createdAt = 200, deadline = null, completed = false),
            Task(id = 3, title = "B Task", priority = Priority.NORMAL, createdAt = 300, deadline = 500, completed = false),
            Task(id = 4, title = "D Task", priority = Priority.HIGH, createdAt = 400, deadline = 2000, completed = true)
        )
    }

    // ==================================================
    // FILTER TESTS (Truncated for brevity in thought, but included fully here)
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

    // SEARCH TESTS

    @Test
    fun filterTasks_searchBlank_returnsAllTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, searchQuery = "   ")
        assertEquals(10, result.size)
    }

    @Test
    fun filterTasks_searchExactMatch_returnsMatchedTask() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, searchQuery = "Tugas Kuliah 1")
        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test
    fun filterTasks_searchPartialMatch_returnsMatchedTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, searchQuery = "Kuliah")
        assertEquals(2, result.size)
    }

    @Test
    fun filterTasks_searchCaseInsensitive_returnsMatchedTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, searchQuery = "pRoYeK")
        assertEquals(2, result.size)
        assertTrue(result.all { it.title.contains("Proyek") })
    }

    @Test
    fun filterTasks_searchNoMatch_returnsEmptyList() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, searchQuery = "Tugas yang tidak ada")
        assertTrue(result.isEmpty())
    }

    @Test
    fun filterTasks_searchWithActiveStatus_returnsMatchingActiveTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, statusFilter = TaskStatusFilter.ACTIVE, searchQuery = "Kuliah")
        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test
    fun filterTasks_searchWithCompletedStatus_returnsMatchingCompletedTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, statusFilter = TaskStatusFilter.COMPLETED, searchQuery = "Kuliah")
        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
    }

    @Test
    fun filterTasks_searchWithCategory_returnsMatchingTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, categoryFilter = "Kerja", searchQuery = "Proyek")
        assertEquals(2, result.size)
    }

    @Test
    fun filterTasks_searchWithStatusAndCategory_returnsMatchingTasks() {
        val result = HomeFilterLogic.filterTasks(
            sampleTasks, 
            statusFilter = TaskStatusFilter.ACTIVE, 
            categoryFilter = "Kerja", 
            searchQuery = "Proyek"
        )
        assertEquals(1, result.size)
        assertEquals(3L, result.first().id)
    }

    @Test
    fun filterTasks_searchSpecialCharacters_returnsMatchedTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, searchQuery = "!@#")
        assertEquals(1, result.size)
        assertEquals(5L, result.first().id)
    }

    @Test
    fun filterTasks_searchLongTitle_returnsMatchedTasks() {
        val result = HomeFilterLogic.filterTasks(sampleTasks, searchQuery = "panjang sekali")
        assertEquals(1, result.size)
        assertEquals(9L, result.first().id)
    }
    
    // ==================================================
    // SORTING TESTS
    // ==================================================

    @Test
    fun sortTasks_deadline_sortsCorrectly() {
        // currentTime = 600, so:
        // Task 1 (deadline 1000): DUE_SOON/UPCOMING
        // Task 3 (deadline 500): OVERDUE
        // Task 2 (deadline null): NO_DEADLINE
        // Task 4 (completed): COMPLETED
        val sorted = HomeFilterLogic.sortTasks(sortTasksList, SortOrder.DEADLINE, currentTime = 600)
        
        assertEquals(3L, sorted[0].id) // OVERDUE
        assertEquals(1L, sorted[1].id) // UPCOMING
        assertEquals(2L, sorted[2].id) // NO DEADLINE
        assertEquals(4L, sorted[3].id) // COMPLETED
    }

    @Test
    fun sortTasks_priority_sortsHighToLow() {
        val sorted = HomeFilterLogic.sortTasks(sortTasksList, SortOrder.PRIORITY)
        
        assertEquals(4L, sorted[0].id) // HIGH
        assertEquals(2L, sorted[1].id) // HIGH
        assertEquals(3L, sorted[2].id) // NORMAL
        assertEquals(1L, sorted[3].id) // LOW
    }

    @Test
    fun sortTasks_title_sortsAToZ() {
        val sorted = HomeFilterLogic.sortTasks(sortTasksList, SortOrder.TITLE)
        
        assertEquals(2L, sorted[0].id) // A Task
        assertEquals(3L, sorted[1].id) // B Task
        assertEquals(1L, sorted[2].id) // C Task
        assertEquals(4L, sorted[3].id) // D Task
    }
    
    @Test
    fun sortTasks_created_sortsNewestFirst() {
        val sorted = HomeFilterLogic.sortTasks(sortTasksList, SortOrder.CREATED)
        
        assertEquals(4L, sorted[0].id) // createdAt 400
        assertEquals(3L, sorted[1].id) // createdAt 300
        assertEquals(2L, sorted[2].id) // createdAt 200
        assertEquals(1L, sorted[3].id) // createdAt 100
    }
}

