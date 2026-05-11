package com.grippo.data.features.api.metrics.performance

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.metrics.performance.models.PerformanceMetric
import com.grippo.data.features.api.metrics.performance.models.PerformanceTrendStatus
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
        val experience = resolveExperience() ?: ExperienceEnum.BEGINNER
        val sorted = trainings.sortedBy { it.createdAt }
        val latestTraining = sorted.lastOrNull()
        val minTrendThresholdPercent = experience.minTrendThresholdPercent()

        return listOf(
            densityMetric(sorted, latestTraining, minTrendThresholdPercent),
            volumeMetric(sorted, latestTraining, minTrendThresholdPercent),
            repetitionsMetric(sorted, latestTraining, minTrendThresholdPercent),
            intensityMetric(sorted, latestTraining, minTrendThresholdPercent),
            durationMetricNeutral(sorted, latestTraining),
        )
    }

    private data class MetricStats(
        val current: Double,
        val average: Double,
        val best: Double,
        val trendPercent: Int,
        val currentVsAveragePercent: Int,
        val status: PerformanceTrendStatus,
    ) {
        companion object {
            val ZERO: MetricStats = MetricStats(
                current = 0.0,
                average = 0.0,
                best = 0.0,
                trendPercent = 0,
                currentVsAveragePercent = 0,
                status = PerformanceTrendStatus.Empty,
            )
        }
    }

    /**
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

    private fun currentVsAveragePercent(current: Double, baselineMean: Double): Int? {
        if (baselineMean <= 0.0) return null
        return (((current - baselineMean) / baselineMean) * 100.0).roundToInt()
    }

    private fun computeStats(
        trainings: List<Training>,
        latest: Training?,
        minTrendThresholdPercent: Int,
        valueOf: (Training) -> Double,
    ): MetricStats {
        if (latest == null) return MetricStats.ZERO

        val latestValue = valueOf(latest)
        if (latestValue <= 0.0) return MetricStats.ZERO

        val values = trainings.map(valueOf).filter { it > 0.0 }
        if (values.isEmpty()) return MetricStats.ZERO

        val baselineValues = values.dropLast(1)
        val baselineMean = if (baselineValues.isNotEmpty()) baselineValues.average() else 0.0
        val baselineStd = if (baselineValues.size >= 2) stddev(baselineValues) else 0.0
        val best = values.maxOrNull() ?: latestValue

        val trend = if (baselineMean > 0.0 && values.size >= 2) {
            trendPercent(baselineMean, values) ?: 0
        } else 0

        val currentVsAverage = if (baselineMean > 0.0) {
            currentVsAveragePercent(latestValue, baselineMean) ?: 0
        } else 0

        val threshold = dynamicThresholdPercent(
            mean = baselineMean,
            stddev = baselineStd,
            minThresholdPercent = minTrendThresholdPercent,
        ) ?: minTrendThresholdPercent

        val status = if (baselineMean > 0.0) {
            statusFromTrend(latestValue, best, trend, threshold)
        } else PerformanceTrendStatus.Stable

        return MetricStats(
            current = latestValue,
            average = baselineMean,
            best = best,
            trendPercent = trend,
            currentVsAveragePercent = currentVsAverage,
            status = status,
        )
    }

    private fun volumeMetric(
        trainings: List<Training>,
        latest: Training?,
        minTrendThresholdPercent: Int,
    ): PerformanceMetric.VolumeMetric {
        val stats = computeStats(
            trainings = trainings,
            latest = latest,
            minTrendThresholdPercent = minTrendThresholdPercent,
            valueOf = { it.volume.toDouble() },
        )
        return PerformanceMetric.VolumeMetric(
            deltaPercentage = stats.trendPercent,
            currentVsAveragePercentage = stats.currentVsAveragePercent,
            current = stats.current.toFloat(),
            average = stats.average.toFloat(),
            best = stats.best.toFloat(),
            status = stats.status,
        )
    }

    private fun densityMetric(
        trainings: List<Training>,
        latest: Training?,
        minTrendThresholdPercent: Int,
    ): PerformanceMetric.DensityMetric {
        fun densityOf(t: Training): Double {
            val minutes = t.duration.inWholeMinutes.toDouble()
            if (minutes <= 0.0) return 0.0
            val v = t.volume.toDouble()
            if (v <= 0.0) return 0.0
            return v / minutes
        }

        val stats = computeStats(
            trainings = trainings,
            latest = latest,
            minTrendThresholdPercent = minTrendThresholdPercent,
            valueOf = ::densityOf,
        )
        return PerformanceMetric.DensityMetric(
            deltaPercentage = stats.trendPercent,
            currentVsAveragePercentage = stats.currentVsAveragePercent,
            current = stats.current.toFloat(),
            average = stats.average.toFloat(),
            best = stats.best.toFloat(),
            status = stats.status,
        )
    }

    private fun repetitionsMetric(
        trainings: List<Training>,
        latest: Training?,
        minTrendThresholdPercent: Int,
    ): PerformanceMetric.RepetitionsMetric {
        val stats = computeStats(
            trainings = trainings,
            latest = latest,
            minTrendThresholdPercent = minTrendThresholdPercent,
            valueOf = { it.repetitions.toDouble() },
        )
        return PerformanceMetric.RepetitionsMetric(
            deltaPercentage = stats.trendPercent,
            currentVsAveragePercentage = stats.currentVsAveragePercent,
            current = stats.current.roundToInt(),
            average = stats.average.roundToInt(),
            best = stats.best.roundToInt(),
            status = stats.status,
        )
    }

    private fun intensityMetric(
        trainings: List<Training>,
        latest: Training?,
        minTrendThresholdPercent: Int,
    ): PerformanceMetric.IntensityMetric {
        val stats = computeStats(
            trainings = trainings,
            latest = latest,
            minTrendThresholdPercent = minTrendThresholdPercent,
            valueOf = { it.intensity.toDouble() },
        )
        return PerformanceMetric.IntensityMetric(
            deltaPercentage = stats.trendPercent,
            currentVsAveragePercentage = stats.currentVsAveragePercent,
            current = stats.current.toFloat(),
            average = stats.average.toFloat(),
            best = stats.best.toFloat(),
            status = stats.status,
        )
    }

    private fun durationMetricNeutral(
        trainings: List<Training>,
        latest: Training?,
    ): PerformanceMetric.DurationMetric {
        val latestValue = latest?.duration?.inWholeMinutes?.toDouble() ?: 0.0
        val values = trainings.map { it.duration.inWholeMinutes.toDouble() }.filter { it > 0.0 }
        val baselineValues = values.dropLast(1)
        val baselineMean = if (baselineValues.isNotEmpty()) baselineValues.average() else 0.0
        val best = values.maxOrNull() ?: latestValue

        val currentVsAverage = if (baselineMean > 0.0) {
            currentVsAveragePercent(latestValue, baselineMean) ?: 0
        } else 0

        val status = if (latestValue <= 0.0) {
            PerformanceTrendStatus.Empty
        } else {
            PerformanceTrendStatus.Stable
        }

        return PerformanceMetric.DurationMetric(
            deltaPercentage = 0,
            currentVsAveragePercentage = currentVsAverage,
            current = latestValue.minutes,
            average = baselineMean.minutes,
            best = best.minutes,
            status = status,
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
