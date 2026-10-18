package com.grippo.data.features.api.metrics.performance

import com.grippo.data.features.api.metrics.performance.models.PerformanceMetric
import com.grippo.data.features.api.metrics.performance.models.PerformanceTrendStatus
import com.grippo.data.features.api.training.models.Training
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

public class PerformanceTrendUseCase {

    private companion object {
        private const val MIN_COMPARISON_PERCENT: Double = 5.0
    }

    /**
     * Compares recorded nonnegative session totals, including zero, with earlier valid totals.
     * Density requires a positive duration; a zero baseline has no percentage comparison.
     */
    public fun fromTrainings(trainings: List<Training>): List<PerformanceMetric> {
        val sorted = trainings.sortedWith(compareBy<Training> { it.createdAt }.thenBy { it.id })

        return listOf(
            densityMetric(sorted),
            volumeMetric(sorted),
            repetitionsMetric(sorted),
            intensityMetric(sorted),
            durationMetricNeutral(sorted),
        )
    }

    private class MetricStats(
        val current: Double?,
        val average: Double?,
        val best: Double?,
        val currentVsAveragePercent: Double?,
        val status: PerformanceTrendStatus,
    )

    private fun computeStats(
        trainings: List<Training>,
        valueOf: (Training) -> Double?,
    ): MetricStats {
        val current = trainings.lastOrNull()?.let(valueOf)?.takeIf(::isValidMeasurement)
        val baselineValues = trainings.dropLast(1).mapNotNull { training ->
            valueOf(training)?.takeIf(::isValidMeasurement)
        }
        val average = baselineValues.takeIf { it.isNotEmpty() }?.average()
        val previousBest = baselineValues.maxOrNull()
        val best = (baselineValues + listOfNotNull(current)).maxOrNull()
        val comparisonPercent = if (current != null && average != null) {
            currentVsAveragePercent(current = current, average = average)
        } else {
            null
        }
        val status = when {
            current == null -> PerformanceTrendStatus.Empty
            previousBest == null -> PerformanceTrendStatus.Stable
            current > previousBest -> PerformanceTrendStatus.Record
            comparisonPercent == null -> PerformanceTrendStatus.Stable
            else -> statusFromComparison(
                comparisonPercent = comparisonPercent,
                thresholdPercent = dynamicThresholdPercent(baselineValues),
            )
        }

        return MetricStats(
            current = current,
            average = average,
            best = best,
            currentVsAveragePercent = comparisonPercent,
            status = status,
        )
    }

    private fun isValidMeasurement(value: Double): Boolean {
        return value.isFinite() && value >= 0.0
    }

    private fun currentVsAveragePercent(current: Double, average: Double): Double? {
        if (average == 0.0) {
            return null
        }
        return ((current - average) / average) * 100.0
    }

    private fun dynamicThresholdPercent(baselineValues: List<Double>): Double {
        if (baselineValues.size < 2) {
            return MIN_COMPARISON_PERCENT
        }

        val mean = baselineValues.average()
        val variance = baselineValues.sumOf { value ->
            val difference = value - mean
            difference * difference
        } / (baselineValues.size - 1)
        val variationPercent = 2.0 * sqrt(variance) / mean * 100.0
        return maxOf(MIN_COMPARISON_PERCENT, variationPercent)
    }

    private fun statusFromComparison(
        comparisonPercent: Double,
        thresholdPercent: Double,
    ): PerformanceTrendStatus {
        return when {
            comparisonPercent >= thresholdPercent -> PerformanceTrendStatus.Improved
            comparisonPercent <= -thresholdPercent -> PerformanceTrendStatus.Declined
            else -> PerformanceTrendStatus.Stable
        }
    }

    private fun volumeMetric(trainings: List<Training>): PerformanceMetric.VolumeMetric {
        val stats = computeStats(trainings) { it.volume.toDouble() }
        return PerformanceMetric.VolumeMetric(
            currentVsAveragePercentage = stats.currentVsAveragePercent,
            current = stats.current?.toFloat(),
            average = stats.average?.toFloat(),
            best = stats.best?.toFloat(),
            status = stats.status,
        )
    }

    private fun densityOf(training: Training): Double? {
        val minutes = training.duration.toDouble(DurationUnit.MINUTES)
        val volume = training.volume.toDouble()
        if (!minutes.isFinite() || minutes <= 0.0 || !isValidMeasurement(volume)) {
            return null
        }

        val density = volume / minutes
        val storedDensity = density.toFloat()
        return density.takeIf { storedDensity.isFinite() && storedDensity >= 0f }
    }

    private fun densityMetric(trainings: List<Training>): PerformanceMetric.DensityMetric {
        val stats = computeStats(trainings = trainings, valueOf = ::densityOf)
        return PerformanceMetric.DensityMetric(
            currentVsAveragePercentage = stats.currentVsAveragePercent,
            current = stats.current?.toFloat(),
            average = stats.average?.toFloat(),
            best = stats.best?.toFloat(),
            status = stats.status,
        )
    }

    private fun repetitionsMetric(trainings: List<Training>): PerformanceMetric.RepetitionsMetric {
        val stats = computeStats(trainings) { it.repetitions.toDouble() }
        return PerformanceMetric.RepetitionsMetric(
            currentVsAveragePercentage = stats.currentVsAveragePercent,
            current = stats.current?.roundToInt(),
            average = stats.average,
            best = stats.best?.roundToInt(),
            status = stats.status,
        )
    }

    private fun intensityMetric(trainings: List<Training>): PerformanceMetric.IntensityMetric {
        val stats = computeStats(trainings) { it.intensity.toDouble() }
        return PerformanceMetric.IntensityMetric(
            currentVsAveragePercentage = stats.currentVsAveragePercent,
            current = stats.current?.toFloat(),
            average = stats.average?.toFloat(),
            best = stats.best?.toFloat(),
            status = stats.status,
        )
    }

    private fun validDuration(training: Training): Duration? {
        return training.duration.takeIf { it.isFinite() && !it.isNegative() }
    }

    private fun durationMetricNeutral(trainings: List<Training>): PerformanceMetric.DurationMetric {
        val current = trainings.lastOrNull()?.let(::validDuration)
        val baselineValues = trainings.dropLast(1).mapNotNull(::validDuration)
        val averageMinutes = baselineValues
            .takeIf { it.isNotEmpty() }
            ?.map { it.toDouble(DurationUnit.MINUTES) }
            ?.average()
        val average = averageMinutes?.minutes?.takeIf { it.isFinite() }
        val best = (baselineValues + listOfNotNull(current)).maxOrNull()
        val comparisonPercent = if (current != null && average != null) {
            currentVsAveragePercent(
                current = current.toDouble(DurationUnit.MINUTES),
                average = averageMinutes,
            )
        } else {
            null
        }

        return PerformanceMetric.DurationMetric(
            currentVsAveragePercentage = comparisonPercent,
            current = current,
            average = average,
            best = best,
            status = if (current == null) {
                PerformanceTrendStatus.Empty
            } else {
                PerformanceTrendStatus.Stable
            },
        )
    }
}
