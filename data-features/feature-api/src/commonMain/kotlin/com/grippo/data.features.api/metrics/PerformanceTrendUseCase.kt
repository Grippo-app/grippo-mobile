package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.metrics.models.PerformanceMetric
import com.grippo.data.features.api.metrics.models.PerformanceTrendStatus
import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.api.user.UserFeature
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

public class PerformanceTrendUseCase(
    private val userFeature: UserFeature,
) {

    private companion object {
        private const val RECORD_EPS: Double = 1e-2
    }

    public suspend fun fromTrainings(trainings: List<Training>): List<PerformanceMetric> {
        if (trainings.isEmpty()) return emptyList()
        val experience = resolveExperience() ?: return emptyList()
        val minTrendThresholdPercent = experience.minTrendThresholdPercent()
        val latestTraining = trainings.maxBy { it.createdAt }

        val sorted = trainings.sortedBy { it.createdAt }

        val metrics = buildList {
            densityMetric(sorted, latestTraining, minTrendThresholdPercent)?.let(::add)
            volumeMetric(sorted, latestTraining, minTrendThresholdPercent)?.let(::add)
            repetitionsMetric(sorted, latestTraining, minTrendThresholdPercent)?.let(::add)
            intensityMetric(sorted, latestTraining, minTrendThresholdPercent)?.let(::add)
            durationMetricNeutral(sorted, latestTraining)?.let(::add)
        }

        return metrics
    }

    /**
     * We intentionally avoid fixed “days” windows here.
     * The caller decides the range by passing the trainings list.
     *
     * Baseline = all points except the latest.
     * Trend = linear regression slope over the whole list (relative to baseline mean).
     */
    private fun trendPercent(baselineMean: Double, values: List<Double>): Int? {
        if (baselineMean <= 0.0) return null
        if (values.size < 2) return null
        val slope = linearRegressionSlope(values)
        val totalChange = slope * (values.size - 1).toDouble()
        return ((totalChange / baselineMean) * 100.0).roundToInt()
    }

    private fun linearRegressionSlope(values: List<Double>): Double {
        val n = values.size
        if (n < 2) return 0.0
        val xs = (0 until n).map { it.toDouble() }
        val xMean = xs.average()
        val yMean = values.average()
        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            val dx = xs[i] - xMean
            num += dx * (values[i] - yMean)
            den += dx * dx
        }
        return if (den == 0.0) 0.0 else num / den
    }

    private fun stddev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1).toDouble()
        return kotlin.math.sqrt(variance)
    }

    private fun dynamicThresholdPercent(
        mean: Double,
        stddev: Double,
        minThresholdPercent: Int,
    ): Int? {
        if (mean <= 0.0) return null
        val percent = ((2.0 * stddev / mean) * 100.0).roundToInt()
        return maxOf(minThresholdPercent, percent)
    }

    private fun statusFromTrend(
        current: Double,
        best: Double,
        trendPercent: Int,
        thresholdPercent: Int,
    ): PerformanceTrendStatus {
        if (current >= best - RECORD_EPS) return PerformanceTrendStatus.Record
        return when {
            trendPercent >= thresholdPercent -> PerformanceTrendStatus.Improved
            trendPercent <= -thresholdPercent -> PerformanceTrendStatus.Declined
            else -> PerformanceTrendStatus.Stable
        }
    }

    private fun volumeMetric(
        trainings: List<Training>,
        latest: Training,
        minTrendThresholdPercent: Int,
    ): PerformanceMetric.VolumeMetric? {
        val values = trainings.map { it.volume.toDouble() }.filter { it > 0.0 }
        if (values.size < 2) return null
        val baselineValues = values.dropLast(1)
        if (baselineValues.isEmpty()) return null

        val baselineMean = baselineValues.average()
        val baselineStd = stddev(baselineValues)
        val trend = trendPercent(baselineMean, values) ?: return null
        val threshold = dynamicThresholdPercent(
            mean = baselineMean,
            stddev = baselineStd,
            minThresholdPercent = minTrendThresholdPercent,
        ) ?: return null
        val best = values.maxOrNull() ?: return null
        val current = values.last()
        val status = statusFromTrend(current, best, trend, threshold)

        return PerformanceMetric.VolumeMetric(
            deltaPercentage = trend,
            current = latest.volume,
            average = baselineMean.toFloat(),
            best = best.toFloat(),
            status = status,
        )
    }

    private fun densityMetric(
        trainings: List<Training>,
        latest: Training,
        minTrendThresholdPercent: Int,
    ): PerformanceMetric.DensityMetric? {
        fun densityOf(t: Training): Double {
            val minutes = t.duration.inWholeMinutes.toDouble()
            if (minutes <= 0.0) return 0.0
            val v = t.volume.toDouble()
            if (v <= 0.0) return 0.0
            return v / minutes
        }

        val values = trainings.map { densityOf(it) }.filter { it > 0.0 }
        if (values.size < 2) return null
        val baselineValues = values.dropLast(1)
        if (baselineValues.isEmpty()) return null

        val baselineMean = baselineValues.average()
        val baselineStd = stddev(baselineValues)
        val trend = trendPercent(baselineMean, values) ?: return null
        val threshold = dynamicThresholdPercent(
            mean = baselineMean,
            stddev = baselineStd,
            minThresholdPercent = minTrendThresholdPercent,
        ) ?: return null
        val best = values.maxOrNull() ?: return null
        val current = values.last()
        val status = statusFromTrend(current, best, trend, threshold)

        return PerformanceMetric.DensityMetric(
            deltaPercentage = trend,
            current = densityOf(latest).toFloat(),
            average = baselineMean.toFloat(),
            best = best.toFloat(),
            status = status,
        )
    }

    private fun repetitionsMetric(
        trainings: List<Training>,
        latest: Training,
        minTrendThresholdPercent: Int,
    ): PerformanceMetric.RepetitionsMetric? {
        val values = trainings.map { it.repetitions.toDouble() }.filter { it > 0.0 }
        if (values.size < 2) return null
        val baselineValues = values.dropLast(1)
        if (baselineValues.isEmpty()) return null

        val baselineMean = baselineValues.average()
        val baselineStd = stddev(baselineValues)
        val trend = trendPercent(baselineMean, values) ?: return null
        val threshold = dynamicThresholdPercent(
            mean = baselineMean,
            stddev = baselineStd,
            minThresholdPercent = minTrendThresholdPercent,
        ) ?: return null
        val best = values.maxOrNull() ?: return null
        val current = values.last()
        val status = statusFromTrend(current, best, trend, threshold)

        return PerformanceMetric.RepetitionsMetric(
            deltaPercentage = trend,
            current = latest.repetitions,
            average = baselineMean.roundToInt(),
            best = best.roundToInt(),
            status = status,
        )
    }

    private fun intensityMetric(
        trainings: List<Training>,
        latest: Training,
        minTrendThresholdPercent: Int,
    ): PerformanceMetric.IntensityMetric? {
        val values = trainings.map { it.intensity.toDouble() }.filter { it > 0.0 }
        if (values.size < 2) return null
        val baselineValues = values.dropLast(1)
        if (baselineValues.isEmpty()) return null

        val baselineMean = baselineValues.average()
        val baselineStd = stddev(baselineValues)
        val trend = trendPercent(baselineMean, values) ?: return null
        val threshold = dynamicThresholdPercent(
            mean = baselineMean,
            stddev = baselineStd,
            minThresholdPercent = minTrendThresholdPercent,
        ) ?: return null
        val best = values.maxOrNull() ?: return null
        val current = values.last()
        val status = statusFromTrend(current, best, trend, threshold)

        return PerformanceMetric.IntensityMetric(
            deltaPercentage = trend,
            current = latest.intensity,
            average = baselineMean.toFloat(),
            best = best.toFloat(),
            status = status,
        )
    }

    private fun durationMetricNeutral(
        trainings: List<Training>,
        latest: Training,
    ): PerformanceMetric.DurationMetric? {
        val values = trainings.map { it.duration.inWholeMinutes.toDouble() }.filter { it > 0.0 }
        if (values.size < 2) return null
        val baselineValues = values.dropLast(1)
        if (baselineValues.isEmpty()) return null

        val baselineMean = baselineValues.average()
        val best = values.maxOrNull() ?: return null

        return PerformanceMetric.DurationMetric(
            deltaPercentage = 0,
            current = latest.duration,
            average = baselineMean.minutes,
            best = best.minutes,
            status = PerformanceTrendStatus.Stable,
        )
    }

    private suspend fun resolveExperience(): ExperienceEnum? {
        return userFeature.observeUser().firstOrNull()?.experience
    }

    private fun ExperienceEnum.minTrendThresholdPercent(): Int {
        return when (this) {
            ExperienceEnum.BEGINNER -> 6
            ExperienceEnum.INTERMEDIATE -> 4
            ExperienceEnum.ADVANCED -> 3
            ExperienceEnum.PRO -> 2
        }
    }
}
