package com.grippo.debug

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.UserExerciseExampleRules
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.logger.AppLogger
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.first
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

public class DebugViewModel(
    private val trainingFeature: TrainingFeature,
    private val exampleFeature: ExerciseExampleFeature,
) : BaseViewModel<DebugState, DebugDirection, DebugLoader>(DebugState()),
    DebugContract {

    init {
        loadLogs()
    }

    override fun onBack() {
        navigateTo(DebugDirection.Back)
    }

    override fun clearLogs() {
        safeLaunch {
            AppLogger.clearLogFile()
            loadLogs()
        }
    }

    override fun generateTraining() {
        safeLaunch(loader = DebugLoader.GenerateTraining) {
            val random = Random.Default

            val queries = ExampleQueries(
                name = null,
                forceType = null,
                weightType = null,
                category = null,
                muscleGroupId = null,
            )
            val page = ExamplePage(limits = 100, number = 1)

            val experiences = ExperienceEnum.entries.toMutableList().apply { shuffle(random) }

            val exerciseExamples = experiences.firstNotNullOfOrNull { experience ->
                val rules = UserExerciseExampleRules(
                    excludedEquipmentIds = emptySet(),
                    excludedMuscleIds = emptySet(),
                    experience = experience,
                )

                exampleFeature
                    .observeExerciseExamples(
                        queries = queries,
                        sorting = ExampleSortingEnum.RecentlyUsed,
                        rules = rules,
                        page = page,
                        experience = experience
                    )
                    .first()
                    .takeIf { it.isNotEmpty() }
            } ?: return@safeLaunch

            val exercisesCount = random.nextInt(2, 9)
            val exercises = buildList {
                repeat(exercisesCount) {
                    val example = exerciseExamples.random(random).value
                    val iterationsCount = random.nextInt(4, 9)
                    val iterations = List(iterationsCount) {
                        val repetitions = random.nextInt(4, 13)
                        val weight = random.nextInt(40, 121).toFloat()
                        SetIteration(
                            volume = weight,
                            repetitions = repetitions
                        )
                    }

                    val totalVolume = iterations.fold(0f) { acc, iteration ->
                        acc + iteration.volume * iteration.repetitions
                    }
                    val totalRepetitions = iterations.sumOf { it.repetitions }
                    val repetitionsWithWeight = iterations.sumOf { iteration ->
                        if (iteration.volume > 0f) iteration.repetitions else 0
                    }
                    val intensity = if (repetitionsWithWeight > 0) {
                        totalVolume / repetitionsWithWeight
                    } else {
                        0f
                    }

                    add(
                        SetExercise(
                            name = example.name,
                            iterations = iterations,
                            exerciseExample = example,
                            repetitions = totalRepetitions,
                            intensity = intensity,
                            volume = totalVolume,
                            createdAt = DateTimeUtils.now()
                        )
                    )
                }
            }

            val totalVolume = exercises.fold(0f) { acc, exercise -> acc + exercise.volume }
            val totalRepetitions = exercises.sumOf { it.repetitions }
            val totalRepetitionsWithWeight = exercises.sumOf { exercise ->
                exercise.iterations.sumOf { iteration ->
                    if (iteration.volume > 0f) iteration.repetitions else 0
                }
            }
            val trainingIntensity = if (totalRepetitionsWithWeight > 0) {
                totalVolume / totalRepetitionsWithWeight
            } else {
                0f
            }

            val durationMinutes = random.nextInt(25, 91).minutes

            val training = SetTraining(
                exercises = exercises,
                duration = durationMinutes,
                repetitions = totalRepetitions,
                intensity = trainingIntensity,
                volume = totalVolume
            )

            trainingFeature.setTraining(training).getOrThrow()
        }
    }

    override fun onSelect(value: DebugMenu) {
        update { it.copy(selected = value) }
        if (value == DebugMenu.Logger) {
            loadLogs()
        }
    }

    override fun onSelectLogCategory(value: String) {
        update { state ->
            val logger = state.logger
            if (logger.selectedCategory == value) state
            else if (!logger.logsByCategory.containsKey(value)) state
            else state.copy(logger = logger.copy(selectedCategory = value))
        }
    }

    private fun loadLogs() {
        safeLaunch(loader = DebugLoader.Logs) {

            val logsByCategory = AppLogger.logFileContentsByCategory()

            update { state ->
                val categories = logsByCategory.keys.toPersistentList()
                val persistentLogs = logsByCategory
                    .mapValues { (_, messages) -> messages.asReversed().toPersistentList() }
                    .toPersistentMap()

                val selectedCategory = state.logger.selectedCategory
                    ?.takeIf { persistentLogs.containsKey(it) }
                    ?: categories.firstOrNull()

                state.copy(
                    logger = state.logger.copy(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        logsByCategory = persistentLogs
                    )
                )
            }
        }
    }
}
