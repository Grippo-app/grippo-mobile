package com.grippo.calculation

import com.grippo.calculation.internal.InternalCalculationUtils
import com.grippo.calculation.internal.InternalCalculationUtils.startOfMonth
import com.grippo.calculation.internal.InternalCalculationUtils.startOfWeek
import com.grippo.calculation.internal.isoWeekNumber
import com.grippo.calculation.models.Bucket
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.calculation.models.daysInclusive
import com.grippo.calculation.models.deriveScale
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
import com.grippo.date.utils.contains
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
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
import com.grippo.design.resources.provider.tooltip_percent1rm_description_day
import com.grippo.design.resources.provider.tooltip_percent1rm_description_month
import com.grippo.design.resources.provider.tooltip_percent1rm_description_training
import com.grippo.design.resources.provider.tooltip_percent1rm_description_year
import com.grippo.design.resources.provider.tooltip_percent1rm_title_day
import com.grippo.design.resources.provider.tooltip_percent1rm_title_month
import com.grippo.design.resources.provider.tooltip_percent1rm_title_training
import com.grippo.design.resources.provider.tooltip_percent1rm_title_year
import com.grippo.design.resources.provider.tooltip_stimulus_description_day
import com.grippo.design.resources.provider.tooltip_stimulus_description_month
import com.grippo.design.resources.provider.tooltip_stimulus_description_training
import com.grippo.design.resources.provider.tooltip_stimulus_description_year
import com.grippo.design.resources.provider.tooltip_stimulus_title_day
import com.grippo.design.resources.provider.tooltip_stimulus_title_month
import com.grippo.design.resources.provider.tooltip_stimulus_title_training
import com.grippo.design.resources.provider.tooltip_stimulus_title_year
import com.grippo.design.resources.provider.tooltip_volume_description_day
import com.grippo.design.resources.provider.tooltip_volume_description_month
import com.grippo.design.resources.provider.tooltip_volume_description_training
import com.grippo.design.resources.provider.tooltip_volume_description_year
import com.grippo.design.resources.provider.tooltip_volume_title_day
import com.grippo.design.resources.provider.tooltip_volume_title_month
import com.grippo.design.resources.provider.tooltip_volume_title_training
import com.grippo.design.resources.provider.tooltip_volume_title_year
import com.grippo.design.resources.provider.w
import com.grippo.state.datetime.PeriodState
import com.grippo.state.formatters.UiText
import com.grippo.state.trainings.ExerciseState
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

    // -------------------------- Public API (only used methods) --------------------------

    /** 📦 Exercise Volume Chart — Σ(weight × reps) per exercise (ThisDay). */
    public suspend fun calculateExerciseVolumeChartFromExercises(
        exercises: List<ExerciseState>
    ): Pair<DSBarData, Instruction> {
        val colors = colorProvider.get()
        val palette = colors.palette.palette9Blue

        val exTxt = stringProvider.get(Res.string.ex)

        val items = exercises.mapIndexed { index, ex ->
            val tonnage = tonnage(ex).coerceAtLeast(0f)
            DSBarItem(
                label = "$exTxt${index + 1}",
                value = tonnage,
                color = palette[index % palette.size]
            )
        }
        val data = DSBarData(items = items)
        val tip = instructionFor(metric = Metric.VOLUME, scale = BucketScale.EXERCISE)
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
        val palette = colors.palette.palette9Blue

        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        return when (scale) {
            BucketScale.EXERCISE -> {
                val exercises = inRange.flatMap { it.exercises }
                calculateExerciseVolumeChartFromExercises(exercises)
            }

            else -> {
                val buckets = buildBuckets(period.range, scale)
                val labeler = defaultLabeler(scale)

                // Group once by bucket start to avoid O(B*N) scanning
                val grouped: Map<LocalDateTime, List<TrainingState>> =
                    groupTrainingsByBucket(inRange, scale)

                val items = buckets.mapIndexed { idx, b ->
                    val total = (grouped[b.start] ?: emptyList())
                        .sumOf { it.tonnage() }
                        .toFloat()
                    DSBarItem(
                        label = labeler(b),
                        value = total.coerceAtLeast(0f),
                        color = palette[idx % palette.size]
                    )
                }
                val data = DSBarData(items)
                val tip = instructionFor(metric = Metric.VOLUME, scale = scale)
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
        val tip = instructionFor(metric = Metric.PCT1RM, scale = BucketScale.EXERCISE)
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
                    xLabel = start.label(scale)
                )
            }
        }

        val data = DSAreaData(points = points)
        val tip = instructionFor(metric = Metric.PCT1RM, scale = scale)
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
        val tip = instructionFor(metric = Metric.STIMULUS, scale = BucketScale.EXERCISE)
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
                xLabel = start.label(scale)
            )
        }

        val data = DSAreaData(points = points)
        val tip = instructionFor(metric = Metric.STIMULUS, scale = scale)
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
        val palette = colors.palette.palette18Colorful

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
        val tip = instructionFor(metric = Metric.E1RM, scale = BucketScale.EXERCISE)
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
        val palette = colors.palette.palette18Colorful

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
                    label = start.label(scale),
                    value = reduced.coerceAtLeast(0f),
                    color = palette[idx % palette.size]
                )
            }
            val data = DSBarData(items)
            val tip = instructionFor(metric = Metric.E1RM, scale = scale)
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

    // -------------------------- Tooltip selector --------------------------

    private enum class Metric { VOLUME, PCT1RM, STIMULUS, E1RM }

    private fun instructionFor(metric: Metric, scale: BucketScale): Instruction {
        return when (metric) {
            Metric.VOLUME -> when (scale) {
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

            Metric.PCT1RM -> when (scale) {
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

            Metric.STIMULUS -> when (scale) {
                BucketScale.EXERCISE ->
                    Instruction(
                        UiText.Res(Res.string.tooltip_stimulus_title_training),
                        UiText.Res(Res.string.tooltip_stimulus_description_training)
                    )

                BucketScale.DAY ->
                    Instruction(
                        UiText.Res(Res.string.tooltip_stimulus_title_day),
                        UiText.Res(Res.string.tooltip_stimulus_description_day)
                    )

                BucketScale.WEEK ->
                    Instruction(
                        UiText.Res(Res.string.tooltip_stimulus_title_month),
                        UiText.Res(Res.string.tooltip_stimulus_description_month)
                    )

                BucketScale.MONTH ->
                    Instruction(
                        UiText.Res(Res.string.tooltip_stimulus_title_year),
                        UiText.Res(Res.string.tooltip_stimulus_description_year)
                    )
            }

            Metric.E1RM -> when (scale) {
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

    // -------------------------- Helpers: math, tonnage, keys, smoothing --------------------------

    /** Returns (Σ(weight*reps), Σ(reps)). */
    private fun sums(ex: ExerciseState): Pair<Float, Int> =
        InternalCalculationUtils.sumIterationsMetrics(ex.iterations)

    /** Σ(weight*reps) for an exercise. */
    private fun tonnage(ex: ExerciseState): Float =
        InternalCalculationUtils.calculateExerciseWorkload(
            ex,
            InternalCalculationUtils.WorkloadStrategy.Volume
        )

    /** Total session tonnage across a training (Double to avoid float round-trip). */
    private fun TrainingState.tonnage(): Double {
        var t = 0.0
        this.exercises.forEach { ex ->
            val exerciseTonnage = InternalCalculationUtils.calculateExerciseWorkload(
                ex,
                InternalCalculationUtils.WorkloadStrategy.Volume
            )
            t += exerciseTonnage
        }
        return t
    }

    /** Weighted average weight per rep = Σ(w*r)/Σ(r). */
    private fun avgWeightPerRep(ex: ExerciseState): Float =
        InternalCalculationUtils.avgWeightPerRep(ex.iterations)

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

    // -------------------------- Period → Buckets logic --------------------------

    /**
     * CUSTOM rules:
     * 1) If it's a day → EXERCISE
     * 2) If < 1 month AND days not divisible by 7 → DAY
     * 3) If < 1 month AND days divisible by 7 → WEEK
     * 4) If up to a year (> month) → if whole-month aligned → MONTH, else → WEEK
     * 5) Otherwise → MONTH
     */

    private fun buildBuckets(range: DateRange, scale: BucketScale): List<Bucket> = when (scale) {
        BucketScale.EXERCISE -> emptyList() // handled by per-exercise path
        BucketScale.DAY -> days(range.from, range.to)
        BucketScale.WEEK -> weeks(range.from, range.to)
        BucketScale.MONTH -> months(range.from, range.to)
    }

    // ---- Bucket builders (LocalDateTime-safe) ----

    private fun days(from: LocalDateTime, to: LocalDateTime): List<Bucket> =
        InternalCalculationUtils.buildDayBuckets(DateRange(from, to))

    private fun weeks(from: LocalDateTime, to: LocalDateTime): List<Bucket> =
        InternalCalculationUtils.buildWeekBuckets(DateRange(from, to))

    private fun months(from: LocalDateTime, to: LocalDateTime): List<Bucket> =
        InternalCalculationUtils.buildMonthBuckets(DateRange(from, to))

    // ---- Grouping and labeling ----

    private fun groupTrainingsByBucket(
        trainings: List<TrainingState>,
        scale: BucketScale
    ): Map<LocalDateTime, List<TrainingState>> {
        return trainings.groupBy { t ->
            when (scale) {
                BucketScale.EXERCISE -> t.createdAt // not used
                BucketScale.DAY -> DateTimeUtils.startOfDay(t.createdAt)
                BucketScale.WEEK -> startOfWeek(t.createdAt)
                BucketScale.MONTH -> startOfMonth(t.createdAt)
            }
        }
    }

    private suspend fun defaultLabeler(scale: BucketScale): (Bucket) -> String {
        val w = stringProvider.get(Res.string.w)
        return when (scale) {
            BucketScale.DAY -> { b ->
                DateTimeUtils.format(b.start, DateFormat.WEEKDAY_SHORT)
            }

            BucketScale.WEEK -> { b ->
                "$w${isoWeekNumber(b.start)}-${
                    DateTimeUtils.format(b.start, DateFormat.MONTH_SHORT)
                }"
            }

            BucketScale.MONTH -> { b ->
                DateTimeUtils.format(b.start, DateFormat.MONTH_SHORT)
            }

            BucketScale.EXERCISE -> { _ -> "" }
        }
    }

    private suspend fun LocalDateTime.label(scale: BucketScale): String {
        val w = stringProvider.get(Res.string.w)
        return when (scale) {
            BucketScale.DAY, BucketScale.EXERCISE -> {
                DateTimeUtils.format(this, DateFormat.DATE_DD_MMM) // e.g., "02 Sep"
            }

            BucketScale.WEEK -> {
                "$w${isoWeekNumber(this)}-${
                    DateTimeUtils.format(this, DateFormat.MONTH_SHORT)
                }"
            }

            BucketScale.MONTH -> {
                DateTimeUtils.format(this, DateFormat.MONTH_SHORT)
            }
        }
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