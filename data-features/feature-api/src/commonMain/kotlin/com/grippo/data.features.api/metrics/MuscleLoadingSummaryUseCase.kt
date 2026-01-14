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
import kotlin.math.pow

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

        private const val SHARE_DAMPING_ALPHA = 1.6f
    }

    public suspend fun fromTrainings(trainings: List<Training>): MuscleLoadSummary {
        val exercises: List<Exercise> = trainings.flatMap { it.exercises }
        return fromExercises(exercises)
    }

    public suspend fun fromTraining(training: Training): MuscleLoadSummary {
        return fromExercises(training.exercises)
    }

    public suspend fun fromExercises(exercises: List<Exercise>): MuscleLoadSummary {
        val groups = loadMuscleGroups()
        if (exercises.isEmpty()) {
            return buildSummary(
                stimulusLoad = emptyMap(),
                volumeLoad = emptyMap(),
                groups = groups,
            )
        }

        val exampleIds = exercises.map { it.exerciseExample.id }.toSet()
        val examples = loadExamples(exampleIds)

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
        if (ids.isEmpty()) return emptyMap()
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
                val factor = sensMultiplier / capacity
                if (factor.isFinite() && factor > 0f) {
                    map[muscle.type] = factor
                }
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
            if (!rawLoad.isFinite() || rawLoad <= EPS) return@forEach

            val exampleId = exercise.exerciseExample.id
            if (exampleId.isBlank()) return@forEach

            val example = exampleMap[exampleId] ?: return@forEach
            val bundles = example.bundles
            if (bundles.isEmpty()) return@forEach

            val totalShare = bundles
                .sumOf { it.percentage.coerceAtLeast(0).toDouble() }
                .toFloat()

            if (!totalShare.isFinite() || totalShare <= EPS) return@forEach

            val complexityFactor = if (applyComplexityFactor) {
                val normalizedPercents = bundles
                    .map { it.percentage.coerceAtLeast(0).toFloat() }
                    .map { share -> (share / totalShare) * 100f }
                    .filter { it.isFinite() && it > EPS }

                val enm = effectiveMuscleCount(
                    percentages = normalizedPercents,
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

            if (!complexityFactor.isFinite() || complexityFactor <= 0f) return@forEach

            val exampleFactor = if (applyExampleFactors) {
                exampleEffortFactor(example)
            } else {
                1f
            }

            if (!exampleFactor.isFinite() || exampleFactor <= 0f) return@forEach

            val load = rawLoad * complexityFactor * exampleFactor
            if (!load.isFinite() || load <= EPS) return@forEach

            val shares = bundles.map { it.percentage.coerceAtLeast(0).toFloat() }
            val ratios = shares.map { share ->
                val r = share / totalShare
                if (r.isFinite() && r >= 0f) r else 0f
            }

            val weights = ratios.map { ratio ->
                ratio.toDouble().pow(SHARE_DAMPING_ALPHA.toDouble()).toFloat()
            }

            val weightsSum = weights.sum()
            if (!weightsSum.isFinite() || weightsSum <= EPS) return@forEach

            bundles.forEachIndexed { index, bundle ->
                val ratio = weights[index] / weightsSum
                if (!ratio.isFinite() || ratio <= EPS) return@forEachIndexed

                val muscle = bundle.muscle.type
                val baseValueToAdd = load * ratio
                if (!baseValueToAdd.isFinite() || baseValueToAdd <= 0f) return@forEachIndexed

                val valueToAdd = if (applyMuscleFactors) {
                    val muscleFactor = fatigueFactorByMuscle[muscle] ?: return@forEachIndexed
                    baseValueToAdd * muscleFactor
                } else {
                    baseValueToAdd
                }

                if (!valueToAdd.isFinite() || valueToAdd <= 0f) return@forEachIndexed

                val current = contributions[muscle] ?: 0f
                val updated = current + valueToAdd
                if (!updated.isFinite() || updated <= 0f) return@forEachIndexed

                contributions[muscle] = updated
            }
        }

        return contributions
            .filterValues { it.isFinite() && it > EPS }
    }

    private fun computeExampleLoad(
        example: ExerciseExample?,
        applyMuscleFactors: Boolean,
        fatigueFactorByMuscle: Map<MuscleEnum, Float>,
    ): Map<MuscleEnum, Float> {
        if (example == null) return emptyMap()
        if (example.bundles.isEmpty()) return emptyMap()

        val bundles = example.bundles
        val totalShare = bundles
            .sumOf { it.percentage.coerceAtLeast(0).toDouble() }
            .toFloat()

        if (!totalShare.isFinite() || totalShare <= EPS) return emptyMap()

        val totals = mutableMapOf<MuscleEnum, Float>()

        val shares = bundles.map { it.percentage.coerceAtLeast(0).toFloat() }
        val ratios = shares.map { share ->
            val r = share / totalShare
            if (r.isFinite() && r >= 0f) r else 0f
        }

        val weights = ratios.map { ratio ->
            ratio.toDouble().pow(SHARE_DAMPING_ALPHA.toDouble()).toFloat()
        }

        val weightsSum = weights.sum()
        if (!weightsSum.isFinite() || weightsSum <= EPS) return emptyMap()

        bundles.forEachIndexed { index, bundle ->
            val ratio = weights[index] / weightsSum
            if (!ratio.isFinite() || ratio <= EPS) return@forEachIndexed

            val muscle = bundle.muscle.type
            val baseValueToAdd = ratio * 100f
            if (!baseValueToAdd.isFinite() || baseValueToAdd <= 0f) return@forEachIndexed

            val valueToAdd = if (applyMuscleFactors) {
                val muscleFactor = fatigueFactorByMuscle[muscle] ?: return@forEachIndexed
                baseValueToAdd * muscleFactor
            } else {
                baseValueToAdd
            }

            if (!valueToAdd.isFinite() || valueToAdd <= 0f) return@forEachIndexed

            val current = totals[muscle] ?: 0f
            val updated = current + valueToAdd
            if (!updated.isFinite() || updated <= 0f) return@forEachIndexed

            totals[muscle] = updated
        }

        return totals
            .filterValues { it.isFinite() && it > EPS }
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

        val factor = categoryFactor * weightTypeFactor
        return if (factor.isFinite() && factor > 0f) factor else 1f
    }

    private fun exerciseStimulus(iterations: List<Iteration>): Float {
        if (iterations.isEmpty()) return 0f

        val total = iterations.sumOf { iteration ->
            val reps = iteration.repetitions.coerceAtLeast(0)
            if (reps == 0) 0.0
            else {
                val weight = iteration.volume.coerceAtLeast(0f)
                val intensity = intensityFactorFromWeight(weight)
                if (!intensity.isFinite() || intensity <= 0f) 0.0
                else reps.toDouble() * intensity.toDouble()
            }
        }.toFloat()

        return if (total.isFinite() && total > 0f) total else 0f
    }

    private fun intensityFactorFromWeight(weight: Float): Float {
        val w = if (weight.isFinite() && weight >= 0f) weight else 0f
        val scaled = ln(1.0 + w.toDouble()).toFloat()
        val intensity = 1f + (scaled / STIMULUS_LN_WEIGHT_SCALE)
        return if (intensity.isFinite() && intensity > 0f) intensity else 1f
    }

    private fun exerciseVolume(iterations: List<Iteration>): Float {
        if (iterations.isEmpty()) return 0f

        val total = iterations.sumOf { iteration ->
            val reps = iteration.repetitions.coerceAtLeast(0)
            if (reps == 0) 0.0
            else {
                val w = iteration.volume
                if (!w.isFinite() || w <= EPS) 0.0 else (w.toDouble() * reps.toDouble())
            }
        }.toFloat()

        return if (total.isFinite() && total > 0f) total else 0f
    }

    private fun effectiveMuscleCount(
        percentages: List<Float>,
        shareThresholdPercent: Float,
    ): Float {
        val clean = percentages.filter { it.isFinite() && it > EPS }
        if (clean.isEmpty()) return 1f

        val filtered = clean.filter { it >= shareThresholdPercent }
        val chosen = if (filtered.size >= 2) filtered else clean

        val total = chosen.sum()
        if (!total.isFinite() || total <= EPS) return 1f

        var sumSq = 0f
        chosen.forEach { s ->
            val p = s / total
            if (p.isFinite() && p > 0f) {
                sumSq += p * p
            }
        }

        if (!sumSq.isFinite() || sumSq <= EPS) return 1f

        val enm = 1f / sumSq
        return if (enm.isFinite() && enm >= 1f) enm else 1f
    }

    private fun complexityFactorFromEnm(
        enm: Float,
        k: Float,
        maxFactor: Float,
    ): Float {
        val e = if (enm.isFinite() && enm >= 1f) enm else 1f
        val kk = if (k.isFinite() && k >= 0f) k else 0f
        val mf = if (maxFactor.isFinite() && maxFactor >= 1f) maxFactor else 1f

        val raw = 1f + kk * ln(e.toDouble()).toFloat()
        if (!raw.isFinite() || raw <= 0f) return 1f

        val clamped = raw.coerceIn(1f, mf)
        return if (clamped.isFinite() && clamped > 0f) clamped else 1f
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
        if (!total.isFinite() || total <= EPS) {
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
        val filtered = muscleLoad
            .filterValues { it.isFinite() && it > EPS }

        if (filtered.isEmpty()) return emptyList()

        val maxValue = filtered.values.maxOrNull()
        if (maxValue == null || !maxValue.isFinite() || maxValue <= EPS) return emptyList()

        return filtered.entries
            .sortedByDescending { it.value }
            .mapNotNull { (muscle, value) ->
                val group = groupByMuscle[muscle] ?: return@mapNotNull null
                val normalized = (value / maxValue) * 100f
                if (!normalized.isFinite()) return@mapNotNull null

                MuscleLoadEntry(
                    group = group.type,
                    percentage = normalized.coerceIn(0f, 100f),
                    muscles = listOf(muscle),
                )
            }
    }

    private fun buildPerGroupBreakdown(
        muscleLoad: Map<MuscleEnum, Float>,
        groups: List<MuscleGroup>,
        groupByMuscle: Map<MuscleEnum, MuscleGroup>,
    ): List<MuscleLoadEntry> {
        val filtered = muscleLoad
            .filterValues { it.isFinite() && it > EPS }

        if (filtered.isEmpty()) return emptyList()

        val totalsByGroupId = mutableMapOf<String, Float>()
        val contributedMusclesByGroupId = mutableMapOf<String, MutableSet<MuscleEnum>>()

        filtered.forEach { (muscle, value) ->
            val group = groupByMuscle[muscle] ?: return@forEach
            val updated = (totalsByGroupId[group.id] ?: 0f) + value
            if (!updated.isFinite() || updated <= 0f) return@forEach

            totalsByGroupId[group.id] = updated
            contributedMusclesByGroupId.getOrPut(group.id) { mutableSetOf() }.add(muscle)
        }

        val sum = totalsByGroupId.values.sum()
        if (!sum.isFinite() || sum <= EPS) return emptyList()

        val groupById = groups.associateBy { it.id }

        return totalsByGroupId.entries
            .sortedByDescending { it.value }
            .mapNotNull { (groupId, totalValue) ->
                val group = groupById[groupId] ?: return@mapNotNull null
                val percent = (totalValue / sum) * 100f
                if (!percent.isFinite()) return@mapNotNull null

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
