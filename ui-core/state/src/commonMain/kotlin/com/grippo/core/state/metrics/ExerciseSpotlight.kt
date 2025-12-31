package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.VolumeFormatState

@Immutable
public data class ExerciseSpotlight(
    val exercise: ExerciseExampleState,
    val totalVolume: VolumeFormatState,
    val sessionCount: Int,
)

public fun stubExerciseSpotlight(): ExerciseSpotlight {
    return ExerciseSpotlight(
        exercise = stubExerciseExample(),
        totalVolume = VolumeFormatState.of(1_200f),
        sessionCount = 3,
    )
}
