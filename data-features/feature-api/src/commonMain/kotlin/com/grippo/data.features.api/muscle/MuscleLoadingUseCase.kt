package com.grippo.data.features.api.muscle

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.muscle.models.MuscleLoadBreakdown
import com.grippo.data.features.api.muscle.models.MuscleLoadEntry
import com.grippo.data.features.api.muscle.models.MuscleLoadSummary
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.data.features.api.training.models.SetTraining
import kotlinx.coroutines.flow.first

public class MuscleLoadingUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val muscleFeature: MuscleFeature,
) {

    public suspend fun fromTrainings(trainings: List<SetTraining>): MuscleLoadSummary {
        val exercises = trainings.flatMap { it.exercises }
        return fromExercises(exercises)
    }

    public suspend fun fromExercises(exercises: List<SetExercise>): MuscleLoadSummary {
        val exampleIds = exercises.map { it.exerciseExample.id }.toSet()
        val examples = loadExamples(exampleIds)
        val groups = loadMuscleGroups()

        val muscleLoad = computeMuscleLoad(exercises, examples)
        return buildSummary(muscleLoad, groups)
    }

    public suspend fun fromExerciseExample(exampleId: String): MuscleLoadSummary {
        val examples = loadExamples(setOf(exampleId))
        val groups = loadMuscleGroups()

        val muscleLoad = computeExampleLoad(examples[exampleId])
        return buildSummary(muscleLoad, groups)
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
        exercises: List<SetExercise>,
        exampleMap: Map<String, ExerciseExample>,
    ): Map<MuscleEnum, Float> {
        if (exercises.isEmpty()) return emptyMap()

        val workload = selectWorkload(exercises)
        val contributions = mutableMapOf<MuscleEnum, Float>()

        exercises.forEach { exercise ->
            val exampleId = exercise.exerciseExample.id
            val load = exerciseLoad(exercise.iterations, workload)
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
                contributions[muscle] = (contributions[muscle] ?: 0f) + load * ratio
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
            totals[muscle] = (totals[muscle] ?: 0f) + percent.toFloat()
        }
        return totals
    }

    private fun selectWorkload(exercises: List<SetExercise>): Workload {
        exercises.forEach { exercise ->
            exercise.iterations.forEach { iteration ->
                val weight = iteration.volume
                val reps = iteration.repetitions
                if (weight > EPS && reps > 0) return Workload.Volume
            }
        }
        return Workload.Repetitions
    }

    private fun exerciseLoad(iterations: List<SetIteration>, workload: Workload): Float {
        return when (workload) {
            Workload.Volume -> iterations.fold(0f) { acc, iteration ->
                val weight = iteration.volume
                val reps = iteration.repetitions.coerceAtLeast(0)
                if (weight <= EPS || reps == 0) acc else acc + weight * reps
            }

            Workload.Repetitions -> iterations.fold(0) { acc, iteration ->
                acc + iteration.repetitions.coerceAtLeast(0)
            }.toFloat()
        }.coerceAtLeast(0f)
    }

    private fun buildSummary(
        muscleLoad: Map<MuscleEnum, Float>,
        groups: List<MuscleGroup>,
    ): MuscleLoadSummary {
        val muscleNames = buildMuscleNameMap(groups)
        val groupByMuscle = buildGroupByMuscleMap(groups)

        val perMuscleEntries = buildPerMuscleBreakdown(muscleLoad, muscleNames)
        val perGroupEntries = buildPerGroupBreakdown(muscleLoad, groups, groupByMuscle)

        return MuscleLoadSummary(
            perGroup = MuscleLoadBreakdown(perGroupEntries),
            perMuscle = MuscleLoadBreakdown(perMuscleEntries),
        )
    }

    private fun buildPerMuscleBreakdown(
        muscleLoad: Map<MuscleEnum, Float>,
        muscleNames: Map<MuscleEnum, String>,
    ): List<MuscleLoadEntry> {
        if (muscleLoad.isEmpty()) return emptyList()
        val maxValue = muscleLoad.values.maxOrNull() ?: 0f
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
            totals[group.id] = ((totals[group.id]?.first ?: 0f) + value) to muscles
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

    private enum class Workload {
        Volume,
        Repetitions
    }

    private companion object Companion {
        private const val EPS = 1e-3f
    }
}
