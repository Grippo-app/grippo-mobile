package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class MuscleLoadEntryState(
    val group: MuscleGroupEnumState,
    val value: Float,
    val muscles: ImmutableList<MuscleEnumState>,
    val hitTrainingsCount: Int = 0,
    val primaryTrainingsCount: Int = 0,
    val avgStimulusPerHitSession: Float = 0f,
    val maxStimulusInOneSession: Float = 0f,
    val avgVolumePerHitSession: Float = 0f,
    val maxVolumeInOneSession: Float = 0f,
    val topExampleIds: ImmutableList<String> = persistentListOf(),
)
