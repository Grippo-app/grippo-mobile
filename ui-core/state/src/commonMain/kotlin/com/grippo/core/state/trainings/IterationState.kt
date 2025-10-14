package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
@Serializable
public data class IterationState(
    val id: String,
    val volume: VolumeFormatState,
    val repetitions: RepetitionsFormatState,
)

public fun stubIteration(): IterationState = IterationState(
    id = Uuid.random().toString(),
    volume = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
    repetitions = RepetitionsFormatState.of(Random.nextInt(2, 16))
)