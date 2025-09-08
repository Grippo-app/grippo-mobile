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
import com.grippo.design.resources.provider.tooltip_volume_description_month
import com.grippo.design.resources.provider.tooltip_volume_description_training
import com.grippo.design.resources.provider.tooltip_volume_description_year
import com.grippo.design.resources.provider.tooltip_volume_title_day
import com.grippo.design.resources.provider.tooltip_volume_title_month
import com.grippo.design.resources.provider.tooltip_volume_title_training
import com.grippo.design.resources.provider.tooltip_volume_title_year
import com.grippo.state.datetime.PeriodState
import com.grippo.state.formatters.UiText
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

public class VolumeAnalytics(
    private val colorProvider: ColorProvider,
    private val stringProvider: StringProvider
) {
    /** Σ(weight × reps) for an exercise. */
    private fun tonnage(ex: ExerciseState): Float =
        ex.iterations.fold(0f) { acc, it ->
            val w = it.volume.value ?: 0f
            val r = it.repetitions.value ?: 0
            if (w <= 0f || r <= 0) acc else acc + w * r
        }

    /** Total session tonnage across a training (Double for accumulation precision). */
    private fun TrainingState.sessionTonnage(): Double {
        var t = 0.0
        exercises.forEach { ex ->
            var eT = 0f
            ex.iterations.forEach { itn ->
                val w = itn.volume.value ?: 0f
                val r = itn.repetitions.value ?: 0
                if (w > 0f && r > 0) eT += w * r
            }
            t += eT
        }
        return t
    }

    public suspend fun calculateExerciseVolumeChartFromExercises(
        exercises: List<ExerciseState>
    ): Pair<DSBarData, Instruction> {
        val colors = colorProvider.get()
        val palette = colors.palette.palette7BlueGrowth
        val exTxt = stringProvider.get(Res.string.ex)

        val values = exercises.map { tonnage(it).coerceAtLeast(0f) }
        val n = values.size
        val pc = palette.size

        val minV = values.minOrNull() ?: 0f
        val maxV = values.maxOrNull() ?: minV
        val allEqual = (maxV == minV)

        val mappedColors = MutableList(n) { Color.Unspecified }
        if (n > 0 && pc > 0) {
            if (allEqual) {
                for (i in 0 until n) mappedColors[i] = palette.first()
            } else {
                val order = values.indices.sortedBy { values[it] }
                for (rank in 0 until n) {
                    val idx = order[rank]
                    val pIdx = if (n <= pc) rank
                    else ((rank.toFloat() * (pc - 1)) / (n - 1)).toInt()
                    mappedColors[idx] = palette[pIdx.coerceIn(0, pc - 1)]
                }
            }
        }

        val items = values.mapIndexed { i, v ->
            DSBarItem(
                label = "$exTxt${i + 1}",
                value = v,
                color = mappedColors.getOrNull(i) ?: Color.Unspecified
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

                val totals = buckets.map { b ->
                    (grouped[b.start] ?: emptyList()).sumOf { it.sessionTonnage() }
                        .toFloat()
                        .coerceAtLeast(0f)
                }
                val n = totals.size
                val pc = palette.size

                val minV = totals.minOrNull() ?: 0f
                val maxV = totals.maxOrNull() ?: minV
                val allEqual = (maxV == minV)

                val mapped = MutableList(n) { Color.Unspecified }
                if (n > 0 && pc > 0) {
                    if (allEqual) {
                        for (i in 0 until n) mapped[i] = palette.first()
                    } else {
                        val order = totals.indices.sortedBy { totals[it] }
                        for (rank in 0 until n) {
                            val idx = order[rank]
                            val pIdx = if (n <= pc) rank
                            else ((rank.toFloat() * (pc - 1)) / (n - 1)).toInt()
                            mapped[idx] = palette[pIdx.coerceIn(0, pc - 1)]
                        }
                    }
                }

                val items = buckets.mapIndexed { idx, b ->
                    DSBarItem(
                        label = labeler(b),
                        value = totals[idx],
                        color = mapped.getOrNull(idx) ?: Color.Unspecified
                    )
                }
                val data = DSBarData(items)
                val tip = instructionForVolume(scale)
                data to tip
            }
        }
    }

    private fun instructionForVolume(
        scale: BucketScale,
    ): Instruction {
        return when (scale) {
            BucketScale.EXERCISE ->
                Instruction(
                    UiText.Res(Res.string.tooltip_volume_title_training),
                    UiText.Res(Res.string.tooltip_volume_description_training)
                )

            BucketScale.DAY ->
                Instruction(
                    UiText.Res(Res.string.tooltip_volume_title_day),
                    UiText.Res(Res.string.tooltip_volume_description_day)
                )

            BucketScale.WEEK ->
                Instruction(
                    UiText.Res(Res.string.tooltip_volume_title_month),
                    UiText.Res(Res.string.tooltip_volume_description_month)
                )

            BucketScale.MONTH ->
                Instruction(
                    UiText.Res(Res.string.tooltip_volume_title_year),
                    UiText.Res(Res.string.tooltip_volume_description_year)
                )
        }
    }
}