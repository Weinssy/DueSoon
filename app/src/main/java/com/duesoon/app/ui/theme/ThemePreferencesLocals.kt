package com.duesoon.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.duesoon.app.domain.model.VisualDensity

val LocalVisualDensity = compositionLocalOf { VisualDensity.COMFORTABLE }
val LocalHapticFeedbackEnabled = compositionLocalOf { true }
