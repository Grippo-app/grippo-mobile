package com.grippo.training.exercise

import com.grippo.analytics.training.TrainingAnalytics
import com.grippo.analytics.training.models.TrainingEvent
import com.grippo.analytics.training.models.TrainingScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import com.grippo.core.foundation.models.ResultKeys
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
import com.grippo.data.features.api.training.models.ExerciseArtifacts.Field
import com.grippo.data.features.api.training.models.ExerciseValidationContext
import com.grippo.data.features.api.training.models.ExerciseValidationInput
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.change_btn
import com.grippo.design.resources.provider.exercise_details_btn
import com.grippo.design.resources.provider.set_value
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.dialog.api.DialogController.Session
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.training.toState
import com.grippo.screen.api.ExercisesRouter
import com.grippo.state.domain.training.toDomain
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class TrainingExerciseViewModel(
    exercise: ExerciseState,
    private val analytics: TrainingAnalytics,
    exerciseExampleFeature: ExerciseExampleFeature,
    private val dialogController: DialogController,
    private val weightHistoryFeature: WeightHistoryFeature,
    private val exerciseValidatorUseCase: ExerciseValidatorUseCase,
    initialState: TrainingExerciseState?,
) : BaseViewModel<TrainingExerciseState, TrainingExerciseDirection, TrainingExerciseLoader>(
    initialState ?: TrainingExerciseState(exercise = exercise)
), TrainingExerciseContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(TrainingScreen.Exercise, reason)
    }

    companion object {
        fun libraryResultKey(exerciseId: String): ResultKey<Result<ExerciseExampleState>> =
            ResultKeys.create("exercise-library:$exerciseId")
    }

    init {
        state
            .map { it.exercise.exerciseExample.id }
            .distinctUntilChanged()
            .flatMapLatest(exerciseExampleFeature::observeExerciseExample)
            .onEach(::provideExerciseExample)
            .safeLaunch()

        val validationContexts = state
            .map { TrainingExerciseValidationTarget(it.exercise.exerciseExample.id, it.exercise.createdAt.value) }
            .distinctUntilChanged()
            .onEach { clearValidation() }
            .flatMapLatest { target ->
                val date = target.performedAt ?: return@flatMapLatest emptyFlow<TrainingExerciseValidationContext>()
                exerciseValidatorUseCase.observeContext(
                    exampleId = target.exampleId,
                    performedAt = date,
                    excludedExerciseId = exercise.id,
                ).map { TrainingExerciseValidationContext(target, it) }
            }

        combine(
            state.map { it.exercise }.distinctUntilChanged(),
            validationContexts,
        ) { source, snapshot ->
            val target = TrainingExerciseValidationTarget(source.exerciseExample.id, source.createdAt.value)
            if (snapshot.target != target) return@combine null
            validateTrainingExercise(source, snapshot.value)
        }
            .filterNotNull()
            .onEach(::provideValidation)
            .safeLaunch(onError = ::clearValidation)
    }

    private fun clearValidation() {
        update { current ->
            current.copy(
                previousIterations = persistentListOf(),
                volumeArtifactIds = persistentSetOf(),
                repetitionArtifactIds = persistentSetOf(),
            )
        }
    }

    private fun provideValidation(value: TrainingExerciseValidation) {
        update { current ->
            if (current.exercise != value.source) return@update current

            current.copy(
                previousIterations = value.previousIterations,
                volumeArtifactIds = value.volumeIds,
                repetitionArtifactIds = value.repetitionIds,
            )
        }
    }

    private fun provideExerciseExample(value: ExerciseExample?) {
        val example = value?.toState() ?: return
        update { it.copy(exerciseExample = example) }
    }

    override fun onAddIteration() {
        val example = state.value.exerciseExample ?: return
        analytics.trackEvent(TrainingEvent.Exercise.AddIteration)
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
        analytics.trackEvent(TrainingEvent.Exercise.DeleteIteration)
        mutateIterations { iterations -> iterations.filter { it.id != id } }
    }

    override fun onStartReorderIterations(fromId: String, toId: String) {
        if (fromId == toId) return
        val iterations = state.value.exercise.iterations
        val from = iterations.indexOfFirst { it.id == fromId }
        val to = iterations.indexOfFirst { it.id == toId }
        if (from < 0 || to < 0) return

        analytics.trackEvent(TrainingEvent.Exercise.ReorderIterations)
        val reordered = iterations.toMutableList().apply { add(to, removeAt(from)) }
        update { it.copy(exercise = it.exercise.copy(iterations = reordered.toPersistentList())) }
    }

    override fun onEndReorderIterations() {
        analytics.trackEvent(TrainingEvent.Exercise.ReorderFinished)
        updateExercise(state.value.exercise)
    }

    override fun onExampleClick() {
        analytics.trackEvent(TrainingEvent.Exercise.ExampleOpened)
        val exampleId = state.value.exercise.exerciseExample.id
        val title = UiText.Res(Res.string.change_btn)
        val dialog = DialogConfig.ExerciseExampleDetails(
            title = UiText.Res(Res.string.exercise_details_btn),
            id = exampleId,
            mode = DialogConfig.ExerciseExampleDetails.Mode.Action(
                title = title,
                onClick = { navigation -> showExerciseLibrary(navigation, exampleId) },
            ),
        )
        dialogController.open(dialog)
    }

    override fun onSave() {
        analytics.trackEvent(TrainingEvent.Exercise.Save)
        navigateTo(TrainingExerciseDirection.Save(exercise = state.value.exercise))
    }

    override fun onBack() {
        analytics.trackEvent(TrainingEvent.Exercise.DismissRequested)
        navigateTo(TrainingExerciseDirection.Back)
    }

    private fun showExerciseLibrary(navigation: Session, exampleId: String) {
        analytics.trackEvent(TrainingEvent.Exercise.ReplaceExample)
        navigation.close()
        navigateTo(TrainingExerciseDirection.ToLibrary(
            ExercisesRouter.Library(
                mode = ExercisesRouter.Mode.SimilarTo(exampleId),
                resultKey = libraryResultKey(state.value.exercise.id),
            )
        ))
    }

    fun swapExerciseExample(value: ExerciseExampleState) {
        val current = state.value.exercise
        if (current.exerciseExample.id == value.value.id) return

        analytics.trackEvent(TrainingEvent.Exercise.ExampleSelected)
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

        when (focus) {
            IterationFocusState.VOLUME -> analytics.trackEvent(TrainingEvent.Exercise.EditVolume)
            IterationFocusState.REPETITIONS -> analytics.trackEvent(TrainingEvent.Exercise.EditRepetitions)
            IterationFocusState.UNIDENTIFIED -> Unit
        }
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
            previousIteration = state.value.previousIterations.getOrNull(number - 1),
            focus = focus,
            example = example,
            onResult = { iteration ->
                analytics.trackEvent(TrainingEvent.Exercise.IterationConfirmed)
                onResult(iteration)
            },
        )

        analytics.trackEvent(TrainingEvent.Exercise.IterationOpened)
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
        mutateIterations { iterations -> iterations.map { if (it.id == id) value else it } }
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

    private fun validateTrainingExercise(
        source: ExerciseState,
        context: ExerciseValidationContext,
    ): TrainingExerciseValidation {
        // The domain mapper drops rows without repetitions. Filter them before capturing IDs,
        // so findings always address the exact rows sent to the validator.
        val completed = source.iterations.filter { !it.isPending && it.repetitions.value != null }
        val input = ExerciseValidationInput(
            name = source.name,
            exampleId = source.exerciseExample.id,
            iterations = completed.toDomain(),
            performedAt = source.createdAt.value,
        )
        val artifacts = exerciseValidatorUseCase.execute(input, context)
        val volumeIds = mutableSetOf<String>()
        val repetitionIds = mutableSetOf<String>()
        artifacts?.iterations?.forEachIndexed { index, row ->
            val id = completed[index].id
            for (artifact in row.artifacts) {
                if (artifact.field == Field.Repetitions) repetitionIds += id else volumeIds += id
            }
        }
        val previousIterations = context.history
            .filter { it.exerciseExample.id == source.exerciseExample.id }
            .filter { it.id != source.id && it.id != context.excludedExerciseId }
            .filter { previous -> source.createdAt.value?.let { previous.createdAt < it } == true }
            .filter { it.iterations.isNotEmpty() }
            .maxByOrNull { it.createdAt }
            ?.iterations?.map { it.toState() }?.toPersistentList() ?: persistentListOf()
        return TrainingExerciseValidation(source, volumeIds.toPersistentSet(), repetitionIds.toPersistentSet(), previousIterations)
    }
}
