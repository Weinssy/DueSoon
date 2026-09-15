package com.duesoon.app.domain.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.duesoon.app.R

enum class CategoryPreset(
    val label: String,
    val color: Color,
    @StringRes val labelResId: Int
) {
    KULIAH("Kuliah", Color(0xFF6366F1), R.string.category_kuliah),   // indigo
    KERJA("Kerja", Color(0xFF8B5CF6), R.string.category_kerja),     // purple
    PRIBADI("Pribadi", Color(0xFF10B981), R.string.category_pribadi), // green
    LAINNYA("Lainnya", Color(0xFF6B7280), R.string.category_lainnya)  // gray
}

fun String?.toCategoryPreset(): CategoryPreset? =
    CategoryPreset.entries.firstOrNull { it.label.equals(this, ignoreCase = true) }
