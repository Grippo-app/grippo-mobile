package com.grippo.data.features.api.metrics.internal

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleBundle
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.metrics.internal.MuscleBundleMath.PUSH_TINY_SHARE_FACTOR
import com.grippo.data.features.api.metrics.internal.MuscleBundleMath.PUSH_TINY_SHARE_MAX_PERCENT
import kotlin.math.pow

/**
 * Shared bundle-distribution constants and helpers consumed by
 * `MuscleLoadingSummaryUseCase` (per-muscle stimulus distribution) and
 * `GoalFollowingUseCase` (per-group set-credit accounting). Keeping the
 * source of truth here prevents the two pipelines from drifting on
 * domain-tuned magic numbers — the damping exponent and the PUSH-tiny-share
 * suppression rule used to live as private companion constants in each file
 * with the (silent) requirement that they stay equal.
 *
 * NOTE — only the *bundle distribution* primitives are exposed here. The
 * fatigue model (capacity / sensitivity from `Muscle.size` / `Muscle.sensitivity`),
 * exercise complexity factor (ENM-based), and category / weight-type bonuses
 * intentionally remain private to `MuscleLoadingSummaryUseCase`: they are
 * stimulus-distribution machinery that has no role in discrete set-credit
 * accounting downstream.
 */
internal object MuscleBundleMath {

    private const val EPS: Float = 1e-3f

    /**
     * Power-law exponent applied to bundle ratios before further processing.
     * With α=1.6 a 30/15/15 bundle splits roughly 3:1:1 instead of the linear
     * 2:1:1 — primary movers gain non-linearly over incidental synergists.
     */
    internal const val SHARE_DAMPING_ALPHA: Float = 1.6f

    /**
     * PUSH-only — bundles whose **raw** share (in percent) is at or below this
     * cutoff are treated as incidental cross-credit and attenuated by
     * [PUSH_TINY_SHARE_FACTOR] before downstream aggregation. This is the
     * "a 4% chest bundle on a triceps extension shouldn't claim a chest
     * synergist set" rule.
     */
    internal const val PUSH_TINY_SHARE_MAX_PERCENT: Float = 4f

    /** Multiplier applied to a tiny PUSH bundle share before aggregation. */
    internal const val PUSH_TINY_SHARE_FACTOR: Float = 0.35f

    /**
     * Returns power-law-damped, re-normalized bundle ratios summing to 1.
     * Returns `null` when the bundle list has no positive shares — callers
     * must omit the contribution rather than infer a zero distribution.
     */
    internal fun dampedBundleRatios(bundles: List<ExerciseExampleBundle>): FloatArray? {
        val totalShare = bundles.sumOf { it.percentage.coerceAtLeast(0) }
        if (totalShare <= 0) return null
        val denom = totalShare.toFloat()

        val weights = FloatArray(bundles.size)
        var weightsSum = 0f
        for (i in bundles.indices) {
            val share = bundles[i].percentage.coerceAtLeast(0).toFloat()
            val ratio = if (share > 0f) share / denom else 0f
            val w = ratio.toDouble().pow(SHARE_DAMPING_ALPHA.toDouble()).toFloat()
            val safe = if (w.isFinite() && w >= 0f) w else 0f
            weights[i] = safe
            weightsSum += safe
        }
        if (!weightsSum.isFinite() || weightsSum <= EPS) return null

        for (i in weights.indices) weights[i] = weights[i] / weightsSum
        return weights
    }

    /**
     * PUSH-tiny-share penalty for a single bundle's share, expressed as a
     * percent of the bundle pool (i.e. normalized so the bundles sum to
     * 100 — callers must apply this normalization before passing the value
     * in, otherwise the 4% cutoff drifts when the raw percentages don't
     * sum exactly to 100 in the database).
     *
     * Returns `1f` when no penalty applies; [PUSH_TINY_SHARE_FACTOR] when
     * the exercise is PUSH and the bundle's normalized share is at or
     * below [PUSH_TINY_SHARE_MAX_PERCENT].
     */
    internal fun pushTinySharePenalty(forceType: ForceTypeEnum?, sharePercent: Float): Float {
        if (forceType != ForceTypeEnum.PUSH) return 1f
        if (!sharePercent.isFinite() || sharePercent <= EPS) return 1f
        if (sharePercent > PUSH_TINY_SHARE_MAX_PERCENT) return 1f
        return PUSH_TINY_SHARE_FACTOR
    }
}
