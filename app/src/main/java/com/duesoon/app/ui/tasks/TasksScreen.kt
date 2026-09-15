package com.duesoon.app.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duesoon.app.R
import com.duesoon.app.domain.model.Task
import com.duesoon.app.ui.AppViewModelProvider
import com.duesoon.app.ui.components.EmptyState
import com.duesoon.app.ui.components.TaskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    navigateToCreateTask: () -> Unit,
    navigateToTaskDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val groupedTasks by viewModel.groupedTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showCompleted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_tasks)) }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = navigateToCreateTask,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_task))
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            // Filters
            if (categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text(stringResource(R.string.filter_category_all)) }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category) }
                        )
                    }
                }
            }

            if (groupedTasks.all { it.value.isEmpty() }) {
                EmptyState(
                    title = stringResource(R.string.empty_tasks_title),
                    message = if (selectedCategory != null) {
                        stringResource(R.string.empty_tasks_category_message)
                    } else {
                        stringResource(R.string.empty_tasks_free_message)
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val needsAttention = groupedTasks["Needs Attention"] ?: emptyList()
                    if (needsAttention.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.section_needs_attention),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(needsAttention, key = { it.id }) { task ->
                            TaskCard(task = task, onClick = { navigateToTaskDetail(task.id) })
                        }
                    }

                    val upcoming = groupedTasks["Upcoming"] ?: emptyList()
                    if (upcoming.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.section_upcoming),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                        }
                        items(upcoming, key = { it.id }) { task ->
                            TaskCard(task = task, onClick = { navigateToTaskDetail(task.id) })
                        }
                    }

                    val completed = groupedTasks["Completed"] ?: emptyList()
                    if (completed.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCompleted = !showCompleted }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.section_completed_count, completed.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = if (showCompleted) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.cd_toggle_completed),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (showCompleted) {
                            items(completed, key = { it.id }) { task ->
                                TaskCard(task = task, onClick = { navigateToTaskDetail(task.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
