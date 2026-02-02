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
    val external: VolumeFormatState,
    val extra: VolumeFormatState,
    val assist: VolumeFormatState,
    val body: VolumeFormatState,
    val repetitions: RepetitionsFormatState,
)

public fun stubIteration(): IterationState = listOf(
    IterationState(
        id = Uuid.random().toString(),
        external = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        repetitions = RepetitionsFormatState.of(Random.nextInt(2, 16)),
        extra = VolumeFormatState.of(0f),
        body = VolumeFormatState.of(0f),
        assist = VolumeFormatState.of(0f),
    ),
    IterationState(
        id = Uuid.random().toString(),
        external = VolumeFormatState.of(0f),
        repetitions = RepetitionsFormatState.of(Random.nextInt(2, 16)),
        extra = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        body = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        assist = VolumeFormatState.of(0f),
    ),
    IterationState(
        id = Uuid.random().toString(),
        external = VolumeFormatState.of(0f),
        repetitions = RepetitionsFormatState.of(Random.nextInt(2, 16)),
        extra = VolumeFormatState.of(0f),
        body = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        assist = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
    )
).random()