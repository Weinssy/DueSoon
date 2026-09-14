package com.duesoon.app.domain.model

import androidx.compose.ui.graphics.Color

enum class CategoryPreset(val label: String, val color: Color) {
    KULIAH("Kuliah", Color(0xFF6366F1)),   // indigo
    KERJA("Kerja", Color(0xFF8B5CF6)),     // purple
    PRIBADI("Pribadi", Color(0xFF10B981)), // green
    LAINNYA("Lainnya", Color(0xFF6B7280))  // gray
}

fun String?.toCategoryPreset(): CategoryPreset? =
    CategoryPreset.entries.firstOrNull { it.label.equals(this, ignoreCase = true) }
