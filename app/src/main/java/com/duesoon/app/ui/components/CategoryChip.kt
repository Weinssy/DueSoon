package com.duesoon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duesoon.app.domain.model.toCategoryPreset

@Composable
fun CategoryChip(category: String?, modifier: Modifier = Modifier) {
    if (category.isNullOrBlank()) return
    val preset = category.trim().toCategoryPreset()
    val containerColor = preset?.color?.copy(alpha = 0.85f)
        ?: MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (preset != null) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val displayText = if (preset != null) stringResource(preset.labelResId) else category.trim()

    Text(
        text = displayText,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
