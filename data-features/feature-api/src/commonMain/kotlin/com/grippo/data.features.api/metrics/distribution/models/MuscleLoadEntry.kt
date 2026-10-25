package com.grippo.data.features.api.metrics.distribution.models

import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum

public data class MuscleLoadEntry(
    val group: MuscleGroupEnum,
    val percentage: Float,
    val muscles: List<MuscleEnum>,
    val hitTrainingsCount: Int = 0,
    val primaryTrainingsCount: Int = 0,
    val avgWeightedSetsPerSession: Float = 0f,
    val maxWeightedSetsInOneSession: Float = 0f,
    val avgVolumePerHitSession: Float = 0f,
    val maxVolumeInOneSession: Float = 0f,
    val topExampleIds: List<String> = emptyList(),
)
