package com.grippo.data.features.api.metrics.profile

import com.grippo.data.features.api.goal.models.Goal

/**
 * Goal-adherence snapshot produced by
 * [com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase].
 *
 * The surface is intentionally small: a headline [score], the
 * strength/hypertrophy/endurance share triplet that drives it, and the
 * concrete evidence (session count + top contributors) behind it. Everything
 * the user should be able to read without a glossary — no raw intensity
 * indices or magic thresholds leaking into the UI layer.
 *
 * All share / ratio fields are integer percentages in the 0..100 range.
 */
public data class GoalAdherence(
    val goal: Goal,
    val score: Int,

    // Pooled dimension shares (percent, 0..100).
    val strengthShare: Int,
    val hypertrophyShare: Int,
    val enduranceShare: Int,

    // Cadence + quality artifacts.
    val sessionCount: Int,
    val compoundRatio: Int,

    // Top contributors within the period.
    val topExercises: List<TopExerciseContribution>,
    val topMuscles: List<TopMuscleContribution>,
)
