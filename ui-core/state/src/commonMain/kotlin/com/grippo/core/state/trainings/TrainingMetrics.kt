package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Immutable
@Serializable
public data class TrainingMetrics(
    val repetitions: RepetitionsFormatState,
    val intensity: IntensityFormatState,
    val volume: VolumeFormatState,
)

public fun stubMetrics(): TrainingMetrics {
    return TrainingMetrics(
        volume = VolumeFormatState.of(Random.nextInt(1000, 10000).toFloat()),
        intensity = IntensityFormatState.of(Random.nextInt(20, 100).toFloat()),
        repetitions = RepetitionsFormatState.of(Random.nextInt(20, 100)),
    )
}