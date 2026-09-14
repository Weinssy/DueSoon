package com.duesoon.app.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duesoon.app.ui.AppViewModelProvider
import com.duesoon.app.ui.components.EmptyState
import com.duesoon.app.ui.components.TaskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navigateToTaskDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
    val tasks by viewModel.tasksOnSelectedDate.collectAsState()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        viewModel.selectDate(datePickerState.selectedDateMillis)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calendar") }) },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            if (tasks.isEmpty()) {
                EmptyState(
                    title = "Free day",
                    message = "No deadlines on this date.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task, 
                            onClick = { navigateToTaskDetail(task.id) },
                            onCompleteToggle = { isComplete ->
                                // CalendarViewModel needs a toggleTaskCompletion method
                                viewModel.toggleTaskCompletion(task, isComplete)
                            }
                        )
                    }
                }
            }
        }
    }
}
