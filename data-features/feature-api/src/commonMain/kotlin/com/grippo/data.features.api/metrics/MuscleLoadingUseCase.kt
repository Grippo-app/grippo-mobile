package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
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

public class MuscleLoadingUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val muscleFeature: MuscleFeature,
) {
    private companion object Companion {
        private const val EPS = 1e-3f
    }

    public suspend fun fromTrainings(trainings: List<Training>): MuscleLoadSummary {
        val exercises: List<Exercise> = trainings.flatMap { training -> training.exercises }
        return fromExercises(exercises)
    }

    public suspend fun fromTraining(training: Training): MuscleLoadSummary {
        val exercises: List<Exercise> = training.exercises
        return fromExercises(exercises)
    }

    public suspend fun fromExercises(exercises: List<Exercise>): MuscleLoadSummary {
        val exampleIds = exercises.map { it.exerciseExample.id }.toSet()
        val examples = loadExamples(exampleIds)
        val groups = loadMuscleGroups()

        val stimulusLoad = computeMuscleLoad(exercises, examples, ::exerciseStimulus)
        val volumeLoad = computeMuscleLoad(exercises, examples, ::exerciseVolume)
        return buildSummary(stimulusLoad, volumeLoad, groups)
    }

    public suspend fun fromExerciseExample(exampleId: String): MuscleLoadSummary {
        val examples = loadExamples(setOf(exampleId))
        val groups = loadMuscleGroups()

        val stimulusLoad = computeExampleLoad(examples[exampleId])
        val volumeLoad = emptyMap<MuscleEnum, Float>()
        return buildSummary(stimulusLoad, volumeLoad, groups)
    }

    private suspend fun loadExamples(ids: Set<String>): Map<String, ExerciseExample> {
        return exerciseExampleFeature.observeExerciseExamples(ids.toList())
            .first()
            .associateBy { it.value.id }
    }

    private suspend fun loadMuscleGroups(): List<MuscleGroup> {
        return muscleFeature.observeMuscles().first()
    }

    private fun computeMuscleLoad(
        exercises: List<Exercise>,
        exampleMap: Map<String, ExerciseExample>,
        baseLoad: (List<Iteration>) -> Float,
    ): Map<MuscleEnum, Float> {
        if (exercises.isEmpty()) return emptyMap()

        val contributions = mutableMapOf<MuscleEnum, Float>()

        exercises.forEach { exercise ->
            val exampleId = exercise.exerciseExample.id
            val load = baseLoad(exercise.iterations)
            if (load <= EPS) return@forEach

            val example = exampleMap[exampleId] ?: return@forEach

            val bundles = example.bundles
            val totalShare = bundles.fold(0f) { acc, bundle ->
                acc + bundle.percentage.coerceAtLeast(0).toFloat()
            }
            if (totalShare <= EPS) return@forEach

            bundles.forEach { bundle ->
                val share = bundle.percentage.coerceAtLeast(0)
                if (share == 0) return@forEach
                val ratio = share / totalShare
                val muscle = bundle.muscle.type
                val currentValue = contributions[muscle] ?: 0f
                contributions[muscle] = currentValue + load * ratio
            }
        }

        return contributions
    }

    private fun computeExampleLoad(example: ExerciseExample?): Map<MuscleEnum, Float> {
        if (example == null || example.bundles.isEmpty()) return emptyMap()
        val totals = mutableMapOf<MuscleEnum, Float>()
        example.bundles.forEach { bundle ->
            val percent = bundle.percentage.coerceAtLeast(0)
            if (percent == 0) return@forEach
            val muscle = bundle.muscle.type
            val currentValue = totals[muscle] ?: 0f
            totals[muscle] = currentValue + percent.toFloat()
        }
        return totals
    }

    private fun exerciseStimulus(iterations: List<Iteration>): Float {
        if (iterations.isEmpty()) return 0f
        return iterations
            .sumOf { it.repetitions.coerceAtLeast(0).toDouble() }
            .toFloat()
            .coerceAtLeast(0f)
    }

    private fun exerciseVolume(iterations: List<Iteration>): Float {
        if (iterations.isEmpty()) return 0f
        return iterations
            .sumOf { iteration ->
                val reps = iteration.repetitions.coerceAtLeast(0)
                if (reps == 0) 0.0
                else {
                    val w = iteration.volume
                    if (w <= EPS) 0.0 else (w.toDouble() * reps.toDouble())
                }
            }
            .toFloat()
            .coerceAtLeast(0f)
    }

    private fun buildSummary(
        stimulusLoad: Map<MuscleEnum, Float>,
        volumeLoad: Map<MuscleEnum, Float>,
        groups: List<MuscleGroup>,
    ): MuscleLoadSummary {
        val muscleNames = buildMuscleNameMap(groups)
        val groupByMuscle = buildGroupByMuscleMap(groups)

        val perMuscleEntries = buildPerMuscleBreakdown(stimulusLoad, muscleNames)
        val perGroupEntries = buildPerGroupBreakdown(stimulusLoad, groups, groupByMuscle)

        val volumePerMuscleEntries = buildPerMuscleBreakdown(volumeLoad, muscleNames)
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
        val sorted = stimulusLoad.values.filter { it.isFinite() && it > EPS }.sortedDescending()
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
        muscleNames: Map<MuscleEnum, String>,
    ): List<MuscleLoadEntry> {
        if (muscleLoad.isEmpty()) return emptyList()
        val maxValue = muscleLoad.values.maxOrNull() ?: return emptyList()
        if (maxValue <= EPS) return emptyList()

        return muscleLoad.entries
            .sortedByDescending { it.value }
            .map { (muscle, value) ->
                val label = muscleNames[muscle] ?: muscle.name.lowercase()
                val normalized = (value / maxValue) * 100f
                MuscleLoadEntry(
                    label = label,
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
        if (muscleLoad.isEmpty()) return emptyList()

        val totals = mutableMapOf<String, Pair<Float, List<MuscleEnum>>>()

        muscleLoad.forEach { (muscle, value) ->
            val group = groupByMuscle[muscle] ?: return@forEach
            val muscles = group.muscles.map { it.type }
            val currentTotal = totals[group.id]?.first ?: 0f
            totals[group.id] = (currentTotal + value) to muscles
        }

        val sum = totals.values.sumOf { it.first.toDouble() }.toFloat()
        if (sum <= EPS) return emptyList()

        return totals.entries
            .sortedByDescending { it.value.first }
            .map { (groupId, valuePair) ->
                val group = groups.firstOrNull { it.id == groupId }
                val label = group?.name ?: groupId
                val percent = (valuePair.first / sum) * 100f
                MuscleLoadEntry(
                    label = label,
                    percentage = percent.coerceIn(0f, 100f),
                    muscles = valuePair.second
                )
            }
    }

    private fun buildMuscleNameMap(groups: List<MuscleGroup>): Map<MuscleEnum, String> {
        return groups
            .flatMap { group -> group.muscles.map { it.type to it.name } }
            .toMap()
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

    // Intentionally stateless: no caches, only pure computations.
}
