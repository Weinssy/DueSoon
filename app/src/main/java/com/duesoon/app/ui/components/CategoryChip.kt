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
    val preset = category.toCategoryPreset()
    val chipColor = preset?.color ?: Color(0xFF6B7280)
    val displayText = if (preset != null) stringResource(preset.labelResId) else category
    Text(
        text = displayText,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .background(
                color = chipColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
