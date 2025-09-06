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
 * - Cell value = Σ workload for that muscle.
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
    // -------------------------- Policy --------------------------

    private sealed interface Normalization {
        data object ViewMax : Normalization
        data class Percentile(val p: Float = 0.95f) : Normalization
        data object PerRowMax : Normalization
        data object PerColMax : Normalization
    }

    private data class Policy(
        val normalization: Normalization = Normalization.ViewMax
    )

    // -------------------------- Public API --------------------------

    public suspend fun calculateMuscleGroupHeatmapFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): Pair<DSHeatmapData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        // Base buckets and labels (X axis)
        val buckets = buildBuckets(period.range, scale).sortedBy { it.start }
        var colLabels = defaultTimeLabels(buckets, scale, stringProvider)
        var cols = buckets.size

        // Rows (Y axis)
        val rowSpec = buildRowSpec(groups, examples)
        val rows = rowSpec.labels.size

        // Preserve original short-circuit
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
                valueUnit = null
            )
            return empty to tipEmpty
        }

        // Index examples by id
        val exampleMap: Map<String, ExerciseExampleState> = examples.associateBy { it.value.id }

        // Trainings per bucket
        val trainingsPerBucket: Array<MutableList<TrainingState>> = Array(cols) { mutableListOf() }
        if (inRange.isNotEmpty()) {
            val sorted = inRange.sortedBy { it.createdAt }
            var tIdx = 0
            for (bIdx in buckets.indices) {
                val b = buckets[bIdx]
                while (tIdx < sorted.size && sorted[tIdx].createdAt < b.start) tIdx++
                var j = tIdx
                while (j < sorted.size && sorted[j].createdAt <= b.end) {
                    trainingsPerBucket[bIdx] += sorted[j]
                    j++
                }
                tIdx = j
                if (tIdx >= sorted.size) break
            }
        }

        // Raw matrix [rows × cols] (absolute)
        var raw = FloatArray(rows * cols)
        for (c in 0 until cols) {
            val ts = trainingsPerBucket[c]
            if (ts.isEmpty()) continue

            val perMuscle = computeBucketMuscleWorkload(ts, exampleMap)
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
            val extraLabels: List<String> = when (scale) {
                // For calendar-based scales, extend by calendar stride and rebuild buckets.
                BucketScale.DAY, BucketScale.WEEK, BucketScale.MONTH -> {
                    val extendedTo: LocalDateTime =
                        extendToByScale(period.range.to, scale, padNeeded)
                    val extendedRange = DateRange(from = period.range.from, to = extendedTo)
                    val allBuckets = buildBuckets(extendedRange, scale).sortedBy { it.start }
                    val tail = allBuckets.drop(cols).take(padNeeded)
                    if (tail.isNotEmpty()) defaultTimeLabels(
                        tail,
                        scale,
                        stringProvider
                    ) else emptyList()
                }
                // For EXERCISE, pad by count-of-trainings: synthesize labels from the last bucket start.
                BucketScale.EXERCISE -> {
                    val base: LocalDateTime = buckets.last().start
                    // Build next dates purely for labeling, keeping the same time-of-day as 'base'.
                    val list = ArrayList<String>(padNeeded)
                    var curDate = base.date
                    repeat(padNeeded) {
                        curDate = curDate.plus(DatePeriod(days = 1))
                        val dt = LocalDateTime(curDate, base.time)
                        // use your suspend label helper
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

        val normMode = chooseNormalizationFor(scale)

        // Normalize to [0..1] using mode chosen by BucketScale
        val values01 = when (normMode) {
            is Normalization.ViewMax    -> normalizeViewMax(raw)
            is Normalization.Percentile -> normalizeByPercentile(raw, normMode.p)
            is Normalization.PerRowMax  -> normalizePerRowMax(raw, rows, cols)
            is Normalization.PerColMax  -> normalizePerColMax(raw, rows, cols)
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

    // -------------------------- Aggregation --------------------------

    private fun computeBucketMuscleWorkload(
        trainings: List<TrainingState>,
        examples: Map<String, ExerciseExampleState>,
    ): Map<MuscleEnumState, Float> {
        val perMuscle = mutableMapOf<MuscleEnumState, Float>()
        trainings.forEach { t ->
            t.exercises.forEach { ex ->
                val exampleId = ex.exerciseExample?.id ?: return@forEach
                val example = examples[exampleId] ?: return@forEach

                val exWorkload = ex.iterations.fold(0f) { acc, it ->
                    val weight = it.volume.value ?: 0f
                    val reps = it.repetitions.value ?: 0
                    val load = weight
                    if (reps == 0 || load <= 0f) acc else acc + load * reps
                }.coerceAtLeast(0f)
                if (exWorkload <= 0f) return@forEach

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
                    perMuscle[muscle] = (perMuscle[muscle] ?: 0f) + exWorkload * share
                }
            }
        }
        return perMuscle
    }

    // -------------------------- Normalization --------------------------

    private fun normalizeViewMax(values: FloatArray): FloatArray {
        val mx = values.maxOrNull() ?: 0f
        if (mx <= 0f || !mx.isFinite()) return values
        for (i in values.indices) values[i] = safe01(values[i] / mx)
        return values
    }

    private fun normalizeByPercentile(values: FloatArray, p: Float): FloatArray {
        val cap = percentile(values, p).takeIf { it > 0f && it.isFinite() }
            ?: (values.maxOrNull() ?: 0f)
        if (cap <= 0f) return values
        for (i in values.indices) values[i] = safe01(values[i] / cap)
        return values
    }

    private fun normalizePerRowMax(values: FloatArray, rows: Int, cols: Int): FloatArray {
        for (r in 0 until rows) {
            var rowMax = 0f
            val base = r * cols
            for (c in 0 until cols) rowMax = maxOf(rowMax, values[base + c])
            if (rowMax > 0f && rowMax.isFinite()) {
                for (c in 0 until cols) values[base + c] = safe01(values[base + c] / rowMax)
            }
        }
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
            BucketScale.DAY      -> Normalization.PerColMax
            BucketScale.WEEK     -> Normalization.PerRowMax
            BucketScale.MONTH    -> Normalization.Percentile(0.95f)
            BucketScale.EXERCISE -> Normalization.PerRowMax
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