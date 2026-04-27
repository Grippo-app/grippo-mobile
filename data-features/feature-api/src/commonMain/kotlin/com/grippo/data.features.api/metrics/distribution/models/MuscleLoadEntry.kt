package com.grippo.data.features.api.metrics.distribution.models

import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum

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
