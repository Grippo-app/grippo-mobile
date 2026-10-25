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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    state = initialState ?: TrainingExerciseState(exercise = exercise),
), TrainingExerciseContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(TrainingScreen.Exercise, reason)
    }

    init {
        state
            .map { it.exercise.exerciseExample.id }
            .distinctUntilChanged()
            .flatMapLatest(exerciseExampleFeature::observeExerciseExample)
            .onEach(::provideExerciseExample)
            .safeLaunch()

        observeValidation(excludedExerciseId = exercise.id)
            .onEach(::provideValidation)
            .safeLaunch(onError = ::clearValidation)
    }

    private fun observeValidation(excludedExerciseId: String): Flow<TrainingExerciseValidation> {
        val validationContexts = state
            .map { current ->
                TrainingExerciseValidationTarget(
                    exampleId = current.exercise.exerciseExample.id,
                    performedAt = current.exercise.createdAt.value,
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { target ->
                val performedAt = target.performedAt
                    ?: return@flatMapLatest flowOf<TrainingExerciseValidationContext?>(null)

                exerciseValidatorUseCase.observeContext(
                    exampleId = target.exampleId,
                    performedAt = performedAt,
                    excludedExerciseId = excludedExerciseId,
                ).map { context ->
                    TrainingExerciseValidationContext(target = target, value = context)
                }
            }

        val exercises = state
            .map { it.exercise }
            .distinctUntilChanged()

        return combine(flow = exercises, flow2 = validationContexts) { source, snapshot ->
            val target = TrainingExerciseValidationTarget(
                exampleId = source.exerciseExample.id,
                performedAt = source.createdAt.value,
            )
            if (snapshot == null || snapshot.target != target) {
                TrainingExerciseValidation(source = source)
            } else {
                validateTrainingExercise(source = source, context = snapshot.value)
            }
        }
    }

    private fun clearValidation() {
        val previousIterations = persistentListOf<IterationState>()

        val volumeArtifactIds = persistentSetOf<String>()

        val repetitionArtifactIds = persistentSetOf<String>()

        update {
            it.copy(
                previousIterations = previousIterations,
                volumeArtifactIds = volumeArtifactIds,
                repetitionArtifactIds = repetitionArtifactIds,
            )
        }
    }

    private fun provideValidation(value: TrainingExerciseValidation) {
        update { current ->
            if (current.exercise != value.source) {
                return@update current
            }

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

    private fun showExerciseLibrary(navigation: Session, exampleId: String) {
        analytics.trackEvent(event = TrainingEvent.Exercise.ReplaceExample)
        navigation.close()

        val resultKey = libraryResultKey(state.value.exercise.id)

        val mode = ExercisesRouter.Mode.SimilarTo(
            targetExerciseExampleId = exampleId,
            resultKey = resultKey,
        )

        val route = ExercisesRouter.Library(mode = mode)

        val direction = TrainingExerciseDirection.ToLibrary(route = route)

        navigateTo(direction)
    }

    private fun editIteration(id: String, focus: IterationFocusState) {
        val example = state.value.exerciseExample ?: return
        val iterations = state.value.exercise.iterations
        val index = iterations.indexOfFirst { it.id == id }
        if (index < 0) {
            return
        }

        when (focus) {
            IterationFocusState.VOLUME -> analytics.trackEvent(event = TrainingEvent.Exercise.EditVolume)
            IterationFocusState.REPETITIONS -> analytics.trackEvent(event = TrainingEvent.Exercise.EditRepetitions)
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
        if (bodyWeight.value != null) {
            return this
        }

        val needsBodyWeight = when (example.components) {
            is ExerciseExampleComponentsState.BodyAndAssist,
            is ExerciseExampleComponentsState.BodyAndExtra,
            is ExerciseExampleComponentsState.BodyOnly -> true

            is ExerciseExampleComponentsState.External -> false
        }
        if (!needsBodyWeight) {
            return this
        }

        val weight = weightHistoryFeature.observeLastWeight().firstOrNull()?.weight ?: return this

        val resolvedBodyWeight = WeightFormatState.of(weight)

        return copy(bodyWeight = resolvedBodyWeight)
    }

    private fun showIterationDialog(
        initial: IterationState,
        example: ExerciseExampleState,
        number: Int,
        focus: IterationFocusState,
        onResult: (IterationState) -> Unit,
    ) {
        val titleArgs = persistentListOf(number)

        val title = UiText.Res(value = Res.string.set_value, formatArgs = titleArgs)

        val suggestions = suggestedIterations()

        val previousIteration = state.value.previousIterations.getOrNull(number - 1)

        val dialog = DialogConfig.Iteration(
            title = title,
            initial = initial,
            number = number,
            suggestions = suggestions,
            previousIteration = previousIteration,
            focus = focus,
            example = example,
            onResult = { iteration ->
                analytics.trackEvent(event = TrainingEvent.Exercise.IterationConfirmed)
                onResult(iteration)
            },
        )

        analytics.trackEvent(event = TrainingEvent.Exercise.IterationOpened)
        dialogController.open(dialog)
    }

    private fun buildBlankIteration(example: ExerciseExampleState): IterationState {
        val bodyMultiplier = when (val components = example.components) {
            is ExerciseExampleComponentsState.BodyAndAssist -> components.bodyMultiplier
            is ExerciseExampleComponentsState.BodyAndExtra -> components.bodyMultiplier
            is ExerciseExampleComponentsState.BodyOnly -> components.bodyMultiplier
            is ExerciseExampleComponentsState.External -> null
        }

        val id = Uuid.random().toString()

        val multiplier = MultiplierFormatState.of(bodyMultiplier)

        return IterationState(
            id = id,
            externalWeight = VolumeFormatState.Empty(),
            extraWeight = VolumeFormatState.Empty(),
            assistWeight = VolumeFormatState.Empty(),
            bodyMultiplier = multiplier,
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
        mutateIterations {
            it + value
        }
    }

    private fun replaceIteration(id: String, value: IterationState) {
        mutateIterations { iterations ->
            iterations.map { iteration ->
                if (iteration.id == id) {
                    value
                } else {
                    iteration
                }
            }
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
            TrainingExerciseDirection.Delete(id = exercise.id)
        } else {
            TrainingExerciseDirection.Update(exercise = exercise)
        }

        navigateTo(direction)
    }

    private fun validateTrainingExercise(
        source: ExerciseState,
        context: ExerciseValidationContext,
    ): TrainingExerciseValidation {
        // Mirror the mapper's drop of rep-less rows so artifact indices address the same rows.
        val completed = source.iterations.filter { !it.isPending && it.repetitions.value != null }

        val iterations = completed.toDomain()

        val input = ExerciseValidationInput(
            name = source.name,
            exampleId = source.exerciseExample.id,
            iterations = iterations,
            performedAt = source.createdAt.value,
        )

        val artifacts = exerciseValidatorUseCase.execute(exercise = input, context = context)

        val volumeIds = mutableSetOf<String>()
        val repetitionIds = mutableSetOf<String>()

        artifacts?.iterations?.forEachIndexed { index, row ->
            val id = completed[index].id
            for (artifact in row.artifacts) {
                if (artifact.field == Field.Repetitions) {
                    repetitionIds += id
                } else {
                    volumeIds += id
                }
            }
        }

        val previousExercise = context.history
            .filter { it.exerciseExample.id == source.exerciseExample.id }
            .filter { it.id != source.id && it.id != context.excludedExerciseId }
            .filter { previous -> source.createdAt.value?.let { previous.createdAt < it } == true }
            .filter { it.iterations.isNotEmpty() }
            .maxByOrNull { it.createdAt }

        val previousIterations = previousExercise
            ?.iterations
            ?.map { it.toState() }
            ?.toPersistentList()
            ?: persistentListOf()

        val volumeArtifactIds = volumeIds.toPersistentSet()

        val repetitionArtifactIds = repetitionIds.toPersistentSet()

        return TrainingExerciseValidation(
            source = source,
            volumeIds = volumeArtifactIds,
            repetitionIds = repetitionArtifactIds,
            previousIterations = previousIterations,
        )
    }

    internal fun swapExerciseExample(value: ExerciseExampleState) {
        val current = state.value.exercise
        if (current.exerciseExample.id == value.value.id) {
            return
        }

        analytics.trackEvent(event = TrainingEvent.Exercise.ExampleSelected)

        val updated = current.copy(
            name = value.value.name,
            exerciseExample = value.value,
        )

        updateExercise(updated)
    }

    override fun onAddIteration() {
        val example = state.value.exerciseExample ?: return

        analytics.trackEvent(event = TrainingEvent.Exercise.AddIteration)

        safeLaunch {
            val blankIteration = buildBlankIteration(example)

            val initial = blankIteration.withResolvedBodyWeight(example)

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
        editIteration(id = id, focus = IterationFocusState.VOLUME)
    }

    override fun onEditRepetition(id: String) {
        editIteration(id = id, focus = IterationFocusState.REPETITIONS)
    }

    override fun onDeleteIteration(id: String) {
        analytics.trackEvent(event = TrainingEvent.Exercise.DeleteIteration)
        mutateIterations { iterations ->
            iterations.filter { it.id != id }
        }
    }

    override fun onStartReorderIterations(fromId: String, toId: String) {
        if (fromId == toId) {
            return
        }

        val iterations = state.value.exercise.iterations
        val fromIndex = iterations.indexOfFirst { it.id == fromId }
        val toIndex = iterations.indexOfFirst { it.id == toId }
        if (fromIndex < 0 || toIndex < 0) {
            return
        }

        analytics.trackEvent(event = TrainingEvent.Exercise.ReorderIterations)

        val reorderedIterations = iterations.toMutableList().apply {
            val iteration = removeAt(fromIndex)
            add(index = toIndex, element = iteration)
        }.toPersistentList()

        update { current ->
            val exercise = current.exercise.copy(iterations = reorderedIterations)

            current.copy(exercise = exercise)
        }
    }

    override fun onEndReorderIterations() {
        analytics.trackEvent(event = TrainingEvent.Exercise.ReorderFinished)
        updateExercise(state.value.exercise)
    }

    override fun onExampleClick() {
        analytics.trackEvent(event = TrainingEvent.Exercise.ExampleOpened)

        val exampleId = state.value.exercise.exerciseExample.id

        val actionTitle = UiText.Res(value = Res.string.change_btn)

        val title = UiText.Res(value = Res.string.exercise_details_btn)

        val mode = DialogConfig.ExerciseExampleDetails.Mode.Action(
            title = actionTitle,
            onClick = { navigation -> showExerciseLibrary(navigation = navigation, exampleId = exampleId) },
        )

        val dialog = DialogConfig.ExerciseExampleDetails(
            title = title,
            id = exampleId,
            mode = mode,
        )

        dialogController.open(dialog)
    }

    override fun onSave() {
        analytics.trackEvent(event = TrainingEvent.Exercise.Save)

        val direction = TrainingExerciseDirection.Save(exercise = state.value.exercise)

        navigateTo(direction)
    }

    override fun onBack() {
        analytics.trackEvent(event = TrainingEvent.Exercise.DismissRequested)
        navigateTo(TrainingExerciseDirection.Back)
    }

    companion object {
        internal fun libraryResultKey(exerciseId: String): ResultKey<Result<ExerciseExampleState>> {
            return ResultKeys.create("exercise-library:$exerciseId")
        }
    }
}
