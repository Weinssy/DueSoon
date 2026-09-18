package com.duesoon.app.ui.home.calendar

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class CalendarDayState(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val densityDot: DayDensityDot?
)
