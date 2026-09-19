package com.duesoon.app.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duesoon.app.R
import com.duesoon.app.domain.model.CategoryPreset
import com.duesoon.app.ui.AppViewModelProvider
import com.duesoon.app.ui.components.EmptyState
import com.duesoon.app.ui.components.TaskCard
import com.duesoon.app.ui.home.calendar.CalendarMonthView
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToCreateTask: () -> Unit,
    navigateToTaskDetail: (Long) -> Unit,
    navigateToArchive: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val statusFilter by viewModel.statusFilter.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSort by viewModel.sortOrder.collectAsState()
    val tasks by viewModel.filteredTasks.collectAsState()
    val calendarUiState by viewModel.calendarUiState.collectAsState()
    
    var isSearching by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text("DueSoon", style = MaterialTheme.typography.headlineSmall)
                    }
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = { 
                            isSearching = false
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search))
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.cd_search))
                        }
                        IconButton(onClick = { viewModel.onToggleCalendarExpanded() }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Toggle Calendar", tint = if (calendarUiState.isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = navigateToArchive) {
                            Icon(Icons.Filled.List, contentDescription = "Archive")
                        }
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_sort))
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            SortOrder.entries.forEach { sortOrder ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = stringResource(sortOrder.labelResId),
                                            color = if (currentSort == sortOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    onClick = { 
                                        viewModel.setSortOrder(sortOrder)
                                        sortMenuExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = navigateToCreateTask,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_task))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskStatusFilter.entries.forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { viewModel.setStatusFilter(status) },
                        label = { Text(stringResource(status.labelResId), style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // Category Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text(stringResource(R.string.filter_category_all), style = MaterialTheme.typography.labelMedium) }
                )
                CategoryPreset.entries.forEach { preset ->
                    val isSelected = categoryFilter == preset.label
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategoryFilter(if (isSelected) null else preset.label) },
                        label = { Text(stringResource(preset.labelResId), style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            CalendarMonthView(
                uiState = calendarUiState,
                densityMap = calendarUiState.densityMap,
                onDateSelected = viewModel::onDateSelected,
                onMonthChanged = viewModel::onMonthChanged,
                onJumpToToday = viewModel::onJumpToToday,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (calendarUiState.selectedDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    val dateFormatted = calendarUiState.selectedDate!!.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    Text("Showing tasks due: $dateFormatted", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = viewModel::onClearDateFilter) {
                        Text("Clear")
                    }
                }
            }

            if (tasks.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_home_title),
                    message = stringResource(R.string.empty_home_message),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = if (statusFilter == TaskStatusFilter.COMPLETED) {
                                stringResource(R.string.section_completed_tasks)
                            } else {
                                stringResource(R.string.section_needs_attention)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onClick = { navigateToTaskDetail(task.id) },
                            onCompleteToggle = { isComplete ->
                                viewModel.toggleTaskCompletion(task, isComplete)
                            }
                        )
                    }
                }
            }
        }
    }
}

