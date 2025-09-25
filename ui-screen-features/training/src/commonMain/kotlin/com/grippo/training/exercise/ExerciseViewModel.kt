package com.grippo.training.exercise

import com.grippo.calculation.AnalyticsApi
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.IterationFocus
import com.grippo.state.trainings.IterationState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach
import kotlin.uuid.Uuid
import com.grippo.training.exercise.ExerciseState as ScreenExerciseState

internal class ExerciseViewModel(
    exercise: ExerciseState,
    exerciseExampleFeature: ExerciseExampleFeature,
    stringProvider: StringProvider,
    colorProvider: ColorProvider,
    private val dialogController: DialogController,
) : BaseViewModel<ScreenExerciseState, ExerciseDirection, ExerciseLoader>(
    ScreenExerciseState(
        exercise = exercise,
    )
), ExerciseContract {

    private val analytics = AnalyticsApi(stringProvider, colorProvider)

    init {
        exerciseExampleFeature
            .observeExerciseExample(exercise.exerciseExample.id)
            .onEach(::provideExerciseExample)
            .safeLaunch()
    }

    private fun provideExerciseExample(value: ExerciseExample?) {
        val example = value?.toState() ?: return
        update { it.copy(exerciseExample = example) }
    }

    override fun onAddIteration() {
        val value = IterationState(
            id = Uuid.random().toString(),
            volume = VolumeFormatState.of(""),
            repetitions = RepetitionsFormatState.of("")
        )

        val number = state.value.exercise.iterations.count() + 1

        val suggestions = state.value.exercise.iterations
            .reversed()
            .distinctBy { it.volume.value to it.repetitions.value }

        val dialog = DialogConfig.Iteration(
            initial = value,
            number = number,
            suggestions = suggestions,
            focus = IterationFocus.UNIDENTIFIED,
            onResult = { iteration ->
                update { s ->
                    // Build new iterations
                    val iterations = s.exercise.iterations
                        .toMutableList()
                        .apply { add(iteration) }
                        .toPersistentList()

                    // Recompute metrics for this exercise
                    val metrics = analytics.metricsFromIterations(iterations)

                    val exercise = s.exercise.copy(
                        iterations = iterations,
                        metrics = metrics
                    )
                    s.copy(exercise = exercise)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onDeleteIteration(id: String) {
        update { s ->
            // Remove by index if present
            val iterations = s.exercise.iterations
                .toMutableList()
                .apply {
                    val idx = indexOfFirst { it.id == id }
                    if (idx >= 0) removeAt(idx)
                }
                .toPersistentList()

            // Recompute metrics after deletion
            val metrics = analytics.metricsFromIterations(iterations)

            val exercise = s.exercise.copy(
                iterations = iterations,
                metrics = metrics
            )
            s.copy(exercise = exercise)
        }
    }

    override fun onEditVolume(id: String) {
        val value = state.value.exercise.iterations.find { it.id == id } ?: return
        val number = state.value.exercise.iterations.indexOfFirst { it.id == id } + 1

        val suggestions = state.value.exercise.iterations
            .reversed()
            .distinctBy { it.volume.value to it.repetitions.value }

        val dialog = DialogConfig.Iteration(
            initial = value,
            suggestions = suggestions,
            number = number,
            focus = IterationFocus.VOLUME,
            onResult = { iteration ->
                update { s ->
                    // Replace updated iteration
                    val iterations = s.exercise.iterations
                        .toMutableList()
                        .map { m -> if (m.id == id) iteration else m }
                        .toPersistentList()

                    // Recompute metrics after edit
                    val metrics = analytics.metricsFromIterations(iterations)

                    val exercise = s.exercise.copy(
                        iterations = iterations,
                        metrics = metrics
                    )
                    s.copy(exercise = exercise)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onEditRepetition(id: String) {
        val value = state.value.exercise.iterations.find { it.id == id } ?: return
        val number = state.value.exercise.iterations.indexOfFirst { it.id == id } + 1

        val suggestions = state.value.exercise.iterations
            .reversed()
            .distinctBy { it.volume.value to it.repetitions.value }

        val dialog = DialogConfig.Iteration(
            initial = value,
            suggestions = suggestions,
            number = number,
            focus = IterationFocus.REPETITIONS,
            onResult = { iteration ->
                update { s ->
                    // Replace updated iteration
                    val iterations = s.exercise.iterations
                        .toMutableList()
                        .map { m -> if (m.id == id) iteration else m }
                        .toPersistentList()

                    // Recompute metrics after edit
                    val metrics = analytics.metricsFromIterations(iterations)

                    val exercise = s.exercise.copy(
                        iterations = iterations,
                        metrics = metrics
                    )
                    s.copy(exercise = exercise)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onRemoveIteration(id: String) {
        update { s ->
            // Filter out the iteration by id
            val iterations = s.exercise.iterations
                .filter { it.id != id }
                .toPersistentList()

            // Recompute metrics after removal
            val metrics = analytics.metricsFromIterations(iterations)

            val exercise = s.exercise.copy(
                iterations = iterations,
                metrics = metrics
            )
            s.copy(exercise = exercise)
        }
    }

    override fun onExampleClick() {
        val dialog = DialogConfig.ExerciseExample(
            id = state.value.exercise.exerciseExample.id,
        )

        dialogController.show(dialog)
    }

    override fun onSave() {
        val direction = ExerciseDirection.Save(
            exercise = state.value.exercise
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(ExerciseDirection.Back)
    }
}
