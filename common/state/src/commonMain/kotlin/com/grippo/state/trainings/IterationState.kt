package com.grippo.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public data class IterationState(
    val id: String,
    val volume: VolumeFormatState,
    val repetitions: RepetitionsFormatState,
)

public fun stubIteration(): IterationState = IterationState(
    id = Uuid.random().toString(),
    volume = VolumeFormatState(Random.nextInt(40, 250).toFloat()),
    repetitions = RepetitionsFormatState(Random.nextInt(2, 16))
)