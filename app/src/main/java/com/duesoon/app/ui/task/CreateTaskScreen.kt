package com.duesoon.app.ui.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duesoon.app.R
import com.duesoon.app.domain.util.DateTimeUtils
import com.duesoon.app.ui.AppViewModelProvider
import com.duesoon.app.ui.components.CategorySelector
import com.duesoon.app.ui.components.DateTimePickerDialog
import com.duesoon.app.ui.components.RecurrenceSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    navigateBack: () -> Unit,
    onTaskSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateTaskViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDateTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onTaskSaved()
        }
    }

    if (showDateTimePicker) {
        DateTimePickerDialog(
            initialDeadline = uiState.deadline,
            onDismiss = { showDateTimePicker = false },
            onConfirm = { newDeadline ->
                viewModel.updateDeadline(newDeadline)
                showDateTimePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_create_task)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.label_title)) },
                isError = uiState.titleError != null,
                supportingText = {
                    if (uiState.titleError != null) Text(stringResource(R.string.error_title_empty))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text(stringResource(R.string.label_description_optional)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Box(modifier = Modifier.fillMaxWidth().clickable { showDateTimePicker = true }) {
                OutlinedTextField(
                    value = DateTimeUtils.formatDeadline(uiState.deadline) ?: stringResource(R.string.deadline_no_deadline),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text(stringResource(R.string.label_deadline)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            CategorySelector(
                selectedCategory = uiState.category,
                onCategorySelected = viewModel::updateCategory,
                modifier = Modifier.fillMaxWidth()
            )

            RecurrenceSelector(
                isRecurring = uiState.isRecurring,
                onIsRecurringChange = viewModel::updateIsRecurring,
                selectedInterval = uiState.recurrenceInterval,
                onIntervalSelected = viewModel::updateRecurrenceInterval,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::saveTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.action_save_task))
            }
        }
    }
}
