package com.grippo.training.recording.calculation

import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingMetrics

internal class TrainingMetricsCalculator(
    private val stringProvider: StringProvider
) {
    suspend fun calculateTotalMetrics(exercises: List<ExerciseState>): TrainingMetrics {
        val totalVolume = exercises.sumOf { exercise ->
            exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }
        }.toFloat()

        val totalReps = exercises.sumOf { exercise ->
            exercise.iterations.sumOf { it.repetitions.value ?: 0 }
        }

        val averageIntensity = if (exercises.isNotEmpty()) {
            exercises.mapNotNull { it.metrics.intensity.value }.let { intensities ->
                if (intensities.isNotEmpty()) intensities.average().toFloat() else 0f
            }
        } else 0f

        return TrainingMetrics(
            volume = VolumeFormatState.of(totalVolume),
            repetitions = RepetitionsFormatState.of(totalReps),
            intensity = IntensityFormatState.of(averageIntensity)
        )
    }
}