package com.duesoon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.duesoon.app.domain.model.Task

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onCompleteToggle: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val alpha = if (task.completed) 0.6f else 1f
        Column(
            modifier = Modifier.padding(16.dp).alpha(alpha),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DeadlineLabel(task = task, modifier = Modifier.padding(top = 4.dp))
                }
                
                if (onCompleteToggle != null) {
                    androidx.compose.material3.IconButton(
                        onClick = { onCompleteToggle(!task.completed) }
                    ) {
                        if (task.completed) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Mark incomplete",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                                    .padding(4.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(category = task.category)
                if (!task.category.isNullOrBlank()) {
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PriorityIndicator(priority = task.priority)
                Text(
                    text = " Priority",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
