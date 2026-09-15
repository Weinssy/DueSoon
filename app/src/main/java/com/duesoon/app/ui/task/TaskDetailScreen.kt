package com.duesoon.app.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duesoon.app.R
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.domain.model.ReminderType
import com.duesoon.app.ui.AppViewModelProvider
import com.duesoon.app.ui.components.CategoryChip
import com.duesoon.app.ui.components.DeadlineLabel
import com.duesoon.app.ui.components.PriorityIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    navigateBack: () -> Unit,
    navigateToEdit: (Long) -> Unit,
    onTaskDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val task by viewModel.task.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val msgTaskCompleted = stringResource(R.string.msg_task_completed)
    val actionUndo = stringResource(R.string.action_undo)

    val currentTask = task

    if (showDeleteDialog && currentTask != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_task_title)) },
            text = { Text(stringResource(R.string.dialog_delete_task_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteTask(onDeleted = onTaskDeleted)
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_task_detail)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (currentTask != null && !currentTask.completed) {
                        IconButton(onClick = { navigateToEdit(currentTask.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_task))
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_task))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (currentTask == null) {
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = currentTask.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    textDecoration = if (currentTask.completed) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            DeadlineLabel(task = currentTask)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(category = currentTask.category)
                if (!currentTask.category.isNullOrBlank()) {
                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PriorityIndicator(priority = currentTask.priority)
                val priorityName = when (currentTask.priority) {
                    Priority.LOW -> stringResource(R.string.priority_low)
                    Priority.NORMAL -> stringResource(R.string.priority_normal)
                    Priority.HIGH -> stringResource(R.string.priority_high)
                }
                Text(
                    text = stringResource(R.string.label_priority_format, priorityName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val reminderName = when (currentTask.reminderType) {
                    ReminderType.SMART -> stringResource(R.string.reminder_smart)
                    ReminderType.CUSTOM -> stringResource(R.string.reminder_custom)
                    ReminderType.NONE -> stringResource(R.string.reminder_none)
                }
                Text(
                    text = stringResource(R.string.label_reminder_format, reminderName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!currentTask.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentTask.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!currentTask.completed) {
                Button(
                    onClick = {
                        viewModel.completeTask()
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = msgTaskCompleted,
                                actionLabel = actionUndo,
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoComplete()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.action_complete_task))
                }
            } else {
                Button(
                    onClick = { viewModel.undoComplete() },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.action_undo_complete))
                }
            }
        }
    }
}
