package com.grippo.data.features.suggestions.prompt.exercise.example.config

/**
 * Parameters controlling day/session habit detection, such as minimum events
 * and grace windows for due/overdue classification.
 */
internal object HabitCycleConfig {
    const val GRACE_DAYS = 1
    const val GRACE_SESSIONS = 1
    const val MIN_EVENTS = 3
    const val MIN_INTERVALS = 2
    const val RECENT_WINDOW = 3
}
