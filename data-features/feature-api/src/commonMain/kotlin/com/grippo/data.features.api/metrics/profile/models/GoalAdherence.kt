package com.grippo.data.features.api.metrics.profile.models

import com.grippo.data.features.api.goal.models.Goal

public data class GoalAdherence(
    val goal: Goal,
    val score: Int,
    val strengthShare: Int,
    val hypertrophyShare: Int,
    val enduranceShare: Int,
    val sessionCount: Int,
    val findings: List<GoalFitFinding>,
) {
    /**
     * Diagnostic thresholds for interpreting an adherence — domain-side
     * calibration, not UX styling. They define what *the project* counts
     * as "focused on the goal axis", "balanced", "third axis bleeding in"
     * etc., and exist next to [GoalAdherence] (rather than buried in any
     * single dialog) so every surface that summarises a goal — dialogs,
     * widgets, future notifications — reaches for the same numbers.
     */
    public companion object {
        /**
         * Minimum percent share on the goal's core axis to count as
         * "focused". Below this we flag the share as low.
         */
        public const val FOCUS_SHARE_TARGET: Int = 50

        /**
         * Percent share on the core axis that we praise as "great" —
         * triggers the positive variant of the focus insight.
         */
        public const val FOCUS_SHARE_STRONG: Int = 65

        /**
         * Maximum allowed share on the third (suppressed) axis for
         * MAINTAIN / GENERAL_FITNESS goals. Above this the suppressed
         * dimension is leaking into the goal pattern.
         */
        public const val SUPPRESSED_AXIS_LIMIT: Int = 30

        /**
         * Tolerance for `|a − b|` between the two balanced axes
         * (strength / hypertrophy for MAINTAIN; strength / endurance for
         * GENERAL_FITNESS). Above this delta the user is drifting toward
         * one axis.
         */
        public const val BALANCE_DELTA_LIMIT: Int = 25

        /**
         * Period day count below which there isn't enough data for a
         * meaningful diagnostic — surfaces emit a "too early" timeline
         * note instead of share-based verdicts.
         */
        public const val EARLY_DAYS: Int = 7

        /**
         * Period progress fraction at or above which we say "almost done"
         * (timeline framing — applies independently of score).
         */
        public const val ALMOST_DONE_FRACTION: Float = 0.85f
    }
}
