package com.grippo.data.features.api.metrics.profile.models

import com.grippo.data.features.api.goal.models.Goal
import com.grippo.toolkit.date.utils.DateRange
import kotlin.math.roundToInt

/**
 * A heuristic assessment of available exercise details, not progress toward a physical outcome.
 * [score] averages the shown findings (PASS = 100, WARN = 50, FAIL = 0), with equal
 * weight per rule. Group-specific findings share one rule's weight. No evaluable
 * findings means no score. Rep shares describe set counts in 1–5, 6–15 and 16+
 * buckets; they do not measure stimulus, effort, cardio or fat loss.
 */
public data class GoalAdherence(
    val goal: Goal,
    val strengthShare: Int,
    val hypertrophyShare: Int,
    val enduranceShare: Int,
    val sessionCount: Int,
    val findings: List<GoalFitFinding>,
    val analysisRange: DateRange?,
    val setCount: Int,
    val excludedSetCount: Int,
    val missingMetadataSetCount: Int,
) {
    public val score: Int?
        get() {
            if (findings.isEmpty()) {
                return null
            }
            return findings.groupBy { it.rule }.values.map { rows ->
                rows.map { finding ->
                    when (finding.severity) {
                        GoalFitSeverity.PASS -> 100
                        GoalFitSeverity.WARN -> 50
                        GoalFitSeverity.FAIL -> 0
                    }
                }.average()
            }.average().roundToInt()
        }

    public companion object {
        public const val MIN_ANALYSIS_DAYS: Int = 7
        public const val ALMOST_DONE_FRACTION: Float = 0.85f
    }
}
