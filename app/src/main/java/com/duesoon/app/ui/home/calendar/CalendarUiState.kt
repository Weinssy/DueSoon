package com.duesoon.app.ui.home.calendar

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.YearMonth

@Immutable
data class CalendarUiState(
    val selectedDate: LocalDate? = null,
    val currentDisplayedMonth: YearMonth,
    val isExpanded: Boolean = false,
    val densityMap: Map<LocalDate, DayDensityDot> = emptyMap()
)
