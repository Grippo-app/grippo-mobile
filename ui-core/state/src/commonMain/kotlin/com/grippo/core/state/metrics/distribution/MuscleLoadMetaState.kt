package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleGroupEnumState

@Immutable
public data class MuscleLoadMetaState(
    val trainingsCount: Int,
    val totalExercises: Int,
    val totalSets: Int,
    val totalRepetitions: Int,
    val totalVolume: Float,
    val dominantGroup: MuscleGroupEnumState?,
    val excludedSets: Int = 0,
    val unassignedSets: Int = 0,
    val unmeasuredVolumeSets: Int = 0,
)
