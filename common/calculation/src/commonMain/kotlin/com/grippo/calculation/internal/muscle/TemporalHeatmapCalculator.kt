package com.grippo.calculation.internal.muscle

import com.grippo.calculation.internal.buildBuckets
import com.grippo.calculation.internal.defaultTimeLabels
import com.grippo.calculation.internal.deriveScale
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Metric
import com.grippo.calculation.models.MuscleLoadMatrix
import com.grippo.core.state.datetime.PeriodState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.date.utils.contains
import com.grippo.design.resources.provider.providers.StringProvider

internal class TemporalHeatmapCalculator(
    private val stringProvider: StringProvider,
) {

    private sealed interface Normalization {
        data class Percentile(val p: Float = 0.95f) : Normalization
        data object PerColMax : Normalization
    }

    suspend fun computeMuscleGroupHeatmap(
        trainings: List<TrainingState>,
        period: PeriodState,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        metric: Metric,
    ): MuscleLoadMatrix {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        val builtBuckets = buildBuckets(period.range, scale).sortedBy { it.start }
        val cols = builtBuckets.size
        val colLabels: List<String> = defaultTimeLabels(builtBuckets, scale, stringProvider)

        if (cols == 0) {
            val empty = MuscleLoadMatrix(
                rows = 0,
                cols = 0,
                values01 = emptyList(),
                rowLabels = emptyList(),
                colLabels = emptyList(),
            )
            return empty
        }

        val rowSpec = buildRowSpec(groups, examples)
        val rows = rowSpec.labels.size

        if (rows == 0 || cols == 0) {
            val empty = MuscleLoadMatrix(
                rows = 0,
                cols = 0,
                values01 = emptyList(),
                rowLabels = emptyList(),
                colLabels = emptyList(),
            )
            return empty
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
                    is Row.Group -> row.muscles.fold(0f) { acc, muscle ->
                        acc + (perMuscle[muscle] ?: 0f)
                    }

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

        return data
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
                rows = muscles.map {
                    Row.Single(
                        name = it.title().text(stringProvider),
                        muscle = it
                    )
                },
                labels = muscles.map { it.title().text(stringProvider) },
            )
        }
    }

    private fun buildMuscleList(examples: List<ExerciseExampleState>): List<MuscleEnumState> {
        val ordered = LinkedHashSet<MuscleEnumState>()
        examples.forEach { example -> example.bundles.forEach { ordered += it.muscle.type } }
        return ordered.toList()
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
