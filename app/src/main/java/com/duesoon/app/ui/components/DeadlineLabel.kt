package com.duesoon.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.duesoon.app.domain.model.DeadlineState
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.util.DeadlineStateCalculator
import com.duesoon.app.ui.theme.Error
import com.duesoon.app.ui.theme.Success
import com.duesoon.app.ui.theme.Warning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
        DeadlineState.NO_DEADLINE -> "No deadline"
        DeadlineState.COMPLETED -> "Completed"
        DeadlineState.OVERDUE -> {
            val dateStr = task.deadline?.let { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it)) }
            "Overdue · $dateStr"
        }
        DeadlineState.DUE_TODAY -> {
            val timeStr = task.deadline?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) }
            "Due today · $timeStr"
        }
        DeadlineState.DUE_SOON, DeadlineState.UPCOMING -> {
            val diff = (task.deadline ?: 0L) - System.currentTimeMillis()
            val daysDiff = (diff / (1000 * 60 * 60 * 24)).toInt()
            if (daysDiff > 0) {
                "Due in $daysDiff days"
            } else {
                "Due tomorrow"
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
