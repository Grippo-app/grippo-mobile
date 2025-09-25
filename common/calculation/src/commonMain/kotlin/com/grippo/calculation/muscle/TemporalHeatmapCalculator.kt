package com.grippo.calculation.muscle

import com.grippo.calculation.internal.buildBuckets
import com.grippo.calculation.internal.daysInclusive
import com.grippo.calculation.internal.defaultTimeLabels
import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.calculation.models.MuscleLoadMatrix
import com.grippo.date.utils.contains
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

public class TemporalHeatmapCalculator(
    private val stringProvider: StringProvider,
) {
    public sealed interface Metric {
        public data object TONNAGE : Metric
        public data object REPS : Metric
    }

    private sealed interface Normalization {
        data class Percentile(val p: Float = 0.95f) : Normalization
        data object PerColMax : Normalization
    }

    public suspend fun calculateMuscleGroupHeatmapFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        metric: Metric,
    ): Pair<MuscleLoadMatrix, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        val builtBuckets = buildBuckets(period.range, scale).sortedBy { it.start }
        val cols = builtBuckets.size
        val colLabels: List<String> = defaultTimeLabels(builtBuckets, scale, stringProvider)

        if (cols == 0) {
            val tipEmpty = instructionForMuscleLoad(period, metric)
            val empty = MuscleLoadMatrix(
                rows = 0,
                cols = 0,
                values01 = emptyList(),
                rowLabels = emptyList(),
                colLabels = emptyList(),
            )
            return empty to tipEmpty
        }

        val rowSpec = buildRowSpec(groups, examples)
        val rows = rowSpec.labels.size

        if (rows == 0 || cols == 0) {
            val tipEmpty = instructionForMuscleLoad(period, metric)
            val empty = MuscleLoadMatrix(
                rows = 0,
                cols = 0,
                values01 = emptyList(),
                rowLabels = emptyList(),
                colLabels = emptyList(),
            )
            return empty to tipEmpty
        }

        val exampleMap: Map<String, ExerciseExampleState> = examples.associateBy { it.value.id }

        val trainingsPerBucket: Array<MutableList<TrainingState>> = Array(cols) { mutableListOf() }
        if (inRange.isNotEmpty()) {
            val sorted = inRange.sortedBy { it.createdAt }
            var tIdx = 0
            for (bIdx in builtBuckets.indices) {
                val bucket = builtBuckets[bIdx]
                while (tIdx < sorted.size && sorted[tIdx].createdAt < bucket.start) tIdx++
                var j = tIdx
                while (j < sorted.size && sorted[j].createdAt < bucket.end) {
                    trainingsPerBucket[bIdx] += sorted[j]
                    j++
                }
                tIdx = j
                if (tIdx >= sorted.size) break
            }
        }

        val raw = FloatArray(rows * cols)
        for (c in 0 until cols) {
            val bucketTrainings = trainingsPerBucket[c]
            if (bucketTrainings.isEmpty()) continue

            val perMuscle = computeBucketMuscleMeasure(bucketTrainings, exampleMap, metric)
            if (perMuscle.isEmpty()) continue

            rowSpec.rows.forEachIndexed { r, row ->
                val value = when (row) {
                    is Row.Group -> row.muscles.fold(0f) { acc, muscle -> acc + (perMuscle[muscle] ?: 0f) }
                    is Row.Single -> perMuscle[row.muscle] ?: 0f
                }
                raw[r * cols + c] = value
            }
        }

        val normMode = chooseNormalizationFor(scale)
        val normalized = when (normMode) {
            is Normalization.Percentile -> normalizeByPercentile(raw, normMode.p)
            is Normalization.PerColMax -> normalizePerColMax(raw, rows, cols)
        }
        val values01 = sanitize01(normalized).toList()

        val data = MuscleLoadMatrix(
            rows = rows,
            cols = cols,
            values01 = values01,
            rowLabels = rowSpec.labels,
            colLabels = colLabels,
        )

        val tip = instructionForMuscleLoad(period, metric)
        return data to tip
    }

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
        examples: List<ExerciseExampleState>,
    ): RowSpec {
        return if (groups.isNotEmpty()) {
            val rows = groups.map { group ->
                Row.Group(
                    name = group.type.title().text(stringProvider),
                    muscles = group.muscles.map { it.value.type },
                )
            }
            RowSpec(rows, rows.map { it.name })
        } else {
            val muscles = buildMuscleList(examples)
            RowSpec(
                rows = muscles.map { Row.Single(name = it.title().text(stringProvider), muscle = it) },
                labels = muscles.map { it.title().text(stringProvider) },
            )
        }
    }

    private fun buildMuscleList(examples: List<ExerciseExampleState>): List<MuscleEnumState> {
        val ordered = LinkedHashSet<MuscleEnumState>()
        examples.forEach { example -> example.bundles.forEach { ordered += it.muscle.type } }
        return ordered.toList()
    }

    private fun instructionForMuscleLoad(
        period: PeriodState,
        metric: Metric,
    ): Instruction {
        val scale = deriveScale(period)
        val isYear = daysInclusive(period.range.from.date, period.range.to.date) >= 365

        val ids = when (scale) {
            BucketScale.EXERCISE -> when (metric) {
                Metric.TONNAGE -> Res.string.tooltip_heatmap_title_training_tonnage to Res.string.tooltip_heatmap_description_training_tonnage
                Metric.REPS -> Res.string.tooltip_heatmap_title_training_reps to Res.string.tooltip_heatmap_description_training_reps
            }

            BucketScale.DAY -> when (metric) {
                Metric.TONNAGE -> Res.string.tooltip_heatmap_title_day_tonnage to Res.string.tooltip_heatmap_description_day_tonnage
                Metric.REPS -> Res.string.tooltip_heatmap_title_day_reps to Res.string.tooltip_heatmap_description_day_reps
            }

            BucketScale.WEEK -> when (metric) {
                Metric.TONNAGE -> Res.string.tooltip_heatmap_title_week_tonnage to Res.string.tooltip_heatmap_description_week_tonnage
                Metric.REPS -> Res.string.tooltip_heatmap_title_week_reps to Res.string.tooltip_heatmap_description_week_reps
            }

            BucketScale.MONTH -> if (isYear) {
                when (metric) {
                    Metric.TONNAGE -> Res.string.tooltip_heatmap_title_year_tonnage to Res.string.tooltip_heatmap_description_year_tonnage
                    Metric.REPS -> Res.string.tooltip_heatmap_title_year_reps to Res.string.tooltip_heatmap_description_year_reps
                }
            } else {
                when (metric) {
                    Metric.TONNAGE -> Res.string.tooltip_heatmap_title_month_tonnage to Res.string.tooltip_heatmap_description_month_tonnage
                    Metric.REPS -> Res.string.tooltip_heatmap_title_month_reps to Res.string.tooltip_heatmap_description_month_reps
                }
            }
        }

        return Instruction(
            title = UiText.Res(ids.first),
            description = UiText.Res(ids.second),
        )
    }

    private fun chooseNormalizationFor(scale: BucketScale): Normalization = when (scale) {
        BucketScale.EXERCISE, BucketScale.DAY -> Normalization.Percentile()
        BucketScale.WEEK, BucketScale.MONTH -> Normalization.PerColMax
    }

    private fun computeBucketMuscleMeasure(
        trainings: List<TrainingState>,
        exampleMap: Map<String, ExerciseExampleState>,
        metric: Metric,
    ): Map<MuscleEnumState, Float> {
        val result = HashMap<MuscleEnumState, Float>()
        trainings.forEach { training ->
            training.exercises.forEach { exercise ->
                val exampleId = exercise.exerciseExample.id
                val example = exampleMap[exampleId] ?: return@forEach

                val base = when (metric) {
                    Metric.TONNAGE -> exercise.iterations.fold(0f) { acc, iteration ->
                        val w = iteration.volume.value ?: 0f
                        val r = (iteration.repetitions.value ?: 0).coerceAtLeast(0)
                        if (w <= 0f || r <= 0) acc else acc + w * r
                    }

                    Metric.REPS -> exercise.iterations.fold(0) { acc, iteration ->
                        acc + (iteration.repetitions.value ?: 0).coerceAtLeast(0)
                    }.toFloat()
                }

                if (base <= 0f) return@forEach

                val sum = example.bundles.fold(0f) { acc, bundle ->
                    acc + (bundle.percentage.value ?: 0).coerceAtLeast(0).toFloat()
                }
                if (sum <= 0f) return@forEach

                example.bundles.forEach { bundle ->
                    val sharePercent = (bundle.percentage.value ?: 0).coerceAtLeast(0)
                    if (sharePercent == 0) return@forEach
                    val share = sharePercent / sum
                    val loadShare = base * share
                    val muscle = bundle.muscle.type
                    result[muscle] = (result[muscle] ?: 0f) + loadShare
                }
            }
        }
        return result
    }

    private fun normalizeByPercentile(values: FloatArray, percentile: Float): FloatArray {
        if (values.isEmpty()) return values
        val sorted = values.filter { it.isFinite() && it > 0f }.sorted()
        if (sorted.isEmpty()) return FloatArray(values.size)
        val index = ((sorted.size - 1) * percentile).toInt().coerceIn(0, sorted.lastIndex)
        val pValue = sorted[index].takeIf { it > 0f } ?: return FloatArray(values.size)
        return FloatArray(values.size) { i -> (values[i] / pValue).coerceIn(0f, 1f) }
    }

    private fun normalizePerColMax(values: FloatArray, rows: Int, cols: Int): FloatArray {
        if (rows == 0 || cols == 0) return values
        val result = FloatArray(values.size)
        for (c in 0 until cols) {
            var max = 0f
            for (r in 0 until rows) {
                val v = values[r * cols + c]
                if (v.isFinite() && v > max) max = v
            }
            for (r in 0 until rows) {
                val v = values[r * cols + c]
                result[r * cols + c] = if (max == 0f) 0f else (v / max).coerceIn(0f, 1f)
            }
        }
        return result
    }

    private fun sanitize01(values: FloatArray): FloatArray = FloatArray(values.size) { idx ->
        val v = values[idx]
        if (!v.isFinite()) 0f else v.coerceIn(0f, 1f)
    }
}
