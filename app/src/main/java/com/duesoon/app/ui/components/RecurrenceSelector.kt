package com.duesoon.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duesoon.app.R
import com.duesoon.app.domain.model.RecurrenceRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceSelector(
    isRecurring: Boolean,
    onIsRecurringChange: (Boolean) -> Unit,
    selectedRule: RecurrenceRule,
    onRuleSelected: (RecurrenceRule) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.recurrence_switch_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = if (enabled) stringResource(R.string.recurrence_switch_subtitle) else stringResource(R.string.recurrence_requires_deadline),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
            Switch(
                checked = isRecurring,
                onCheckedChange = onIsRecurringChange,
                enabled = enabled
            )
        }

        AnimatedVisibility(visible = isRecurring && enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val basicRules = listOf(RecurrenceRule.Daily, RecurrenceRule.Weekly, RecurrenceRule.Monthly)
                basicRules.forEach { rule ->
                    val label = when (rule) {
                        is RecurrenceRule.Daily -> stringResource(R.string.recurrence_daily)
                        is RecurrenceRule.Weekly -> stringResource(R.string.recurrence_weekly)
                        is RecurrenceRule.Monthly -> stringResource(R.string.recurrence_monthly)
                        else -> ""
                    }
                    FilterChip(
                        selected = selectedRule == rule,
                        onClick = { onRuleSelected(rule) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}
