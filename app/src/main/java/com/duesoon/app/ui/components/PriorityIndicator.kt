package com.duesoon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.duesoon.app.domain.model.Priority
import com.duesoon.app.ui.theme.Error
import com.duesoon.app.ui.theme.Success
import com.duesoon.app.ui.theme.Warning

@Composable
fun PriorityIndicator(priority: Priority, modifier: Modifier = Modifier) {
    val color = when(priority) {
        Priority.HIGH -> Error
        Priority.NORMAL -> Warning
        Priority.LOW -> Success
    }
    
    val description = "${priority.name.lowercase().replaceFirstChar { it.uppercase() }} priority"
    
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
            .semantics { contentDescription = description }
    )
}
