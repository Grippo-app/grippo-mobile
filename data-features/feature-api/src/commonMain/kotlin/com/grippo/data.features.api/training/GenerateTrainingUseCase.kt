package com.grippo.data.features.api.training

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.UserExerciseExampleRules
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.data.features.api.training.models.SetTraining
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public class GenerateTrainingUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
) {

    public suspend fun execute(date: LocalDateTime): SetTraining? {
        val random: Random = Random.Default
        // Ensure we have a fresh cache of examples before sampling
        exerciseExampleFeature.getExerciseExamples().getOrThrow()

        val examples = loadExamples(random) ?: return null

        val exercisesCount = random.nextInt(MIN_EXERCISES, MAX_EXERCISES + 1)
        val exercises = buildList {
            repeat(exercisesCount) {
                val example = examples.random(random)
                add(generateExercise(random, example, date))
            }
        }

        val trainingTotals = aggregateTraining(exercises)
        val duration: Duration =
            random.nextInt(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES + 1).minutes

        return SetTraining(
            exercises = exercises,
            duration = duration,
            repetitions = trainingTotals.totalRepetitions,
            intensity = trainingTotals.intensity,
            volume = trainingTotals.totalVolume
        )
    }

    private suspend fun loadExamples(random: Random): List<ExerciseExample>? {
        val queries = ExampleQueries(
            name = null,
            muscleGroupId = null,
        )

        val page = ExamplePage(limits = 100, number = 1)

        val experiences = ExperienceEnum.entries.toMutableList().apply { shuffle(random) }

        return experiences.firstNotNullOfOrNull { experience ->
            val rules = UserExerciseExampleRules(
                excludedEquipmentIds = emptySet(),
                excludedMuscleIds = emptySet(),
                experience = experience
            )

            exerciseExampleFeature
                .observeExerciseExamples(
                    queries = queries,
                    sorting = ExampleSortingEnum.RecentlyUsed,
                    rules = rules,
                    page = page,
                    experience = experience
                )
                .first()
                .takeIf { it.isNotEmpty() }
        }
    }

    private fun generateExercise(
        random: Random,
        example: ExerciseExample,
        createdAt: LocalDateTime
    ): SetExercise {
        var iterations = createIterations(random, MIN_WEIGHT, MAX_WEIGHT)

        repeat(MAX_ITERATION_ATTEMPTS) {
            val stats = buildIterationStats(iterations)
            if (stats.intensity <= MAX_ALLOWED_INTENSITY) {
                return stats.toSetExercise(example, createdAt)
            }
            iterations = createIterations(random, MIN_WEIGHT, MAX_WEIGHT - 10)
        }

        val currentStats = buildIterationStats(iterations)
        val baseIntensity = currentStats.intensity.takeIf { it > 0f } ?: MAX_ALLOWED_INTENSITY
        val factor = (MAX_ALLOWED_INTENSITY / baseIntensity).coerceIn(0.1f, 1f)

        val cooledIterations = iterations.map { iteration ->
            val safeWeight = (iteration.volume * factor).coerceAtLeast(MIN_WEIGHT.toFloat())
            iteration.copy(
                externalWeight = safeWeight.toSingleDecimal()
            )
        }

        return buildIterationStats(cooledIterations).toSetExercise(example, createdAt)
    }

    private fun createIterations(
        random: Random,
        minWeight: Int,
        maxWeight: Int
    ): List<SetIteration> {
        val count = random.nextInt(MIN_ITERATIONS, MAX_ITERATIONS + 1)
        return List(count) {
            val repetitions = random.nextInt(MIN_REPETITIONS, MAX_REPETITIONS + 1)
            val weightUnits = random.nextInt(minWeight * 2, maxWeight * 2 + 1)
            val weight = (weightUnits / 2f).toSingleDecimal()
            SetIteration(
                externalWeight = weight,
                repetitions = repetitions,
                assistWeight = null,
                bodyWeight = null,
                extraWeight = null,
                bodyMultiplier = null,
            )
        }
    }

    private fun buildIterationStats(iterations: List<SetIteration>): ExerciseStats {
        var volume = 0f
        var totalRepetitions = 0
        var weightedRepetitions = 0

        iterations.forEach { iteration ->
            volume += iteration.volume * iteration.repetitions
            totalRepetitions += iteration.repetitions
            if (iteration.volume > 0f) {
                weightedRepetitions += iteration.repetitions
            }
        }

        val intensityRaw = if (weightedRepetitions > 0) {
            volume / weightedRepetitions
        } else {
            0f
        }

        val intensity = intensityRaw.toSingleDecimal()

        val roundedVolume = volume.toSingleDecimal(min = MIN_VOLUME, max = MAX_VOLUME)

        return ExerciseStats(
            iterations = iterations,
            totalVolume = roundedVolume,
            totalRepetitions = totalRepetitions,
            intensity = intensity
        )
    }

    private fun aggregateTraining(exercises: List<SetExercise>): ExerciseStats {
        var volume = 0f
        var totalRepetitions = 0
        var weightedRepetitions = 0

        exercises.forEach { exercise ->
            volume += exercise.volume
            totalRepetitions += exercise.repetitions
            exercise.iterations.forEach { iteration ->
                if (iteration.volume > 0f) {
                    weightedRepetitions += iteration.repetitions
                }
            }
        }

        val intensityRaw = if (weightedRepetitions > 0) {
            volume / weightedRepetitions
        } else {
            0f
        }

        val intensity = intensityRaw.toSingleDecimal()

        val roundedVolume = volume.toSingleDecimal(min = MIN_VOLUME, max = MAX_VOLUME)

        return ExerciseStats(
            iterations = emptyList(),
            totalVolume = roundedVolume,
            totalRepetitions = totalRepetitions,
            intensity = intensity
        )
    }

    private fun ExerciseStats.toSetExercise(
        example: ExerciseExample,
        createdAt: LocalDateTime
    ): SetExercise {
        val value = example.value
        return SetExercise(
            name = value.name,
            iterations = iterations,
            exerciseExample = value,
            repetitions = totalRepetitions,
            intensity = intensity,
            volume = totalVolume,
            createdAt = createdAt
        )
    }

    private data class ExerciseStats(
        val iterations: List<SetIteration>,
        val totalVolume: Float,
        val totalRepetitions: Int,
        val intensity: Float,
    )

    private companion object {
        private const val MIN_EXERCISES = 2
        private const val MAX_EXERCISES = 8
        private const val MIN_ITERATIONS = 4
        private const val MAX_ITERATIONS = 8
        private const val MIN_REPETITIONS = 4
        private const val MAX_REPETITIONS = 8
        private const val MIN_WEIGHT = 40
        private const val MAX_WEIGHT = 120
        private const val MIN_DURATION_MINUTES = 25
        private const val MAX_DURATION_MINUTES = 90
        private const val MIN_VOLUME = 0f
        private const val MAX_VOLUME = 20000f
        private const val MAX_ALLOWED_INTENSITY = 100f
        private const val MAX_ITERATION_ATTEMPTS = 5
    }

    private fun Float.toSingleDecimal(min: Float? = null, max: Float? = null): Float {
        val clamped = when {
            min != null && max != null -> this.coerceIn(min, max)
            min != null -> this.coerceAtLeast(min)
            max != null -> this.coerceAtMost(max)
            else -> this
        }
        return (clamped * 10f).roundToInt() / 10f
    }
}
