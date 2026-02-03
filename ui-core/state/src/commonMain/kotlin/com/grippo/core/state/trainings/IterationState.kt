package com.grippo.core.state.trainings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
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
    val bodyWeight: WeightFormatState,
    val bodyMultiplier: MultiplierFormatState,
    val repetitions: RepetitionsFormatState,
) {
    @Composable
    public fun volume(): VolumeFormatState {
        return remember(externalWeight, extraWeight, bodyWeight, assistWeight) {
            val external = (externalWeight.value ?: 0f)
            val extra = (extraWeight.value ?: 0f)
            val body = (bodyWeight.value ?: 0f)
            val assist = (assistWeight.value ?: 0f)
            val multiplier = (bodyMultiplier.value ?: 0f)
            VolumeFormatState.of(external + extra + (body * multiplier) - assist)
        }
    }
}

public fun stubIteration(): IterationState = listOf(
    IterationState(
        id = Uuid.random().toString(),
        externalWeight = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        repetitions = RepetitionsFormatState.of(Random.nextInt(2, 16)),
        extraWeight = VolumeFormatState.Empty(),
        bodyWeight = WeightFormatState.Empty(),
        assistWeight = VolumeFormatState.Empty(),
        bodyMultiplier = MultiplierFormatState.Empty()
    ),
    IterationState(
        id = Uuid.random().toString(),
        externalWeight = VolumeFormatState.Empty(),
        repetitions = RepetitionsFormatState.of(Random.nextInt(2, 16)),
        extraWeight = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        bodyWeight = WeightFormatState.of(Random.nextInt(40, 250).toFloat()),
        assistWeight = VolumeFormatState.Empty(),
        bodyMultiplier = MultiplierFormatState.of(1.2f)
    ),
    IterationState(
        id = Uuid.random().toString(),
        externalWeight = VolumeFormatState.Empty(),
        repetitions = RepetitionsFormatState.of(Random.nextInt(2, 16)),
        extraWeight = VolumeFormatState.Empty(),
        bodyWeight = WeightFormatState.of(Random.nextInt(40, 250).toFloat()),
        assistWeight = VolumeFormatState.of(Random.nextInt(40, 250).toFloat()),
        bodyMultiplier = MultiplierFormatState.of(0.43f)
    )
).random()