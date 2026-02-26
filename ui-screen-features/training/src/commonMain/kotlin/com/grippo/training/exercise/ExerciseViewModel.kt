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

            val suggestions = suggestedIterations()

            val dialog = DialogConfig.Iteration(
                initial = value,
                number = number,
                suggestions = suggestions,
                focus = IterationFocusState.UNIDENTIFIED,
                example = example,
                onResult = ::addIteration
            )

            dialogController.show(dialog)
        }
    }

    override fun onDeleteIteration(id: String) {
        removeIteration(id)
    }

    override fun onEditVolume(id: String) {
        val value = state.value.exercise.iterations.find { it.id == id } ?: return

        val number = state.value.exercise.iterations.indexOfFirst { it.id == id } + 1

        val suggestions = suggestedIterations()

        val example = state.value.exerciseExample ?: return

        val dialog = DialogConfig.Iteration(
            initial = value,
            suggestions = suggestions,
            number = number,
            focus = IterationFocusState.VOLUME,
            example = example,
            onResult = { iteration -> replaceIteration(id = id, value = iteration) }
        )

        dialogController.show(dialog)
    }

    override fun onEditRepetition(id: String) {
        val value = state.value.exercise.iterations.find { it.id == id } ?: return

        val number = state.value.exercise.iterations.indexOfFirst { it.id == id } + 1

        val suggestions = suggestedIterations()

        val example = state.value.exerciseExample ?: return

        val dialog = DialogConfig.Iteration(
            initial = value,
            suggestions = suggestions,
            number = number,
            example = example,
            focus = IterationFocusState.REPETITIONS,
            onResult = { iteration -> replaceIteration(id = id, value = iteration) }
        )

        dialogController.show(dialog)
    }

    override fun onRemoveIteration(id: String) {
        removeIteration(id)
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

    private fun suggestedIterations(): List<IterationState> {
        return state.value.exercise.iterations
            .reversed()
            .distinctBy {
                listOf(
                    it.externalWeight.value,
                    it.extraWeight.value,
                    it.assistWeight.value,
                    it.bodyWeight.value,
                    it.bodyMultiplier.value,
                    it.repetitions.value,
                )
            }
    }

    override fun onBack() {
        navigateTo(ExerciseDirection.Back)
    }

    private fun addIteration(iteration: IterationState) {
        val currentExercise = state.value.exercise

        val iterations = (currentExercise.iterations + iteration).toPersistentList()

        val updatedExercise = currentExercise.copy(
            iterations = iterations,
            total = recalculateTotal(iterations)
        )

        update { screenState -> screenState.copy(exercise = updatedExercise) }
        navigateTo(ExerciseDirection.Update(updatedExercise))
    }

    private fun replaceIteration(id: String, value: IterationState) {
        val currentExercise = state.value.exercise

        val iterations = currentExercise.iterations
            .map { iteration -> if (iteration.id == id) value else iteration }
            .toPersistentList()

        val updatedExercise = currentExercise.copy(
            iterations = iterations,
            total = recalculateTotal(iterations)
        )

        update { screenState -> screenState.copy(exercise = updatedExercise) }
        navigateTo(ExerciseDirection.Update(updatedExercise))
    }

    private fun removeIteration(id: String) {
        val currentExercise = state.value.exercise

        val iterations = currentExercise.iterations
            .filter { iteration -> iteration.id != id }
            .toPersistentList()

        val updatedExercise = currentExercise.copy(
            iterations = iterations,
            total = recalculateTotal(iterations)
        )

        update { screenState -> screenState.copy(exercise = updatedExercise) }
        navigateTo(ExerciseDirection.Update(updatedExercise))
    }

    private fun recalculateTotal(
        iterations: List<IterationState>,
    ): TrainingTotalState {
        val domainIterations = iterations.toDomain()
        val domainMetrics = trainingTotalUseCase.fromSetIterations(domainIterations)
        return domainMetrics.toState()
    }
}
