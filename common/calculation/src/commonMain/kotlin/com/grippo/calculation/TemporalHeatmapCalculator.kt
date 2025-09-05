package com.grippo.calculation

import com.grippo.calculation.internal.InternalCalculationUtils
import com.grippo.calculation.internal.buildDayBuckets
import com.grippo.calculation.internal.buildMonthBuckets
import com.grippo.calculation.internal.buildWeekBuckets
import com.grippo.calculation.internal.daysInclusive
import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.internal.isoWeekNumber
import com.grippo.calculation.models.Bucket
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
import com.grippo.date.utils.contains
import com.grippo.design.components.chart.DSHeatmapData
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.tooltip_muscle_load_description_day
import com.grippo.design.resources.provider.tooltip_muscle_load_description_month
import com.grippo.design.resources.provider.tooltip_muscle_load_description_training
import com.grippo.design.resources.provider.tooltip_muscle_load_description_year
import com.grippo.design.resources.provider.tooltip_muscle_load_title_day
import com.grippo.design.resources.provider.tooltip_muscle_load_title_month
import com.grippo.design.resources.provider.tooltip_muscle_load_title_training
import com.grippo.design.resources.provider.tooltip_muscle_load_title_year
import com.grippo.design.resources.provider.w
import com.grippo.state.datetime.PeriodState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.formatters.UiText
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

/**
 * 🔥 Temporal Heatmap (Muscle × Time)
 *
 * Builds a muscle-by-time heatmap of workload aggregated over the selected period.
 * - X axis = time buckets (DAY/WEEK/MONTH depending on PeriodState)
 * - Y axis = muscle groups (or individual muscles if no groups are provided)
 * - Cell value = Σ workload for that muscle (Volume or Reps; auto-chosen like in LoadCalculator)
 *
 * Output values are normalized to [0..1] for coloring via your OrangeRed gradient.
 *
 * Inputs:
 * - trainings: List<TrainingState>
 * - period: PeriodState
 * - examples: List<ExerciseExampleState> (for muscle bundles and shares)
 * - groups: List<MuscleGroupState<MuscleRepresentationState.Plain>> (rows; falls back to muscles)
 */
