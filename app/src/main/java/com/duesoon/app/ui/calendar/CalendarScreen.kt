package com.duesoon.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duesoon.app.R
import com.duesoon.app.ui.AppViewModelProvider
import com.duesoon.app.ui.components.EmptyState
import com.duesoon.app.ui.components.TaskCard
import com.duesoon.app.ui.home.calendar.CalendarMonthView
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navigateToTaskDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val tasks by viewModel.tasksOnSelectedDate.collectAsState()
    val calendarUiState by viewModel.calendarUiState.collectAsState()

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(stringResource(R.string.nav_calendar)) },
                actions = {
                    IconButton(onClick = { viewModel.onToggleCalendarExpanded() }) {
                        Icon(
                            Icons.Filled.DateRange, 
                            contentDescription = "Toggle Calendar", 
                            tint = if (calendarUiState.isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            ) 
        },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormatted = calendarUiState.selectedDate!!.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    Text("Showing tasks due: $dateFormatted", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (tasks.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_calendar_title),
                    message = stringResource(R.string.empty_calendar_message),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = tasks, 
                        key = { it.id },
                        contentType = { "task_item" }
                    ) { task ->
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
