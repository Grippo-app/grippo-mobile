package com.grippo.calculation

import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.state.datetime.PeriodState
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.TrainingState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.pow

/**
 * 📊 Training Analytics Calculator
 *
 * Provides multiple analytic charts and metrics:
 * - 📦 Exercise Volume (Σ weight × reps)
 * - 📈 Intra-Workout Progression:
 *     - Absolute (avg kg/rep by exercise order)
 *     - %1RM-normalized (relative intensity by exercise order)
 *     - Stimulus (tonnage × rel_intensity^α)
 * - 🏋 Estimated 1RM (robust session estimate using Brzycki+Epley)
 *
 * ---
 * 🔎 Inputs
 * - `List<ExerciseState>` → single training
 * - `List<TrainingState>` → multiple trainings (flattened)
 *
 * ⚠️ Assumptions
 * - `iteration.volume.value` stores **weight (kg)**, NOT tonnage.
 * - `iteration.repetitions.value` stores **reps**.
 * - Exercises are ordered by execution for progression charts.
 * - 1RM is only estimated for non-bodyweight exercises (unless system load is provided).
 *
 * Notes:
 * - Comments are in English by user's preference.
 */
public class AnalyticsCalculator(
    private val colorProvider: ColorProvider,
    private val policy: Policy = Policy()
) {

    // -------------------------- Policy knobs --------------------------

    /** Aggregation policies for period buckets. */
    public data class Policy(
        val intensityAggregator: IntensityAggregator = IntensityAggregator.RepsWeighted,
        val stimulusAlpha: Float = 0.75f,      // exponent for relative intensity
        val relCap: Float = 1.2f,              // cap for rel in bucketed modes
        val oneRMReducer: OneRMAggregator = OneRMAggregator.P90
    )

    /** How to average relative intensity across sets/exercises within a bucket. */
    public enum class IntensityAggregator { RepsWeighted, TonnageWeighted }

    /** How to reduce multiple session 1RMs within a bucket to one value. */
    public enum class OneRMAggregator { Max, P90, Median }

    // -------------------------- Public result/metrics models --------------------------

    /** Trend KPIs computed from a Y-series over time or exercise order. */
    public data class IntraTrendMetrics(
        val slopePerStep: Float,   // linear regression slope in y-units per step
        val dropPercent: Float     // (%): tail avg vs head avg (last 2 vs first 2)
    )

    /** Container returning chart data together with its trend metrics. */
    public data class IntraResult(
        val data: DSAreaData,
        val metrics: IntraTrendMetrics
    )

    // -------------------------- Exercise Volume --------------------------

    /** 📦 Exercise Volume Chart — Σ(weight × reps) per exercise (single-day view). */
    public suspend fun calculateExerciseVolumeChartFromExercises(
        exercises: List<ExerciseState>
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.palette.palette9Blue

        val items = exercises.mapIndexed { index, ex ->
            val tonnage = tonnage(ex).coerceAtLeast(0f)
            DSBarItem(
                label = ex.name,
                value = tonnage,
                color = palette[index % palette.size]
            )
        }

        return DSBarData(items = items)
    }

    /**
     * 📦 Exercise Volume across trainings with PeriodState.
     *
     * - ThisDay    -> per-exercise bars (labels = exercise names)
     * - ThisWeek   -> per-day bars       (labels = dates)
     * - ThisMonth  -> per-week bars      (labels = week ranges or W##)
     * - CUSTOM     -> derived per rules (see deriveScale)
     */
    public suspend fun calculateExerciseVolumeChartFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.palette.palette9Blue

        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        return when (scale) {
            BucketScale.EXERCISE -> {
                // Day-mode: per-exercise within range
                val exercises = inRange.flatMap { it.exercises }
                calculateExerciseVolumeChartFromExercises(exercises)
            }

            else -> {
                val buckets = buildBuckets(period.range, scale)
                val labeler = defaultLabeler(scale)
                val items = buckets.mapIndexed { idx, b ->
                    val total = inRange
                        .asSequence()
                        .filter { it.belongsTo(b) }
                        .sumOf { it.tonnage() }
                        .toFloat()
                    DSBarItem(
                        label = labeler(b),
                        value = total.coerceAtLeast(0f),
                        color = palette[idx % palette.size]
                    )
                }
                DSBarData(items)
            }
        }
    }

    // -------------------------- Intra-Workout Progression (Absolute) --------------------------

    /** 📈 Intra-Workout Load Progression (absolute avg kg/rep by exercise order). */
    public fun calculateIntraProgressionAbsoluteFromExercises(
        exercises: List<ExerciseState>
    ): IntraResult {
        val points = exercises.mapIndexed { index, ex ->
            val avg = avgWeightPerRep(ex)
            DSAreaPoint(x = index.toFloat(), y = avg.coerceAtLeast(0f), xLabel = ex.name)
        }
        val data = DSAreaData(points = points)
        val metrics = computeTrend(points) { it.y }
        return IntraResult(data, metrics)
    }

    /** 📈 Intra Absolute across trainings with PeriodState. */
    public fun calculateIntraProgressionAbsoluteFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState
    ): IntraResult {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        if (scale == BucketScale.EXERCISE) {
            val exs = inRange.flatMap { it.exercises }
            return calculateIntraProgressionAbsoluteFromExercises(exs)
        }

        val buckets: List<Pair<LocalDate, List<TrainingState>>> =
            groupTrainingsByBucket(inRange, scale)
                .toList()
                .sortedBy { (start, _) -> start }

        val points = buckets.mapIndexedNotNull { idx, (start, ts) ->
            val (sumT, sumR) = ts.flatMap { it.exercises }.fold(0f to 0) { acc, ex ->
                val (t, r) = sums(ex)
                (acc.first + t) to (acc.second + r)
            }
            if (sumR <= 0) null
            else DSAreaPoint(idx.toFloat(), (sumT / sumR).coerceAtLeast(0f), start.label(scale))
        }
        val data = DSAreaData(points)
        val metrics = computeTrend(points) { it.y }
        return IntraResult(data, metrics)
    }

    // -------------------------- Intra-Workout Progression (%1RM) --------------------------

    /**
     * 📈 Intra-Workout %1RM-normalized trend (single session).
     * Each exercise is mapped to relative intensity:
     * rel = avgWeightPerRep / rolling1RM  (returned as % on Y-axis).
     */
    public fun calculateIntraProgressionPercent1RMFromExercises(
        exercises: List<ExerciseState>,
        previous1RM: Map<String, Float>? = null,
        ewmaAlpha: Float = 0.20f,
        maxChangeClamp: Float = 0.05f,
        skipBodyweight: Boolean = true,
        systemLoad: ((ExerciseState) -> Float?)? = null
    ): IntraResult {
        // 1) session 1RM per exercise (robust estimate)
        val session1RMs: Map<String, Float> = buildMap {
            exercises.forEach { ex ->
                val key = exerciseKey(ex)
                val isBW = ex.exerciseExample?.weightType == WeightTypeEnumState.BODY_WEIGHT
                val e1 = if (isBW && systemLoad != null) {
                    estimateSession1RMWithLoader(ex) { itn ->
                        val r = itn.repetitions.value ?: return@estimateSession1RMWithLoader null
                        val w = systemLoad(ex) ?: return@estimateSession1RMWithLoader null
                        w to r
                    }
                } else {
                    if (isBW && skipBodyweight) null else estimateSession1RM(ex)
                }
                if (e1 != null && e1.isFinite() && e1 > 0f) put(key, e1)
            }
        }

        // 2) rolling 1RM smoothing
        val rolling1RM: Map<String, Float> = buildMap {
            session1RMs.forEach { (key, current) ->
                val prev = previous1RM?.get(key)
                val smoothed = smoothRolling1RM(current, prev, ewmaAlpha, maxChangeClamp)
                put(key, smoothed)
            }
        }

        // 3) %1RM per exercise order
        val points = exercises.mapIndexedNotNull { index, ex ->
            val key = exerciseKey(ex)
            val r1rm = rolling1RM[key] ?: return@mapIndexedNotNull null
            val avg = avgWeightPerRep(ex)
            val rel =
                if (r1rm > 0f) (avg / r1rm).coerceIn(0f, 2f) else return@mapIndexedNotNull null
            DSAreaPoint(
                x = index.toFloat(),
                y = rel * 100f, // % on Y axis
                xLabel = ex.name
            )
        }

        val data = DSAreaData(points = points)
        val metrics = computeTrend(points) { it.y }
        return IntraResult(data, metrics)
    }

    /** 📈 Intra %1RM across trainings with PeriodState (bucketed). */
    public fun calculateIntraProgressionPercent1RMFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        previous1RM: Map<String, Float>? = null,
        ewmaAlpha: Float = 0.20f,
        maxChangeClamp: Float = 0.05f,
        skipBodyweight: Boolean = true,
        systemLoad: ((ExerciseState) -> Float?)? = null
    ): IntraResult {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        if (scale == BucketScale.EXERCISE) {
            val exercises = inRange.flatMap { it.exercises }
            return calculateIntraProgressionPercent1RMFromExercises(
                exercises, previous1RM, ewmaAlpha, maxChangeClamp, skipBodyweight, systemLoad
            )
        }

        val buckets: List<Pair<LocalDate, List<TrainingState>>> =
            groupTrainingsByBucket(inRange, scale)
                .toList()
                .sortedBy { (start, _) -> start }

        val rollingPrev = previous1RM?.toMutableMap() ?: mutableMapOf()
        val points = mutableListOf<DSAreaPoint>()
        var xIdx = 0

        buckets.forEach { (start, ts) ->
            val exs = ts.flatMap { it.exercises }

            val session = buildMap {
                exs.forEach { ex ->
                    val key = exerciseKey(ex)
                    val isBW = ex.exerciseExample?.weightType == WeightTypeEnumState.BODY_WEIGHT
                    val e1 = if (isBW && systemLoad != null) {
                        estimateSession1RMWithLoader(ex) { itn ->
                            val r =
                                itn.repetitions.value ?: return@estimateSession1RMWithLoader null
                            val w = systemLoad(ex) ?: return@estimateSession1RMWithLoader null
                            w to r
                        }
                    } else {
                        if (isBW && skipBodyweight) null else estimateSession1RM(ex)
                    }
                    if (e1 != null && e1.isFinite() && e1 > 0f) put(key, e1)
                }
            }

            val rolling = session.mapValues { (k, cur) ->
                smoothRolling1RM(cur, rollingPrev[k], ewmaAlpha, maxChangeClamp)
            }
            rolling.forEach { (k, v) -> rollingPrev[k] = v }

            var num = 0f
            var den = 0f
            exs.forEach { ex ->
                val key = exerciseKey(ex)
                val r1rm = rolling[key] ?: return@forEach
                val (sumT, sumR) = sums(ex)
                if (sumR <= 0 || r1rm <= 0f) return@forEach
                val avg = sumT / sumR
                val rel = (avg / r1rm).coerceAtMost(policy.relCap).coerceAtLeast(0f)

                when (policy.intensityAggregator) {
                    IntensityAggregator.RepsWeighted -> {
                        num += rel * sumR
                        den += sumR
                    }

                    IntensityAggregator.TonnageWeighted -> {
                        num += rel * sumT
                        den += sumT
                    }
                }
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

        val data = DSAreaData(points)
        val metrics = computeTrend(points) { it.y }
        return IntraResult(data, metrics)
    }

    // -------------------------- Intra-Workout Progression (Stimulus) --------------------------

    /**
     * 📈 Stimulus by exercise order:
     * stimulus = tonnage × (rel_intensity^alpha),
     * where rel_intensity = avgWeightPerRep / rolling1RM.
     */
    public fun calculateIntraProgressionStimulusFromExercises(
        exercises: List<ExerciseState>,
        previous1RM: Map<String, Float>? = null,
        alphaRelIntensity: Float = policy.stimulusAlpha,
        ewmaAlpha: Float = 0.20f,
        maxChangeClamp: Float = 0.05f
    ): IntraResult {
        // session 1RM and rolling smoothing
        val session1RMs =
            exercises.associate { exerciseKey(it) to (estimateSession1RM(it) ?: Float.NaN) }
                .filterValues { it.isFinite() && it > 0f }
        val rolling = session1RMs.mapValues { (k, cur) ->
            smoothRolling1RM(cur, previous1RM?.get(k), ewmaAlpha, maxChangeClamp)
        }

        val points = exercises.mapIndexedNotNull { index, ex ->
            val r1rm = rolling[exerciseKey(ex)] ?: return@mapIndexedNotNull null
            val (sumTonnage, sumReps) = sums(ex)
            if (sumReps <= 0) return@mapIndexedNotNull null
            val avg = sumTonnage / sumReps
            val rel = (avg / r1rm).coerceAtLeast(0f)
            val stimulus = sumTonnage * rel.pow(alphaRelIntensity)
            DSAreaPoint(x = index.toFloat(), y = stimulus, xLabel = ex.name)
        }

        val data = DSAreaData(points = points)
        val metrics = computeTrend(points) { it.y }
        return IntraResult(data, metrics)
    }

    /** 📈 Stimulus across trainings with PeriodState (bucketed). */
    public fun calculateIntraProgressionStimulusFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        alphaRelIntensity: Float = policy.stimulusAlpha,
        ewmaAlpha: Float = 0.20f,
        maxChangeClamp: Float = 0.05f
    ): IntraResult {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        if (scale == BucketScale.EXERCISE) {
            val exercises = inRange.flatMap { it.exercises }
            return calculateIntraProgressionStimulusFromExercises(
                exercises, previous1RM = null,
                alphaRelIntensity = alphaRelIntensity,
                ewmaAlpha = ewmaAlpha,
                maxChangeClamp = maxChangeClamp
            )
        }

        val buckets: List<Pair<LocalDate, List<TrainingState>>> =
            groupTrainingsByBucket(inRange, scale)
                .toList()
                .sortedBy { (start, _) -> start }

        val rollingPrev = mutableMapOf<String, Float>()
        val points = mutableListOf<DSAreaPoint>()
        var xIdx = 0

        buckets.forEach { (start, ts) ->
            val exs = ts.flatMap { it.exercises }

            val session = exs.mapNotNull { ex ->
                val e1 = estimateSession1RM(ex)
                if (e1 != null && e1.isFinite() && e1 > 0f) exerciseKey(ex) to e1 else null
            }.toMap()

            val rolling = session.mapValues { (k, cur) ->
                smoothRolling1RM(cur, rollingPrev[k], ewmaAlpha, maxChangeClamp)
            }
            rolling.forEach { (k, v) -> rollingPrev[k] = v }

            var sumStimulus = 0f
            exs.forEach { ex ->
                val r1rm = rolling[exerciseKey(ex)] ?: return@forEach
                val (sumT, sumR) = sums(ex)
                if (sumR <= 0 || r1rm <= 0f) return@forEach
                val avg = sumT / sumR
                val rel = (avg / r1rm).coerceAtMost(policy.relCap).coerceAtLeast(0f)
                sumStimulus += sumT * rel.pow(alphaRelIntensity)
            }

            points += DSAreaPoint(
                x = xIdx++.toFloat(),
                y = sumStimulus.coerceAtLeast(0f),
                xLabel = start.label(scale)
            )
        }

        val data = DSAreaData(points)
        val metrics = computeTrend(points) { it.y }
        return IntraResult(data, metrics)
    }

    // -------------------------- Estimated 1RM (robust) --------------------------

    /**
     * 🏋 Robust per-exercise 1RM estimate (within this session) using:
     * - Brzycki & Epley formulas on sets with 1..12 reps
     * - quality weights favoring lower reps
     * - weighted median across sets (robust to outliers)
     */
    public suspend fun calculateEstimated1RMFromExercises(
        exercises: List<ExerciseState>,
        skipBodyweight: Boolean = true
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.palette.palette18Colorful

        val items = exercises.mapIndexedNotNull { index, ex ->
            val isBodyweight = ex.exerciseExample?.weightType == WeightTypeEnumState.BODY_WEIGHT
            if (isBodyweight && skipBodyweight) return@mapIndexedNotNull null

            val e1 = estimateSession1RM(ex) ?: return@mapIndexedNotNull null
            DSBarItem(
                label = ex.name,
                value = e1.coerceAtLeast(0f),
                color = palette[index % palette.size]
            )
        }
        return DSBarData(items = items)
    }

    /**
     * 🏋 Robust 1RM across trainings with PeriodState.
     * For non-day buckets, reduces multiple session 1RMs via policy.oneRMReducer (Max|P90|Median).
     */
    public suspend fun calculateEstimated1RMFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        skipBodyweight: Boolean = true
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.palette.palette18Colorful

        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        if (scale == BucketScale.EXERCISE) {
            val exs = inRange.flatMap { it.exercises }
            return calculateEstimated1RMFromExercises(exs, skipBodyweight)
        }

        val buckets: List<Pair<LocalDate, List<TrainingState>>> =
            groupTrainingsByBucket(inRange, scale)
                .toList()
                .sortedBy { (start, _) -> start }

        val items = buckets.mapIndexed { idx, (start, ts) ->
            val e1s = ts.flatMap { it.exercises }
                .mapNotNull { estimateSession1RM(it) }
                .filter { it.isFinite() && it > 0f }

            val reduced = when (policy.oneRMReducer) {
                OneRMAggregator.Max -> e1s.maxOrNull() ?: 0f
                OneRMAggregator.P90 -> percentile(e1s, 0.90f)
                OneRMAggregator.Median -> percentile(e1s, 0.50f)
            }

            DSBarItem(
                label = start.label(scale),
                value = reduced.coerceAtLeast(0f),
                color = palette[idx % palette.size]
            )
        }
        return DSBarData(items)
    }

    // -------------------------- Helpers: math, tonnage, keys --------------------------

    /** Returns (Σ(weight*reps), Σ(reps)). */
    private fun sums(ex: ExerciseState): Pair<Float, Int> {
        var sumT = 0f
        var sumR = 0
        ex.iterations.forEach { itn ->
            val w = itn.volume.value
            val r = itn.repetitions.value
            if (w != null && r != null && r > 0) {
                sumT += w * r
                sumR += r
            }
        }
        return sumT to sumR
    }

    /** Σ(weight*reps) for an exercise. */
    private fun tonnage(ex: ExerciseState): Float {
        var t = 0f
        ex.iterations.forEach { itn ->
            val w = itn.volume.value
            val r = itn.repetitions.value
            if (w != null && r != null && r > 0) t += w * r
        }
        return t
    }

    /** Total session tonnage across all exercises in a training (Double to avoid float round-trip). */
    private fun TrainingState.tonnage(): Double {
        var t = 0.0
        this.exercises.forEach { ex ->
            ex.iterations.forEach { itn ->
                val w = itn.volume.value
                val r = itn.repetitions.value
                if (w != null && r != null && r > 0) t += (w * r)
            }
        }
        return t
    }

    /** Weighted average weight per rep = Σ(w*r)/Σ(r). */
    private fun avgWeightPerRep(ex: ExerciseState): Float {
        val (sumT, sumR) = sums(ex)
        return if (sumR > 0) sumT / sumR else 0f
    }

    /** Key to identify an exercise when storing rolling 1RM. */
    private fun exerciseKey(ex: ExerciseState): String {
        return ex.exerciseExample?.id ?: ex.name
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
            val avg = listOfNotNull(b, e).let { if (it.isEmpty()) null else it.sum() / it.size }
            if (avg != null) {
                val q = qualityWeight(r)
                pairs += avg to q
            }
        }
        if (pairs.isEmpty()) return null
        return weightedMedian(pairs)
    }

    /**
     * Estimate session 1RM with a custom loader for (weight,reps),
     * e.g., to inject system load for bodyweight moves.
     */
    private fun estimateSession1RMWithLoader(
        ex: ExerciseState,
        loader: (itn: IterationState) -> Pair<Float, Int>?
    ): Float? {
        val pairs = mutableListOf<Pair<Float, Float>>() // (e1rm, weight)
        ex.iterations.forEach { itn ->
            val wp = loader(itn) ?: return@forEach
            val w = wp.first
            val r = wp.second.toFloat()
            val b = brzycki(w, r)
            val e = epley(w, r)
            val avg = listOfNotNull(b, e).let { if (it.isEmpty()) null else it.sum() / it.size }
            if (avg != null) {
                val q = qualityWeight(r)
                pairs += avg to q
            }
        }
        if (pairs.isEmpty()) return null
        return weightedMedian(pairs)
    }

    /** EWMA smoothing with change clamp (±maxChangePct). */
    private fun smoothRolling1RM(
        current: Float,
        previous: Float?,
        alpha: Float,
        maxChangePct: Float
    ): Float {
        val raw = if (previous == null) current else (previous * (1f - alpha) + current * alpha)
        if (previous == null) return raw
        val minAllowed = previous * (1f - maxChangePct)
        val maxAllowed = previous * (1f + maxChangePct)
        return raw.coerceIn(minAllowed, maxAllowed)
    }

    // -------------------------- Trend metrics --------------------------

    /** Compute trend metrics (slope & drop%) for a list of DSAreaPoints. */
    private inline fun computeTrend(
        points: List<DSAreaPoint>,
        crossinline yOf: (DSAreaPoint) -> Float
    ): IntraTrendMetrics {
        if (points.isEmpty()) return IntraTrendMetrics(0f, 0f)
        val xs = points.indices.map { it.toFloat() }
        val ys = points.map { yOf(it) }

        val slope = linearSlope(xs, ys)
        val drop = dropPercent(ys)
        return IntraTrendMetrics(slopePerStep = slope, dropPercent = drop)
    }

    /** Ordinary least squares slope for y ~ x. */
    private fun linearSlope(xs: List<Float>, ys: List<Float>): Float {
        val n = xs.size
        if (n < 2) return 0f
        val xMean = xs.average().toFloat()
        val yMean = ys.average().toFloat()
        var num = 0f
        var den = 0f
        for (i in 0 until n) {
            val dx = xs[i] - xMean
            num += dx * (ys[i] - yMean)
            den += dx * dx
        }
        return if (den == 0f) 0f else num / den
    }

    /** Drop% between first-two and last-two points. */
    private fun dropPercent(vals: List<Float>): Float {
        if (vals.isEmpty()) return 0f
        val head = if (vals.size >= 2) (vals[0] + vals[1]) / 2f else vals[0]
        val tail =
            if (vals.size >= 2) (vals[vals.size - 1] + vals[vals.size - 2]) / 2f else vals.last()
        if (head <= 0f) return 0f
        return ((tail / head) - 1f) * 100f
    }

    // -------------------------- Period → Buckets logic --------------------------

    private enum class BucketScale { EXERCISE, DAY, WEEK, MONTH }

    /** Inclusive date span bucket (closed range [start .. end]). */
    private data class Bucket(val start: LocalDateTime, val end: LocalDateTime)

    private fun deriveScale(period: PeriodState): BucketScale = when (period) {
        is PeriodState.ThisDay -> BucketScale.EXERCISE
        is PeriodState.ThisWeek -> BucketScale.DAY
        is PeriodState.ThisMonth -> BucketScale.WEEK
        is PeriodState.CUSTOM -> deriveCustomScale(period.range)
    }

    /**
     * CUSTOM rules (exactly as requested):
     * 1) If it's a day → EXERCISE
     * 2) If < 1 month AND days not divisible by 7 → DAY
     * 3) If up to a year but > month → if fully month-aligned → MONTH, else → WEEK
     * 4) Otherwise → MONTH
     */
    private fun deriveCustomScale(range: DateRange): BucketScale {
        val from = range.from.date
        val to = range.to.date
        if (from == to) return BucketScale.EXERCISE

        val days = daysInclusive(from, to)
        val fullMonths = isWholeMonths(range)

        return when {
            days < 30 && (days % 7 != 0) -> BucketScale.DAY
            days in 30..366 -> if (fullMonths) BucketScale.MONTH else BucketScale.WEEK
            else -> BucketScale.MONTH
        }
    }

    private fun buildBuckets(range: DateRange, scale: BucketScale): List<Bucket> = when (scale) {
        BucketScale.EXERCISE -> emptyList() // handled by per-exercise path
        BucketScale.DAY -> days(range.from, range.to)
        BucketScale.WEEK -> weeks(range.from, range.to)
        BucketScale.MONTH -> months(range.from, range.to)
    }

    // ---- Bucket builders (LocalDateTime-friendly via LocalDate iteration) ----

    private fun days(from: LocalDateTime, to: LocalDateTime): List<Bucket> {
        val out = mutableListOf<Bucket>()
        var d = from.date
        val endDate = to.date
        while (d <= endDate) {
            val start = maxDT(d.atStartOfDay(), from)
            val end = minDT(d.atEndOfDay(), to)
            out += Bucket(start, end)
            d = d.plus(DatePeriod(days = 1))
        }
        return out
    }

    private fun weeks(from: LocalDateTime, to: LocalDateTime): List<Bucket> {
        val out = mutableListOf<Bucket>()
        var weekStart = startOfWeek(from.date) // Monday-based
        val endDate = to.date
        while (weekStart <= endDate) {
            val weekEndDate = minLocalDate(endOfWeek(weekStart), endDate)
            val start = maxDT(weekStart.atStartOfDay(), from)
            val end = minDT(weekEndDate.atEndOfDay(), to)
            out += Bucket(start, end)
            weekStart = weekStart.plus(DatePeriod(days = 7))
        }
        return out
    }

    private fun months(from: LocalDateTime, to: LocalDateTime): List<Bucket> {
        val out = mutableListOf<Bucket>()
        var monthStart = startOfMonth(from.date)
        val endDate = to.date
        while (monthStart <= endDate) {
            val monthEndDate = minLocalDate(endOfMonth(monthStart), endDate)
            val start = maxDT(monthStart.atStartOfDay(), from)
            val end = minDT(monthEndDate.atEndOfDay(), to)
            out += Bucket(start, end)
            monthStart = monthStart.plus(DatePeriod(months = 1))
        }
        return out
    }

    // ---- Grouping and labeling ----

    private fun groupTrainingsByBucket(
        trainings: List<TrainingState>,
        scale: BucketScale
    ): Map<LocalDate, List<TrainingState>> {
        return trainings.groupBy { t ->
            val d = t.createdAt.date
            when (scale) {
                BucketScale.EXERCISE -> d // not used in grouping path
                BucketScale.DAY -> d
                BucketScale.WEEK -> startOfWeek(d)
                BucketScale.MONTH -> startOfMonth(d)
            }
        }
    }

    private fun defaultLabeler(scale: BucketScale): (Bucket) -> String = when (scale) {
        BucketScale.DAY -> { b -> DateTimeUtils.format(b.start, DateFormat.WEEKDAY_SHORT) }
        BucketScale.WEEK -> { b -> "W${isoWeekNumber(b.start)} ${b.start}" }
        BucketScale.MONTH -> { b ->
            "${b.start.year}-${b.start.monthNumber.toString().padStart(2, '0')}"
        }

        BucketScale.EXERCISE -> { _ -> "" }
    }

    private fun LocalDate.label(scale: BucketScale): String = when (scale) {
        BucketScale.DAY -> this.toString()
        BucketScale.WEEK -> "W${isoWeekNumber(this)} $this"
        BucketScale.MONTH -> "${this.year}-${this.monthNumber.toString().padStart(2, '0')}"
        BucketScale.EXERCISE -> this.toString()
    }

    private fun TrainingState.belongsTo(bucket: Bucket): Boolean {
        val ts = createdAt
        return ts >= bucket.start && ts <= bucket.end
    }

    // ---- Date utils (kotlinx.datetime + DateTimeUtils for starts/ends) ----

    private fun startOfWeek(d: LocalDate): LocalDate {
        // Monday-based, consistent with DateTimeUtils.thisWeek()
        val shift = d.dayOfWeek.ordinal // 0..6
        return d.minus(DatePeriod(days = shift))
    }

    private fun endOfWeek(sow: LocalDate): LocalDate = sow.plus(DatePeriod(days = 6))

    private fun startOfMonth(d: LocalDate): LocalDate = LocalDate(d.year, d.monthNumber, 1)

    private fun endOfMonth(som: LocalDate): LocalDate {
        val next = som.plus(DatePeriod(months = 1))
        return next.minus(DatePeriod(days = 1))
    }

    private fun daysInclusive(from: LocalDate, to: LocalDate): Int {
        var cnt = 0
        var cur = from
        while (cur <= to) {
            cnt++
            cur = cur.plus(DatePeriod(days = 1))
        }
        return cnt
    }

    /** Check if the DateRange covers whole months exactly (from 1st day to last day). */
    private fun isWholeMonths(range: DateRange): Boolean {
        val from = range.from.date
        val to = range.to.date
        if (from.dayOfMonth != 1) return false
        val toIsLast = to.plus(DatePeriod(days = 1)).dayOfMonth == 1
        return toIsLast && from <= to
    }

    /** Simple ISO-like week number for labeling (approx.). */
    private fun isoWeekNumber(weekStartMonday: LocalDateTime): Int =
        isoWeekNumber(weekStartMonday.date)

    private fun isoWeekNumber(weekStartMonday: LocalDate): Int {
        val doy = dayOfYear(weekStartMonday)
        return ((doy - 1) / 7) + 1
    }

    private fun dayOfYear(d: LocalDateTime): Int = dayOfYear(d.date)

    private fun dayOfYear(d: LocalDate): Int {
        var count = 0
        var cur = LocalDate(d.year, 1, 1)
        while (cur < d) {
            count++
            cur = cur.plus(DatePeriod(days = 1))
        }
        return count + 1
    }

    // ---- LocalDateTime helpers using DateTimeUtils ----

    private fun LocalDate.atStartOfDay(): LocalDateTime =
        DateTimeUtils.startOfDay(LocalDateTime(this, LocalTime(0, 0)))

    private fun LocalDate.atEndOfDay(): LocalDateTime =
        DateTimeUtils.endOfDay(LocalDateTime(this, LocalTime(0, 0)))

    private fun minDT(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if (a <= b) a else b
    private fun maxDT(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if (a >= b) a else b
    private fun minLocalDate(a: LocalDate, b: LocalDate): LocalDate = if (a <= b) a else b

    // -------------------------- Misc helpers --------------------------

    private operator fun DateRange.contains(ts: LocalDateTime): Boolean {
        // Inclusive range: [from .. to]
        return (ts >= from) && (ts <= to)
    }

    /** Percentile (0..1) over Float list; returns 0f for empty. */
    private fun percentile(values: List<Float>, p: Float): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val clamped = p.coerceIn(0f, 1f)
        val idx = ((sorted.size - 1) * clamped).toDouble()
        val lo = idx.toInt()
        val hi = minOf(lo + 1, sorted.lastIndex)
        val frac = (idx - lo).toFloat()
        return (sorted[lo] * (1 - frac) + sorted[hi] * frac)
    }
}
