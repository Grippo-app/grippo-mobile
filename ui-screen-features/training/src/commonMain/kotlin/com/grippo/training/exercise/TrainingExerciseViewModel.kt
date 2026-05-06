package com.grippo.training.exercise

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.metrics.volume.TrainingTotalUseCase
import com.grippo.data.features.api.training.ExerciseValidatorUseCase
import com.grippo.data.features.api.training.models.ExerciseArtifacts
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.metrics.volume.toState
import com.grippo.state.domain.training.toDomain
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.uuid.Uuid

internal class TrainingExerciseViewModel(
    exercise: ExerciseState,
    exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingTotalUseCase: TrainingTotalUseCase,
    private val dialogController: DialogController,
    private val weightHistoryFeature: WeightHistoryFeature,
    private val exerciseValidatorUseCase: ExerciseValidatorUseCase,
) : BaseViewModel<TrainingExerciseState, TrainingExerciseDirection, TrainingExerciseLoader>(
    TrainingExerciseState(exercise = exercise)
), TrainingExerciseContract {

    init {
        exerciseExampleFeature
            .observeExerciseExample(exercise.exerciseExample.id)
            .onEach(::provideExerciseExample)
            .safeLaunch()

        state
            .map { current ->
                current.exercise.copy(
                    iterations = current.exercise.iterations
                        .filterNot { it.isPending }
                        .toPersistentList()
                )
            }
            .map { it.toDomain() }
            .filterNotNull()
            .map(exerciseValidatorUseCase::execute)
            .onEach(::provideValidation)
            .safeLaunch()

        state.value.exercise.iterations
            .firstOrNull { it.isPending }
            ?.let { onEditVolume(it.id) }
    }

    private fun provideValidation(value: ExerciseArtifacts?) {
        update { current ->
            val volumeIds = mutableSetOf<String>()
            val repetitionIds = mutableSetOf<String>()
            val completed = current.exercise.iterations.filterNot { it.isPending }

            value?.iterations?.forEachIndexed { index, (_, artifact) ->
                val iteration = completed.getOrNull(index) ?: return@forEachIndexed

                when (artifact) {
                    is ExerciseArtifacts.Artifact.SuspiciousWeight,
                    is ExerciseArtifacts.Artifact.SuspiciousVolume -> volumeIds += iteration.id
                    is ExerciseArtifacts.Artifact.SuspiciousRepetitions -> repetitionIds += iteration.id
                    null -> Unit
                }
            }

            current.copy(
                volumeArtifactIds = volumeIds.toPersistentSet(),
                repetitionArtifactIds = repetitionIds.toPersistentSet(),
            )
        }
    }

    private fun provideExerciseExample(value: ExerciseExample?) {
        val example = value?.toState() ?: return
        update { it.copy(exerciseExample = example) }
    }

    override fun onAddIteration() {
        safeLaunch {
            val example = state.value.exerciseExample ?: return@safeLaunch
            val initial = buildBlankIteration(example)

            showIterationDialog(
                initial = initial,
                example = example,
                number = state.value.exercise.iterations.size + 1,
                focus = IterationFocusState.UNIDENTIFIED,
                onResult = ::addIteration,
            )
        }
    }

    override fun onEditVolume(id: String) {
        editIteration(id, IterationFocusState.VOLUME)
    }

    override fun onEditRepetition(id: String) {
        editIteration(id, IterationFocusState.REPETITIONS)
    }

    override fun onDeleteIteration(id: String) {
        mutateIterations { iterations -> iterations.filter { it.id != id } }
    }

    override fun onExampleClick() {
        val dialog = DialogConfig.ExerciseExample(
            id = state.value.exercise.exerciseExample.id,
        )

        dialogController.show(dialog)
    }

    override fun onSave() {
        navigateTo(TrainingExerciseDirection.Save(exercise = state.value.exercise))
    }

    override fun onBack() {
        navigateTo(TrainingExerciseDirection.Back)
    }

    private fun editIteration(id: String, focus: IterationFocusState) {
        val example = state.value.exerciseExample ?: return
        val iterations = state.value.exercise.iterations
        val index = iterations.indexOfFirst { it.id == id }
        if (index < 0) return

        showIterationDialog(
            initial = iterations[index],
            example = example,
            number = index + 1,
            focus = focus,
            onResult = { iteration -> replaceIteration(id = id, value = iteration) },
        )
    }

    private fun showIterationDialog(
        initial: IterationState,
        example: ExerciseExampleState,
        number: Int,
        focus: IterationFocusState,
        onResult: (IterationState) -> Unit,
    ) {
        val dialog = DialogConfig.Iteration(
            initial = initial,
            number = number,
            suggestions = suggestedIterations(),
            focus = focus,
            example = example,
            onResult = onResult,
        )
        dialogController.show(dialog)
    }

    private suspend fun buildBlankIteration(example: ExerciseExampleState): IterationState {
        val components = example.components

        val bodyWeight = when (components) {
            is ExerciseExampleComponentsState.BodyAndAssist,
            is ExerciseExampleComponentsState.BodyAndExtra,
            is ExerciseExampleComponentsState.BodyOnly -> weightHistoryFeature
                .observeLastWeight()
                .firstOrNull()
                ?.weight

            is ExerciseExampleComponentsState.External -> null
        }

        val bodyMultiplier = when (components) {
            is ExerciseExampleComponentsState.BodyAndAssist -> components.bodyMultiplier
            is ExerciseExampleComponentsState.BodyAndExtra -> components.bodyMultiplier
            is ExerciseExampleComponentsState.BodyOnly -> components.bodyMultiplier
            is ExerciseExampleComponentsState.External -> null
        }

        return IterationState(
            id = Uuid.random().toString(),
            externalWeight = VolumeFormatState.Empty(),
            extraWeight = VolumeFormatState.Empty(),
            assistWeight = VolumeFormatState.Empty(),
            bodyMultiplier = MultiplierFormatState.of(bodyMultiplier),
            bodyWeight = WeightFormatState.of(bodyWeight),
            repetitions = RepetitionsFormatState.Empty(),
        )
    }

    private fun suggestedIterations(): List<IterationState> {
        return state.value.exercise.iterations
            .reversed()
            .filterNot { it.isPending }
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

    private fun addIteration(value: IterationState) {
        mutateIterations { it + value }
    }

    private fun replaceIteration(id: String, value: IterationState) {
        mutateIterations { iterations ->
            iterations.map { if (it.id == id) value else it }
        }
    }

    private fun mutateIterations(transform: (List<IterationState>) -> List<IterationState>) {
        val currentExercise = state.value.exercise
        val iterations = transform(currentExercise.iterations).toPersistentList()

        val updatedExercise = currentExercise.copy(
            iterations = iterations,
            total = recalculateTotal(iterations),
        )

        update { it.copy(exercise = updatedExercise) }
        navigateTo(TrainingExerciseDirection.Update(updatedExercise))
    }

    private fun recalculateTotal(iterations: List<IterationState>): TrainingTotalState {
        val completed = iterations.filterNot { it.isPending }
        val domainIterations = completed.toDomain()
        return trainingTotalUseCase.fromSetIterations(domainIterations).toState()
    }
}
