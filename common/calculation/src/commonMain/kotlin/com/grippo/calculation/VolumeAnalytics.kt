package com.grippo.calculation

import androidx.compose.ui.graphics.Color
import com.grippo.calculation.internal.buildBuckets
import com.grippo.calculation.internal.defaultLabeler
import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.internal.groupTrainingsByBucket
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.contains
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.ex
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.tooltip_volume_description_day
import com.grippo.design.resources.provider.tooltip_volume_description_training
import com.grippo.design.resources.provider.tooltip_volume_description_week
import com.grippo.design.resources.provider.tooltip_volume_description_year
import com.grippo.design.resources.provider.tooltip_volume_title_day
import com.grippo.design.resources.provider.tooltip_volume_title_training
import com.grippo.design.resources.provider.tooltip_volume_title_week
import com.grippo.design.resources.provider.tooltip_volume_title_year
import com.grippo.state.datetime.PeriodState
import com.grippo.state.formatters.UiText
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

public class VolumeAnalytics(
    private val colorProvider: ColorProvider,
    private val stringProvider: StringProvider
) {

    private companion object {
        private const val EPS: Float = 1e-3f
    }

    /** Σ(weight × reps) for an exercise. */
    private fun tonnage(ex: ExerciseState): Float =
        ex.iterations.fold(0f) { acc, it ->
            val w = it.volume.value ?: 0f
            val r = it.repetitions.value ?: 0
            if (w <= 0f || r <= 0) acc else acc + w * r
        }

    /** Total session tonnage across a training (Double for accumulation precision). */
    private fun TrainingState.sessionTonnage(): Double {
        var total = 0.0
        // Accumulate directly into Double to reduce intermediate rounding
        exercises.forEach { ex ->
            ex.iterations.forEach { itn ->
                val w = itn.volume.value ?: 0f
                val r = itn.repetitions.value ?: 0
                if (w > 0f && r > 0) {
                    total += (w * r).toDouble()
                }
            }
        }
        return total
    }

    public suspend fun calculateExerciseVolumeChartFromExercises(
        exercises: List<ExerciseState>
    ): Pair<DSBarData, Instruction> {
        val colors = colorProvider.get()
        val palette = colors.palette.palette7BlueGrowth
        val exTxt = stringProvider.get(Res.string.ex)

        val n = exercises.size
        if (n == 0) {
            return DSBarData(emptyList()) to instructionForVolume(BucketScale.EXERCISE)
        }

        // Build values and track min/max in one pass
        val values = ArrayList<Float>(n)
        var minV = Float.POSITIVE_INFINITY
        var maxV = Float.NEGATIVE_INFINITY
        exercises.forEach { ex ->
            val v = tonnage(ex).coerceAtLeast(0f)
            values += v
            if (v < minV) minV = v
            if (v > maxV) maxV = v
        }
        if (!minV.isFinite()) minV = 0f
        if (!maxV.isFinite()) maxV = minV

        val mappedColors = ArrayList<Color>(n)
        val pc = palette.size
        val allEqual = (maxV - minV).let { it >= 0f && it <= EPS }

        // O(n) color mapping via linear normalization instead of rank-sorting
        if (pc > 0) {
            if (allEqual) {
                repeat(n) { mappedColors += palette.first() }
            } else {
                val span = (maxV - minV).coerceAtLeast(EPS)
                values.forEach { v ->
                    val t = ((v - minV) / span).coerceIn(0f, 1f)
                    val idx = (t * (pc - 1)).toInt().coerceIn(0, pc - 1)
                    mappedColors += palette[idx]
                }
            }
        } else {
            repeat(n) { mappedColors += Color.Unspecified }
        }

        val items = ArrayList<DSBarItem>(n)
        values.forEachIndexed { i, v ->
            items += DSBarItem(
                label = "$exTxt${i + 1}",
                value = v,
                color = mappedColors[i]
            )
        }

        val data = DSBarData(items)
        val tip = instructionForVolume(BucketScale.EXERCISE)
        return data to tip
    }

    public suspend fun calculateExerciseVolumeChartFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState
    ): Pair<DSBarData, Instruction> {
        val colors = colorProvider.get()
        val palette = colors.palette.palette7BlueGrowth

        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        return when (scale) {
            BucketScale.EXERCISE -> {
                val exs = inRange.flatMap { it.exercises }
                calculateExerciseVolumeChartFromExercises(exs)
            }
            else -> {
                val buckets = buildBuckets(period.range, scale)
                val labeler = defaultLabeler(scale, stringProvider)
                val grouped = groupTrainingsByBucket(inRange, scale)

                val n = buckets.size
                if (n == 0) {
                    return DSBarData(emptyList()) to instructionForVolume(scale)
                }

                // Build totals and track min/max in one pass
                val totals = ArrayList<Float>(n)
                var minV = Float.POSITIVE_INFINITY
                var maxV = Float.NEGATIVE_INFINITY
                buckets.forEach { b ->
                    val v = (grouped[b.start] ?: emptyList())
                        .sumOf { it.sessionTonnage() }
                        .toFloat()
                        .coerceAtLeast(0f)
                    totals += v
                    if (v < minV) minV = v
                    if (v > maxV) maxV = v
                }
                if (!minV.isFinite()) minV = 0f
                if (!maxV.isFinite()) maxV = minV

                val pc = palette.size
                val allEqual = (maxV - minV).let { it >= 0f && it <= EPS }
                val mapped = ArrayList<Color>(n)

                if (pc > 0) {
                    if (allEqual) {
                        repeat(n) { mapped += palette.first() }
                    } else {
                        val span = (maxV - minV).coerceAtLeast(EPS)
                        totals.forEach { v ->
                            val t = ((v - minV) / span).coerceIn(0f, 1f)
                            val idx = (t * (pc - 1)).toInt().coerceIn(0, pc - 1)
                            mapped += palette[idx]
                        }
                    }
                } else {
                    repeat(n) { mapped += Color.Unspecified }
                }

                val items = ArrayList<DSBarItem>(n)
                buckets.forEachIndexed { idx, b ->
                    items += DSBarItem(
                        label = labeler(b),
                        value = totals[idx],
                        color = mapped[idx]
                    )
                }

                val data = DSBarData(items)
                val tip = instructionForVolume(scale)
                return data to tip
            }
        }
    }

    private fun instructionForVolume(
        scale: BucketScale,
    ): Instruction {
        return when (scale) {
            BucketScale.EXERCISE -> Instruction(
                title = UiText.Res(Res.string.tooltip_volume_title_training),
                description = UiText.Res(Res.string.tooltip_volume_description_training)
            )
            BucketScale.DAY -> Instruction(
                title = UiText.Res(Res.string.tooltip_volume_title_day),
                description = UiText.Res(Res.string.tooltip_volume_description_day)
            )
            BucketScale.WEEK -> Instruction(
                title = UiText.Res(Res.string.tooltip_volume_title_week),
                description = UiText.Res(Res.string.tooltip_volume_description_week)
            )
            BucketScale.MONTH -> Instruction(
                title = UiText.Res(Res.string.tooltip_volume_title_year),
                description = UiText.Res(Res.string.tooltip_volume_description_year)
            )
        }
    }
}