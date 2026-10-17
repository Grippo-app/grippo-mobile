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
    public val isPending: Boolean = externalWeight.value == null
            && extraWeight.value == null
            && assistWeight.value == null
            && bodyWeight.value == null

    public fun recordedVolume(): Float? {
        if (bodyWeight.value != null && bodyMultiplier.value == null) {
            return null
        }
        if (isPending) {
            return null
        }
        return calculatedVolume()
    }

    public fun hasComparableLoadTo(other: IterationState): Boolean {
        val current = loadBasis() ?: return false
        return current == other.loadBasis()
    }

    internal fun loadBasis(): LoadBasis? {
        recordedVolume() ?: return null
        return LoadBasis(
            external = externalWeight.value != null,
            body = bodyWeight.value != null,
            assisted = assistWeight.value != null,
        )
    }

    @Composable
    public fun volumeCalculation(): String? {
        val body = bodyWeight.value ?: return null
        bodyMultiplier.value ?: return null
        return buildString {
            append(VolumeFormatState.of(body).short())
            append(" × ")
            append(bodyMultiplier.short())
            extraWeight.value?.takeIf { it != 0f }?.let {
                append(" + ")
                append(extraWeight.short())
            }
            assistWeight.value?.takeIf { it != 0f }?.let {
                append(" − ")
                append(assistWeight.short())
            }
            externalWeight.value?.takeIf { it != 0f }?.let {
                append(" + ")
                append(externalWeight.short())
            }
        }
    }

    @Composable
    public fun volume(): VolumeFormatState {
        return remember(externalWeight, extraWeight, bodyWeight, assistWeight, bodyMultiplier) {
            VolumeFormatState.of(calculatedVolume())
        }
    }

    private fun calculatedVolume(): Float {
        val external = externalWeight.value ?: 0f
        val extra = extraWeight.value ?: 0f
        val body = bodyWeight.value ?: 0f
        val assist = assistWeight.value ?: 0f
        val multiplier = bodyMultiplier.value ?: 0f
        return external + extra + body * multiplier - assist
    }

    @Immutable
    internal data class LoadBasis(
        val external: Boolean,
        val body: Boolean,
        val assisted: Boolean,
    )
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

public fun stubPendingIteration(): IterationState = IterationState(
    id = Uuid.random().toString(),
    externalWeight = VolumeFormatState.Empty(),
    extraWeight = VolumeFormatState.Empty(),
    assistWeight = VolumeFormatState.Empty(),
    bodyWeight = WeightFormatState.Empty(),
    bodyMultiplier = MultiplierFormatState.Empty(),
    repetitions = RepetitionsFormatState.of(Random.nextInt(6, 12)),
)
