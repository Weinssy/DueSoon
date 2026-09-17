package com.duesoon.app.domain.model

/**
 * Defines the logical ranking tiers for tasks, independent of how they are filtered.
 * The order of enums defines the natural sort order (lower ordinal = higher priority).
 */
enum class AttentionTier {
    OVERDUE,
    CRITICAL,
    HIGH,
    ELEVATED,
    NORMAL,
    OPTIONAL,
    COMPLETED
}
