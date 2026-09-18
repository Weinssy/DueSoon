package com.duesoon.app.ui.home.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CalendarDayCell(
    state: CalendarDayState,
    onClick: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        state.isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    
    val textColor = when {
        state.isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        state.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val fontWeight = when {
        state.isSelected || state.isToday -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .run {
                if (state.isToday && !state.isSelected) {
                    border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                } else {
                    this
                }
            }
            .clickable(onClick = { onClick(state.date) })
            .alpha(if (state.isCurrentMonth) 1f else 0.4f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.date.dayOfMonth.toString(),
                color = textColor,
                fontWeight = fontWeight,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        color = when (state.densityDot) {
                            DayDensityDot.CRITICAL -> MaterialTheme.colorScheme.error
                            DayDensityDot.WARNING -> Color(0xFFF57C00) // Orange
                            DayDensityDot.MUTED -> MaterialTheme.colorScheme.outlineVariant
                            null -> Color.Transparent
                        }
                    )
            )
        }
    }
}
