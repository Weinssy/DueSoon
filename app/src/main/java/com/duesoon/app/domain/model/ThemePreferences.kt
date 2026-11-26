package com.duesoon.app.domain.model

enum class AccentPalette(val primaryHex: Long, val displayName: String) {
    INDIGO(0xFF6366F1, "Default (Indigo)"),
    EMERALD(0xFF10B981, "Emerald"),
    AMBER(0xFFF59E0B, "Amber"),
    ROSE(0xFFF43F5E, "Rose"),
    CAMPUS_PAPER(0xFFB8432E, "Kertas & Tinta Kampus")
}

enum class VisualDensity {
    COMFORTABLE,
    COMPACT
}
