package com.grippo.data.features.api.metrics.distribution.models

public enum class DistributionWeighting {
    /** Exercise records with at least one positive-repetition set; repeated records count separately. */
    Count,

    /** Sets with positive repetitions, regardless of whether their load is recorded. */
    Sets,

    /** Sum of positive repetitions, regardless of whether their load is recorded. */
    Repetitions,

    /**
     * Positive quantified effective load in kilograms multiplied by positive repetitions.
     * Effective load is external + extra + body weight × body multiplier − assistance.
     * This describes recorded load volume, not effort or training balance.
     */
    Volume,
}