public class TemporalHeatmapCalculator(
    private val stringProvider: StringProvider,
    private val policy: Policy = Policy()
) {

    // -------------------------- Policy (tunable knobs) --------------------------

    /**
     * Normalization policy for the heatmap.
     *
     * ViewMax:   divide by global max over the whole matrix (default).
     * Percentile(p): divide by p-quantile (e.g., 0.95) for robustness to outliers.
     * PerRowMax: each row normalized by its own max (good when groups differ a lot).
     * PerColMax: each column normalized by its own max (good when period spans vary).
     */
    public sealed interface Normalization {
        public data object ViewMax : Normalization
        public data class Percentile(val p: Float = 0.95f) : Normalization
        public data object PerRowMax : Normalization
        public data object PerColMax : Normalization
    }

    /** Public policy that can be supplied by caller. Backwards-compatible defaults. */
    public data class Policy(
        val normalization: Normalization = Normalization.ViewMax
    )

    // -------------------------- Public API --------------------------

    /** 📊 Heatmap: rows = muscle groups (or muscles), cols = time buckets (day/week/month). */
    public suspend fun calculateMuscleGroupHeatmapFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): Pair<DSHeatmapData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        // Build time buckets (X axis) and labels
        val buckets = buildBuckets(period.range, scale).sortedBy { it.start }
        val colLabels = defaultTimeLabels(buckets, scale)
        val cols = buckets.size

        // Row dimension (Y axis): provided groups or individual muscles from examples
        val rowSpec = buildRowSpec(groups, examples)
        val rows = rowSpec.labels.size

        // Early exit (empty grid)
        if (rows == 0 || cols == 0) {
            val tipEmpty = instructionForMuscleHeatmap(period)
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

        // Auto workload choice using all exercises within range (stable across buckets)
        val allExercises = inRange.flatMap { it.exercises }
        val workload = chooseWorkload(allExercises)

        // Pre-index examples by id (consistent access)
        // NOTE: in your codebase ExerciseExampleState likely wraps real value with .value.id.
        // Delegate fields may expose .bundles directly; here we access .bundles directly to match your usage.
        val exampleMap: Map<String, ExerciseExampleState> = examples.associateBy { it.value.id }

        // Pre-group trainings by bucket in a single pass (O(N + B))
        val trainingsPerBucket: Array<MutableList<TrainingState>> = Array(cols) { mutableListOf() }
        if (inRange.isNotEmpty()) {
            val sorted = inRange.sortedBy { it.createdAt }
            var tIdx = 0
            for (bIdx in buckets.indices) {
                val b = buckets[bIdx]
                // advance to first training that may belong to this bucket
                while (tIdx < sorted.size && sorted[tIdx].createdAt < b.start) tIdx++
                var j = tIdx
                while (j < sorted.size && sorted[j].createdAt <= b.end) {
                    trainingsPerBucket[bIdx] += sorted[j]
                    j++
                }
                // keep pointer at j for the next bucket
                tIdx = j
                if (tIdx >= sorted.size) break
            }
        }

        // Accumulate raw matrix [rows × cols] (absolute values)
        val raw = FloatArray(rows * cols) { 0f }
        for (c in 0 until cols) {
            val ts = trainingsPerBucket[c]
            if (ts.isEmpty()) continue

            // Absolute workload per muscle for this bucket
            val perMuscle = computeBucketMuscleWorkload(
                trainings = ts,
                examples = exampleMap,
                workload = workload
            )
            if (perMuscle.isEmpty()) continue

            // Roll-up into row indices (group → sum of its muscles)
            rowSpec.rows.forEachIndexed { r, row ->
                val v = when (row) {
                    is Row.Group -> row.muscles.fold(0f) { acc, m -> acc + (perMuscle[m] ?: 0f) }
                    is Row.Single -> perMuscle[row.muscle] ?: 0f
                }
                raw[r * cols + c] = v
            }
        }

        // Normalize to [0..1]
        val values01 = when (policy.normalization) {
            is Normalization.ViewMax -> normalizeViewMax(raw)
            is Normalization.Percentile -> normalizeByPercentile(raw, policy.normalization.p)
            is Normalization.PerRowMax -> normalizePerRowMax(raw, rows, cols)
            is Normalization.PerColMax -> normalizePerColMax(raw, rows, cols)
        }.toList()

        val data = DSHeatmapData(
            rows = rows,
            cols = cols,
            values01 = values01,
            rowLabels = rowSpec.labels,
            colLabels = colLabels,
            rowDim = if (rowSpec.usesGroups) "MuscleGroup" else "Muscle",
            colDim = when (scale) {
                BucketScale.DAY -> "Day"
                BucketScale.WEEK -> "Week"
                BucketScale.MONTH -> "Month"
                BucketScale.EXERCISE -> "Day"
            },
            valueUnit = when (workload) {
                InternalCalculationUtils.WorkloadStrategy.Volume -> "kg·reps"
                InternalCalculationUtils.WorkloadStrategy.Reps -> "reps"
            }
        )

        val tip =
            instructionForMuscleHeatmap(period) // reuse your muscle-load instructions per scale
        return data to tip
    }

    // -------------------------- Row spec (groups or singles) --------------------------

    private sealed interface Row {
        data class Group(val name: String, val muscles: List<MuscleEnumState>) : Row
        data class Single(val name: String, val muscle: MuscleEnumState) : Row
    }

    private data class RowSpec(
        val rows: List<Row>,
        val labels: List<String>,
        val usesGroups: Boolean
    )

    /** If groups provided → use them (stable order). Else derive singles from examples (stable order). */
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
            // Derive all present muscles; use .value.bundles if needed in your model
            examples.forEach { ex -> ex.bundles.forEach { b -> muscles += b.muscle.type } }
            val rows = muscles
                .toList()
                .sortedBy { it.ordinal } // stable order across runs
                .map { m -> Row.Single(name = m.toString(), muscle = m) }
            RowSpec(rows, rows.map { it.name }, usesGroups = false)
        }
    }

    // -------------------------- Bucket aggregation --------------------------

    /** Compute absolute workload per muscle inside a single bucket. */
    private fun computeBucketMuscleWorkload(
        trainings: List<TrainingState>,
        examples: Map<String, ExerciseExampleState>,
        workload: InternalCalculationUtils.WorkloadStrategy
    ): Map<MuscleEnumState, Float> {
        val perMuscle = mutableMapOf<MuscleEnumState, Float>()

        trainings.forEach { t ->
            t.exercises.forEach { ex ->
                val exampleId = ex.exerciseExample?.id ?: return@forEach
                val example = examples[exampleId] ?: return@forEach

                val exWorkload = InternalCalculationUtils.calculateExerciseWorkload(ex, workload)
                    .coerceAtLeast(0f)
                if (exWorkload <= 0f) return@forEach

                // Distribute by example bundles (% shares)
                val bundles =
                    example.bundles // delegated in your model; if not, use example.value.bundles
                val denom = bundles.fold(0f) { acc, b ->
                    acc + (b.percentage.value ?: 0).coerceAtLeast(0).toFloat()
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

    // -------------------------- Workload choice (delegated to your utils) --------------------------

    private fun chooseWorkload(
        exercises: List<ExerciseState>
    ): InternalCalculationUtils.WorkloadStrategy {
        return InternalCalculationUtils.chooseWorkloadStrategy(exercises)
    }

    // -------------------------- Instructions --------------------------

    /** Reuse your muscle-load copy per scale (titles/descriptions already exist in your strings). */
    private fun instructionForMuscleHeatmap(period: PeriodState): Instruction {
        val scale = deriveScale(period)
        val (titleRes, descRes) = when (scale) {
            BucketScale.EXERCISE ->
                Res.string.tooltip_muscle_load_title_training to
                        Res.string.tooltip_muscle_load_description_training

            BucketScale.DAY ->
                Res.string.tooltip_muscle_load_title_day to
                        Res.string.tooltip_muscle_load_description_day

            BucketScale.WEEK ->
                Res.string.tooltip_muscle_load_title_month to
                        Res.string.tooltip_muscle_load_description_month

            BucketScale.MONTH -> {
                val days = daysInclusive(period.range.from.date, period.range.to.date)
                if (days >= 365)
                    Res.string.tooltip_muscle_load_title_year to
                            Res.string.tooltip_muscle_load_description_year
                else
                    Res.string.tooltip_muscle_load_title_month to
                            Res.string.tooltip_muscle_load_description_month
            }
        }

        return Instruction(title = UiText.Res(titleRes), description = UiText.Res(descRes))
    }

    // -------------------------- Time bucketing --------------------------

    private fun buildBuckets(range: DateRange, scale: BucketScale): List<Bucket> = when (scale) {
        BucketScale.DAY -> buildDayBuckets(range)
        BucketScale.WEEK -> buildWeekBuckets(range)
        BucketScale.MONTH -> buildMonthBuckets(range)
        BucketScale.EXERCISE -> buildDayBuckets(range) // single-day → 1 column
    }

    private suspend fun defaultTimeLabels(buckets: List<Bucket>, scale: BucketScale): List<String> {
        val w = stringProvider.get(Res.string.w)
        return when (scale) {
            BucketScale.DAY -> buckets.map {
                DateTimeUtils.format(it.start, DateFormat.DATE_DD_MMM) // e.g., "02 Sep"
            }

            BucketScale.WEEK -> buckets.map {
                "$w${isoWeekNumber(it.start)}-${
                    DateTimeUtils.format(it.start, DateFormat.MONTH_SHORT)
                }" // e.g., "W36-Sep"
            }

            BucketScale.MONTH -> buckets.map {
                DateTimeUtils.format(it.start, DateFormat.MONTH_SHORT) // e.g., "Sep"
            }

            BucketScale.EXERCISE -> buckets.map {
                DateTimeUtils.format(it.start, DateFormat.DATE_DD_MMM)
            }
        }
    }

    // -------------------------- Normalization helpers --------------------------

    /** Global ViewMax normalization. */
    private fun normalizeViewMax(values: FloatArray): FloatArray {
        val max = values.maxOrNull() ?: 0f
        if (max <= 0f || !max.isFinite()) return values
        for (i in values.indices) values[i] = safe01(values[i] / max)
        return values
    }

    /** Percentile-based normalization for robustness to outliers. */
    private fun normalizeByPercentile(values: FloatArray, p: Float): FloatArray {
        val cap = percentile(values, p).takeIf { it > 0f && it.isFinite() }
            ?: (values.maxOrNull() ?: 0f)
        if (cap <= 0f) return values
        for (i in values.indices) values[i] = safe01(values[i] / cap)
        return values
    }

    /** Normalize each row by its own max. */
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

    /** Normalize each column by its own max. */
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

    /** Percentile (0..1). Simple N log N implementation; sufficient for grid sizes. */
    private fun percentile(values: FloatArray, p: Float): Float {
        val list = values.filter { it.isFinite() }.sorted()
        if (list.isEmpty()) return 0f
        val clamped = p.coerceIn(0f, 1f)
        val idx = (clamped * (list.size - 1)).toInt()
        return list[idx]
    }
}
