package com.grippo.data.features.api.metrics.performance

import com.grippo.data.features.api.metrics.performance.models.PerformanceMetric
import com.grippo.data.features.api.metrics.performance.models.PerformanceReference
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
        private val trainingOrder: Comparator<Training> = compareBy<Training> { it.createdAt }.thenBy { it.id }
    }

    public fun fromTrainings(
        trainings: List<Training>,
        reference: PerformanceReference,
    ): List<PerformanceMetric> {
        val sorted = trainings.sortedWith(trainingOrder)

        return metricsOf(trainings = sorted, reference = reference)
    }

    public fun fromTraining(
        training: Training,
        periodTrainings: List<Training>,
        reference: PerformanceReference,
    ): List<PerformanceMetric> {
        val history = periodTrainings
            .filter { trainingOrder.compare(it, training) < 0 }
            .sortedWith(trainingOrder)

        return metricsOf(trainings = history + training, reference = reference)
    }

    private fun metricsOf(
        trainings: List<Training>,
        reference: PerformanceReference,
    ): List<PerformanceMetric> {
        return listOf(
            densityMetric(trainings = trainings, reference = reference),
            volumeMetric(trainings = trainings, reference = reference),
            repetitionsMetric(trainings = trainings, reference = reference),
            intensityMetric(trainings = trainings, reference = reference),
            durationMetricNeutral(trainings = trainings, reference = reference),
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
        reference: PerformanceReference,
        valueOf: (Training) -> Double?,
    ): MetricStats {
        val validValueOf: (Training) -> Double? = { training ->
            valueOf(training)?.takeIf(::isValidMeasurement)
        }
        val previous = trainings.dropLast(1)
        val current = trainings.lastOrNull()?.let(validValueOf)
        val previousValues = previous.mapNotNull(validValueOf)
        val referenceValues = referenceValues(
            previous = previous,
            reference = reference,
            valueOf = validValueOf,
        )
        val average = referenceValues.takeIf { it.isNotEmpty() }?.average()
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
            reference == PerformanceReference.Average && current > previousBest -> PerformanceTrendStatus.Record
            comparisonPercent == null -> PerformanceTrendStatus.Stable
            else -> statusFromComparison(
                comparisonPercent = comparisonPercent,
                thresholdPercent = dynamicThresholdPercent(referenceValues),
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

    private fun <T : Any> referenceValues(
        previous: List<Training>,
        reference: PerformanceReference,
        valueOf: (Training) -> T?,
    ): List<T> {
        return when (reference) {
            PerformanceReference.Average -> previous.mapNotNull(valueOf)
            PerformanceReference.Previous -> listOfNotNull(previous.lastOrNull()?.let(valueOf))
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

    private fun dynamicThresholdPercent(referenceValues: List<Double>): Double {
        if (referenceValues.size < 2) {
            return MIN_COMPARISON_PERCENT
        }

        val mean = referenceValues.average()
        val variance = referenceValues.sumOf { value ->
            val difference = value - mean
            difference * difference
        } / (referenceValues.size - 1)
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
        reference: PerformanceReference,
    ): PerformanceMetric.VolumeMetric {
        val stats = computeStats(trainings = trainings, reference = reference) { it.volume.toDouble() }
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
        reference: PerformanceReference,
    ): PerformanceMetric.DensityMetric {
        val stats = computeStats(trainings = trainings, reference = reference, valueOf = ::densityOf)
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
        reference: PerformanceReference,
    ): PerformanceMetric.RepetitionsMetric {
        val stats = computeStats(trainings = trainings, reference = reference) { it.repetitions.toDouble() }
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
        reference: PerformanceReference,
    ): PerformanceMetric.IntensityMetric {
        val stats = computeStats(trainings = trainings, reference = reference) { it.intensity.toDouble() }
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
        reference: PerformanceReference,
    ): PerformanceMetric.DurationMetric {
        val previous = trainings.dropLast(1)
        val current = trainings.lastOrNull()?.let(::validDuration)
        val previousValues = previous.mapNotNull(::validDuration)
        val referenceValues = referenceValues(
            previous = previous,
            reference = reference,
            valueOf = ::validDuration,
        )
        val averageMinutes = referenceValues
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
