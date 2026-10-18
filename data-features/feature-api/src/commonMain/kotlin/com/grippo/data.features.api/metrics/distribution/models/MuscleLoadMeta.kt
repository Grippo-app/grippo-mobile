package com.grippo.data.features.api.metrics.distribution.models

import com.grippo.data.features.api.muscle.models.MuscleGroupEnum

public data class MuscleLoadMeta(
    /** Number of supplied logical sessions, including sessions without accepted sets. */
    val trainingsCount: Int,
    /** Exercise records containing at least one accepted set. */
    val totalExercises: Int,
    /** Positive-repetition sets with valid supplied components, including unmeasured resistance. */
    val totalSets: Int,
    val totalRepetitions: Int,
    /**
     * Measured effective resistance × repetitions only; required components must be recorded.
     * Known measurements remain included when muscle shares are absent.
     */
    val totalVolume: Float,
    /** A unique leading group by weighted sets; null for empty or tied distributions. */
    val dominantGroup: MuscleGroupEnum?,
    /** Sets excluded for nonpositive repetitions or invalid supplied component values. */
    val excludedSets: Int = 0,
    /** Accepted sets whose exercise anatomy is missing, invalid or has no positive muscle shares. */
    val unassignedSets: Int = 0,
    /**
     * Accepted sets omitted entirely from volume because their load requirements are unknown,
     * required fields are missing, historical body fields are unpaired, unexpected fields exist,
     * or recorded arithmetic cannot represent nonnegative physical resistance.
     */
    val unmeasuredVolumeSets: Int = 0,
)
