package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.metrics.models.PerformanceMetric
import com.grippo.data.features.api.metrics.models.PerformanceTrendStatus
import com.grippo.data.features.api.training.models.Training
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

public class PerformanceTrendUseCase {

    private companion object {
        private const val PERFORMANCE_EPS: Double = 1e-2
        private const val IMPROVED_THRESHOLD: Int = 5
        private const val DECLINED_THRESHOLD: Int = -5
    }

    public fun fromTrainings(trainings: List<Training>): List<PerformanceMetric> {
        val latestTraining = trainings.maxByOrNull { it.createdAt } ?: return emptyList()
        val performance = mutableListOf<PerformanceMetric>()

        // Duration (vs average)
        run {
            val values = trainings.map { it.duration.inWholeMinutes.toDouble() }
            val current = latestTraining.duration.inWholeMinutes.toDouble()
            if (values.isNotEmpty() && current > 0) {
                val average = values.average()
                val best = values.maxOrNull() ?: current
                val delta = percentageDelta(current, average) ?: 0
                val status =
                    determinePerformanceStatus(delta = delta, current = current, best = best)
                performance += PerformanceMetric.DurationMetric(
                    deltaPercentage = delta,
                    current = latestTraining.duration,
                    average = average.minutes,
                    best = best.minutes,
                    status = status
                )
            }
        }

        // Volume (vs average)
        run {
            val currentValue = latestTraining.volume.toDouble()
            val values = trainings.map { it.volume.toDouble() }
            if (values.isNotEmpty() && currentValue > 0) {
                val average = values.average()
                val best = values.maxOrNull() ?: currentValue
                val delta = percentageDelta(currentValue, average) ?: 0
                val status = determinePerformanceStatus(
                    delta = delta,
                    current = currentValue,
                    best = best
                )
                performance += PerformanceMetric.VolumeMetric(
                    deltaPercentage = delta,
                    current = latestTraining.volume,
                    average = average.toFloat(),
                    best = best.toFloat(),
                    status = status
                )
            }
        }

        // Repetitions (vs average)
        run {
            val currentValue = latestTraining.repetitions.toDouble()
            val values = trainings.map { it.repetitions.toDouble() }
            if (values.isNotEmpty() && currentValue > 0) {
                val average = values.average()
                val best = values.maxOrNull() ?: currentValue
                val delta = percentageDelta(currentValue, average) ?: 0
                val status = determinePerformanceStatus(
                    delta = delta,
                    current = currentValue,
                    best = best
                )
                performance += PerformanceMetric.RepetitionsMetric(
                    deltaPercentage = delta,
                    current = latestTraining.repetitions,
                    average = average.roundToInt(),
                    best = best.roundToInt(),
                    status = status
                )
            }
        }

        // Intensity (vs average)
        run {
            val currentValue = latestTraining.intensity.toDouble()
            val values = trainings.map { it.intensity.toDouble() }
            if (values.isNotEmpty() && currentValue > 0) {
                val average = values.average()
                val best = values.maxOrNull() ?: currentValue
                val delta = percentageDelta(currentValue, average) ?: 0
                val status = determinePerformanceStatus(
                    delta = delta,
                    current = currentValue,
                    best = best
                )
                performance += PerformanceMetric.IntensityMetric(
                    deltaPercentage = delta,
                    current = latestTraining.intensity,
                    average = average.toFloat(),
                    best = best.toFloat(),
                    status = status
                )
            }
        }

        return performance
    }

    private fun percentageDelta(current: Double, average: Double): Int? {
        if (average == 0.0) return null
        val delta = ((current - average) / average) * 100
        return delta.roundToInt()
    }

    private fun determinePerformanceStatus(
        delta: Int,
        current: Double,
        best: Double?,
    ): PerformanceTrendStatus {
        val bestValue = best ?: current
        if (current >= bestValue - PERFORMANCE_EPS) {
            return PerformanceTrendStatus.Record
        }
        return when {
            delta >= IMPROVED_THRESHOLD -> PerformanceTrendStatus.Improved
            delta <= DECLINED_THRESHOLD -> PerformanceTrendStatus.Declined
            else -> PerformanceTrendStatus.Stable
        }
    }
}
