package com.duesoon.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.duesoon.app.R
import com.duesoon.app.domain.model.DeadlineState
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.util.DateTimeUtils
import com.duesoon.app.domain.util.DeadlineStateCalculator
import com.duesoon.app.ui.theme.Error
import com.duesoon.app.ui.theme.Success
import com.duesoon.app.ui.theme.Warning
import java.util.Locale

@Composable
fun DeadlineLabel(task: Task, modifier: Modifier = Modifier) {
    val state = DeadlineStateCalculator.calculate(task)
    
    val color = when (state) {
        DeadlineState.OVERDUE -> Error
        DeadlineState.DUE_TODAY, DeadlineState.DUE_SOON -> Warning
        DeadlineState.COMPLETED -> Success
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val text = when (state) {
        DeadlineState.NO_DEADLINE -> stringResource(R.string.deadline_no_deadline)
        DeadlineState.COMPLETED -> stringResource(R.string.deadline_completed)
        DeadlineState.OVERDUE -> {
            val dateStr = task.deadline?.let { DateTimeUtils.formatDeadline(it) } ?: ""
            stringResource(R.string.deadline_overdue, dateStr)
        }
        DeadlineState.DUE_TODAY -> {
            val timeStr = task.deadline?.let {
                val localTime = DateTimeUtils.toLocalTime(it)
                String.format(Locale("id", "ID"), "%02d:%02d", localTime.hour, localTime.minute)
            } ?: ""
            stringResource(R.string.deadline_due_today, timeStr)
        }
        DeadlineState.DUE_SOON, DeadlineState.UPCOMING -> {
            val diff = (task.deadline ?: 0L) - System.currentTimeMillis()
            val daysDiff = (diff / (1000 * 60 * 60 * 24)).toInt()
            if (daysDiff > 0) {
                stringResource(R.string.deadline_due_in_days, daysDiff)
            } else {
                stringResource(R.string.deadline_due_tomorrow)
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}
