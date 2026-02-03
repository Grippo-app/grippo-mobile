package com.grippo.training.exercise

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.metrics.TrainingTotalState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.metrics.TrainingTotalUseCase
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.metrics.toState
import com.grippo.state.domain.training.toDomain
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlin.uuid.Uuid
import com.grippo.training.exercise.ExerciseState as ScreenExerciseState

internal class ExerciseViewModel(
    exercise: ExerciseState,
    exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingTotalUseCase: TrainingTotalUseCase,
    private val dialogController: DialogController,
    private val weightHistoryFeature: WeightHistoryFeature,
) : BaseViewModel<ScreenExerciseState, ExerciseDirection, ExerciseLoader>(
    ScreenExerciseState(
        exercise = exercise
    )
), ExerciseContract {

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
        safeLaunch {
            val example = state.value.exerciseExample ?: return@safeLaunch

            val weight = when (example.components) {
                is ExerciseExampleComponentsState.BodyAndAssist,
                is ExerciseExampleComponentsState.BodyAndExtra,
                is ExerciseExampleComponentsState.BodyOnly -> weightHistoryFeature
                    .observeLastWeight()
                    .firstOrNull()

                is ExerciseExampleComponentsState.External -> null
            }

            val multiplier = when (val c = example.components) {
                is ExerciseExampleComponentsState.BodyAndAssist -> c.bodyMultiplier
                is ExerciseExampleComponentsState.BodyAndExtra -> c.bodyMultiplier
                is ExerciseExampleComponentsState.BodyOnly -> c.bodyMultiplier
                is ExerciseExampleComponentsState.External -> null
            }

            val value = IterationState(
                id = Uuid.random().toString(),
                externalWeight = VolumeFormatState.Empty(),
                extraWeight = VolumeFormatState.Empty(),
                assistWeight = VolumeFormatState.Empty(),
                bodyMultiplier = MultiplierFormatState.of(multiplier),
                bodyWeight = WeightFormatState.of(weight?.weight),
                repetitions = RepetitionsFormatState.Empty(),
            )

            val number = state.value.exercise.iterations.count() + 1

            val suggestions = state.value.exercise.iterations
                .reversed()
                .distinctBy { it.externalWeight.value to it.repetitions.value }

            val dialog = DialogConfig.Iteration(
                initial = value,
                number = number,
                suggestions = suggestions,
                focus = IterationFocusState.UNIDENTIFIED,
                example = example,
                onResult = { iteration ->
                    update { s ->
                        // Build new iterations
                        val iterations = s.exercise.iterations
                            .toMutableList()
                            .apply { add(iteration) }
                            .toPersistentList()

                        // Recompute metrics for this exercise
                        val total = recalculateTotal(iterations)

                        val exercise = s.exercise.copy(
                            iterations = iterations,
                            total = total
                        )
                        s.copy(exercise = exercise)
                    }
                }
            )

            dialogController.show(dialog)
        }
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
            val metrics = recalculateTotal(iterations)

            val exercise = s.exercise.copy(
                iterations = iterations,
                total = metrics
            )
            s.copy(exercise = exercise)
        }
    }

    override fun onEditVolume(id: String) {
        val value = state.value.exercise.iterations.find { it.id == id } ?: return

        val number = state.value.exercise.iterations.indexOfFirst { it.id == id } + 1

        val suggestions = state.value.exercise.iterations
            .reversed()
            .distinctBy { it.externalWeight.value to it.repetitions.value }

        val example = state.value.exerciseExample ?: return

        val dialog = DialogConfig.Iteration(
            initial = value,
            suggestions = suggestions,
            number = number,
            focus = IterationFocusState.VOLUME,
            example = example,
            onResult = { iteration ->
                update { s ->
                    // Replace updated iteration
                    val iterations = s.exercise.iterations
                        .toMutableList()
                        .map { m -> if (m.id == id) iteration else m }
                        .toPersistentList()

                    // Recompute metrics after edit
                    val metrics = recalculateTotal(iterations)

                    val exercise = s.exercise.copy(
                        iterations = iterations,
                        total = metrics
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
            .distinctBy { it.externalWeight.value to it.repetitions.value }

        val example = state.value.exerciseExample ?: return

        val dialog = DialogConfig.Iteration(
            initial = value,
            suggestions = suggestions,
            number = number,
            example = example,
            focus = IterationFocusState.REPETITIONS,
            onResult = { iteration ->
                update { s ->
                    // Replace updated iteration
                    val iterations = s.exercise.iterations
                        .toMutableList()
                        .map { m -> if (m.id == id) iteration else m }
                        .toPersistentList()

                    // Recompute metrics after edit
                    val metrics = recalculateTotal(iterations)

                    val exercise = s.exercise.copy(
                        iterations = iterations,
                        total = metrics
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
            val metrics = recalculateTotal(iterations)

            val exercise = s.exercise.copy(
                iterations = iterations,
                total = metrics
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

    private fun recalculateTotal(
        iterations: List<IterationState>,
    ): TrainingTotalState {
        val domainIterations = iterations.toDomain()
        val domainMetrics = trainingTotalUseCase.fromSetIterations(domainIterations)
        return domainMetrics.toState()
    }
}
