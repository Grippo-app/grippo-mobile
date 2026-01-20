package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.metrics.models.MuscleLoadBreakdown
import com.grippo.data.features.api.metrics.models.MuscleLoadDominance
import com.grippo.data.features.api.metrics.models.MuscleLoadEntry
import com.grippo.data.features.api.metrics.models.MuscleLoadMeta
import com.grippo.data.features.api.metrics.models.MuscleLoadSummary
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
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

        private const val HIT_SHARE_THRESHOLD_PERCENT = 10f
        private const val TOP_EXAMPLES_LIMIT = 3
    }

    public suspend fun fromTrainings(trainings: List<Training>): MuscleLoadSummary {
        val sessions = trainings.map { it.exercises }
        return fromSessions(sessions)
    }

    public suspend fun fromTraining(training: Training): MuscleLoadSummary {
        return fromSessions(listOf(training.exercises))
    }

    public suspend fun fromExercises(exercises: List<Exercise>): MuscleLoadSummary {
        return fromSessions(listOf(exercises))
    }

    public suspend fun fromExerciseExample(exampleId: String): MuscleLoadSummary {
        val examples = loadExamples(setOf(exampleId))
        val groups = loadMuscleGroups()

        val stimulusLoad = computeExampleLoad(example = examples[exampleId])
        val volumeLoad = emptyMap<MuscleEnum, Float>()

        val meta = MuscleLoadMeta(
            trainingsCount = 0,
            totalExercises = 0,
            totalSets = 0,
            totalRepetitions = 0,
            totalVolume = 0f,
            dominantGroup = null,
        )

        return buildSummary(
            meta = meta,
            stimulusLoad = stimulusLoad,
            volumeLoad = volumeLoad,
            groups = groups,
            perGroupStats = emptyMap(),
            perMuscleStats = emptyMap(),
            topExamplesByGroupEnum = emptyMap(),
            topExamplesByMuscle = emptyMap(),
        )
    }

    private suspend fun fromSessions(sessions: List<List<Exercise>>): MuscleLoadSummary {
        val groups = loadMuscleGroups()
        val allExercises = sessions.flatten()

        val meta = computeMeta(sessions)

        if (allExercises.isEmpty()) {
            return buildSummary(
                meta = meta,
                stimulusLoad = emptyMap(),
                volumeLoad = emptyMap(),
                groups = groups,
                perGroupStats = emptyMap(),
                perMuscleStats = emptyMap(),
                topExamplesByGroupEnum = emptyMap(),
                topExamplesByMuscle = emptyMap(),
            )
        }

        val exampleIds =
            allExercises.map { it.exerciseExample.id }.filter { it.isNotBlank() }.toSet()
        val examples = loadExamples(exampleIds)

        val fatigueFactorByMuscle = buildFatigueFactorByMuscle(groups)
        val groupByMuscle = buildGroupByMuscleMap(groups)

        val overallStimulus = mutableMapOf<MuscleEnum, Float>()
        val overallVolume = mutableMapOf<MuscleEnum, Float>()

        val perGroupStats = mutableMapOf<MuscleGroupEnum, EntryStats>()
        val perMuscleStats = mutableMapOf<MuscleEnum, EntryStats>()

        val topExampleScoreByGroupEnum = mutableMapOf<MuscleGroupEnum, MutableMap<String, Float>>()
        val topExampleScoreByMuscle = mutableMapOf<MuscleEnum, MutableMap<String, Float>>()

        sessions.forEach { sessionExercises ->
            val sessionStimulusByMuscle = mutableMapOf<MuscleEnum, Float>()
            val sessionVolumeByMuscle = mutableMapOf<MuscleEnum, Float>()

            val sessionExampleScoreByGroupEnum =
                mutableMapOf<MuscleGroupEnum, MutableMap<String, Float>>()
            val sessionExampleScoreByMuscle = mutableMapOf<MuscleEnum, MutableMap<String, Float>>()

            sessionExercises.forEach { exercise ->
                val exampleId = exercise.exerciseExample.id
                if (exampleId.isBlank()) return@forEach

                val example = examples[exampleId] ?: return@forEach
                val bundles = example.bundles
                if (bundles.isEmpty()) return@forEach

                val rawStimulus = exerciseStimulus(exercise.iterations)
                val rawVolume = exerciseVolume(exercise.iterations)

                if (!rawStimulus.isFinite() && !rawVolume.isFinite()) return@forEach
                if (rawStimulus <= EPS && rawVolume <= EPS) return@forEach

                val totalShare =
                    bundles.sumOf { it.percentage.coerceAtLeast(0).toDouble() }.toFloat()
                if (!totalShare.isFinite() || totalShare <= EPS) return@forEach

                val originalPercents = bundles
                    .map { it.percentage.coerceAtLeast(0).toFloat() }
                    .map { share -> (share / totalShare) * 100f }

                val (complexityFactor, enm) = run {
                    val cleanPercents = originalPercents.filter { it.isFinite() && it > EPS }
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
                }

                val exampleFactor = exampleEffortFactor(
                    example = example,
                    complexityFactor = complexityFactor,
                    enm = enm,
                ) ?: return@forEach

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
                    val groupEnum = groupByMuscle[muscle]?.type

                    val forceTypePenalty = forceTypePenalty(
                        example = example,
                        originalSharePercent = originalPercents.getOrNull(index) ?: 0f,
                    )

                    if (!forceTypePenalty.isFinite() || forceTypePenalty <= 0f) return@forEachIndexed

                    val fatigueMultiplier = run {
                        val raw = fatigueFactorByMuscle[muscle] ?: return@forEachIndexed
                        blendedFatigueMultiplier(raw) ?: return@forEachIndexed
                    }

                    if (rawStimulus.isFinite() && rawStimulus > EPS) {
                        val stimulusLoad = rawStimulus * complexityFactor * exampleFactor
                        if (stimulusLoad.isFinite() && stimulusLoad > EPS) {
                            val baseValue = stimulusLoad * dampedRatio
                            if (baseValue.isFinite() && baseValue > EPS) {
                                val valueToAdd = baseValue * forceTypePenalty * fatigueMultiplier
                                if (valueToAdd.isFinite() && valueToAdd > EPS) {
                                    sessionStimulusByMuscle[muscle] =
                                        (sessionStimulusByMuscle[muscle] ?: 0f) + valueToAdd

                                    sessionExampleScoreByMuscle
                                        .getOrPut(muscle) { mutableMapOf() }[exampleId] =
                                        (sessionExampleScoreByMuscle[muscle]?.get(exampleId)
                                            ?: 0f) + valueToAdd

                                    if (groupEnum != null) {
                                        sessionExampleScoreByGroupEnum
                                            .getOrPut(groupEnum) { mutableMapOf() }[exampleId] =
                                            (sessionExampleScoreByGroupEnum[groupEnum]?.get(
                                                exampleId
                                            ) ?: 0f) + valueToAdd
                                    }
                                }
                            }
                        }
                    }

                    if (rawVolume.isFinite() && rawVolume > EPS) {
                        val baseValue = rawVolume * dampedRatio
                        if (baseValue.isFinite() && baseValue > EPS) {
                            val valueToAdd = baseValue * forceTypePenalty
                            if (valueToAdd.isFinite() && valueToAdd > EPS) {
                                sessionVolumeByMuscle[muscle] =
                                    (sessionVolumeByMuscle[muscle] ?: 0f) + valueToAdd
                            }
                        }
                    }
                }
            }

            mergeInto(overallStimulus, sessionStimulusByMuscle)
            mergeInto(overallVolume, sessionVolumeByMuscle)

            val sessionStimulusTotal =
                sessionStimulusByMuscle.values.sum().takeIf { it.isFinite() } ?: 0f
            sessionVolumeByMuscle.values.sum().takeIf { it.isFinite() } ?: 0f

            val sessionStimulusByGroupEnum =
                aggregateByGroupEnum(sessionStimulusByMuscle, groupByMuscle)
            val sessionVolumeByGroupEnum =
                aggregateByGroupEnum(sessionVolumeByMuscle, groupByMuscle)

            val primaryGroupEnum = sessionStimulusByGroupEnum.entries.maxByOrNull { it.value }?.key
            if (primaryGroupEnum != null) {
                perGroupStats.getOrPut(primaryGroupEnum) { EntryStats() }.primaryTrainingsCount += 1
            }

            sessionStimulusByGroupEnum.forEach { (groupEnum, value) ->
                val sharePercent =
                    if (sessionStimulusTotal > EPS) (value / sessionStimulusTotal) * 100f else 0f
                val hit = sharePercent.isFinite() && sharePercent >= HIT_SHARE_THRESHOLD_PERCENT

                val stats = perGroupStats.getOrPut(groupEnum) { EntryStats() }
                stats.maxStimulusInOneSession = maxOf(stats.maxStimulusInOneSession, value)
                stats.totalStimulusAcrossHits += if (hit) value else 0f
                stats.totalVolumeAcrossHits += if (hit) (sessionVolumeByGroupEnum[groupEnum]
                    ?: 0f) else 0f
                stats.maxVolumeInOneSession =
                    maxOf(stats.maxVolumeInOneSession, sessionVolumeByGroupEnum[groupEnum] ?: 0f)
                if (hit) stats.hitTrainingsCount += 1
            }

            sessionStimulusByMuscle.forEach { (muscle, value) ->
                val sharePercent =
                    if (sessionStimulusTotal > EPS) (value / sessionStimulusTotal) * 100f else 0f
                val hit = sharePercent.isFinite() && sharePercent >= HIT_SHARE_THRESHOLD_PERCENT

                val stats = perMuscleStats.getOrPut(muscle) { EntryStats() }
                stats.maxStimulusInOneSession = maxOf(stats.maxStimulusInOneSession, value)
                stats.totalStimulusAcrossHits += if (hit) value else 0f
                stats.totalVolumeAcrossHits += if (hit) (sessionVolumeByMuscle[muscle]
                    ?: 0f) else 0f
                stats.maxVolumeInOneSession =
                    maxOf(stats.maxVolumeInOneSession, sessionVolumeByMuscle[muscle] ?: 0f)
                if (hit) stats.hitTrainingsCount += 1
            }

            mergeExampleScoresByGroup(topExampleScoreByGroupEnum, sessionExampleScoreByGroupEnum)
            mergeExampleScoresByMuscle(topExampleScoreByMuscle, sessionExampleScoreByMuscle)
        }

        val topExamplesByGroupEnum = topExampleScoreByGroupEnum.mapValues { (_, m) ->
            m.entries.sortedByDescending { it.value }.take(TOP_EXAMPLES_LIMIT).map { it.key }
        }
        val topExamplesByMuscle = topExampleScoreByMuscle.mapValues { (_, m) ->
            m.entries.sortedByDescending { it.value }.take(TOP_EXAMPLES_LIMIT).map { it.key }
        }

        meta.dominantGroup?.let {
            // meta already computed, keep as is
        }

        return buildSummary(
            meta = meta,
            stimulusLoad = overallStimulus,
            volumeLoad = overallVolume,
            groups = groups,
            perGroupStats = perGroupStats.mapValues { it.value.toPublic(meta.trainingsCount) },
            perMuscleStats = perMuscleStats.mapValues { it.value.toPublic(meta.trainingsCount) },
            topExamplesByGroupEnum = topExamplesByGroupEnum,
            topExamplesByMuscle = topExamplesByMuscle,
        )
    }

    private data class EntryStats(
        var hitTrainingsCount: Int = 0,
        var primaryTrainingsCount: Int = 0,
        var totalStimulusAcrossHits: Float = 0f,
        var maxStimulusInOneSession: Float = 0f,
        var totalVolumeAcrossHits: Float = 0f,
        var maxVolumeInOneSession: Float = 0f,
    ) {
        fun toPublic(trainingsCount: Int): PublicStats {
            val hit = hitTrainingsCount.coerceAtLeast(0)
            val avgStimulus = if (hit > 0) totalStimulusAcrossHits / hit else 0f
            val avgVolume = if (hit > 0) totalVolumeAcrossHits / hit else 0f
            return PublicStats(
                hitTrainingsCount = hit,
                primaryTrainingsCount = primaryTrainingsCount.coerceAtLeast(0),
                avgStimulusPerHitSession = avgStimulus.takeIf { it.isFinite() } ?: 0f,
                maxStimulusInOneSession = maxStimulusInOneSession.takeIf { it.isFinite() } ?: 0f,
                avgVolumePerHitSession = avgVolume.takeIf { it.isFinite() } ?: 0f,
                maxVolumeInOneSession = maxVolumeInOneSession.takeIf { it.isFinite() } ?: 0f,
            )
        }
    }

    private data class PublicStats(
        val hitTrainingsCount: Int,
        val primaryTrainingsCount: Int,
        val avgStimulusPerHitSession: Float,
        val maxStimulusInOneSession: Float,
        val avgVolumePerHitSession: Float,
        val maxVolumeInOneSession: Float,
    )

    private suspend fun loadExamples(ids: Set<String>): Map<String, ExerciseExample> {
        if (ids.isEmpty()) return emptyMap()
        return exerciseExampleFeature.observeExerciseExamples(ids.toList())
            .first()
            .associateBy { it.value.id }
    }

    private suspend fun loadMuscleGroups(): List<MuscleGroup> {
        return muscleFeature.observeMuscles().first()
    }

    private fun computeMeta(sessions: List<List<Exercise>>): MuscleLoadMeta {
        val trainingsCount = sessions.size
        val totalExercises = sessions.sumOf { it.size }
        val totalSets = sessions.sumOf { s -> s.sumOf { it.iterations.size } }
        val totalReps = sessions.sumOf { s ->
            s.sumOf { ex -> ex.iterations.sumOf { it.repetitions.coerceAtLeast(0) } }
        }

        val totalVolume = sessions.sumOf { s ->
            s.sumOf { ex ->
                ex.iterations.sumOf {
                    it.volume.toDouble() * it.repetitions.coerceAtLeast(0).toDouble()
                }
            }
        }.toFloat().takeIf { it.isFinite() } ?: 0f

        return MuscleLoadMeta(
            trainingsCount = trainingsCount,
            totalExercises = totalExercises,
            totalSets = totalSets,
            totalRepetitions = totalReps,
            totalVolume = totalVolume,
            dominantGroup = null,
        )
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

    private fun forceTypePenalty(
        example: ExerciseExample,
        originalSharePercent: Float,
    ): Float {
        if (!originalSharePercent.isFinite() || originalSharePercent <= EPS) return 1f

        val isPush = example.value.forceType == ForceTypeEnum.PUSH
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
            if (reps == 0) 0.0 else {
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
            if (reps == 0) 0.0 else {
                val w = iteration.volume
                if (!w.isFinite() || w <= EPS) 0.0 else (w.toDouble() * reps.toDouble())
            }
        }.toFloat()

        return if (total.isFinite() && total > 0f) total else 0f
    }

    private fun computeExampleLoad(example: ExerciseExample?): Map<MuscleEnum, Float> {
        if (example == null) return emptyMap()
        val bundles = example.bundles
        if (bundles.isEmpty()) return emptyMap()

        val totals = mutableMapOf<MuscleEnum, Float>()

        bundles.forEach { bundle ->
            val muscle = bundle.muscle.type
            val valueToAdd = bundle.percentage.toFloat()
            if (!valueToAdd.isFinite() || valueToAdd <= EPS) return@forEach

            val updated = (totals[muscle] ?: 0f) + valueToAdd
            if (!updated.isFinite() || updated <= EPS) return@forEach

            totals[muscle] = updated
        }

        return totals.filterValues { it.isFinite() && it > EPS }
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
        meta: MuscleLoadMeta,
        stimulusLoad: Map<MuscleEnum, Float>,
        volumeLoad: Map<MuscleEnum, Float>,
        groups: List<MuscleGroup>,
        perGroupStats: Map<MuscleGroupEnum, PublicStats>,
        perMuscleStats: Map<MuscleEnum, PublicStats>,
        topExamplesByGroupEnum: Map<MuscleGroupEnum, List<String>>,
        topExamplesByMuscle: Map<MuscleEnum, List<String>>,
    ): MuscleLoadSummary {
        val groupByMuscle = buildGroupByMuscleMap(groups)

        val perMuscleEntries = buildPerMuscleBreakdown(
            muscleLoad = stimulusLoad,
            groupByMuscle = groupByMuscle,
            statsByMuscle = perMuscleStats,
            topExamplesByMuscle = topExamplesByMuscle,
        )

        val perGroupEntries = buildPerGroupBreakdown(
            muscleLoad = stimulusLoad,
            groups = groups,
            groupByMuscle = groupByMuscle,
            statsByGroupEnum = perGroupStats,
            topExamplesByGroupEnum = topExamplesByGroupEnum,
        )

        val volumePerMuscleEntries = buildPerMuscleBreakdown(
            muscleLoad = volumeLoad,
            groupByMuscle = groupByMuscle,
            statsByMuscle = perMuscleStats,
            topExamplesByMuscle = topExamplesByMuscle,
            isVolume = true,
        )

        val volumePerGroupEntries = buildPerGroupBreakdown(
            muscleLoad = volumeLoad,
            groups = groups,
            groupByMuscle = groupByMuscle,
            statsByGroupEnum = perGroupStats,
            topExamplesByGroupEnum = topExamplesByGroupEnum,
            isVolume = true,
        )

        val dominance = buildDominanceFromValues(stimulusLoad.values.toList())
        val groupDominance = buildDominanceFromValues(
            aggregateByGroupId(stimulusLoad, groupByMuscle).values.toList()
        )

        val dominantGroup = run {
            val byGroupEnum = aggregateByGroupEnum(stimulusLoad, groupByMuscle)
            byGroupEnum.entries.maxByOrNull { it.value }?.key
        }

        val fixedMeta = meta.copy(dominantGroup = dominantGroup)

        return MuscleLoadSummary(
            meta = fixedMeta,
            perGroup = MuscleLoadBreakdown(perGroupEntries),
            perMuscle = MuscleLoadBreakdown(perMuscleEntries),
            volumePerGroup = MuscleLoadBreakdown(volumePerGroupEntries),
            volumePerMuscle = MuscleLoadBreakdown(volumePerMuscleEntries),
            dominance = dominance,
            groupDominance = groupDominance,
        )
    }

    private fun buildDominanceFromValues(values: List<Float>): MuscleLoadDominance {
        val sorted = values.filter { it.isFinite() && it > EPS }.sortedDescending()
        if (sorted.isEmpty()) return MuscleLoadDominance(0f, 0f)

        val total = sorted.sum()
        if (!total.isFinite() || total <= EPS) return MuscleLoadDominance(0f, 0f)

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
        statsByMuscle: Map<MuscleEnum, PublicStats>,
        topExamplesByMuscle: Map<MuscleEnum, List<String>>,
        isVolume: Boolean = false,
    ): List<MuscleLoadEntry> {
        val filtered = muscleLoad.filterValues { it.isFinite() && it > EPS }
        if (filtered.isEmpty()) return emptyList()

        val sum = filtered.values.sum()
        if (!sum.isFinite() || sum <= EPS) return emptyList()

        return filtered.entries
            .sortedByDescending { it.value }
            .mapNotNull { (muscle, value) ->
                val group = groupByMuscle[muscle] ?: return@mapNotNull null
                val percent = (value / sum) * 100f
                if (!percent.isFinite() || percent <= EPS) return@mapNotNull null

                val stats = statsByMuscle[muscle]
                val topExamples = topExamplesByMuscle[muscle] ?: emptyList()

                MuscleLoadEntry(
                    group = group.type,
                    percentage = percent.coerceIn(0f, 100f),
                    muscles = listOf(muscle),
                    hitTrainingsCount = stats?.hitTrainingsCount ?: 0,
                    primaryTrainingsCount = 0,
                    avgStimulusPerHitSession = if (!isVolume) (stats?.avgStimulusPerHitSession
                        ?: 0f) else 0f,
                    maxStimulusInOneSession = if (!isVolume) (stats?.maxStimulusInOneSession
                        ?: 0f) else 0f,
                    avgVolumePerHitSession = if (isVolume) (stats?.avgVolumePerHitSession
                        ?: 0f) else (stats?.avgVolumePerHitSession ?: 0f),
                    maxVolumeInOneSession = if (isVolume) (stats?.maxVolumeInOneSession
                        ?: 0f) else (stats?.maxVolumeInOneSession ?: 0f),
                    topExampleIds = if (!isVolume) topExamples else emptyList(),
                )
            }
    }

    private fun buildPerGroupBreakdown(
        muscleLoad: Map<MuscleEnum, Float>,
        groups: List<MuscleGroup>,
        groupByMuscle: Map<MuscleEnum, MuscleGroup>,
        statsByGroupEnum: Map<MuscleGroupEnum, PublicStats>,
        topExamplesByGroupEnum: Map<MuscleGroupEnum, List<String>>,
        isVolume: Boolean = false,
    ): List<MuscleLoadEntry> {
        val filtered = muscleLoad.filterValues { it.isFinite() && it > EPS }
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

                val stats = statsByGroupEnum[group.type]
                val topExamples = topExamplesByGroupEnum[group.type] ?: emptyList()

                MuscleLoadEntry(
                    group = group.type,
                    percentage = percent.coerceIn(0f, 100f),
                    muscles = muscles,
                    hitTrainingsCount = stats?.hitTrainingsCount ?: 0,
                    primaryTrainingsCount = stats?.primaryTrainingsCount ?: 0,
                    avgStimulusPerHitSession = if (!isVolume) (stats?.avgStimulusPerHitSession
                        ?: 0f) else 0f,
                    maxStimulusInOneSession = if (!isVolume) (stats?.maxStimulusInOneSession
                        ?: 0f) else 0f,
                    avgVolumePerHitSession = stats?.avgVolumePerHitSession ?: 0f,
                    maxVolumeInOneSession = stats?.maxVolumeInOneSession ?: 0f,
                    topExampleIds = if (!isVolume) topExamples else emptyList(),
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

    private fun mergeInto(target: MutableMap<MuscleEnum, Float>, src: Map<MuscleEnum, Float>) {
        src.forEach { (k, v) ->
            if (!v.isFinite() || v <= EPS) return@forEach
            target[k] = (target[k] ?: 0f) + v
        }
    }

    private fun aggregateByGroupEnum(
        byMuscle: Map<MuscleEnum, Float>,
        groupByMuscle: Map<MuscleEnum, MuscleGroup>,
    ): Map<MuscleGroupEnum, Float> {
        val out = mutableMapOf<MuscleGroupEnum, Float>()
        byMuscle.forEach { (muscle, value) ->
            val groupEnum = groupByMuscle[muscle]?.type ?: return@forEach
            out[groupEnum] = (out[groupEnum] ?: 0f) + value
        }
        return out
    }

    private fun aggregateByGroupId(
        byMuscle: Map<MuscleEnum, Float>,
        groupByMuscle: Map<MuscleEnum, MuscleGroup>,
    ): Map<String, Float> {
        val out = mutableMapOf<String, Float>()
        byMuscle.forEach { (muscle, value) ->
            val groupId = groupByMuscle[muscle]?.id ?: return@forEach
            out[groupId] = (out[groupId] ?: 0f) + value
        }
        return out
    }

    private fun mergeExampleScoresByGroup(
        target: MutableMap<MuscleGroupEnum, MutableMap<String, Float>>,
        src: Map<MuscleGroupEnum, MutableMap<String, Float>>,
    ) {
        src.forEach { (k, v) ->
            val t = target.getOrPut(k) { mutableMapOf() }
            v.forEach { (exampleId, score) ->
                if (!score.isFinite() || score <= EPS) return@forEach
                t[exampleId] = (t[exampleId] ?: 0f) + score
            }
        }
    }

    private fun mergeExampleScoresByMuscle(
        target: MutableMap<MuscleEnum, MutableMap<String, Float>>,
        src: Map<MuscleEnum, MutableMap<String, Float>>,
    ) {
        src.forEach { (k, v) ->
            val t = target.getOrPut(k) { mutableMapOf() }
            v.forEach { (exampleId, score) ->
                if (!score.isFinite() || score <= EPS) return@forEach
                t[exampleId] = (t[exampleId] ?: 0f) + score
            }
        }
    }
}