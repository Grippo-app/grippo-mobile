package com.grippo.data.features.suggestions.prompt.exercise.example.config

/**
 * Limits and heuristics used when forming candidate tiers and enforcing
 * residual fatigue safety thresholds.
 */
internal object CandidateTierConfig {
    const val MAX_CANDIDATE_COUNT = 20
    const val STRICT_UNREC_WEIGHTED_SHARE_LIMIT = 0.60
    const val RESIDUAL_DEAD_ZONE = 0.10
}
