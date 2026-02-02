package com.grippo.core.state.trainings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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
) {
    @Composable
    public fun volume(): VolumeFormatState {
        return remember(externalWeight, extraWeight, bodyWeight, assistWeight) {
            val external = (externalWeight.value ?: 0f)
            val extra = (extraWeight.value ?: 0f)
            val body = (bodyWeight.value ?: 0f)
            val assist = (assistWeight.value ?: 0f)
            VolumeFormatState.of(external + extra + body - assist)
        }
    }

}

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