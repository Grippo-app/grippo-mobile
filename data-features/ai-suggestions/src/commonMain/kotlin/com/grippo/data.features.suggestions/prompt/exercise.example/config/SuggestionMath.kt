package com.grippo.data.features.suggestions.prompt.exercise.example.config

/**
 * Numeric thresholds used throughout selection logic (e.g., deficit epsilon,
 * minimum share to treat as significant).
 */
internal object SuggestionMath {
    const val DEFICIT_EPS = 0.1
    const val SIGNIFICANT_SHARE_THRESHOLD = 30
}
