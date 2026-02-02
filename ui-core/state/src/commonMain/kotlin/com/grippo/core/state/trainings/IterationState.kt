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
    val externalWeight: VolumeFormatState,
    val extraWeight: VolumeFormatState,
    val assistWeight: VolumeFormatState,
    val bodyWeight: VolumeFormatState,
    val repetitions: RepetitionsFormatState,
)

public fun stubIteration(): IterationState = listOf(
    IterationState(
        id = Uuid.random().toString(),
        externalWeight = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        repetitions = RepetitionsFormatState.of(Random.nextInt(2, 16)),
        extraWeight = VolumeFormatState.of(0f),
        bodyWeight = VolumeFormatState.of(0f),
        assistWeight = VolumeFormatState.of(0f),
    ),
    IterationState(
        id = Uuid.random().toString(),
        externalWeight = VolumeFormatState.of(0f),
        repetitions = RepetitionsFormatState.of(Random.nextInt(2, 16)),
        extraWeight = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        bodyWeight = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        assistWeight = VolumeFormatState.of(0f),
    ),
    IterationState(
        id = Uuid.random().toString(),
        externalWeight = VolumeFormatState.of(0f),
        repetitions = RepetitionsFormatState.of(Random.nextInt(2, 16)),
        extraWeight = VolumeFormatState.of(0f),
        bodyWeight = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        assistWeight = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
    )
).random()