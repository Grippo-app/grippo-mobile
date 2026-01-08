package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.metrics.models.MuscleLoadBreakdown
import com.grippo.data.features.api.metrics.models.MuscleLoadDominance
import com.grippo.data.features.api.metrics.models.MuscleLoadEntry
import com.grippo.data.features.api.metrics.models.MuscleLoadSummary
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.first
import kotlin.math.ln

public class MuscleLoadingSummaryUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val muscleFeature: MuscleFeature,
) {
    private companion object Companion {
        private const val EPS = 1e-3f

        private const val COMPLEXITY_SHARE_THRESHOLD_PERCENT = 5f
        private const val COMPLEXITY_K = 0.20f
        private const val COMPLEXITY_MAX_FACTOR = 1.30f

        private const val STIMULUS_LN_WEIGHT_SCALE = 8f

        private const val FATIGUE_CAPACITY_BASE = 0.80f
        private const val FATIGUE_CAPACITY_RANGE = 0.40f

        private const val FATIGUE_SENS_BASE = 0.80f
        private const val FATIGUE_SENS_RANGE = 0.40f

        private const val EXAMPLE_COMPOUND_FACTOR = 1.12f
        private const val EXAMPLE_ISOLATION_FACTOR = 1.00f

        private const val EXAMPLE_FREE_WEIGHT_FACTOR = 1.06f
        private const val EXAMPLE_FIXED_WEIGHT_FACTOR = 1.00f
        private const val EXAMPLE_BODY_WEIGHT_FACTOR = 1.03f
    }

    public suspend fun fromTrainings(trainings: List<Training>): MuscleLoadSummary {
        val exercises: List<Exercise> = trainings.flatMap { it.exercises }
        return fromExercises(exercises)
    }

    public suspend fun fromTraining(training: Training): MuscleLoadSummary {
        return fromExercises(training.exercises)
    }

    public suspend fun fromExercises(exercises: List<Exercise>): MuscleLoadSummary {
        val exampleIds = exercises.map { it.exerciseExample.id }.toSet()
        val examples = loadExamples(exampleIds)
        val groups = loadMuscleGroups()

        val fatigueFactorByMuscle = buildFatigueFactorByMuscle(groups)

        val stimulusLoad = computeMuscleLoad(
            exercises = exercises,
            exampleMap = examples,
            baseLoad = ::exerciseStimulus,
            applyComplexityFactor = true,
            applyExampleFactors = true,
            applyMuscleFactors = true,
            fatigueFactorByMuscle = fatigueFactorByMuscle,
        )

        val volumeLoad = computeMuscleLoad(
            exercises = exercises,
            exampleMap = examples,
            baseLoad = ::exerciseVolume,
            applyComplexityFactor = false,
            applyExampleFactors = false,
            applyMuscleFactors = false,
            fatigueFactorByMuscle = fatigueFactorByMuscle,
        )

        return buildSummary(stimulusLoad, volumeLoad, groups)
    }

    public suspend fun fromExerciseExample(exampleId: String): MuscleLoadSummary {
        val examples = loadExamples(setOf(exampleId))
        val groups = loadMuscleGroups()

        val fatigueFactorByMuscle = buildFatigueFactorByMuscle(groups)

        val stimulusLoad = computeExampleLoad(
            example = examples[exampleId],
            applyMuscleFactors = true,
            fatigueFactorByMuscle = fatigueFactorByMuscle,
        )

        val volumeLoad = emptyMap<MuscleEnum, Float>()

        return buildSummary(stimulusLoad, volumeLoad, groups)
    }

    private suspend fun loadExamples(ids: Set<String>): Map<String, ExerciseExample> {
        return exerciseExampleFeature.observeExerciseExamples(ids.toList())
            .first()
            .associateBy { it.value.id }
    }

    private suspend fun loadMuscleGroups(): List<MuscleGroup> {
        return muscleFeature.observeMuscles()
            .first()
    }

    private fun buildFatigueFactorByMuscle(groups: List<MuscleGroup>): Map<MuscleEnum, Float> {
        val map = mutableMapOf<MuscleEnum, Float>()

        groups.forEach { group ->
            group.muscles.forEach { muscle ->
                val capacity = FATIGUE_CAPACITY_BASE + FATIGUE_CAPACITY_RANGE * muscle.size
                val sensMultiplier = FATIGUE_SENS_BASE + FATIGUE_SENS_RANGE * muscle.sensitivity
                map[muscle.type] = sensMultiplier / capacity
            }
        }

        return map
    }

    private fun computeMuscleLoad(
        exercises: List<Exercise>,
        exampleMap: Map<String, ExerciseExample>,
        baseLoad: (List<Iteration>) -> Float,
        applyComplexityFactor: Boolean,
        applyExampleFactors: Boolean,
        applyMuscleFactors: Boolean,
        fatigueFactorByMuscle: Map<MuscleEnum, Float>,
    ): Map<MuscleEnum, Float> {
        if (exercises.isEmpty()) return emptyMap()

        val contributions = mutableMapOf<MuscleEnum, Float>()

        exercises.forEach { exercise ->
            val rawLoad = baseLoad(exercise.iterations)
            if (rawLoad <= EPS) return@forEach

            val exampleId = exercise.exerciseExample.id
            val example = exampleMap[exampleId] ?: return@forEach

            val bundles = example.bundles
            if (bundles.isEmpty()) return@forEach

            val totalShare = bundles.sumOf { it.percentage.coerceAtLeast(0).toDouble() }.toFloat()
            if (totalShare <= EPS) return@forEach

            val complexityFactor = if (applyComplexityFactor) {
                val percentages = bundles
                    .map { it.percentage.coerceAtLeast(0).toFloat() }
                    .filter { it > EPS }

                val enm = effectiveMuscleCount(
                    percentages = percentages,
                    shareThresholdPercent = COMPLEXITY_SHARE_THRESHOLD_PERCENT,
                )

                complexityFactorFromEnm(
                    enm = enm,
                    k = COMPLEXITY_K,
                    maxFactor = COMPLEXITY_MAX_FACTOR,
                )
            } else {
                1f
            }

            val exampleFactor = if (applyExampleFactors) {
                exampleEffortFactor(example)
            } else {
                1f
            }

            val load = rawLoad * complexityFactor * exampleFactor
            if (load <= EPS) return@forEach

            bundles.forEach { bundle ->
                val share = bundle.percentage.coerceAtLeast(0).toFloat()
                if (share <= EPS) return@forEach

                val ratio = share / totalShare
                val muscle = bundle.muscle.type

                val baseValueToAdd = load * ratio

                val valueToAdd = if (applyMuscleFactors) {
                    baseValueToAdd * fatigueFactorByMuscle.getValue(muscle)
                } else {
                    baseValueToAdd
                }

                val current = contributions[muscle] ?: 0f
                contributions[muscle] = current + valueToAdd
            }
        }

        return contributions
    }

    private fun computeExampleLoad(
        example: ExerciseExample?,
        applyMuscleFactors: Boolean,
        fatigueFactorByMuscle: Map<MuscleEnum, Float>,
    ): Map<MuscleEnum, Float> {
        if (example == null || example.bundles.isEmpty()) return emptyMap()

        val totals = mutableMapOf<MuscleEnum, Float>()

        example.bundles.forEach { bundle ->
            val percent = bundle.percentage.coerceAtLeast(0).toFloat()
            if (percent <= EPS) return@forEach

            val muscle = bundle.muscle.type

            val valueToAdd = if (applyMuscleFactors) {
                percent * fatigueFactorByMuscle.getValue(muscle)
            } else {
                percent
            }

            val current = totals[muscle] ?: 0f
            totals[muscle] = current + valueToAdd
        }

        return totals
    }

    private fun exampleEffortFactor(example: ExerciseExample): Float {
        val categoryFactor = when (example.value.category) {
            CategoryEnum.COMPOUND -> EXAMPLE_COMPOUND_FACTOR
            CategoryEnum.ISOLATION -> EXAMPLE_ISOLATION_FACTOR
        }

        val weightTypeFactor = when (example.value.weightType) {
            WeightTypeEnum.FREE -> EXAMPLE_FREE_WEIGHT_FACTOR
            WeightTypeEnum.FIXED -> EXAMPLE_FIXED_WEIGHT_FACTOR
            WeightTypeEnum.BODY_WEIGHT -> EXAMPLE_BODY_WEIGHT_FACTOR
        }

        return categoryFactor * weightTypeFactor
    }

    private fun exerciseStimulus(iterations: List<Iteration>): Float {
        if (iterations.isEmpty()) return 0f

        val total = iterations.sumOf { iteration ->
            val reps = iteration.repetitions.coerceAtLeast(0)
            if (reps == 0) 0.0
            else {
                val weight = iteration.volume.coerceAtLeast(0f)
                val intensity = intensityFactorFromWeight(weight)
                reps.toDouble() * intensity.toDouble()
            }
        }.toFloat()

        return total.coerceAtLeast(0f)
    }

    private fun intensityFactorFromWeight(weight: Float): Float {
        val scaled = ln(1.0 + weight.toDouble()).toFloat()
        return 1f + (scaled / STIMULUS_LN_WEIGHT_SCALE)
    }

    private fun exerciseVolume(iterations: List<Iteration>): Float {
        if (iterations.isEmpty()) return 0f

        val total = iterations.sumOf { iteration ->
            val reps = iteration.repetitions.coerceAtLeast(0)
            if (reps == 0) 0.0
            else {
                val w = iteration.volume
                if (w <= EPS) 0.0 else (w.toDouble() * reps.toDouble())
            }
        }.toFloat()

        return total.coerceAtLeast(0f)
    }

    private fun effectiveMuscleCount(
        percentages: List<Float>,
        shareThresholdPercent: Float,
    ): Float {
        val filtered = percentages.filter { it >= shareThresholdPercent }
        val chosen = if (filtered.size >= 2) filtered else percentages

        val total = chosen.sum()
        if (total <= EPS) return 1f

        var sumSq = 0f
        chosen.forEach { s ->
            val p = s / total
            sumSq += p * p
        }

        return 1f / sumSq
    }

    private fun complexityFactorFromEnm(
        enm: Float,
        k: Float,
        maxFactor: Float,
    ): Float {
        val factor = 1f + k * ln(enm.toDouble()).toFloat()
        return factor.coerceIn(1f, maxFactor)
    }

    private fun buildSummary(
        stimulusLoad: Map<MuscleEnum, Float>,
        volumeLoad: Map<MuscleEnum, Float>,
        groups: List<MuscleGroup>,
    ): MuscleLoadSummary {
        val groupByMuscle = buildGroupByMuscleMap(groups)

        val perMuscleEntries = buildPerMuscleBreakdown(stimulusLoad, groupByMuscle)
        val perGroupEntries = buildPerGroupBreakdown(stimulusLoad, groups, groupByMuscle)

        val volumePerMuscleEntries = buildPerMuscleBreakdown(volumeLoad, groupByMuscle)
        val volumePerGroupEntries = buildPerGroupBreakdown(volumeLoad, groups, groupByMuscle)

        val dominance = buildDominance(stimulusLoad)

        return MuscleLoadSummary(
            perGroup = MuscleLoadBreakdown(perGroupEntries),
            perMuscle = MuscleLoadBreakdown(perMuscleEntries),
            volumePerGroup = MuscleLoadBreakdown(volumePerGroupEntries),
            volumePerMuscle = MuscleLoadBreakdown(volumePerMuscleEntries),
            dominance = dominance,
        )
    }

    private fun buildDominance(stimulusLoad: Map<MuscleEnum, Float>): MuscleLoadDominance {
        val sorted = stimulusLoad.values
            .filter { it.isFinite() && it > EPS }
            .sortedDescending()

        if (sorted.isEmpty()) {
            return MuscleLoadDominance(top1SharePercent = 0f, top2SharePercent = 0f)
        }

        val total = sorted.sum()
        if (total <= EPS) {
            return MuscleLoadDominance(top1SharePercent = 0f, top2SharePercent = 0f)
        }

        val top1 = (sorted.first() / total) * 100f
        val top2 = ((sorted.take(2).sum()) / total) * 100f

        return MuscleLoadDominance(
            top1SharePercent = top1.coerceIn(0f, 100f),
            top2SharePercent = top2.coerceIn(0f, 100f),
        )
    }

    private fun buildPerMuscleBreakdown(
        muscleLoad: Map<MuscleEnum, Float>,
        groupByMuscle: Map<MuscleEnum, MuscleGroup>,
    ): List<MuscleLoadEntry> {
        if (muscleLoad.isEmpty()) return emptyList()

        val maxValue = muscleLoad.values.maxOrNull()
        if (maxValue == null || maxValue <= EPS) return emptyList()

        return muscleLoad.entries
            .sortedByDescending { it.value }
            .mapNotNull { (muscle, value) ->
                groupByMuscle[muscle]?.let { group ->
                    val normalized = (value / maxValue) * 100f
                    MuscleLoadEntry(
                        group = group.type,
                        percentage = normalized.coerceIn(0f, 100f),
                        muscles = listOf(muscle),
                    )
                }
            }
    }

    private fun buildPerGroupBreakdown(
        muscleLoad: Map<MuscleEnum, Float>,
        groups: List<MuscleGroup>,
        groupByMuscle: Map<MuscleEnum, MuscleGroup>,
    ): List<MuscleLoadEntry> {
        if (muscleLoad.isEmpty()) return emptyList()

        val totalsByGroupId = mutableMapOf<String, Float>()
        val contributedMusclesByGroupId = mutableMapOf<String, MutableSet<MuscleEnum>>()

        muscleLoad.forEach { (muscle, value) ->
            groupByMuscle[muscle]?.let { group ->
                totalsByGroupId[group.id] = (totalsByGroupId[group.id] ?: 0f) + value
                contributedMusclesByGroupId.getOrPut(group.id) { mutableSetOf() }.add(muscle)
            }
        }

        val sum = totalsByGroupId.values.sum()
        if (sum <= EPS) return emptyList()

        val groupById = groups.associateBy { it.id }

        return totalsByGroupId.entries
            .sortedByDescending { it.value }
            .mapNotNull { (groupId, totalValue) ->
                groupById[groupId]?.let { group ->
                    val percent = (totalValue / sum) * 100f
                    val muscles = contributedMusclesByGroupId[groupId]
                        ?.toList()
                        ?.sortedBy { it.name }
                        ?: emptyList()

                    MuscleLoadEntry(
                        group = group.type,
                        percentage = percent.coerceIn(0f, 100f),
                        muscles = muscles,
                    )
                }
            }
    }

    private fun buildGroupByMuscleMap(groups: List<MuscleGroup>): Map<MuscleEnum, MuscleGroup> {
        val map = mutableMapOf<MuscleEnum, MuscleGroup>()
        groups.forEach { group ->
            group.muscles.forEach { muscle ->
                map[muscle.type] = group
            }
        }
        return map
    }
}
