package com.grippo.data.features.suggestions.prompt.exercise.example

import kotlinx.datetime.TimeZone

internal object TrainingHistoryConfig {
    const val LOOKBACK_DAYS = 90
    const val RECENT_TRAININGS_LIMIT = 16
}

internal object CandidateTierConfig {
    const val MAX_CANDIDATE_COUNT = 20
    const val STRICT_UNREC_WEIGHTED_SHARE_LIMIT = 0.60
    const val RESIDUAL_DEAD_ZONE = 0.10
}

internal object HabitCycleConfig {
    const val GRACE_DAYS = 1
    const val GRACE_SESSIONS = 1
    const val MIN_EVENTS = 3
    const val MIN_INTERVALS = 2
    const val RECENT_WINDOW = 3
}

internal object SuggestionMath {
    const val DEFICIT_EPS = 0.1
    const val SIGNIFICANT_SHARE_THRESHOLD = 30
}

internal object PromptGuidelineConfig {
    const val ANTI_MONOTONY_HOURS = 48
}

internal val USER_TIME_ZONE: TimeZone = TimeZone.currentSystemDefault()
