package com.grippo.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Immutable
@Serializable
public data class TrainingMetrics(
    val repetitions: RepetitionsFormatState,
    val intensity: IntensityFormatState,
    val volume: VolumeFormatState,
) {
    public companion object {
        // ─────────────────────────────────────────────────────────────────────────────
        // TrainingMetrics: operator +
        //  • repetitions: sum
        //  • volume     : sum (null-safe; if both null -> null)
        //  • intensity  : weighted average by repetitions (fallback to simple average)
        // Notes:
        //  - Valid + Valid -> Valid; otherwise -> Invalid but with aggregated numeric value.
        //  - Display is derived from numeric value via toString() (adjust formatting if needed).
        // ─────────────────────────────────────────────────────────────────────────────

        public operator fun TrainingMetrics.plus(other: TrainingMetrics): TrainingMetrics {
            val reps = this.repetitions.sumWith(other.repetitions)

            val vol = this.volume.sumWith(other.volume)

            val intensity = this.intensity.avgWith(
                other = other.intensity,
                wA = this.repetitions.numericOrNull(),
                wB = other.repetitions.numericOrNull()
            )

            return TrainingMetrics(
                repetitions = reps,
                intensity = intensity,
                volume = vol
            )
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Helpers: numeric extractors
        // ─────────────────────────────────────────────────────────────────────────────

        private fun RepetitionsFormatState.numericOrNull(): Int? = when (this) {
            is RepetitionsFormatState.Valid -> value
            is RepetitionsFormatState.Invalid -> value
        }

        private fun VolumeFormatState.numericOrNull(): Float? = when (this) {
            is VolumeFormatState.Valid -> value
            is VolumeFormatState.Invalid -> value
        }

        private fun IntensityFormatState.numericOrNull(): Float? = when (this) {
            is IntensityFormatState.Valid -> value
            is IntensityFormatState.Invalid -> value
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Aggregations for states
        // ─────────────────────────────────────────────────────────────────────────────

        private fun RepetitionsFormatState.sumWith(other: RepetitionsFormatState): RepetitionsFormatState {
            val a = numericOrNull()
            val b = other.numericOrNull()
            if (a == null && b == null) return RepetitionsFormatState.Invalid(
                display = "",
                value = null
            )

            val sum = (a ?: 0) + (b ?: 0)
            val bothValid =
                this is RepetitionsFormatState.Valid && other is RepetitionsFormatState.Valid
            return if (bothValid)
                RepetitionsFormatState.Valid(display = sum.toString(), value = sum)
            else
                RepetitionsFormatState.Invalid(display = sum.toString(), value = sum)
        }

        private fun VolumeFormatState.sumWith(other: VolumeFormatState): VolumeFormatState {
            val a = numericOrNull()
            val b = other.numericOrNull()
            if (a == null && b == null) return VolumeFormatState.Invalid(display = "", value = null)

            val sum = (a ?: 0f) + (b ?: 0f)
            val bothValid = this is VolumeFormatState.Valid && other is VolumeFormatState.Valid
            return if (bothValid)
                VolumeFormatState.Valid(display = sum.toString(), value = sum)
            else
                VolumeFormatState.Invalid(display = sum.toString(), value = sum)
        }

        private fun IntensityFormatState.avgWith(
            other: IntensityFormatState,
            wA: Int? = null,
            wB: Int? = null
        ): IntensityFormatState {
            val a = numericOrNull()
            val b = other.numericOrNull()
            if (a == null && b == null) return IntensityFormatState.Invalid(
                display = "",
                value = null
            )

            val avg = when {
                a != null && b != null -> {
                    val wa = (wA ?: 1).coerceAtLeast(1)
                    val wb = (wB ?: 1).coerceAtLeast(1)
                    ((a * wa) + (b * wb)) / (wa + wb).toFloat()
                }

                a != null -> a
                else -> b!!
            }

            val bothValid =
                this is IntensityFormatState.Valid && other is IntensityFormatState.Valid
            return if (bothValid)
                IntensityFormatState.Valid(display = avg.toString(), value = avg)
            else
                IntensityFormatState.Invalid(display = avg.toString(), value = avg)
        }

        // Optional: aggregate a collection of metrics
        public fun Iterable<TrainingMetrics>.sumMetrics(): TrainingMetrics? =
            fold(null as TrainingMetrics?) { acc, item -> acc?.plus(item) ?: item }
    }
}

public fun stubMetrics(): TrainingMetrics {
    return TrainingMetrics(
        volume = VolumeFormatState.of(Random.nextInt(1000, 10000).toFloat()),
        intensity = IntensityFormatState.of(Random.nextInt(20, 100).toFloat()),
        repetitions = RepetitionsFormatState.of(Random.nextInt(20, 100)),
    )
}