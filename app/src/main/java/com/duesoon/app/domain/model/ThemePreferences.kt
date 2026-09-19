package com.duesoon.app.domain.model

enum class AccentPalette(val primaryHex: Long, val displayName: String) {
    INDIGO(0xFF6366F1, "Default (Indigo)"),
    EMERALD(0xFF10B981, "Emerald"),
    AMBER(0xFFF59E0B, "Amber"),
    ROSE(0xFFF43F5E, "Rose")
}

enum class VisualDensity {
    COMFORTABLE,
    COMPACT
}

data class UserPreferencesState(
    val theme: String = "SYSTEM",
    val notificationsEnabled: Boolean = true,
    val accentPalette: AccentPalette = AccentPalette.INDIGO,
    val visualDensity: VisualDensity = VisualDensity.COMFORTABLE,
    val hapticsEnabled: Boolean = true
)
