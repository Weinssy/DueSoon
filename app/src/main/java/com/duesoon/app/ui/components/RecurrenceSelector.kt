package com.duesoon.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duesoon.app.domain.model.RecurrenceInterval

@Composable
fun RecurrenceSelector(
    isRecurring: Boolean,
    onIsRecurringChange: (Boolean) -> Unit,
    selectedInterval: RecurrenceInterval,
    onIntervalSelected: (RecurrenceInterval) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ulangi Deadline",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Buat deadline baru otomatis saat selesai",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isRecurring,
                onCheckedChange = onIsRecurringChange
            )
        }

        AnimatedVisibility(visible = isRecurring) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecurrenceInterval.entries.forEach { interval ->
                    FilterChip(
                        selected = selectedInterval == interval,
                        onClick = { onIntervalSelected(interval) },
                        label = { Text(interval.label) }
                    )
                }
            }
        }
    }
}
