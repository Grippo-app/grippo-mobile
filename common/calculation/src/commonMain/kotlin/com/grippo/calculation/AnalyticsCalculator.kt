package com.grippo.calculation

import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.TrainingState
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
    private val colorProvider: ColorProvider
) {

    // -------------------------- Public result/metrics models --------------------------

    /** Trend KPIs computed from a Y-series over exercise order. */
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

    /** 📦 Exercise Volume Chart — Σ(weight × reps) per exercise. */
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

    /** 📦 Exercise Volume across multiple trainings. */
    public suspend fun calculateExerciseVolumeChartFromTrainings(
        trainings: List<TrainingState>
    ): DSBarData = calculateExerciseVolumeChartFromExercises(trainings.flatMap { it.exercises })

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

    /** 📈 Intra-Workout Absolute across multiple trainings. */
    public fun calculateIntraProgressionAbsoluteFromTrainings(
        trainings: List<TrainingState>
    ): IntraResult =
        calculateIntraProgressionAbsoluteFromExercises(trainings.flatMap { it.exercises })

    // -------------------------- Intra-Workout Progression (%1RM) --------------------------

    /**
     * 📈 Intra-Workout %1RM-normalized trend.
     *
     * Each exercise is mapped to relative intensity:
     * rel = avgWeightPerRep / rolling1RM  (returned as % on Y-axis).
     *
     * @param previous1RM optional map to smooth against (exerciseKey -> previous rolling 1RM)
     * @param alpha EWMA smoothing factor for rolling 1RM (0..1)
     * @param maxChangeClamp maximum one-step change clamp (±%) for rolling 1RM (e.g., 0.05 = ±5%)
     * @param skipBodyweight skip pure bodyweight exercises if no system load is known
     * @param systemLoad optional provider returning "BW + external" for bodyweight exercises
     *                   (if provided, bodyweight moves can be included)
     * @return IntraResult with DSAreaData (Y in %) and trend metrics
     */
    public fun calculateIntraProgressionPercent1RMFromExercises(
        exercises: List<ExerciseState>,
        previous1RM: Map<String, Float>? = null,
        alpha: Float = 0.20f,
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
                    // Recompute sets with system load if provider is available
                    estimateSession1RMWithLoader(ex) { itn ->
                        val r = itn.repetitions.value ?: 0
                        val w = (systemLoad(ex) ?: return@estimateSession1RMWithLoader null)
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
                val smoothed = smoothRolling1RM(current, prev, alpha, maxChangeClamp)
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

    /** 📈 Intra-Workout %1RM across multiple trainings (flattened). */
    public fun calculateIntraProgressionPercent1RMFromTrainings(
        trainings: List<TrainingState>,
        previous1RM: Map<String, Float>? = null,
        alpha: Float = 0.20f,
        maxChangeClamp: Float = 0.05f,
        skipBodyweight: Boolean = true,
        systemLoad: ((ExerciseState) -> Float?)? = null
    ): IntraResult = calculateIntraProgressionPercent1RMFromExercises(
        trainings.flatMap { it.exercises },
        previous1RM,
        alpha,
        maxChangeClamp,
        skipBodyweight,
        systemLoad
    )

    // -------------------------- Intra-Workout Progression (Stimulus) --------------------------

    /**
     * 📈 Stimulus trend by exercise order:
     * stimulus = tonnage × (rel_intensity^alpha),
     * where rel_intensity = avgWeightPerRep / rolling1RM.
     */
    public fun calculateIntraProgressionStimulusFromExercises(
        exercises: List<ExerciseState>,
        previous1RM: Map<String, Float>? = null,
        alphaRelIntensity: Float = 0.75f,
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
            val rel = (avg / r1rm).coerceAtMost(2f).coerceAtLeast(0f)
            val stimulus = sumTonnage * rel.pow(alphaRelIntensity)
            DSAreaPoint(x = index.toFloat(), y = stimulus, xLabel = ex.name)
        }

        val data = DSAreaData(points = points)
        val metrics = computeTrend(points) { it.y }
        return IntraResult(data, metrics)
    }

    // -------------------------- Estimated 1RM (robust) --------------------------

    /**
     * 🏋 Robust per-exercise 1RM estimate (within this session) using:
     * - Brzycki & Epley formulas on sets with 1..12 reps
     * - quality weights favoring lower reps
     * - weighted median across sets (robust to outliers)
     *
     * Returns DSBarData with one item per exercise (skips pure bodyweight by default).
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

    /** 🏋 Robust 1RM across multiple trainings. */
    public suspend fun calculateEstimated1RMFromTrainings(
        trainings: List<TrainingState>,
        skipBodyweight: Boolean = true
    ): DSBarData = calculateEstimated1RMFromExercises(
        trainings.flatMap { it.exercises },
        skipBodyweight
    )

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

    /** Weighted average weight per rep = Σ(w*r)/Σ(r). */
    private fun avgWeightPerRep(ex: ExerciseState): Float {
        val (sumT, sumR) = sums(ex)
        return if (sumR > 0) sumT / sumR else 0f
    }

    /** Key to identify an exercise when storing rolling 1RM. */
    private fun exerciseKey(ex: ExerciseState): String {
        // Prefer stable example id, otherwise fallback to name
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
}
