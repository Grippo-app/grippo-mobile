package com.grippo.calculation.internal.strength

import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.internal.groupTrainingsByBucket
import com.grippo.calculation.internal.label
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.MetricPoint
import com.grippo.calculation.models.MetricSeries
import com.grippo.date.utils.contains
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.ex
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.datetime.PeriodState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

internal class Estimated1RMAnalytics(
    private val colorProvider: ColorProvider,
    private val stringProvider: StringProvider,
) {
    private fun brzycki(weight: Float, reps: Float): Float? {
        if (reps < 1f || reps > 12f) return null
        val denom = 1.0278f - 0.0278f * reps
        if (denom <= 0f) return null
        return weight / denom
    }

    private fun epley(weight: Float, reps: Float): Float? {
        if (reps < 1f || reps > 12f) return null
        return weight * (1f + reps / 30f)
    }

    private fun qualityWeight(reps: Float): Float {
        val diff = reps - 4f
        return 1f / (1f + diff * diff)
    }

    private fun weightedMedian(values: List<Pair<Float, Float>>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sortedBy { it.first }
        val totalWeight = sorted.sumOf { it.second.toDouble() }
        var acc = 0.0
        for ((value, weight) in sorted) {
            acc += weight
            if (acc >= totalWeight * 0.5) return value
        }
        return sorted.last().first
    }

    private fun estimateSession1RM(exercise: ExerciseState): Float? {
        val pairs = mutableListOf<Pair<Float, Float>>()
        exercise.iterations.forEach { iteration ->
            val weight = iteration.volume.value ?: return@forEach
            val repsInt = iteration.repetitions.value ?: return@forEach
            val reps = repsInt.toFloat()
            val brzycki = brzycki(weight, reps)
            val epley = epley(weight, reps)
            val average = listOfNotNull(brzycki, epley).let {
                if (it.isEmpty()) null else it.average().toFloat()
            }
            if (average != null && average.isFinite() && average > 0f) {
                val quality = qualityWeight(reps)
                pairs += average to quality
            }
        }
        if (pairs.isEmpty()) return null
        val median = weightedMedian(pairs)
        return median.takeIf { it.isFinite() && it > 0f }
    }

    private fun percentile(values: List<Float>, p: Float): Float {
        if (values.isEmpty()) return 0f
        val valid = values.filter { it.isFinite() }
        if (valid.isEmpty()) return 0f
        val sorted = valid.sorted()
        val clamped = p.coerceIn(0f, 1f)
        val idx = ((sorted.size - 1) * clamped).toDouble()
        val lo = idx.toInt()
        val hi = minOf(lo + 1, sorted.lastIndex)
        val fraction = (idx - lo).toFloat()
        val value = sorted[lo] * (1 - fraction) + sorted[hi] * fraction
        return if (value.isFinite()) value else 0f
    }

    suspend fun computeEstimated1RmFromExercises(
        exercises: List<ExerciseState>,
    ): MetricSeries {
        val colors = colorProvider.get()
        val palette = colors.palette.palette18ColorfulRandom
        val exTxt = stringProvider.get(Res.string.ex)

        val points = exercises.mapIndexedNotNull { index, exercise ->
            val estimate = estimateSession1RM(exercise) ?: return@mapIndexedNotNull null
            MetricPoint(
                label = "$exTxt${index + 1}",
                value = estimate.coerceAtLeast(0f),
                color = palette[index % palette.size],
            )
        }
        val data = MetricSeries(points)
        return data
    }

    suspend fun computeEstimated1RmFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
    ): MetricSeries {
        val colors = colorProvider.get()
        val palette = colors.palette.palette18ColorfulRandom

        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        return if (scale == BucketScale.EXERCISE) {
            val exercises = inRange.flatMap { it.exercises }
            computeEstimated1RmFromExercises(exercises)
        } else {
            val buckets = groupTrainingsByBucket(inRange, scale).toList().sortedBy { it.first }
            val points = buckets.mapIndexedNotNull { index, (start, groupedTrainings) ->
                val estimates = groupedTrainings.flatMap { it.exercises }
                    .mapNotNull { estimateSession1RM(it) }
                    .filter { it.isFinite() && it > 0f }
                if (estimates.isEmpty()) return@mapIndexedNotNull null
                val reduced = percentile(estimates, 0.90f)
                MetricPoint(
                    label = start.label(scale, stringProvider),
                    value = reduced.coerceAtLeast(0f),
                    color = palette[index % palette.size],
                )
            }
            val data = MetricSeries(points)
            data
        }
    }
}
