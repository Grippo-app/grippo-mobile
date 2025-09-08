package com.grippo.calculation

import com.grippo.calculation.internal.buildBuckets
import com.grippo.calculation.internal.daysInclusive
import com.grippo.calculation.internal.defaultTimeLabels
import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.contains
import com.grippo.design.components.chart.DSHeatmapData
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.tooltip_heatmap_description_day_reps
import com.grippo.design.resources.provider.tooltip_heatmap_description_day_tonnage
import com.grippo.design.resources.provider.tooltip_heatmap_description_month_reps
import com.grippo.design.resources.provider.tooltip_heatmap_description_month_tonnage
import com.grippo.design.resources.provider.tooltip_heatmap_description_training_reps
import com.grippo.design.resources.provider.tooltip_heatmap_description_training_tonnage
import com.grippo.design.resources.provider.tooltip_heatmap_description_week_reps
import com.grippo.design.resources.provider.tooltip_heatmap_description_week_tonnage
import com.grippo.design.resources.provider.tooltip_heatmap_description_year_reps
import com.grippo.design.resources.provider.tooltip_heatmap_description_year_tonnage
import com.grippo.design.resources.provider.tooltip_heatmap_title_day_reps
import com.grippo.design.resources.provider.tooltip_heatmap_title_day_tonnage
import com.grippo.design.resources.provider.tooltip_heatmap_title_month_reps
import com.grippo.design.resources.provider.tooltip_heatmap_title_month_tonnage
import com.grippo.design.resources.provider.tooltip_heatmap_title_training_reps
import com.grippo.design.resources.provider.tooltip_heatmap_title_training_tonnage
import com.grippo.design.resources.provider.tooltip_heatmap_title_week_reps
import com.grippo.design.resources.provider.tooltip_heatmap_title_week_tonnage
import com.grippo.design.resources.provider.tooltip_heatmap_title_year_reps
import com.grippo.design.resources.provider.tooltip_heatmap_title_year_tonnage
import com.grippo.state.datetime.PeriodState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.formatters.UiText
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.TrainingState

