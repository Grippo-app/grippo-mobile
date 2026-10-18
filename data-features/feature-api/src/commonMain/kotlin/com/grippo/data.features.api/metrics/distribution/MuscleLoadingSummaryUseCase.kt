package com.grippo.data.features.api.metrics.distribution

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents
import com.grippo.data.features.api.metrics.distribution.models.MuscleLoadBreakdown
import com.grippo.data.features.api.metrics.distribution.models.MuscleLoadDominance
import com.grippo.data.features.api.metrics.distribution.models.MuscleLoadEntry
import com.grippo.data.features.api.metrics.distribution.models.MuscleLoadMeta
import com.grippo.data.features.api.metrics.distribution.models.MuscleLoadSummary
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.math.abs

/**
 * Estimates the distribution of performed sets using the exercise's recorded muscle shares.
 * These weighted sets describe exercise selection, not physiological stimulus or recovery.
 * Recorded resistance × repetitions is distributed separately using the same linear shares.
 */
public class MuscleLoadingSummaryUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
) {
    private companion object {
        private const val TOP_EXAMPLES_LIMIT = 3

        // Summing equal shares in different orders must not manufacture a single leader.
        private const val LEADER_RELATIVE_TOLERANCE = 1e-12
    }

    public suspend fun fromTrainings(trainings: List<Training>): MuscleLoadSummary {
        return observeFromTrainings(trainings).first()
    }

    public suspend fun fromTraining(training: Training): MuscleLoadSummary {
        return fromTrainings(listOf(training))
    }

    public suspend fun fromExercises(exercises: List<Exercise>): MuscleLoadSummary {
        return observeFromExercises(exercises).first()
    }

    /** Recalculates when the referenced exercise anatomy changes in the local cache. */
    public fun observeFromTrainings(trainings: List<Training>): Flow<MuscleLoadSummary> {
        return observeFromSessions(sessions = trainings.map { it.exercises })
    }

    /** Treats the supplied exercises as one logical session. */
    public fun observeFromExercises(exercises: List<Exercise>): Flow<MuscleLoadSummary> {
        return observeFromSessions(listOf(exercises))
    }

    public suspend fun fromExerciseExample(exampleId: String): MuscleLoadSummary {
        val example = exerciseExampleFeature.observeExerciseExample(exampleId).first()
        return if (example != null) {
            fromExerciseExample(example)
        } else {
            summarize(meta = emptyMeta(), muscles = emptyMap(), groups = emptyMap())
        }
    }

    /** Anatomy preview; no sessions, performed work or session statistics are inferred. */
    public fun fromExerciseExample(example: ExerciseExample): MuscleLoadSummary {
        val muscles = normalizedShares(example).mapValues { (_, ratio) ->
            EntryStats().apply {
                total.weightedSets = ratio
            }
        }
        val groups = mutableMapOf<MuscleGroupEnum, EntryStats>()
        muscles.forEach { (muscle, stats) ->
            groups.getOrPut(muscle.group()) { EntryStats() }.total.merge(stats.total)
        }
        return summarize(meta = emptyMeta(), muscles = muscles, groups = groups)
    }

    private fun observeFromSessions(
        sessions: List<List<Exercise>>,
    ): Flow<MuscleLoadSummary> {
        val ids = sessions.asSequence()
            .flatMap { it.asSequence() }
            .map { it.exerciseExample.id }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()

        if (ids.isEmpty()) {
            val summary = calculate(
                sessions = sessions,
                examples = emptyMap(),
            )
            return flowOf(summary)
        }

        return exerciseExampleFeature.observeExerciseExamples(ids).map { examples ->
            calculate(
                sessions = sessions,
                examples = examples.associateBy { it.value.id },
            )
        }
    }

    private fun calculate(
        sessions: List<List<Exercise>>,
        examples: Map<String, ExerciseExample>,
    ): MuscleLoadSummary {
        var totalExercises = 0L
        var totalSets = 0L
        var totalRepetitions = 0L
        var totalVolume = 0.0
        var excludedSets = 0L
        var unassignedSets = 0L
        var unmeasuredVolumeSets = 0L
        val muscles = mutableMapOf<MuscleEnum, EntryStats>()
        val groups = mutableMapOf<MuscleGroupEnum, EntryStats>()
        val sharesByExample = examples.mapValues { (_, example) -> normalizedShares(example) }

        sessions.forEach { exercises ->
            val sessionMuscles = mutableMapOf<MuscleEnum, Load>()

            exercises.forEach { exercise ->
                var acceptedSets = 0
                var exerciseVolume = 0.0
                var hasMeasuredVolume = false
                val exampleId = exercise.exerciseExample.id
                val components = examples[exampleId]?.components
                for (iteration in exercise.iterations) {
                    if (!hasValidRecordedValues(iteration = iteration, components = components)) {
                        excludedSets = (excludedSets + 1).checkedCount()
                        continue
                    }

                    val resistance = measuredResistance(iteration = iteration, components = components)
                    acceptedSets += 1
                    totalRepetitions = (totalRepetitions + iteration.repetitions).checkedCount()
                    if (resistance == null) {
                        unmeasuredVolumeSets = (unmeasuredVolumeSets + 1).checkedCount()
                    } else {
                        hasMeasuredVolume = true
                        exerciseVolume += resistance * iteration.repetitions
                    }
                }

                if (acceptedSets > 0) {
                    totalExercises = (totalExercises + 1).checkedCount()
                    totalSets = (totalSets + acceptedSets).checkedCount()
                    totalVolume += exerciseVolume

                    val shares = sharesByExample[exampleId].orEmpty()
                    if (shares.isEmpty()) {
                        unassignedSets = (unassignedSets + acceptedSets).checkedCount()
                    }
                    shares.forEach { (muscle, ratio) ->
                        sessionMuscles.getOrPut(muscle) { Load() }.add(
                            weightedSets = acceptedSets * ratio,
                            volume = exerciseVolume * ratio,
                            hasMeasuredVolume = hasMeasuredVolume,
                            exampleId = exampleId,
                        )
                    }
                }
            }

            val sessionGroups = mutableMapOf<MuscleGroupEnum, Load>()
            sessionMuscles.forEach { (muscle, load) ->
                muscles.getOrPut(muscle) { EntryStats() }.record(load)
                sessionGroups.getOrPut(muscle.group()) { Load() }.merge(load)
            }
            val leaders = leaders(sessionGroups.mapValues { it.value.weightedSets })
            sessionGroups.forEach { (group, load) ->
                groups.getOrPut(group) { EntryStats() }.record(load = load, primary = group in leaders)
            }
        }

        return summarize(
            meta = MuscleLoadMeta(
                trainingsCount = sessions.size,
                totalExercises = totalExercises.toInt(),
                totalSets = totalSets.toInt(),
                totalRepetitions = totalRepetitions.toInt(),
                totalVolume = totalVolume.checkedFloat(),
                dominantGroup = null,
                excludedSets = excludedSets.toInt(),
                unassignedSets = unassignedSets.toInt(),
                unmeasuredVolumeSets = unmeasuredVolumeSets.toInt(),
            ),
            muscles = muscles,
            groups = groups,
        )
    }

    private fun normalizedShares(example: ExerciseExample): Map<MuscleEnum, Double> {
        val shares = mutableMapOf<MuscleEnum, Double>()
        example.bundles.forEach { bundle ->
            if (bundle.percentage > 0) {
                val muscle = bundle.muscle.type
                shares[muscle] = (shares[muscle] ?: 0.0) + bundle.percentage
            }
        }
        val total = shares.values.sum()
        return shares.mapValues { (_, share) -> share / total }
    }

    private fun hasValidRecordedValues(
        iteration: Iteration,
        components: ExerciseExampleComponents?,
    ): Boolean {
        if (iteration.repetitions <= 0) {
            return false
        }

        val weights = listOf(
            iteration.externalWeight,
            iteration.extraWeight,
            iteration.bodyWeight,
            iteration.assistWeight,
        )
        if (weights.any { it != null && (!it.isFinite() || it < 0f) }) {
            return false
        }
        if (iteration.bodyWeight != null && iteration.bodyWeight <= 0f) {
            return false
        }

        val multiplier = iteration.bodyMultiplier
        if (multiplier != null && (!multiplier.isFinite() || multiplier <= 0f)) {
            return false
        }
        if (components is ExerciseExampleComponents.External && components.required &&
            iteration.externalWeight == 0f
        ) {
            return false
        }
        return true
    }

    private fun measuredResistance(
        iteration: Iteration,
        components: ExerciseExampleComponents?,
    ): Double? {
        if (components == null) {
            return null
        }

        val hasBodyWeight = iteration.bodyWeight != null
        val hasBodyMultiplier = iteration.bodyMultiplier != null
        if (hasBodyWeight != hasBodyMultiplier) {
            return null
        }

        // Unexpected supplied fields make the measurement ambiguous, even when their value is zero.
        val complete = when (components) {
            is ExerciseExampleComponents.External ->
                (!components.required || iteration.externalWeight != null) &&
                        !hasBodyWeight && iteration.extraWeight == null && iteration.assistWeight == null

            is ExerciseExampleComponents.BodyOnly ->
                (!components.required || hasBodyWeight) &&
                        iteration.externalWeight == null && iteration.extraWeight == null &&
                        iteration.assistWeight == null

            is ExerciseExampleComponents.BodyAndExtra ->
                (!components.bodyRequired || hasBodyWeight) &&
                        (!components.extraRequired || iteration.extraWeight != null) &&
                        iteration.externalWeight == null && iteration.assistWeight == null

            is ExerciseExampleComponents.BodyAndAssist ->
                (!components.bodyRequired || hasBodyWeight) &&
                        (!components.assistRequired || iteration.assistWeight != null) &&
                        iteration.externalWeight == null && iteration.extraWeight == null
        }
        if (!complete) {
            return null
        }

        // Schema checks establish absent terms as known absent; historical values are never filled.
        val bodyResistance = (iteration.bodyWeight ?: 0f) * (iteration.bodyMultiplier ?: 0f)
        if (!bodyResistance.isFinite()) {
            return null
        }
        val resistance = (iteration.externalWeight?.toDouble() ?: 0.0) +
                (iteration.extraWeight?.toDouble() ?: 0.0) +
                bodyResistance.toDouble() -
                (iteration.assistWeight?.toDouble() ?: 0.0)
        // Negative machine conventions require metadata before they can represent physical volume.
        return resistance.takeIf { it.isFinite() && it >= 0.0 }
    }

    private fun summarize(
        meta: MuscleLoadMeta,
        muscles: Map<MuscleEnum, EntryStats>,
        groups: Map<MuscleGroupEnum, EntryStats>,
    ): MuscleLoadSummary {
        val groupSets = groups.mapValues { it.value.total.weightedSets }
        val perGroup = groupEntries(groups = groups, muscles = muscles, volume = false)
        val perMuscle = muscleEntries(muscles = muscles, volume = false)
        val volumePerGroup = groupEntries(groups = groups, muscles = muscles, volume = true)
        val volumePerMuscle = muscleEntries(muscles = muscles, volume = true)

        return MuscleLoadSummary(
            meta = meta.copy(dominantGroup = leaders(groupSets).singleOrNull()),
            perGroup = MuscleLoadBreakdown(perGroup),
            perMuscle = MuscleLoadBreakdown(perMuscle),
            volumePerGroup = MuscleLoadBreakdown(volumePerGroup),
            volumePerMuscle = MuscleLoadBreakdown(volumePerMuscle),
            dominance = dominance(muscles.values.map { it.total.weightedSets }),
            groupDominance = dominance(groupSets.values.toList()),
        )
    }

    private fun muscleEntries(
        muscles: Map<MuscleEnum, EntryStats>,
        volume: Boolean,
    ): List<MuscleLoadEntry> {
        val sorted = muscles.entries
            .filter { it.value.total.quantity(volume) > 0.0 }
            .sortedWith(compareByDescending<Map.Entry<MuscleEnum, EntryStats>> {
                it.value.total.quantity(volume).checkedFloat()
            }.thenBy { it.key.name })
        val total = sorted.sumOf { it.value.total.quantity(volume) }
        return sorted.map { (muscle, stats) ->
            stats.toEntry(
                group = muscle.group(),
                muscles = listOf(muscle),
                percentage = stats.total.quantity(volume) / total * 100.0,
            )
        }
    }

    private fun groupEntries(
        groups: Map<MuscleGroupEnum, EntryStats>,
        muscles: Map<MuscleEnum, EntryStats>,
        volume: Boolean,
    ): List<MuscleLoadEntry> {
        val sorted = groups.entries
            .filter { it.value.total.quantity(volume) > 0.0 }
            .sortedWith(compareByDescending<Map.Entry<MuscleGroupEnum, EntryStats>> {
                it.value.total.quantity(volume).checkedFloat()
            }.thenBy { it.key.name })
        val total = sorted.sumOf { it.value.total.quantity(volume) }
        return sorted.map { (group, stats) ->
            stats.toEntry(
                group = group,
                muscles = muscles.keys.filter {
                    it.group() == group && muscles.getValue(it).total.quantity(volume) > 0.0
                }.sortedBy { it.name },
                percentage = stats.total.quantity(volume) / total * 100.0,
            )
        }
    }

    private fun dominance(values: List<Double>): MuscleLoadDominance {
        val sorted = values.filter { it > 0.0 }.sortedDescending()
        val total = sorted.sum()
        if (sorted.isEmpty()) {
            return MuscleLoadDominance(top1SharePercent = 0f, top2SharePercent = 0f)
        }
        return MuscleLoadDominance(
            top1SharePercent = (sorted.first() / total * 100.0).checkedFloat(),
            top2SharePercent = (sorted.take(2).sum() / total * 100.0).checkedFloat(),
        )
    }

    private fun leaders(values: Map<MuscleGroupEnum, Double>): Set<MuscleGroupEnum> {
        val maximum = values.values.maxOrNull() ?: return emptySet()
        if (maximum <= 0.0) {
            return emptySet()
        }
        return values.filterValues {
            abs(it - maximum) <= maximum * LEADER_RELATIVE_TOLERANCE
        }.keys
    }

    private class Load {
        var weightedSets: Double = 0.0
        var volume: Double = 0.0
        var hasMeasuredVolume: Boolean = false
        val examples: MutableMap<String, Double> = mutableMapOf()

        fun add(
            weightedSets: Double,
            volume: Double,
            hasMeasuredVolume: Boolean,
            exampleId: String,
        ) {
            this.weightedSets += weightedSets
            this.volume += volume
            this.hasMeasuredVolume = this.hasMeasuredVolume || hasMeasuredVolume
            examples[exampleId] = (examples[exampleId] ?: 0.0) + weightedSets
        }

        fun merge(other: Load) {
            weightedSets += other.weightedSets
            volume += other.volume
            hasMeasuredVolume = hasMeasuredVolume || other.hasMeasuredVolume
            other.examples.forEach { (id, score) ->
                examples[id] = (examples[id] ?: 0.0) + score
            }
        }

        fun quantity(volume: Boolean): Double {
            return if (volume) {
                this.volume
            } else {
                weightedSets
            }
        }
    }

    private class EntryStats {
        val total: Load = Load()
        var hitTrainingsCount: Int = 0
        var primaryTrainingsCount: Int = 0
        var measuredVolumeSessions: Int = 0
        var maxWeightedSets: Double = 0.0
        var maxVolume: Double = 0.0

        fun record(load: Load, primary: Boolean = false) {
            total.merge(load)
            hitTrainingsCount += 1
            if (primary) {
                primaryTrainingsCount += 1
            }
            maxWeightedSets = maxOf(a = maxWeightedSets, b = load.weightedSets)
            if (load.hasMeasuredVolume) {
                measuredVolumeSessions += 1
                maxVolume = maxOf(a = maxVolume, b = load.volume)
            }
        }

        fun toEntry(
            group: MuscleGroupEnum,
            muscles: List<MuscleEnum>,
            percentage: Double,
        ): MuscleLoadEntry {
            return MuscleLoadEntry(
                group = group,
                percentage = percentage.checkedFloat(),
                muscles = muscles,
                hitTrainingsCount = hitTrainingsCount,
                primaryTrainingsCount = primaryTrainingsCount,
                avgWeightedSetsPerSession = if (hitTrainingsCount > 0) {
                    (total.weightedSets / hitTrainingsCount).checkedFloat()
                } else {
                    0f
                },
                maxWeightedSetsInOneSession = maxWeightedSets.checkedFloat(),
                avgVolumePerHitSession = if (measuredVolumeSessions > 0) {
                    (total.volume / measuredVolumeSessions).checkedFloat()
                } else {
                    0f
                },
                maxVolumeInOneSession = maxVolume.checkedFloat(),
                topExampleIds = total.examples.entries
                    .sortedWith(compareByDescending<Map.Entry<String, Double>> {
                        it.value.checkedFloat()
                    }.thenBy { it.key })
                    .take(TOP_EXAMPLES_LIMIT)
                    .map { it.key },
            )
        }
    }

    private fun emptyMeta(): MuscleLoadMeta {
        return MuscleLoadMeta(
            trainingsCount = 0,
            totalExercises = 0,
            totalSets = 0,
            totalRepetitions = 0,
            totalVolume = 0f,
            dominantGroup = null,
        )
    }
}

private fun Long.checkedCount(): Long {
    check(this in 0..Int.MAX_VALUE.toLong()) {
        "Muscle load count exceeds the supported Int range"
    }
    return this
}

private fun Double.checkedFloat(): Float {
    check(isFinite() && this in 0.0..Float.MAX_VALUE.toDouble()) {
        "Muscle load quantity exceeds the supported finite Float range"
    }
    return toFloat()
}
