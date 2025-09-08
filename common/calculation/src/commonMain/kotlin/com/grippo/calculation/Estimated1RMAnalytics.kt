package com.grippo.calculation

import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.internal.groupTrainingsByBucket
import com.grippo.calculation.internal.label
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.contains
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.ex
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.tooltip_estimated1rm_description_day
import com.grippo.design.resources.provider.tooltip_estimated1rm_description_month
import com.grippo.design.resources.provider.tooltip_estimated1rm_description_training
import com.grippo.design.resources.provider.tooltip_estimated1rm_description_year
import com.grippo.design.resources.provider.tooltip_estimated1rm_title_day
import com.grippo.design.resources.provider.tooltip_estimated1rm_title_month
import com.grippo.design.resources.provider.tooltip_estimated1rm_title_training
import com.grippo.design.resources.provider.tooltip_estimated1rm_title_year
import com.grippo.state.datetime.PeriodState
import com.grippo.state.formatters.UiText
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

public class Estimated1RMAnalytics(
    private val colorProvider: ColorProvider,
    private val stringProvider: StringProvider
) {
    // ---------- 1RM estimation (independent copy) ----------
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
        val d = reps - 4f
        return 1f / (1f + d * d)
    }

    private fun weightedMedian(values: List<Pair<Float, Float>>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sortedBy { it.first }
        val totalW = sorted.sumOf { it.second.toDouble() }
        var acc = 0.0
        for ((v, w) in sorted) {
            acc += w
            if (acc >= totalW * 0.5) return v
        }
        return sorted.last().first
    }

    private fun estimateSession1RM(ex: ExerciseState): Float? {
        val pairs = mutableListOf<Pair<Float, Float>>()
        ex.iterations.forEach { itn ->
            val w = itn.volume.value ?: return@forEach
            val rI = itn.repetitions.value ?: return@forEach
            val r = rI.toFloat()
            val b = brzycki(w, r)
            val e = epley(w, r)
            val avg = listOfNotNull(b, e).let { if (it.isEmpty()) null else it.average().toFloat() }
            if (avg != null && avg.isFinite() && avg > 0f) {
                val q = qualityWeight(r)
                pairs += avg to q
            }
        }
        if (pairs.isEmpty()) return null
        val wm = weightedMedian(pairs)
        return wm.takeIf { it.isFinite() && it > 0f }
    }

    // ---------- stats (independent copy) ----------
    private fun percentile(values: List<Float>, p: Float): Float {
        if (values.isEmpty()) return 0f
        val valid = values.filter { it.isFinite() }
        if (valid.isEmpty()) return 0f
        val sorted = valid.sorted()
        val clamped = p.coerceIn(0f, 1f)
        val idx = ((sorted.size - 1) * clamped).toDouble()
        val lo = idx.toInt()
        val hi = minOf(lo + 1, sorted.lastIndex)
        val frac = (idx - lo).toFloat()
        val v = sorted[lo] * (1 - frac) + sorted[hi] * frac
        return if (v.isFinite()) v else 0f
    }

    // ---------- public API ----------
    public suspend fun calculateEstimated1RMFromExercises(
        exercises: List<ExerciseState>
    ): Pair<DSBarData, Instruction> {
        val colors = colorProvider.get()
        val palette = colors.palette.palette18ColorfulRandom
        val exTxt = stringProvider.get(Res.string.ex)

        val items = exercises.mapIndexedNotNull { index, ex ->
            val e1 = estimateSession1RM(ex) ?: return@mapIndexedNotNull null
            DSBarItem(
                label = "$exTxt${index + 1}",
                value = e1.coerceAtLeast(0f),
                color = palette[index % palette.size]
            )
        }
        val data = DSBarData(items)
        val tip = instructionForEstimated1RM(BucketScale.EXERCISE)
        return data to tip
    }

    public suspend fun calculateEstimated1RMFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState
    ): Pair<DSBarData, Instruction> {
        val colors = colorProvider.get()
        val palette = colors.palette.palette18ColorfulRandom

        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        return if (scale == BucketScale.EXERCISE) {
            val exs = inRange.flatMap { it.exercises }
            calculateEstimated1RMFromExercises(exs)
        } else {
            val buckets = groupTrainingsByBucket(inRange, scale).toList().sortedBy { it.first }
            val items = buckets.mapIndexedNotNull { idx, (start, ts) ->
                val e1s = ts.flatMap { it.exercises }
                    .mapNotNull { estimateSession1RM(it) }
                    .filter { it.isFinite() && it > 0f }
                if (e1s.isEmpty()) return@mapIndexedNotNull null
                val reduced = percentile(e1s, 0.90f)
                DSBarItem(
                    label = start.label(scale, stringProvider),
                    value = reduced.coerceAtLeast(0f),
                    color = palette[idx % palette.size]
                )
            }
            val data = DSBarData(items)
            val tip = instructionForEstimated1RM(scale)
            data to tip
        }
    }

    private fun instructionForEstimated1RM(
        scale: BucketScale,
    ): Instruction {
        return when (scale) {
            BucketScale.EXERCISE ->
                Instruction(
                    UiText.Res(Res.string.tooltip_estimated1rm_title_training),
                    UiText.Res(Res.string.tooltip_estimated1rm_description_training)
                )

            BucketScale.DAY ->
                Instruction(
                    UiText.Res(Res.string.tooltip_estimated1rm_title_day),
                    UiText.Res(Res.string.tooltip_estimated1rm_description_day)
                )

            BucketScale.WEEK ->
                Instruction(
                    UiText.Res(Res.string.tooltip_estimated1rm_title_month),
                    UiText.Res(Res.string.tooltip_estimated1rm_description_month)
                )

            BucketScale.MONTH ->
                Instruction(
                    UiText.Res(Res.string.tooltip_estimated1rm_title_year),
                    UiText.Res(Res.string.tooltip_estimated1rm_description_year)
                )
        }
    }
}