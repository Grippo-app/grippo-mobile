package com.grippo.data.features.api.metrics.distribution.models

public data class MuscleLoadDominance(
    /**
     * Share of the top-1 item in total load (0..100).
     */
    val top1SharePercent: Float,

    /**
     * Share of the top-2 items together in total load (0..100).
     */
    val top2SharePercent: Float,
)
