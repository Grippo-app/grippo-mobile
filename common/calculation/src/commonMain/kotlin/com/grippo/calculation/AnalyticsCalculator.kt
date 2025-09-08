package com.grippo.calculation

import androidx.compose.ui.graphics.Color
import com.grippo.calculation.internal.buildBuckets
import com.grippo.calculation.internal.daysInclusive
import com.grippo.calculation.internal.defaultLabeler
import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.internal.groupTrainingsByBucket
import com.grippo.calculation.internal.instructionForEstimated1RM
import com.grippo.calculation.internal.instructionForPercent1RM
import com.grippo.calculation.internal.instructionForStimulus
import com.grippo.calculation.internal.instructionForVolume
import com.grippo.calculation.internal.label
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.contains
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.ex
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.datetime.PeriodState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.TrainingState
import kotlinx.datetime.LocalDateTime
import kotlin.math.pow

/**
 * 📊 Training Analytics Calculator
 *
 * Provides multiple analytic charts and metrics:
 * - 📦 Exercise Volume (Σ weight × reps)
 * - 📈 Intra-Workout Progression:
 *     - %1RM-normalized (relative intensity by exercise order / time buckets)
 *     - Stimulus (tonnage × rel_intensity^α)
 * - 🏋 Estimated 1RM (robust session estimate using Brzycki+Epley)
 *
 * ---
 * 🔎 Inputs
 * - `List<ExerciseState>` → single training (ThisDay)
 * - `List<TrainingState>` → multi-trainings bucketed by PeriodState
 *
 * ⚠️ Assumptions
 * - `iteration.volume.value` stores **weight (kg)**, NOT tonnage.
 * - `iteration.repetitions.value` stores **reps**.
 * - Exercises are ordered by execution (for ThisDay charts).
 * - No special-casing by weight type here; all exercises are treated uniformly.
 */
