package com.grippo.calculation.training

import androidx.compose.ui.graphics.Color
import com.grippo.calculation.internal.buildBuckets
import com.grippo.calculation.internal.defaultLabeler
import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.internal.groupTrainingsByBucket
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

public class VolumeAnalytics(
    private val colorProvider: ColorProvider,
    private val stringProvider: StringProvider,
) {

    private companion object {
        private const val EPS: Float = 1e-3f
    }

    private fun tonnage(ex: ExerciseState): Float =
        ex.iterations.fold(0f) { acc, iteration ->
            val w = iteration.volume.value ?: 0f
            val r = iteration.repetitions.value ?: 0
            if (w <= 0f || r <= 0) acc else acc + w * r
        }

    private fun TrainingState.sessionTonnage(): Double {
        var total = 0.0
        exercises.forEach { exercise ->
            exercise.iterations.forEach { iteration ->
                val w = iteration.volume.value ?: 0f
                val r = iteration.repetitions.value ?: 0
                if (w > 0f && r > 0) {
                    total += (w * r).toDouble()
                }
            }
        }
        return total
    }

    public suspend fun computeVolumeSeriesFromExercises(
        exercises: List<ExerciseState>,
    ): MetricSeries {
        val colors = colorProvider.get()
        val palette = colors.palette.palette7BlueGrowth
        val exTxt = stringProvider.get(Res.string.ex)

        val n = exercises.size
        if (n == 0) {
            return MetricSeries(emptyList())
        }

        val values = ArrayList<Float>(n)
        var minV = Float.POSITIVE_INFINITY
        var maxV = Float.NEGATIVE_INFINITY
        exercises.forEach { exercise ->
            val v = tonnage(exercise).coerceAtLeast(0f)
            values += v
            if (v < minV) minV = v
            if (v > maxV) maxV = v
        }
        if (!minV.isFinite()) minV = 0f
        if (!maxV.isFinite()) maxV = minV

        val mappedColors = ArrayList<Color>(n)
        val paletteSize = palette.size
        val allEqual = (maxV - minV).let { it >= 0f && it <= EPS }

        if (paletteSize > 0) {
            if (allEqual) {
                repeat(n) { mappedColors += palette.first() }
            } else {
                val span = (maxV - minV).coerceAtLeast(EPS)
                values.forEach { value ->
                    val t = ((value - minV) / span).coerceIn(0f, 1f)
                    val idx = (t * (paletteSize - 1)).toInt().coerceIn(0, paletteSize - 1)
                    mappedColors += palette[idx]
                }
            }
        } else {
            repeat(n) { mappedColors += Color.Unspecified }
        }

        val points = ArrayList<MetricPoint>(n)
        values.forEachIndexed { index, value ->
            points += MetricPoint(
                label = "$exTxt${index + 1}",
                value = value,
                color = mappedColors[index],
            )
        }

        val series = MetricSeries(points)
        return series
    }

    public suspend fun computeVolumeSeriesFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
    ): MetricSeries {
        val colors = colorProvider.get()
        val palette = colors.palette.palette7BlueGrowth

        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        return when (scale) {
            BucketScale.EXERCISE -> {
                val exercises = inRange.flatMap { it.exercises }
                computeVolumeSeriesFromExercises(exercises)
            }

            else -> {
                val buckets = buildBuckets(period.range, scale)
                val labeler = defaultLabeler(scale, stringProvider)
                val grouped = groupTrainingsByBucket(inRange, scale)

                val n = buckets.size
                if (n == 0) {
                    return MetricSeries(emptyList())
                }

                val totals = ArrayList<Float>(n)
                var minV = Float.POSITIVE_INFINITY
                var maxV = Float.NEGATIVE_INFINITY
                buckets.forEach { bucket ->
                    val v = (grouped[bucket.start] ?: emptyList())
                        .sumOf { it.sessionTonnage() }
                        .toFloat()
                        .coerceAtLeast(0f)
                    totals += v
                    if (v < minV) minV = v
                    if (v > maxV) maxV = v
                }
                if (!minV.isFinite()) minV = 0f
                if (!maxV.isFinite()) maxV = minV

                val paletteSize = palette.size
                val allEqual = (maxV - minV).let { it >= 0f && it <= EPS }
                val mapped = ArrayList<Color>(n)

                if (paletteSize > 0) {
                    if (allEqual) {
                        repeat(n) { mapped += palette.first() }
                    } else {
                        val span = (maxV - minV).coerceAtLeast(EPS)
                        totals.forEach { value ->
                            val t = ((value - minV) / span).coerceIn(0f, 1f)
                            val idx = (t * (paletteSize - 1)).toInt().coerceIn(0, paletteSize - 1)
                            mapped += palette[idx]
                        }
                    }
                } else {
                    repeat(n) { mapped += Color.Unspecified }
                }

                val points = ArrayList<MetricPoint>(n)
                buckets.forEachIndexed { index, bucket ->
                    points += MetricPoint(
                        label = labeler(bucket),
                        value = totals[index],
                        color = mapped[index],
                    )
                }

                val series = MetricSeries(points)
                series
            }
        }
    }
}