/**
 * Temporal Heatmap (Muscle × Time)
 *
 * - X axis = time buckets (DAY/WEEK/MONTH/EXERCISE)
 * - Y axis = muscle groups (or individual muscles)
 * - Cell value = Σ workload for that muscle (metric-configurable: TONNAGE or REPS).
 *
 * Rendering policy:
 * - No synthetic columns: only buckets fully derived from the given period range.
 * - Column labels are produced via the existing helpers for the real buckets only.
 * - Normalization is selected by scale and values are sanitized into [0, 1] for the chart.
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
        val cols = builtBuckets.size
        val colLabels: List<String> = defaultTimeLabels(builtBuckets, scale, stringProvider)

        if (cols == 0) {
            // No real buckets → return empty dataset
            val tipEmpty = instructionForMuscleLoad(period, metric)
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

        // Rows (Y axis)
        val rowSpec = buildRowSpec(groups, examples)
        val rows = rowSpec.labels.size

        // Early exit if no rows or no columns (extremely rare now)
        if (rows == 0 || cols == 0) {
            val tipEmpty = instructionForMuscleLoad(period, metric)
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
        // [start, end) buckets: boundary events go to the next bucket, no double counting
        val trainingsPerBucket: Array<MutableList<TrainingState>> = Array(cols) { mutableListOf() }
        if (inRange.isNotEmpty()) {
            val sorted = inRange.sortedBy { it.createdAt }
            var tIdx = 0
            for (bIdx in builtBuckets.indices) {
                val b = builtBuckets[bIdx]
                while (tIdx < sorted.size && sorted[tIdx].createdAt < b.start) tIdx++
                var j = tIdx
                while (j < sorted.size && sorted[j].createdAt < b.end) { // strict <
                    trainingsPerBucket[bIdx] += sorted[j]
                    j++
                }
                tIdx = j
                if (tIdx >= sorted.size) break
            }
        }

        // Raw matrix [rows × cols] (absolute values)
        val raw = FloatArray(rows * cols)
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

        // Pick normalization by scale and apply
        val normMode = chooseNormalizationFor(scale)
        val normalized = when (normMode) {
            is Normalization.Percentile -> normalizeByPercentile(raw, normMode.p)
            is Normalization.PerColMax -> normalizePerColMax(raw, rows, cols)
        }
        val values01 = sanitize01(normalized).toList()

        val data = DSHeatmapData(
            rows = rows,
            cols = cols,
            values01 = values01,
            rowLabels = rowSpec.labels,
            colLabels = colLabels,
        )

        val tip = instructionForMuscleLoad(period, metric)
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
            RowSpec(rows, rows.map { it.name })
        } else {
            val muscles: LinkedHashSet<MuscleEnumState> = linkedSetOf()
            examples.forEach { ex -> ex.bundles.forEach { b -> muscles += b.muscle.type } }
            val rows = muscles
                .toList()
                .sortedBy { it.ordinal }
                .map { m -> Row.Single(name = m.toString(), muscle = m) }
            RowSpec(rows, rows.map { it.name })
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

                // Use explicit Float math to avoid any implicit integer division
                val denom: Float = bundles.fold(0f) { acc, bundle ->
                    acc + ((bundle.percentage.value ?: 0).coerceAtLeast(0)).toFloat()
                }
                if (denom <= 0f) return@forEach

                bundles.forEach { b ->
                    val p: Float = ((b.percentage.value ?: 0).coerceAtLeast(0)).toFloat()
                    if (p == 0f) return@forEach
                    val share: Float = p / denom
                    val muscle = b.muscle.type
                    perMuscle[muscle] = (perMuscle[muscle] ?: 0f) + exMeasure * share
                }
            }
        }
        return perMuscle
    }

    // -------------------------- Normalization --------------------------

    private fun normalizeByPercentile(values: FloatArray, p: Float): FloatArray {
        val perc = percentile(values, p) // finite-only
        val cap = if (perc > 0f && perc.isFinite()) perc else (values.maxOrNull() ?: 0f)
        if (cap <= 0f || !cap.isFinite()) {
            // nothing to scale → zeros
            for (i in values.indices) values[i] = 0f
            return values
        }
        for (i in values.indices) {
            val v = values[i]
            values[i] = if (v.isFinite()) safe01(v / cap) else 0f
        }
        return values
    }

    private fun normalizePerColMax(values: FloatArray, rows: Int, cols: Int): FloatArray {
        for (c in 0 until cols) {
            var colMax = 0f
            var hasFinite = false
            // find finite max
            for (r in 0 until rows) {
                val v = values[r * cols + c]
                if (v.isFinite()) {
                    hasFinite = true
                    if (v > colMax) colMax = v
                }
            }
            if (!hasFinite || colMax <= 0f || !colMax.isFinite()) {
                // whole column is empty → set to zeros
                for (r in 0 until rows) values[r * cols + c] = 0f
                continue
            }
            for (r in 0 until rows) {
                val idx = r * cols + c
                val v = values[idx]
                values[idx] = if (v.isFinite()) safe01(v / colMax) else 0f
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

    private fun sanitize01(values: FloatArray): FloatArray {
        for (i in values.indices) {
            val v = values[i]
            values[i] = if (v.isFinite()) v.coerceIn(0f, 1f) else 0f
        }
        return values
    }

    // Pick normalization mode by scale.
    private fun chooseNormalizationFor(scale: BucketScale): Normalization =
        when (scale) {
            BucketScale.DAY -> Normalization.PerColMax
            BucketScale.WEEK -> Normalization.PerColMax
            BucketScale.MONTH -> Normalization.Percentile(0.95f)
            BucketScale.EXERCISE -> Normalization.PerColMax
        }

    private fun instructionForMuscleLoad(period: PeriodState, metric: Metric): Instruction {
        val scale = deriveScale(period)
        val isYear = when (scale) {
            BucketScale.MONTH -> {
                val days = daysInclusive(period.range.from.date, period.range.to.date)
                days >= 365
            }

            else -> false
        }

        // Choose resource ids by (scale × metric). Normalization is implied by scale:
        // DAY/WEEK -> Per-column max (colors are relative per column),
        // MONTH/YEAR -> Global cap at P95 (colors comparable across columns),
        // EXERCISE -> Per-column max (per session).
        val (titleRes, descRes) = when (scale) {
            BucketScale.EXERCISE -> when (metric) {
                Metric.TONNAGE -> Res.string.tooltip_heatmap_title_training_tonnage to
                        Res.string.tooltip_heatmap_description_training_tonnage

                Metric.REPS -> Res.string.tooltip_heatmap_title_training_reps to
                        Res.string.tooltip_heatmap_description_training_reps
            }

            BucketScale.DAY -> when (metric) {
                Metric.TONNAGE -> Res.string.tooltip_heatmap_title_day_tonnage to
                        Res.string.tooltip_heatmap_description_day_tonnage

                Metric.REPS -> Res.string.tooltip_heatmap_title_day_reps to
                        Res.string.tooltip_heatmap_description_day_reps
            }

            BucketScale.WEEK -> when (metric) {
                Metric.TONNAGE -> Res.string.tooltip_heatmap_title_week_tonnage to
                        Res.string.tooltip_heatmap_description_week_tonnage

                Metric.REPS -> Res.string.tooltip_heatmap_title_week_reps to
                        Res.string.tooltip_heatmap_description_week_reps
            }

            BucketScale.MONTH -> {
                if (isYear) {
                    when (metric) {
                        Metric.TONNAGE -> Res.string.tooltip_heatmap_title_year_tonnage to
                                Res.string.tooltip_heatmap_description_year_tonnage

                        Metric.REPS -> Res.string.tooltip_heatmap_title_year_reps to
                                Res.string.tooltip_heatmap_description_year_reps
                    }
                } else {
                    when (metric) {
                        Metric.TONNAGE -> Res.string.tooltip_heatmap_title_month_tonnage to
                                Res.string.tooltip_heatmap_description_month_tonnage

                        Metric.REPS -> Res.string.tooltip_heatmap_title_month_reps to
                                Res.string.tooltip_heatmap_description_month_reps
                    }
                }
            }
        }

        return Instruction(title = UiText.Res(titleRes), description = UiText.Res(descRes))
    }
}