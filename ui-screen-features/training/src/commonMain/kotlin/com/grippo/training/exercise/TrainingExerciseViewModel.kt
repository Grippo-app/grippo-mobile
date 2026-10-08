package com.grippo.training.exercise

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.training.ExerciseValidatorUseCase
import com.grippo.data.features.api.training.models.ExerciseArtifacts
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.change_btn
import com.grippo.design.resources.provider.exercise_details_btn
import com.grippo.design.resources.provider.exercise_example_picker_title_replace
import com.grippo.design.resources.provider.set_value
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.dialog.api.DialogController.Session
import com.grippo.domain.state.exercise.example.toState
import com.grippo.state.domain.training.toDomain
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class TrainingExerciseViewModel(
    exercise: ExerciseState,
    exerciseExampleFeature: ExerciseExampleFeature,
    private val dialogController: DialogController,
    private val weightHistoryFeature: WeightHistoryFeature,
    private val exerciseValidatorUseCase: ExerciseValidatorUseCase,
) : BaseViewModel<TrainingExerciseState, TrainingExerciseDirection, TrainingExerciseLoader>(
    TrainingExerciseState(exercise = exercise)
), TrainingExerciseContract {

    init {
        state
            .map { it.exercise.exerciseExample.id }
            .distinctUntilChanged()
            .flatMapLatest(exerciseExampleFeature::observeExerciseExample)
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
        val example = state.value.exerciseExample ?: return
        safeLaunch {
            val initial = buildBlankIteration(example)
                .withResolvedBodyWeight(example)

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

    override fun onStartReorderIterations(fromId: String, toId: String) {
        if (fromId == toId) return
        val iterations = state.value.exercise.iterations
        val from = iterations.indexOfFirst { it.id == fromId }
        val to = iterations.indexOfFirst { it.id == toId }
        if (from < 0 || to < 0) return

        val reordered = iterations.toMutableList().apply { add(to, removeAt(from)) }
        update {
            it.copy(exercise = it.exercise.copy(iterations = reordered.toPersistentList()))
        }
    }

    override fun onEndReorderIterations() {
        updateExercise(state.value.exercise)
    }

    override fun onExampleClick() {
        val exampleId = state.value.exercise.exerciseExample.id
        val title = UiText.Res(Res.string.change_btn)
        val dialog = DialogConfig.ExerciseExample(
            title = UiText.Res(Res.string.exercise_details_btn),
            id = exampleId,
            mode = DialogConfig.ExerciseExample.Mode.Action(
                title = title,
                onClick = { navigation -> showExercisePicker(navigation, exampleId) },
            ),
        )
        dialogController.open(dialog)
    }

    override fun onSave() {
        navigateTo(TrainingExerciseDirection.Save(exercise = state.value.exercise))
    }

    override fun onBack() {
        navigateTo(TrainingExerciseDirection.Back)
    }

    private fun showExercisePicker(navigation: Session, exampleId: String) {
        val dialog = DialogConfig.ExerciseExamplePicker.SimilarTo(
            title = UiText.Res(Res.string.exercise_example_picker_title_replace),
            targetExerciseExampleId = exampleId,
            onResult = ::swapExerciseExample,
        )
        navigation.replaceCurrent(dialog)
    }

    private fun swapExerciseExample(value: ExerciseExampleState) {
        val current = state.value.exercise
        if (current.exerciseExample.id == value.value.id) return

        val updated = current.copy(
            name = value.value.name,
            exerciseExample = value.value,
        )

        updateExercise(updated)
    }

    private fun editIteration(id: String, focus: IterationFocusState) {
        val example = state.value.exerciseExample ?: return
        val iterations = state.value.exercise.iterations
        val index = iterations.indexOfFirst { it.id == id }
        if (index < 0) return

        safeLaunch {
            val initial = iterations[index]
                .withResolvedBodyWeight(example)

            showIterationDialog(
                initial = initial,
                example = example,
                number = index + 1,
                focus = focus,
                onResult = { iteration -> replaceIteration(id = id, value = iteration) },
            )
        }
    }

    private suspend fun IterationState.withResolvedBodyWeight(example: ExerciseExampleState): IterationState {
        if (bodyWeight.value != null) return this
        val needsBodyWeight = when (example.components) {
            is ExerciseExampleComponentsState.BodyAndAssist,
            is ExerciseExampleComponentsState.BodyAndExtra,
            is ExerciseExampleComponentsState.BodyOnly -> true

            is ExerciseExampleComponentsState.External -> false
        }
        if (!needsBodyWeight) return this
        val weight = weightHistoryFeature.observeLastWeight().firstOrNull()?.weight ?: return this

        return copy(bodyWeight = WeightFormatState.of(weight))
    }

    private fun showIterationDialog(
        initial: IterationState,
        example: ExerciseExampleState,
        number: Int,
        focus: IterationFocusState,
        onResult: (IterationState) -> Unit,
    ) {
        val dialog = DialogConfig.Iteration(
            title = UiText.Res(Res.string.set_value, persistentListOf(number)),
            initial = initial,
            number = number,
            suggestions = suggestedIterations(),
            focus = focus,
            example = example,
            onResult = onResult,
        )

        dialogController.open(dialog)
    }

    private fun buildBlankIteration(example: ExerciseExampleState): IterationState {
        val bodyMultiplier = when (val components = example.components) {
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
            bodyWeight = WeightFormatState.Empty(),
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
        val updatedExercise = currentExercise.copy(iterations = iterations)

        updateExercise(updatedExercise)
    }

    private fun updateExercise(exercise: ExerciseState) {
        update { it.copy(exercise = exercise) }

        val direction = if (exercise.iterations.isEmpty()) {
            TrainingExerciseDirection.Delete(exercise.id)
        } else {
            TrainingExerciseDirection.Update(exercise)
        }

        navigateTo(direction)
    }
}