public class AnalyticsCalculator(
    private val colorProvider: ColorProvider,
    private val stringProvider: StringProvider
) {

    /** 📦 Exercise Volume Chart — Σ(weight × reps) per exercise (ThisDay). */
    public suspend fun calculateExerciseVolumeChartFromExercises(
        exercises: List<ExerciseState>
    ): Pair<DSBarData, Instruction> {
        val colors = colorProvider.get()
        val palette = colors.palette.palette7BlueGrowth // ordered light -> dark
        val exTxt = stringProvider.get(Res.string.ex)

        // 1) Compute values once
        val values: List<Float> = exercises.map { tonnage(it).coerceAtLeast(0f) }
        val n = values.size
        val paletteCount = palette.size

        // 2) Detect trivial case (all values equal → same light color)
        val minValue = values.minOrNull() ?: 0f
        val maxValue = values.maxOrNull() ?: minValue
        val allEqual = (maxValue == minValue)

        // 3) Prepare color mapping: rank-based (lightest=min, darkest=max), uses all colors when possible
        val colorsPerIndex: MutableList<Color> = MutableList(n) { Color.Unspecified }
        if (n > 0) {
            if (paletteCount == 0) {
                // No palette available; leave Unspecified
            } else if (allEqual) {
                for (i in 0 until n) colorsPerIndex[i] = palette.first()
            } else {
                val orderAsc: List<Int> =
                    values.indices.sortedBy { values[it] } // indices sorted by value asc
                for (rank in 0 until n) {
                    val idx = orderAsc[rank]
                    val paletteIdx =
                        if (n <= paletteCount) rank
                        else ((rank.toFloat() * (paletteCount - 1)) / (n - 1)).toInt()
                    colorsPerIndex[idx] = palette[paletteIdx.coerceIn(0, paletteCount - 1)]
                }
            }
        }

        // 4) Build items
        val items: List<DSBarItem> = values.mapIndexed { index, v ->
            DSBarItem(
                label = "$exTxt${index + 1}",
                value = v,
                color = colorsPerIndex.getOrNull(index) ?: Color.Unspecified
            )
        }

        val data = DSBarData(items = items)
        val tip = instructionForVolume(BucketScale.EXERCISE)
        return data to tip
    }

    /**
     * 📦 Exercise Volume across trainings with PeriodState.
     *
     * - ThisDay    -> per-exercise bars (labels = exercise names)
     * - ThisWeek   -> per-day bars       (labels = dates)
     * - ThisMonth  -> per-week bars      (labels = W##)
     * - CUSTOM     -> derived per rules (see deriveScale)
     */
    public suspend fun calculateExerciseVolumeChartFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState
    ): Pair<DSBarData, Instruction> {
        val colors = colorProvider.get()
        val palette = colors.palette.palette7BlueGrowth // ordered light -> dark

        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        return when (scale) {
            BucketScale.EXERCISE -> {
                val exercises = inRange.flatMap { it.exercises }
                calculateExerciseVolumeChartFromExercises(exercises)
            }

            else -> {
                val buckets = buildBuckets(period.range, scale)
                val labeler = defaultLabeler(scale, stringProvider)

                // Group once by bucket start to avoid O(B*N) scanning
                val grouped: Map<LocalDateTime, List<TrainingState>> =
                    groupTrainingsByBucket(inRange, scale)

                // 1) Compute totals aligned with buckets order
                val totals: List<Float> = buckets.map { b ->
                    (grouped[b.start] ?: emptyList()).sumOf { it.tonnage() }.toFloat()
                        .coerceAtLeast(0f)
                }
                val n = totals.size
                val paletteCount = palette.size

                // 2) Detect trivial case (all values equal)
                val minValue = totals.minOrNull() ?: 0f
                val maxValue = totals.maxOrNull() ?: minValue
                val allEqual = (maxValue == minValue)

                // 3) Rank-based color mapping over bucket indices
                val colorsPerIndex: MutableList<Color> = MutableList(n) { Color.Unspecified }
                if (n > 0) {
                    if (paletteCount == 0) {
                        // No palette available; leave Unspecified
                    } else if (allEqual) {
                        for (i in 0 until n) colorsPerIndex[i] = palette.first()
                    } else {
                        val orderAsc: List<Int> = totals.indices.sortedBy { totals[it] }
                        for (rank in 0 until n) {
                            val idx = orderAsc[rank]
                            val paletteIdx =
                                if (n <= paletteCount) rank
                                else ((rank.toFloat() * (paletteCount - 1)) / (n - 1)).toInt()
                            colorsPerIndex[idx] = palette[paletteIdx.coerceIn(0, paletteCount - 1)]
                        }
                    }
                }

                // 4) Build items with mapped colors
                val items: List<DSBarItem> = buckets.mapIndexed { idx, b ->
                    DSBarItem(
                        label = labeler(b),
                        value = totals[idx],
                        color = colorsPerIndex.getOrNull(idx) ?: Color.Unspecified
                    )
                }

                val data = DSBarData(items)
                val tip = instructionForVolume(scale)
                data to tip
            }
        }
    }

    /** 📈 %1RM by exercise order (ThisDay). */
    public suspend fun calculateIntraProgressionPercent1RMFromExercises(
        exercises: List<ExerciseState>
    ): Pair<DSAreaData, Instruction> {
        // 1) session 1RM per exercise (robust estimate)
        val session1RMs: Map<String, Float> = buildMap {
            exercises.forEach { ex ->
                val e1 = estimateSession1RM(ex)
                if (e1 != null && e1.isFinite() && e1 > 0f) {
                    put(exerciseKey(ex), e1)
                }
            }
        }

        // 2) rolling 1RM smoothing (no prior map in ThisDay)
        val rolling1RM: Map<String, Float> = session1RMs.mapValues { (_, current) -> current }

        val exTxt = stringProvider.get(Res.string.ex)

        // 3) %1RM per exercise index
        val points = exercises.mapIndexedNotNull { index, ex ->
            val key = exerciseKey(ex)
            val r1rm = rolling1RM[key] ?: return@mapIndexedNotNull null
            val avg = avgWeightPerRep(ex)
            val rel =
                if (r1rm > 0f) (avg / r1rm).coerceIn(0f, 2f) else return@mapIndexedNotNull null
            DSAreaPoint(
                x = index.toFloat(),
                y = rel * 100f,
                xLabel = "$exTxt${index + 1}"
            )
        }
        val data = DSAreaData(points = points)
        val tip = instructionForPercent1RM(BucketScale.EXERCISE)
        return data to tip
    }

    /** 📈 %1RM across trainings bucketed by PeriodState. */
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

        val buckets: List<Pair<LocalDateTime, List<TrainingState>>> =
            groupTrainingsByBucket(inRange, scale).toList().sortedBy { (start, _) -> start }

        val points = mutableListOf<DSAreaPoint>()
        var xIdx = 0

        // rolling map per exercise key across buckets (EWMA with clamp)
        val rollingPrev = mutableMapOf<String, Float>()

        buckets.forEach { (start, ts) ->
            val exs = ts.flatMap { it.exercises }

            val session = exs.mapNotNull { ex ->
                estimateSession1RM(ex)?.takeIf { it.isFinite() && it > 0f }
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
                val rel = (avg / r1rm)
                    .coerceAtLeast(0f)
                    .coerceAtMost(relCap)

                // Default aggregator = RepsWeighted (keep API minimal)
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

        val data = DSAreaData(points = points)
        val tip = instructionForPercent1RM(scale)
        return data to tip
    }

    /**
     * 📈 Stimulus (ThisDay):
     * stimulus = tonnage × (rel_intensity^alpha)
     * where rel_intensity = avgWeightPerRep / rolling1RM.
     */
    public suspend fun calculateIntraProgressionStimulusFromExercises(
        exercises: List<ExerciseState>
    ): Pair<DSAreaData, Instruction> {
        // session 1RM, no prior smoothing
        val session1RMs =
            exercises.associate { exerciseKey(it) to (estimateSession1RM(it) ?: Float.NaN) }
                .filterValues { it.isFinite() && it > 0f }

        val exTxt = stringProvider.get(Res.string.ex)

        val points = exercises.mapIndexedNotNull { index, ex ->
            val r1rm = session1RMs[exerciseKey(ex)] ?: return@mapIndexedNotNull null
            val (sumT, sumR) = sums(ex)
            if (sumR <= 0) return@mapIndexedNotNull null
            val avg = sumT / sumR
            val rel = (avg / r1rm).coerceAtLeast(0f)
            val alpha = 0.75f // fixed for session view
            val stimulus = sumT * rel.pow(alpha)
            DSAreaPoint(
                x = index.toFloat(),
                y = stimulus,
                xLabel = "$exTxt${index + 1}"
            )
        }

        val data = DSAreaData(points = points)
        val tip = instructionForStimulus(BucketScale.EXERCISE)
        return data to tip
    }

    /**
     * 📈 Stimulus across trainings bucketed by PeriodState:
     * stimulus = Σ(tonnage × (rel^alpha)), alpha internally adapted by span.
     */
    public suspend fun calculateIntraProgressionStimulusFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState
    ): Pair<DSAreaData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)
        val days = daysInclusive(period.range.from.date, period.range.to.date)
        val relCap = relCapFor(scale, days)
        val alpha = stimulusAlphaFor(scale, days)

        if (scale == BucketScale.EXERCISE) {
            val exs = inRange.flatMap { it.exercises }
            return calculateIntraProgressionStimulusFromExercises(exs)
        }

        val buckets: List<Pair<LocalDateTime, List<TrainingState>>> =
            groupTrainingsByBucket(inRange, scale).toList().sortedBy { (start, _) -> start }

        val points = mutableListOf<DSAreaPoint>()
        var xIdx = 0

        // rolling 1RM per exercise across buckets (EWMA with clamp)
        val rollingPrev = mutableMapOf<String, Float>()

        buckets.forEach { (start, ts) ->
            val exs = ts.flatMap { it.exercises }

            val session = exs.mapNotNull { ex ->
                estimateSession1RM(ex)?.takeIf { it.isFinite() && it > 0f }
                    ?.let { exerciseKey(ex) to it }
            }.toMap()

            val rolling = session.mapValues { (k, cur) ->
                val smoothed = smooth(rollingPrev[k], cur, alpha = 0.20f, clamp = 0.05f)
                rollingPrev[k] = smoothed
                smoothed
            }

            var sumStimulus = 0f
            exs.forEach { ex ->
                val key = exerciseKey(ex)
                val r1rm = rolling[key] ?: rollingPrev[key] ?: return@forEach
                val (sumT, sumR) = sums(ex)
                if (sumR <= 0 || r1rm <= 0f) return@forEach
                val avg = sumT / sumR
                val rel = (avg / r1rm)
                    .coerceAtLeast(0f)
                    .coerceAtMost(relCap)
                sumStimulus += sumT * rel.pow(alpha)
            }

            points += DSAreaPoint(
                x = xIdx++.toFloat(),
                y = sumStimulus.coerceAtLeast(0f),
                xLabel = start.label(scale, stringProvider)
            )
        }

        val data = DSAreaData(points = points)
        val tip = instructionForStimulus(scale)
        return data to tip
    }

    /**
     * 🏋 Robust per-exercise 1RM estimate (ThisDay) using:
     * - Brzycki & Epley formulas on sets with 1..12 reps
     * - quality weights favoring lower reps
     * - weighted median across sets (robust to outliers)
     */
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

    /**
     * 🏋 Robust 1RM across trainings with PeriodState.
     * Reduces multiple session 1RMs within a bucket via P90 percentile (no public policy surface).
     */
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
            val buckets: List<Pair<LocalDateTime, List<TrainingState>>> =
                groupTrainingsByBucket(inRange, scale).toList().sortedBy { (start, _) -> start }

            val items = buckets.mapIndexedNotNull { idx, (start, ts) ->
                val e1s = ts
                    .flatMap { it.exercises }
                    .mapNotNull { estimateSession1RM(it) }
                    .filter { it.isFinite() && it > 0f }

                if (e1s.isEmpty()) return@mapIndexedNotNull null

                val reduced = percentile(e1s, 0.90f) // P90 by default

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

    // -------------------------- Internals: relCap & alpha adaptation --------------------------

    private fun relCapFor(scale: BucketScale, days: Int): Float = when (scale) {
        BucketScale.EXERCISE -> 1.30f  // day/session: show peaks
        BucketScale.DAY -> 1.25f
        BucketScale.WEEK -> 1.20f
        BucketScale.MONTH -> if (days >= 365) 1.10f else 1.15f
    }

    private fun stimulusAlphaFor(scale: BucketScale, days: Int): Float {
        // Default hypertrophy-friendly curvature; slightly soften on very long spans
        return if (days >= 180) 0.70f else 0.75f
    }


    // -------------------------- Helpers: math, tonnage, keys, smoothing --------------------------

    internal data class IterationMetrics(
        val weight: Float,
        val reps: Int,
        val tonnage: Float,
        val isValid: Boolean
    )

    /** Returns (Σ(weight*reps), Σ(reps)). */
    internal fun sums(ex: ExerciseState): Pair<Float, Int> {
        var totalTonnage = 0f
        var totalReps = 0
        ex.iterations.forEach { iteration ->
            val metrics = extractIterationMetrics(iteration)
            if (metrics.isValid) {
                totalTonnage += metrics.tonnage
                totalReps += metrics.reps
            }
        }
        return totalTonnage to totalReps
    }

    /**
     * Extracts weight, reps, and tonnage from an iteration.
     * Handles null values and validation.
     */
    internal fun extractIterationMetrics(iteration: IterationState): IterationMetrics {
        val weight = iteration.volume.value ?: 0f
        val reps = iteration.repetitions.value ?: 0
        val tonnage = if (weight > 0f && reps > 0) weight * reps else 0f
        val isValid = weight > 0f && reps > 0
        return IterationMetrics(weight, reps, tonnage, isValid)
    }

    /** Σ(weight*reps) for an exercise. */
    private fun tonnage(ex: ExerciseState): Float =
        ex.iterations.fold(0f) { acc, iteration ->
            val weight = iteration.volume.value ?: 0f
            val reps = iteration.repetitions.value ?: 0
            val load = weight
            if (reps == 0 || load <= 0f) acc else acc + load * reps
        }

    /** Total session tonnage across a training (Double to avoid float round-trip). */
    private fun TrainingState.tonnage(): Double {
        var t = 0.0
        this.exercises.forEach { ex ->
            val exerciseTonnage = ex.iterations.fold(0f) { acc, iteration ->
                val weight = iteration.volume.value ?: 0f
                val reps = iteration.repetitions.value ?: 0
                val load = weight
                if (reps == 0 || load <= 0f) acc else acc + load * reps
            }
            t += exerciseTonnage
        }
        return t
    }

    /** Weighted average weight per rep = Σ(w*r)/Σ(r). */
    private fun avgWeightPerRep(ex: ExerciseState): Float {
        val (sumT, sumR) = sums(ex)
        return if (sumR > 0) sumT / sumR else 0f
    }

    /** Key to identify an exercise when storing/smoothing rolling 1RM. */
    private fun exerciseKey(ex: ExerciseState): String =
        ex.exerciseExample?.id ?: ex.name

    /** EWMA smoothing with change clamp (±maxChangePct); warm start without clamp. */
    private fun smooth(
        prev: Float?,
        cur: Float,
        alpha: Float = 0.20f,
        clamp: Float = 0.05f
    ): Float {
        if (!cur.isFinite()) return prev ?: 0f
        if (prev == null || !prev.isFinite()) return cur // warm start
        val raw = prev * (1 - alpha) + cur * alpha
        val minAllowed = prev * (1 - clamp)
        val maxAllowed = prev * (1 + clamp)
        val clamped = raw.coerceIn(minAllowed, maxAllowed)
        return if (clamped.isFinite()) clamped else prev
    }

    // -------------------------- 1RM estimation core --------------------------

    /** Brzycki 1RM; valid for 1..12 reps. */
    private fun brzycki(weight: Float, reps: Float): Float? {
        if (reps < 1f || reps > 12f) return null
        val denom = 1.0278f - 0.0278f * reps
        if (denom <= 0f) return null
        return weight / denom
    }

    /** Epley 1RM; valid for 1..12 reps (practical range). */
    private fun epley(weight: Float, reps: Float): Float? {
        if (reps < 1f || reps > 12f) return null
        return weight * (1f + reps / 30f)
    }

    /** Down-weights high-rep estimates; peak quality near ~4 reps. */
    private fun qualityWeight(reps: Float): Float {
        val d = reps - 4f
        return 1f / (1f + d * d) // 1 / (1 + (r-4)^2)
    }

    /** Weighted median; robust to outliers. */
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

    /** Session 1RM from exercise iterations using Brzycki+Epley with quality weights. */
    private fun estimateSession1RM(ex: ExerciseState): Float? {
        val pairs = mutableListOf<Pair<Float, Float>>() // (e1rm, weight)
        ex.iterations.forEach { itn ->
            val w = itn.volume.value ?: return@forEach
            val rInt = itn.repetitions.value ?: return@forEach
            val r = rInt.toFloat()
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

    // -------------------------- Misc helpers --------------------------

    /** Percentile (0..1) over Float list; returns 0f for empty or invalid input. */
    private fun percentile(values: List<Float>, p: Float): Float {
        if (values.isEmpty()) return 0f
        val validValues = values.filter { it.isFinite() }
        if (validValues.isEmpty()) return 0f

        val sorted = validValues.sorted()
        val clamped = p.coerceIn(0f, 1f)
        val idx = ((sorted.size - 1) * clamped).toDouble()
        val lo = idx.toInt()
        val hi = minOf(lo + 1, sorted.lastIndex)
        val frac = (idx - lo).toFloat()
        return (sorted[lo] * (1 - frac) + sorted[hi] * frac).takeIf { it.isFinite() } ?: 0f
    }
}