package com.duesoon.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.duesoon.app.R
import com.duesoon.app.domain.util.DateTimeUtils
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private enum class PickerStep {
    DATE,
    TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initialDeadline: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    var currentStep by remember { mutableStateOf(PickerStep.DATE) }

    val initialDate = remember(initialDeadline) {
        if (initialDeadline != null) {
            DateTimeUtils.toLocalDate(initialDeadline)
        } else {
            LocalDateTime.now().toLocalDate()
        }
    }

    val initialUtcMillis = remember(initialDate) {
        initialDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }

    val initialTime = remember(initialDeadline) {
        if (initialDeadline != null) {
            DateTimeUtils.toLocalTime(initialDeadline)
        } else {
            LocalDateTime.now().toLocalTime()
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtcMillis
    )

    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    when (currentStep) {
        PickerStep.DATE -> {
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        onClick = { currentStep = PickerStep.TIME },
                        enabled = datePickerState.selectedDateMillis != null
                    ) {
                        Text(stringResource(R.string.action_next))
                    }
                },
                dismissButton = {
                    Row {
                        if (initialDeadline != null) {
                            TextButton(onClick = { onConfirm(null) }) {
                                Text(stringResource(R.string.action_clear), color = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        PickerStep.TIME -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = stringResource(R.string.dialog_set_time_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(state = timePickerState)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedUtcMillis = datePickerState.selectedDateMillis ?: initialUtcMillis
                            val selectedDate = DateTimeUtils.fromDatePickerUtcMillis(selectedUtcMillis)
                            val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            val finalMillis = DateTimeUtils.toEpochMillis(selectedDate, selectedTime)
                            onConfirm(finalMillis)
                        }
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { currentStep = PickerStep.DATE }) {
                            Text(stringResource(R.string.action_back))
                        }
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            )
        }
    }
}
