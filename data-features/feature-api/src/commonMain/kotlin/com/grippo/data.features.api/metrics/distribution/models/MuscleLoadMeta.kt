package com.grippo.data.features.api.metrics.distribution.models

import com.grippo.data.features.api.muscle.models.MuscleGroupEnum

public data class MuscleLoadMeta(
    val trainingsCount: Int,
    val totalExercises: Int,
    val totalSets: Int,
    val totalRepetitions: Int,
    val totalVolume: Float,
    val dominantGroup: MuscleGroupEnum?,
    val excludedSets: Int = 0,
    val unassignedSets: Int = 0,
    val unmeasuredVolumeSets: Int = 0,
)
