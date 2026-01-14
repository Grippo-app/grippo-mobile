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
    private companion object {
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

        private const val FATIGUE_IMPACT = 0.35f

        private const val PUSH_TINY_SHARE_MAX_PERCENT = 4f
        private const val PUSH_TINY_SHARE_FACTOR = 0.35f
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
            applyFatigueFactor = true,
            applyForceTypeRules = true,
            fatigueFactorByMuscle = fatigueFactorByMuscle,
        )

        val volumeLoad = computeMuscleLoad(
            exercises = exercises,
            exampleMap = examples,
            baseLoad = ::exerciseVolume,
            applyComplexityFactor = false,
            applyExampleFactors = false,
            applyFatigueFactor = false,
            applyForceTypeRules = true,
            fatigueFactorByMuscle = fatigueFactorByMuscle,
        )

        return buildSummary(stimulusLoad, volumeLoad, groups)
    }

    public suspend fun fromExerciseExample(exampleId: String): MuscleLoadSummary {
        val examples = loadExamples(setOf(exampleId))
        val groups = loadMuscleGroups()

        val stimulusLoad = computeExampleLoad(
            example = examples[exampleId],
            applyForceTypeRules = true,
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
                val sens = FATIGUE_SENS_BASE + FATIGUE_SENS_RANGE * muscle.sensitivity
                val factor = sens / capacity
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
        applyFatigueFactor: Boolean,
        applyForceTypeRules: Boolean,
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

            val originalPercents = bundles
                .map { it.percentage.coerceAtLeast(0).toFloat() }
                .map { share -> (share / totalShare) * 100f }

            val (complexityFactor, enm) = if (applyComplexityFactor) {
                val cleanPercents = originalPercents
                    .filter { it.isFinite() && it > EPS }

                if (cleanPercents.isEmpty()) return@forEach

                val e = effectiveMuscleCount(
                    percentages = cleanPercents,
                    shareThresholdPercent = COMPLEXITY_SHARE_THRESHOLD_PERCENT,
                )

                val cf = complexityFactorFromEnm(
                    enm = e,
                    k = COMPLEXITY_K,
                    maxFactor = COMPLEXITY_MAX_FACTOR,
                ) ?: return@forEach

                cf to e
            } else {
                1f to 1f
            }

            val exampleFactor = if (applyExampleFactors) {
                exampleEffortFactor(
                    example = example,
                    complexityFactor = complexityFactor,
                    enm = enm,
                ) ?: return@forEach
            } else {
                1f
            }

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
                val dampedRatio = weights[index] / weightsSum
                if (!dampedRatio.isFinite() || dampedRatio <= EPS) return@forEachIndexed

                val muscle = bundle.muscle.type
                val baseValue = load * dampedRatio
                if (!baseValue.isFinite() || baseValue <= EPS) return@forEachIndexed

                val forceTypePenalty = if (applyForceTypeRules) {
                    forceTypePenalty(
                        example = example,
                        originalSharePercent = originalPercents.getOrNull(index) ?: 0f,
                    )
                } else {
                    1f
                }

                if (!forceTypePenalty.isFinite() || forceTypePenalty <= 0f) return@forEachIndexed

                val fatiguePenalty = if (applyFatigueFactor) {
                    val raw = fatigueFactorByMuscle[muscle] ?: return@forEachIndexed
                    blendedFatigueMultiplier(raw) ?: return@forEachIndexed
                } else {
                    1f
                }

                val valueToAdd = baseValue * forceTypePenalty * fatiguePenalty
                if (!valueToAdd.isFinite() || valueToAdd <= EPS) return@forEachIndexed

                val updated = (contributions[muscle] ?: 0f) + valueToAdd
                if (!updated.isFinite() || updated <= EPS) return@forEachIndexed

                contributions[muscle] = updated
            }
        }

        return contributions.filterValues { it.isFinite() && it > EPS }
    }

    private fun computeExampleLoad(
        example: ExerciseExample?,
        applyForceTypeRules: Boolean,
    ): Map<MuscleEnum, Float> {
        if (example == null) return emptyMap()
        val bundles = example.bundles
        if (bundles.isEmpty()) return emptyMap()

        val totalShare = bundles
            .sumOf { it.percentage.coerceAtLeast(0).toDouble() }
            .toFloat()

        if (!totalShare.isFinite() || totalShare <= EPS) return emptyMap()

        val originalPercents = bundles
            .map { it.percentage.coerceAtLeast(0).toFloat() }
            .map { share -> (share / totalShare) * 100f }

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

        val totals = mutableMapOf<MuscleEnum, Float>()

        bundles.forEachIndexed { index, bundle ->
            val dampedRatio = weights[index] / weightsSum
            if (!dampedRatio.isFinite() || dampedRatio <= EPS) return@forEachIndexed

            val forceTypePenalty = if (applyForceTypeRules) {
                forceTypePenalty(
                    example = example,
                    originalSharePercent = originalPercents.getOrNull(index) ?: 0f,
                )
            } else {
                1f
            }

            if (!forceTypePenalty.isFinite() || forceTypePenalty <= 0f) return@forEachIndexed

            val muscle = bundle.muscle.type
            val valueToAdd = dampedRatio * 100f * forceTypePenalty
            if (!valueToAdd.isFinite() || valueToAdd <= EPS) return@forEachIndexed

            val updated = (totals[muscle] ?: 0f) + valueToAdd
            if (!updated.isFinite() || updated <= EPS) return@forEachIndexed

            totals[muscle] = updated
        }

        return totals.filterValues { it.isFinite() && it > EPS }
    }

    private fun forceTypePenalty(
        example: ExerciseExample,
        originalSharePercent: Float,
    ): Float {
        if (!originalSharePercent.isFinite() || originalSharePercent <= EPS) return 1f

        val forceType = example.value.forceType.toString()
        val isPush = forceType.equals("push", ignoreCase = true)
        if (!isPush) return 1f

        val isTinyShare = originalSharePercent <= PUSH_TINY_SHARE_MAX_PERCENT
        if (!isTinyShare) return 1f

        return PUSH_TINY_SHARE_FACTOR
    }

    private fun blendedFatigueMultiplier(rawFatigue: Float): Float? {
        if (!rawFatigue.isFinite() || rawFatigue <= 0f) return null

        val m = 1f + (rawFatigue - 1f) * FATIGUE_IMPACT
        if (!m.isFinite() || m <= 0f) return null

        return m
    }

    private fun exampleEffortFactor(
        example: ExerciseExample,
        complexityFactor: Float,
        enm: Float,
    ): Float? {
        if (!complexityFactor.isFinite() || complexityFactor <= 0f) return null
        if (!enm.isFinite() || enm <= 0f) return null

        val weightTypeFactor = when (example.value.weightType) {
            WeightTypeEnum.FREE -> EXAMPLE_FREE_WEIGHT_FACTOR
            WeightTypeEnum.FIXED -> EXAMPLE_FIXED_WEIGHT_FACTOR
            WeightTypeEnum.BODY_WEIGHT -> EXAMPLE_BODY_WEIGHT_FACTOR
        }

        if (!weightTypeFactor.isFinite() || weightTypeFactor <= 0f) return null

        val categoryFactor = when (example.value.category) {
            CategoryEnum.COMPOUND -> {
                val bonus = EXAMPLE_COMPOUND_FACTOR - 1f
                val scaledBonus = bonus * (1f / complexityFactor)
                val f = 1f + scaledBonus
                if (f.isFinite() && f > 0f) f else return null
            }

            CategoryEnum.ISOLATION -> EXAMPLE_ISOLATION_FACTOR
        }

        if (!categoryFactor.isFinite() || categoryFactor <= 0f) return null

        val factor = categoryFactor * weightTypeFactor
        if (!factor.isFinite() || factor <= 0f) return null

        return factor
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
            if (p.isFinite() && p > 0f) sumSq += p * p
        }

        if (!sumSq.isFinite() || sumSq <= EPS) return 1f

        val enm = 1f / sumSq
        return if (enm.isFinite() && enm >= 1f) enm else 1f
    }

    private fun complexityFactorFromEnm(
        enm: Float,
        k: Float,
        maxFactor: Float,
    ): Float? {
        if (!enm.isFinite() || enm < 1f) return null
        if (!k.isFinite() || k < 0f) return null
        if (!maxFactor.isFinite() || maxFactor < 1f) return null

        val raw = 1f + k * ln(enm.toDouble()).toFloat()
        if (!raw.isFinite() || raw <= 0f) return null

        val clamped = raw.coerceIn(1f, maxFactor)
        if (!clamped.isFinite() || clamped <= 0f) return null

        return clamped
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

        val sum = filtered.values.sum()
        if (!sum.isFinite() || sum <= EPS) return emptyList()

        return filtered.entries
            .sortedByDescending { it.value }
            .mapNotNull { (muscle, value) ->
                val group = groupByMuscle[muscle] ?: return@mapNotNull null
                val percent = (value / sum) * 100f
                if (!percent.isFinite() || percent <= EPS) return@mapNotNull null

                MuscleLoadEntry(
                    group = group.type,
                    percentage = percent.coerceIn(0f, 100f),
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
            if (!updated.isFinite() || updated <= EPS) return@forEach

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
                if (!percent.isFinite() || percent <= EPS) return@mapNotNull null

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
