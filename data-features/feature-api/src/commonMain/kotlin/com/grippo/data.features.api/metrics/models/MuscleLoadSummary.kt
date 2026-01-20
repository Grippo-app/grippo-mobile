package com.grippo.data.features.api.metrics.models

import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum

public data class MuscleLoadSummary(
    val meta: MuscleLoadMeta,

    val perGroup: MuscleLoadBreakdown,
    val perMuscle: MuscleLoadBreakdown,

    /**
     * Same breakdowns, but computed using volume (tonnage = weight * reps).
     */
    val volumePerGroup: MuscleLoadBreakdown,
    val volumePerMuscle: MuscleLoadBreakdown,

    /**
     * Dominance indicators from stimulus-based per-muscle load.
     */
    val dominance: MuscleLoadDominance,

    /**
     * Dominance indicators from stimulus-based per-group load.
     */
    val groupDominance: MuscleLoadDominance,
)

public data class MuscleLoadMeta(
    val trainingsCount: Int,
    val totalExercises: Int,
    val totalSets: Int,
    val totalRepetitions: Int,
    val totalVolume: Float,
    val dominantGroup: MuscleGroupEnum?,
)

public data class MuscleLoadBreakdown(
    val entries: List<MuscleLoadEntry>,
)

public data class MuscleLoadEntry(
    val group: MuscleGroupEnum,
    val percentage: Float,
    val muscles: List<MuscleEnum>,

    /**
     * In how many sessions this entry was "hit" (thresholded by session share).
     */
    val hitTrainingsCount: Int = 0,

    /**
     * In how many sessions this entry was the #1 by stimulus share (group-level use case).
     */
    val primaryTrainingsCount: Int = 0,

    /**
     * Average stimulus per session where it was hit.
     */
    val avgStimulusPerHitSession: Float = 0f,

    /**
     * Max stimulus in a single session.
     */
    val maxStimulusInOneSession: Float = 0f,

    /**
     * Average volume per session where it was hit.
     */
    val avgVolumePerHitSession: Float = 0f,

    /**
     * Max volume in a single session.
     */
    val maxVolumeInOneSession: Float = 0f,

    /**
     * Top exerciseExample ids contributing to this entry (stimulus-based).
     */
    val topExampleIds: List<String> = emptyList(),
)

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
