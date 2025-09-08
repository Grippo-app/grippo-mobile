package com.grippo.calculation

import com.grippo.calculation.internal.daysInclusive
import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.internal.groupTrainingsByBucket
import com.grippo.calculation.internal.label
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.contains
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.ex
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.tooltip_percent1rm_description_day
import com.grippo.design.resources.provider.tooltip_percent1rm_description_month
import com.grippo.design.resources.provider.tooltip_percent1rm_description_training
import com.grippo.design.resources.provider.tooltip_percent1rm_description_year
import com.grippo.design.resources.provider.tooltip_percent1rm_title_day
import com.grippo.design.resources.provider.tooltip_percent1rm_title_month
import com.grippo.design.resources.provider.tooltip_percent1rm_title_training
import com.grippo.design.resources.provider.tooltip_percent1rm_title_year
import com.grippo.state.datetime.PeriodState
import com.grippo.state.formatters.UiText
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

public class Percent1RMAnalytics(
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

    // ---------- basic per-exercise helpers (independent copies) ----------
    private fun sums(ex: ExerciseState): Pair<Float, Int> {
        var totalT = 0f
        var totalR = 0
        ex.iterations.forEach { itn ->
            val w = itn.volume.value ?: 0f
            val r = itn.repetitions.value ?: 0
            if (w > 0f && r > 0) {
                totalT += w * r
                totalR += r
            }
        }
        return totalT to totalR
    }

    private fun avgWeightPerRep(ex: ExerciseState): Float {
        val (t, r) = sums(ex)
        return if (r > 0) t / r else 0f
    }

    private fun exerciseKey(ex: ExerciseState): String =
        ex.exerciseExample?.id ?: ex.name

    private fun smooth(
        prev: Float?,
        cur: Float,
        alpha: Float = 0.20f,
        clamp: Float = 0.05f
    ): Float {
        if (!cur.isFinite()) return prev ?: 0f
        if (prev == null || !prev.isFinite()) return cur
        val raw = prev * (1 - alpha) + cur * alpha
        val minAllowed = prev * (1 - clamp)
        val maxAllowed = prev * (1 + clamp)
        val clamped = raw.coerceIn(minAllowed, maxAllowed)
        return if (clamped.isFinite()) clamped else prev
    }

    private fun relCapFor(scale: BucketScale, days: Int): Float = when (scale) {
        BucketScale.EXERCISE -> 1.30f
        BucketScale.DAY -> 1.25f
        BucketScale.WEEK -> 1.20f
        BucketScale.MONTH -> if (days >= 365) 1.10f else 1.15f
    }

    // ---------- public API ----------
    public suspend fun calculateIntraProgressionPercent1RMFromExercises(
        exercises: List<ExerciseState>
    ): Pair<DSAreaData, Instruction> {
        val session1RMs = buildMap {
            exercises.forEach { ex ->
                val e1 = estimateSession1RM(ex)
                if (e1 != null && e1.isFinite() && e1 > 0f) put(exerciseKey(ex), e1)
            }
        }
        val exTxt = stringProvider.get(Res.string.ex)
        val points = exercises.mapIndexedNotNull { index, ex ->
            val key = exerciseKey(ex)
            val r1rm = session1RMs[key] ?: return@mapIndexedNotNull null
            val avg = avgWeightPerRep(ex)
            val rel =
                if (r1rm > 0f) (avg / r1rm).coerceIn(0f, 2f) else return@mapIndexedNotNull null
            DSAreaPoint(x = index.toFloat(), y = rel * 100f, xLabel = "$exTxt${index + 1}")
        }
        val data = DSAreaData(points)
        val tip = instructionForPercent1RM(BucketScale.EXERCISE)
        return data to tip
    }

    public suspend fun calculateIntraProgressionPercent1RMFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState
    ): Pair<DSAreaData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)
        val days = daysInclusive(period.range.from.date, period.range.to.date)
        val relCap = relCapFor(scale, days)

        if (scale == BucketScale.EXERCISE) {
            val exs = inRange.flatMap { it.exercises }
            return calculateIntraProgressionPercent1RMFromExercises(exs)
        }

        val buckets = groupTrainingsByBucket(inRange, scale).toList().sortedBy { it.first }
        val points = mutableListOf<DSAreaPoint>()
        var xIdx = 0
        val rollingPrev = mutableMapOf<String, Float>()

        buckets.forEach { (start, ts) ->
            val exs = ts.flatMap { it.exercises }
            val session = exs.mapNotNull { ex ->
                estimateSession1RM(ex)?.takeIf { it > 0f && it.isFinite() }
                    ?.let { exerciseKey(ex) to it }
            }.toMap()

            val rolling = session.mapValues { (k, cur) ->
                val smoothed = smooth(rollingPrev[k], cur, alpha = 0.20f, clamp = 0.05f)
                rollingPrev[k] = smoothed
                smoothed
            }

            var num = 0f
            var den = 0f
            exs.forEach { ex ->
                val key = exerciseKey(ex)
                val r1rm = rolling[key] ?: rollingPrev[key] ?: return@forEach
                val (sumT, sumR) = sums(ex)
                if (sumR <= 0 || r1rm <= 0f) return@forEach
                val avg = sumT / sumR
                val rel = (avg / r1rm).coerceAtLeast(0f).coerceAtMost(relCap)
                num += rel * sumR
                den += sumR
            }
            if (den > 0f) {
                val relPct = (num / den) * 100f
                points += DSAreaPoint(
                    x = xIdx++.toFloat(),
                    y = relPct,
                    xLabel = start.label(scale, stringProvider)
                )
            }
        }

        val data = DSAreaData(points)
        val tip = instructionForPercent1RM(scale)
        return data to tip
    }

    internal fun instructionForPercent1RM(
        scale: BucketScale,
    ): Instruction {
        return when (scale) {
            BucketScale.EXERCISE ->
                Instruction(
                    UiText.Res(Res.string.tooltip_percent1rm_title_training),
                    UiText.Res(Res.string.tooltip_percent1rm_description_training)
                )

            BucketScale.DAY ->
                Instruction(
                    UiText.Res(Res.string.tooltip_percent1rm_title_day),
                    UiText.Res(Res.string.tooltip_percent1rm_description_day)
                )

            BucketScale.WEEK ->
                Instruction(
                    UiText.Res(Res.string.tooltip_percent1rm_title_month),
                    UiText.Res(Res.string.tooltip_percent1rm_description_month)
                )

            BucketScale.MONTH ->
                Instruction(
                    UiText.Res(Res.string.tooltip_percent1rm_title_year),
                    UiText.Res(Res.string.tooltip_percent1rm_description_year)
                )
        }
    }
}