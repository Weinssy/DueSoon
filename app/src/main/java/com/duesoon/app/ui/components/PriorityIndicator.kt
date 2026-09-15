package com.duesoon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.duesoon.app.R
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
    
    val description = when(priority) {
        Priority.HIGH -> stringResource(R.string.priority_high_cd)
        Priority.NORMAL -> stringResource(R.string.priority_normal_cd)
        Priority.LOW -> stringResource(R.string.priority_low_cd)
    }
    
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
            .semantics { contentDescription = description }
    )
}
