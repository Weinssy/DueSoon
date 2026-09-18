package com.duesoon.app.ui.home.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarMonthView(
    uiState: CalendarUiState,
    densityMap: Map<LocalDate, DayDensityDot>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onJumpToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysOfWeek = remember {
        listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        )
    }

    AnimatedVisibility(visible = uiState.isExpanded) {
        Column(modifier = modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.currentDisplayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${uiState.currentDisplayedMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onJumpToToday) {
                        Text("Today")
                    }
                    IconButton(onClick = { onMonthChanged(uiState.currentDisplayedMonth.minusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                    }
                    IconButton(onClick = { onMonthChanged(uiState.currentDisplayedMonth.plusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                    }
                }
            }

            // Days of week header
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in daysOfWeek) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Calendar Grid
            val firstDayOfMonth = uiState.currentDisplayedMonth.atDay(1)
            val firstDayOfWeekIndex = daysOfWeek.indexOf(firstDayOfMonth.dayOfWeek)
            val daysInMonth = uiState.currentDisplayedMonth.lengthOfMonth()
            
            // Backfill previous month
            val prevMonth = uiState.currentDisplayedMonth.minusMonths(1)
            val daysInPrevMonth = prevMonth.lengthOfMonth()
            
            var currentDay = 1
            var nextMonthDay = 1

            val today = LocalDate.now()

            Column(modifier = Modifier.fillMaxWidth()) {
                for (week in 0 until 6) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (dayOfWeekIndex in 0 until 7) {
                            val state = if (week == 0 && dayOfWeekIndex < firstDayOfWeekIndex) {
                                // Previous month days
                                val date = prevMonth.atDay(daysInPrevMonth - firstDayOfWeekIndex + dayOfWeekIndex + 1)
                                CalendarDayState(
                                    date = date,
                                    isCurrentMonth = false,
                                    isToday = date == today,
                                    isSelected = date == uiState.selectedDate,
                                    densityDot = densityMap[date]
                                )
                            } else if (currentDay <= daysInMonth) {
                                // Current month days
                                val date = uiState.currentDisplayedMonth.atDay(currentDay)
                                currentDay++
                                CalendarDayState(
                                    date = date,
                                    isCurrentMonth = true,
                                    isToday = date == today,
                                    isSelected = date == uiState.selectedDate,
                                    densityDot = densityMap[date]
                                )
                            } else {
                                // Next month days
                                val date = uiState.currentDisplayedMonth.plusMonths(1).atDay(nextMonthDay)
                                nextMonthDay++
                                CalendarDayState(
                                    date = date,
                                    isCurrentMonth = false,
                                    isToday = date == today,
                                    isSelected = date == uiState.selectedDate,
                                    densityDot = densityMap[date]
                                )
                            }

                            CalendarDayCell(
                                state = state,
                                onClick = onDateSelected,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (currentDay > daysInMonth) break
                }
            }
        }
    }
}
