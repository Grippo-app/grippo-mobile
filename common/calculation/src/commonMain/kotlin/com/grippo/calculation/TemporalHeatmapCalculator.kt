package com.grippo.calculation

import com.grippo.calculation.internal.buildBuckets
import com.grippo.calculation.internal.defaultTimeLabels
import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.internal.instructionForMuscleLoad
import com.grippo.calculation.internal.label
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.contains
import com.grippo.design.components.chart.DSHeatmapData
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.datetime.PeriodState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.TrainingState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.math.max

/**
 * Temporal Heatmap (Muscle × Time)
 *
 * - X axis = time buckets (DAY/WEEK/MONTH/EXERCISE)
 * - Y axis = muscle groups (or individual muscles)
 * - Cell value = Σ workload for that muscle (metric-configurable: TONNAGE or REPS).
 *
 * Ensures there are at least 2× more columns than rows by appending zero-valued
 * columns on the right. Labels are produced via your existing helpers.
 *
 * Special case for EXERCISE:
 * - Padding is based on the number of trainings (index-like), not by extending calendar range.
 * - Extra labels are generated with LocalDateTime.label(scale, stringProvider) starting
 *   from the last real bucket start, stepping by +1 day per synthetic column (purely for labeling).
 */
public class TemporalHeatmapCalculator(
    private val stringProvider: StringProvider,
) {
    // -------------------------- Metric (new) --------------------------

    /** What we aggregate per muscle. */
    public sealed interface Metric {
        /** Sum of (weight * reps). */
        public data object TONNAGE : Metric

        /** Sum of repetitions (ignores weight). */
        public data object REPS : Metric
    }

    // -------------------------- Policy --------------------------

    private sealed interface Normalization {
        data class Percentile(val p: Float = 0.95f) : Normalization
        data object PerColMax : Normalization
    }

    // -------------------------- Public API --------------------------

    public suspend fun calculateMuscleGroupHeatmapFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        metric: Metric,
    ): Pair<DSHeatmapData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        // Base buckets and labels (X axis)
        val builtBuckets = buildBuckets(period.range, scale).sortedBy { it.start }
        val bucketsAreReal = builtBuckets.isNotEmpty()
        var cols = if (bucketsAreReal) builtBuckets.size else 1 // fail-safe: at least one column
        var colLabels: List<String> = if (bucketsAreReal) {
            defaultTimeLabels(builtBuckets, scale, stringProvider)
        } else {
            // Single-day (or ultra-narrow) fail-safe label
            listOf(period.range.from.label(scale, stringProvider))
        }

        // Rows (Y axis)
        val rowSpec = buildRowSpec(groups, examples)
        val rows = rowSpec.labels.size

        // Early exit if no rows or no columns (extremely rare now)
        if (rows == 0 || cols == 0) {
            val tipEmpty = instructionForMuscleLoad(period)
            val empty = DSHeatmapData(
                rows = 0,
                cols = 0,
                values01 = emptyList(),
                rowLabels = emptyList(),
                colLabels = emptyList(),
                rowDim = null,
                colDim = null,
            )
            return empty to tipEmpty
        }

        // Index examples by id
        val exampleMap: Map<String, ExerciseExampleState> = examples.associateBy { it.value.id }

        // Group trainings by bucket
        val trainingsPerBucket: Array<MutableList<TrainingState>> = Array(cols) { mutableListOf() }
        if (inRange.isNotEmpty()) {
            if (bucketsAreReal) {
                val sorted = inRange.sortedBy { it.createdAt }
                var tIdx = 0
                for (bIdx in builtBuckets.indices) {
                    val b = builtBuckets[bIdx]
                    while (tIdx < sorted.size && sorted[tIdx].createdAt < b.start) tIdx++
                    var j = tIdx
                    while (j < sorted.size && sorted[j].createdAt <= b.end) {
                        trainingsPerBucket[bIdx] += sorted[j]
                        j++
                    }
                    tIdx = j
                    if (tIdx >= sorted.size) break
                }
            } else {
                // Fail-safe: one synthetic bucket that just holds all in-range trainings
                trainingsPerBucket[0].addAll(inRange)
            }
        }

        // Raw matrix [rows × cols] (absolute values)
        var raw = FloatArray(rows * cols)
        for (c in 0 until cols) {
            val ts = trainingsPerBucket[c]
            if (ts.isEmpty()) continue

            val perMuscle = computeBucketMuscleMeasure(ts, exampleMap, metric)
            if (perMuscle.isEmpty()) continue

            rowSpec.rows.forEachIndexed { r, row ->
                val v = when (row) {
                    is Row.Group -> row.muscles.fold(0f) { acc, m -> acc + (perMuscle[m] ?: 0f) }
                    is Row.Single -> perMuscle[row.muscle] ?: 0f
                }
                raw[r * cols + c] = v
            }
        }

        // -------- Right-pad columns with zeros until cols ≥ rows * 2 --------
        val targetCols = max(cols, rows * 2)
        val padNeeded = targetCols - cols
        if (padNeeded > 0) {
            // Use last bucket start if exists; otherwise fallback to period.range.from
            val lastBucketStart: LocalDateTime =
                if (bucketsAreReal) builtBuckets.last().start else period.range.from

            val extraLabels: List<String> = when (scale) {
                BucketScale.DAY, BucketScale.WEEK, BucketScale.MONTH -> {
                    val extendedTo = extendToByScale(period.range.to, scale, padNeeded)
                    val extendedRange = DateRange(from = period.range.from, to = extendedTo)
                    val allBuckets = buildBuckets(extendedRange, scale).sortedBy { it.start }
                    val tail = if (bucketsAreReal) allBuckets.drop(cols).take(padNeeded) else {
                        // If original buckets were empty, simply take the first `padNeeded` buckets
                        allBuckets.take(padNeeded)
                    }
                    if (tail.isNotEmpty()) defaultTimeLabels(
                        tail,
                        scale,
                        stringProvider
                    ) else emptyList()
                }

                BucketScale.EXERCISE -> {
                    // Label padding by count (index-like), stepping +1 day from the last "known" start
                    val base = lastBucketStart
                    val list = ArrayList<String>(padNeeded)
                    var curDate = base.date
                    repeat(padNeeded) {
                        curDate = curDate.plus(DatePeriod(days = 1))
                        val dt = LocalDateTime(curDate, base.time)
                        list += dt.label(BucketScale.EXERCISE, stringProvider)
                    }
                    list
                }
            }

            // Pad numeric matrix with zeros on the right
            val padded = FloatArray(rows * targetCols)
            for (r in 0 until rows) {
                val src = r * cols
                val dst = r * targetCols
                raw.copyInto(
                    destination = padded,
                    destinationOffset = dst,
                    startIndex = src,
                    endIndex = src + cols
                )
            }
            raw = padded
            cols = targetCols

            // Glue labels (fill any rare shortfall with "")
            val produced = colLabels.size + extraLabels.size
            colLabels = if (produced < targetCols) {
                colLabels + extraLabels + List(targetCols - produced) { "" }
            } else {
                (colLabels + extraLabels).take(targetCols)
            }
        }

        // Pick normalization by scale and apply
        val normMode = chooseNormalizationFor(scale)
        val values01 = when (normMode) {
            is Normalization.Percentile -> normalizeByPercentile(raw, normMode.p)
            is Normalization.PerColMax -> normalizePerColMax(raw, rows, cols)
        }.toList()

        val data = DSHeatmapData(
            rows = rows,
            cols = cols,
            values01 = values01,
            rowLabels = rowSpec.labels,
            colLabels = colLabels,
        )

        val tip = instructionForMuscleLoad(period)
        return data to tip
    }

    // -------------------------- Rows --------------------------

    private sealed interface Row {
        data class Group(val name: String, val muscles: List<MuscleEnumState>) : Row
        data class Single(val name: String, val muscle: MuscleEnumState) : Row
    }

    private data class RowSpec(
        val rows: List<Row>,
        val labels: List<String>,
        val usesGroups: Boolean
    )

    private suspend fun buildRowSpec(
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        examples: List<ExerciseExampleState>
    ): RowSpec {
        return if (groups.isNotEmpty()) {
            val rows = groups.map { g ->
                Row.Group(
                    name = g.type.title().text(stringProvider),
                    muscles = g.muscles.map { it.value.type }
                )
            }
            RowSpec(rows, rows.map { it.name }, usesGroups = true)
        } else {
            val muscles: LinkedHashSet<MuscleEnumState> = linkedSetOf()
            examples.forEach { ex -> ex.bundles.forEach { b -> muscles += b.muscle.type } }
            val rows = muscles
                .toList()
                .sortedBy { it.ordinal }
                .map { m -> Row.Single(name = m.toString(), muscle = m) }
            RowSpec(rows, rows.map { it.name }, usesGroups = false)
        }
    }

    // -------------------------- Aggregation (metric-aware) --------------------------

    /** Aggregates per-muscle values for a single bucket using the requested metric. */
    private fun computeBucketMuscleMeasure(
        trainings: List<TrainingState>,
        examples: Map<String, ExerciseExampleState>,
        metric: Metric
    ): Map<MuscleEnumState, Float> {
        val perMuscle = mutableMapOf<MuscleEnumState, Float>()

        trainings.forEach { t ->
            t.exercises.forEach { ex ->
                val exampleId = ex.exerciseExample?.id ?: return@forEach
                val example = examples[exampleId] ?: return@forEach

                val exMeasure = ex.iterations.fold(0f) { acc, it ->
                    val reps = (it.repetitions.value ?: 0).coerceAtLeast(0)
                    when (metric) {
                        Metric.TONNAGE -> {
                            val weight = (it.volume.value ?: 0f)
                            val ton = if (reps == 0 || weight <= 0f) 0f else weight * reps
                            acc + ton
                        }

                        Metric.REPS -> {
                            // Count repetitions regardless of weight (good for bodyweight/smaller muscles)
                            if (reps == 0) acc else acc + reps.toFloat()
                        }
                    }
                }.coerceAtLeast(0f)
                if (exMeasure <= 0f) return@forEach

                val bundles = example.bundles
                val denom = bundles.fold(0f) { a, b ->
                    a + (b.percentage.value ?: 0).coerceAtLeast(0).toFloat()
                }
                if (denom <= 0f) return@forEach

                bundles.forEach { b ->
                    val p = (b.percentage.value ?: 0).coerceAtLeast(0)
                    if (p == 0) return@forEach
                    val share = p / denom
                    val muscle = b.muscle.type
                    perMuscle[muscle] = (perMuscle[muscle] ?: 0f) + exMeasure * share
                }
            }
        }
        return perMuscle
    }

    // -------------------------- Normalization --------------------------

    private fun normalizeByPercentile(values: FloatArray, p: Float): FloatArray {
        val cap = percentile(values, p).takeIf { it > 0f && it.isFinite() }
            ?: (values.maxOrNull() ?: 0f)
        if (cap <= 0f) return values
        for (i in values.indices) values[i] = safe01(values[i] / cap)
        return values
    }

    private fun normalizePerColMax(values: FloatArray, rows: Int, cols: Int): FloatArray {
        for (c in 0 until cols) {
            var colMax = 0f
            for (r in 0 until rows) colMax = maxOf(colMax, values[r * cols + c])
            if (colMax > 0f && colMax.isFinite()) {
                for (r in 0 until rows) {
                    val idx = r * cols + c
                    values[idx] = safe01(values[idx] / colMax)
                }
            }
        }
        return values
    }

    private fun safe01(x: Float): Float = if (x.isFinite()) x.coerceIn(0f, 1f) else 0f

    private fun percentile(values: FloatArray, p: Float): Float {
        val list = values.filter { it.isFinite() }.sorted()
        if (list.isEmpty()) return 0f
        val clamped = p.coerceIn(0f, 1f)
        val idx = (clamped * (list.size - 1)).toInt()
        return list[idx]
    }

    // -------------------------- Helpers --------------------------

    // Pick normalization mode by scale.
    private fun chooseNormalizationFor(scale: BucketScale): Normalization =
        when (scale) {
            BucketScale.DAY -> Normalization.PerColMax
            BucketScale.WEEK -> Normalization.PerColMax
            BucketScale.MONTH -> Normalization.Percentile(0.95f)
            BucketScale.EXERCISE -> Normalization.PerColMax
        }

    /** Extends a LocalDateTime by calendar steps according to scale (DAY/WEEK/MONTH), preserving time-of-day. */
    private fun extendToByScale(to: LocalDateTime, scale: BucketScale, steps: Int): LocalDateTime {
        val add: DatePeriod = when (scale) {
            BucketScale.DAY -> DatePeriod(days = steps)
            BucketScale.WEEK -> DatePeriod(days = 7 * steps)
            BucketScale.MONTH -> DatePeriod(months = steps)
            BucketScale.EXERCISE -> DatePeriod(days = 0) // not used here; EXERCISE handled separately
        }
        val newDate: LocalDate = to.date.plus(add)
        return LocalDateTime(newDate, to.time)
    }
}