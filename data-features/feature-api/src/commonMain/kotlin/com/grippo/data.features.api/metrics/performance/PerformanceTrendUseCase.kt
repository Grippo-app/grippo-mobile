package com.grippo.data.features.api.metrics.performance

import com.grippo.data.features.api.metrics.performance.models.PerformanceBaseline
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

    public fun fromTrainings(
        trainings: List<Training>,
        baseline: PerformanceBaseline = PerformanceBaseline.Average,
    ): List<PerformanceMetric> {
        val sorted = trainings.sortedWith(compareBy<Training> { it.createdAt }.thenBy { it.id })

        return listOf(
            densityMetric(trainings = sorted, baseline = baseline),
            volumeMetric(trainings = sorted, baseline = baseline),
            repetitionsMetric(trainings = sorted, baseline = baseline),
            intensityMetric(trainings = sorted, baseline = baseline),
            durationMetricNeutral(trainings = sorted, baseline = baseline),
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
        baseline: PerformanceBaseline,
        valueOf: (Training) -> Double?,
    ): MetricStats {
        val validValueOf: (Training) -> Double? = { training ->
            valueOf(training)?.takeIf(::isValidMeasurement)
        }
        val previous = trainings.dropLast(1)
        val current = trainings.lastOrNull()?.let(validValueOf)
        val previousValues = previous.mapNotNull(validValueOf)
        val baselineValues = baselineValues(
            previous = previous,
            baseline = baseline,
            valueOf = validValueOf,
        )
        val average = baselineValues.takeIf { it.isNotEmpty() }?.average()
        val previousBest = previousValues.maxOrNull()
        val best = (previousValues + listOfNotNull(current)).maxOrNull()
        val comparisonPercent = if (current != null && average != null) {
            currentVsAveragePercent(current = current, average = average)
        } else {
            null
        }
        val status = when {
            current == null -> PerformanceTrendStatus.Empty
            previousBest == null -> PerformanceTrendStatus.Stable
            baseline == PerformanceBaseline.Average && current > previousBest -> PerformanceTrendStatus.Record
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

    private fun <T : Any> baselineValues(
        previous: List<Training>,
        baseline: PerformanceBaseline,
        valueOf: (Training) -> T?,
    ): List<T> {
        return when (baseline) {
            PerformanceBaseline.Average -> previous.mapNotNull(valueOf)
            PerformanceBaseline.Previous -> listOfNotNull(previous.lastOrNull()?.let(valueOf))
        }
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

    private fun volumeMetric(
        trainings: List<Training>,
        baseline: PerformanceBaseline,
    ): PerformanceMetric.VolumeMetric {
        val stats = computeStats(trainings = trainings, baseline = baseline) { it.volume.toDouble() }
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

    private fun densityMetric(
        trainings: List<Training>,
        baseline: PerformanceBaseline,
    ): PerformanceMetric.DensityMetric {
        val stats = computeStats(trainings = trainings, baseline = baseline, valueOf = ::densityOf)
        return PerformanceMetric.DensityMetric(
            currentVsAveragePercentage = stats.currentVsAveragePercent,
            current = stats.current?.toFloat(),
            average = stats.average?.toFloat(),
            best = stats.best?.toFloat(),
            status = stats.status,
        )
    }

    private fun repetitionsMetric(
        trainings: List<Training>,
        baseline: PerformanceBaseline,
    ): PerformanceMetric.RepetitionsMetric {
        val stats = computeStats(trainings = trainings, baseline = baseline) { it.repetitions.toDouble() }
        return PerformanceMetric.RepetitionsMetric(
            currentVsAveragePercentage = stats.currentVsAveragePercent,
            current = stats.current?.roundToInt(),
            average = stats.average,
            best = stats.best?.roundToInt(),
            status = stats.status,
        )
    }

    private fun intensityMetric(
        trainings: List<Training>,
        baseline: PerformanceBaseline,
    ): PerformanceMetric.IntensityMetric {
        val stats = computeStats(trainings = trainings, baseline = baseline) { it.intensity.toDouble() }
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

    private fun durationMetricNeutral(
        trainings: List<Training>,
        baseline: PerformanceBaseline,
    ): PerformanceMetric.DurationMetric {
        val previous = trainings.dropLast(1)
        val current = trainings.lastOrNull()?.let(::validDuration)
        val previousValues = previous.mapNotNull(::validDuration)
        val baselineValues = baselineValues(
            previous = previous,
            baseline = baseline,
            valueOf = ::validDuration,
        )
        val averageMinutes = baselineValues
            .takeIf { it.isNotEmpty() }
            ?.map { it.toDouble(DurationUnit.MINUTES) }
            ?.average()
        val average = averageMinutes?.minutes?.takeIf { it.isFinite() }
        val best = (previousValues + listOfNotNull(current)).maxOrNull()
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
