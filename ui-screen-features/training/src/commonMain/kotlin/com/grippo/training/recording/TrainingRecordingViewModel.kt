package com.grippo.training.recording

import com.grippo.analytics.training.TrainingAnalytics
import com.grippo.analytics.training.models.TrainingEvent
import com.grippo.analytics.training.models.TrainingOperation
import com.grippo.analytics.training.models.TrainingScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import com.grippo.core.foundation.models.ResultKeys
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.metrics.volume.TrainingTotalUseCase
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.confirm_training_completion_title
import com.grippo.design.resources.provider.notification_forgot_training_description
import com.grippo.design.resources.provider.notification_forgot_training_title
import com.grippo.design.resources.provider.planned_sets_unfinished_description
import com.grippo.design.resources.provider.planned_sets_unfinished_title
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.start_training_title
import com.grippo.design.resources.provider.training_progress_lost_description
import com.grippo.design.resources.provider.training_progress_lost_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.metrics.volume.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.domain.state.training.toState
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.TrainingRouter
import com.grippo.screen.api.deeplink.Deeplink
import com.grippo.state.domain.training.toDomain
import com.grippo.state.domain.training.toDraftDomain
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationKey
import com.grippo.toolkit.local.notification.NotificationManager
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class TrainingRecordingViewModel(
    stage: StageState,
    private val analytics: TrainingAnalytics,
    muscleFeature: MuscleFeature,
    exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider,
    private val notificationManager: NotificationManager,
    private val trainingTotalUseCase: TrainingTotalUseCase,
    initialState: TrainingRecordingState?,
) : BaseViewModel<TrainingRecordingState, TrainingRecordingDirection, TrainingRecordingLoader>(
    initialState ?: TrainingRecordingState(stage = stage, startAt = DateTimeUtils.now())
), TrainingRecordingContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(TrainingScreen.Recording, reason)
    }

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { current ->
                current.exercises.orEmpty()
                    .map { it.exerciseExample.id }
                    .distinct()
            }
            .distinctUntilChanged()
            .flatMapLatest(exerciseExampleFeature::observeExerciseExamples)
            .onEach(::provideExerciseExamples)
            .safeLaunch()

        when (initialState?.exercises) {
            null -> {
                safeLaunch {
                    when (val stage = state.value.stage) {
                        StageState.Add -> {
                            analytics.trackOperation(TrainingOperation.DeleteDraft) {
                                trainingFeature.deleteDraftTraining()
                            }

                            showStartTrainingDialog()
                        }

                        is StageState.Edit -> {
                            analytics.trackOperation(TrainingOperation.DeleteDraft) {
                                trainingFeature.deleteDraftTraining()
                            }

                            val training = analytics.trackOperation(TrainingOperation.LoadTraining) {
                                trainingFeature.observeTraining(stage.id).firstOrNull()
                            }

                            provideTraining(training)
                        }

                        StageState.Draft -> {
                            val draft = analytics.trackOperation(TrainingOperation.LoadDraft) {
                                trainingFeature.getDraftTraining().firstOrNull()
                            }

                            provideDraftTraining(draft)
                        }
                    }
                }
            }
        }
    }

    private suspend fun showStartTrainingDialog() {
        delay(START_TRAINING_DIALOG_DELAY)

        if (state.value.exercises != null) {
            return
        }

        val dialog = DialogConfig.StartTraining(
            title = UiText.Res(Res.string.start_training_title),
            onStartEmpty = {
                analytics.trackEvent(TrainingEvent.Recording.StartConfirmed)
                openExerciseLibrary()
            },
            onUseExercises = { exercises ->
                if (exercises.isEmpty()) {
                    return@StartTraining
                }

                analytics.trackEvent(TrainingEvent.Recording.StartConfirmed)

                val refreshed = exercises
                    .map { recalculateTotal(it) }
                    .toPersistentList()

                update { it.copy(exercises = refreshed) }
                saveDraftTraining()
            },
        )

        analytics.trackEvent(TrainingEvent.Recording.StartOpened)
        dialogController.open(dialog)
    }

    private fun provideDraftTraining(value: DraftTraining?) {
        value ?: return

        if (state.value.exercises != null) {
            return
        }

        val exercises = value.exercises.map { exercise ->
            val id = Uuid.random().toString()

            val iterations = exercise.iterations.map { iteration ->
                val iterationId = Uuid.random().toString()

                iteration.toState(id = iterationId)
            }.toPersistentList()

            val exerciseState = exercise.toState(id = id, iterations = iterations)

            recalculateTotal(exerciseState)
        }.toPersistentList()

        val now = DateTimeUtils.now()

        val startAt = DateTimeUtils.minus(value = now, minus = value.duration)

        val stage = when (val trainingId = value.trainingId) {
            null -> {
                StageState.Add
            }

            else -> {
                StageState.Edit(id = trainingId)
            }
        }

        update { it.copy(stage = stage, exercises = exercises, startAt = startAt) }
    }

    private fun recalculateTotal(
        exercise: ExerciseState,
        components: ExerciseExampleComponentsState? = state.value.examples
            .firstOrNull { it.value.id == exercise.exerciseExample.id }?.components,
    ): ExerciseState {
        val completed = exercise.iterations.filterNot { it.isPending }.toDomain()

        val totals = trainingTotalUseCase.fromSetIterations(completed).toState()

        if (needsAssistanceCorrection(exercise = exercise, components = components)) {
            val total = totals.copy(
                volume = VolumeFormatState.Invalid(display = "", value = null),
                intensity = IntensityFormatState.Invalid(display = "", value = null),
            )

            return exercise.copy(total = total)
        }

        return exercise.copy(total = totals)
    }

    private fun needsAssistanceCorrection(
        exercise: ExerciseState,
        components: ExerciseExampleComponentsState?,
    ): Boolean {
        if (components !is ExerciseExampleComponentsState.BodyAndAssist || !components.assistRequired) {
            return false
        }

        return exercise.iterations.any { !it.isPending && it.assistWeight !is VolumeFormatState.Valid }
    }

    private fun openAssistanceCorrection(): Boolean {
        val snapshot = state.value
        val exercise = snapshot.exercises.orEmpty().firstOrNull { exercise ->
            val components = snapshot.examples
                .firstOrNull { it.value.id == exercise.exerciseExample.id }?.components

            needsAssistanceCorrection(exercise = exercise, components = components)
        } ?: return false

        navigateTo(
            TrainingRecordingDirection.ToExercise(exercise = exercise, resultKey = EXERCISE_RESULT_KEY)
        )

        return true
    }

    private fun provideTraining(value: Training?) {
        value ?: return

        if (state.value.exercises != null) {
            return
        }

        val exercises = value.exercises.toState()
            .map { recalculateTotal(it) }
            .toPersistentList()

        val now = DateTimeUtils.now()

        val startAt = DateTimeUtils.minus(value = now, minus = value.duration)

        update { it.copy(exercises = exercises, startAt = startAt) }
    }

    private fun provideMuscles(value: List<MuscleGroup>) {
        val muscles = value.toState()

        update { it.copy(muscles = muscles) }
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val examples = value.toState()

        val exercises = state.value.exercises?.map { exercise ->
            val components = examples.firstOrNull { it.value.id == exercise.exerciseExample.id }?.components

            recalculateTotal(exercise = exercise, components = components)
        }?.toPersistentList()

        update { it.copy(examples = examples, exercises = exercises) }
    }

    private fun openExerciseLibrary() {
        if (state.value.exercises == null) {
            update { it.copy(exercises = persistentListOf()) }
        }

        val muscleGroupId = lastTargetMuscleGroupId()

        val mode = ExercisesRouter.Mode.Default(
            preselectedMuscleGroupId = muscleGroupId,
            resultKey = LIBRARY_RESULT_KEY,
        )

        val route = ExercisesRouter.Library(mode = mode)

        navigateTo(TrainingRecordingDirection.ToLibrary(route = route))
    }

    private fun clearDraftTraining() {
        safeLaunch {
            analytics.trackOperation(TrainingOperation.DeleteDraft) {
                trainingFeature.deleteDraftTraining()
            }
        }
    }

    private fun showCompletionDialog() {
        if (openAssistanceCorrection()) {
            return
        }

        val snapshot = state.value
        val exercises = snapshot.exercises ?: return

        val hasCompletedSets = exercises.any { exercise ->
            exercise.iterations.any { !it.isPending }
        }

        if (!hasCompletedSets) {
            return
        }

        val duration = DateTimeUtils.ago(snapshot.startAt)

        val dialog = DialogConfig.ConfirmTrainingCompletion(
            title = UiText.Res(Res.string.confirm_training_completion_title),
            initial = duration,
            exercises = exercises,
            onResult = { result ->
                if (openAssistanceCorrection()) {
                    return@ConfirmTrainingCompletion
                }

                analytics.trackEvent(TrainingEvent.Recording.CompletionConfirmed)

                val now = DateTimeUtils.now()

                val startAt = DateTimeUtils.minus(value = now, minus = result)

                update { current ->
                    val cleaned = current.exercises.orEmpty()
                        .map { exercise ->
                            val iterations = exercise.iterations
                                .filterNot { it.isPending }
                                .toPersistentList()

                            exercise.copy(iterations = iterations)
                        }
                        .filter { it.iterations.isNotEmpty() }
                        .toPersistentList()

                    current.copy(exercises = cleaned, startAt = startAt)
                }

                notificationManager.cancel(NotificationKey.FinishWorkout)
                toCompleteTraining()
            },
        )

        analytics.trackEvent(TrainingEvent.Recording.CompletionOpened)
        dialogController.open(dialog)
    }

    private fun toCompleteTraining() {
        val snapshot = state.value

        navigateTo(
            TrainingRecordingDirection.ToCompleted(
                stage = snapshot.stage,
                exercises = snapshot.exercises.orEmpty(),
                startAt = snapshot.startAt,
            )
        )
    }

    private fun discardDraftAndExit() {
        safeLaunch {
            analytics.trackOperation(TrainingOperation.DeleteDraft) {
                trainingFeature.deleteDraftTraining()
            }

            navigateTo(TrainingRecordingDirection.Back)
        }
    }

    private fun saveDraftTraining() {
        safeLaunch {
            val snapshot = state.value

            val duration = DateTimeUtils.ago(snapshot.startAt)

            val exercises = snapshot.exercises.orEmpty().toDraftDomain()

            val draft = DraftTraining(
                trainingId = snapshot.stage.id,
                duration = duration,
                exercises = exercises,
            )

            analytics.trackOperation(TrainingOperation.SaveDraft) {
                trainingFeature.setDraftTraining(draft)
            }

            scheduleNotificationReminder()
        }
    }

    private suspend fun scheduleNotificationReminder() {
        val title = stringProvider.get(Res.string.notification_forgot_training_title)

        val body = stringProvider.get(Res.string.notification_forgot_training_description)

        val notification = AppNotification(
            id = NotificationKey.FinishWorkout,
            title = title,
            body = body,
            deeplink = Deeplink.TrainingDraft.key,
        )

        notificationManager.show(notification = notification, delay = 45.minutes)
    }

    private fun lastTargetMuscleGroupId(): String? {
        val snapshot = state.value
        val exercises = snapshot.exercises.orEmpty().reversed()
        val examples = snapshot.examples
        val muscles = snapshot.muscles

        for (exercise in exercises) {
            val example = examples.firstOrNull { it.value.id == exercise.exerciseExample.id } ?: continue

            val muscleId = example.bundles
                .maxByOrNull { (it.percentage as? PercentageFormatState.Valid)?.value ?: 0 }
                ?.muscle
                ?.id ?: continue

            val muscleGroupId = muscles
                .find { group -> group.muscles.any { it.value.id == muscleId } }
                ?.id

            if (muscleGroupId != null) {
                return muscleGroupId
            }
        }

        return null
    }

    private fun createExerciseFromExample(example: ExerciseExampleValueState): ExerciseState {
        val id = Uuid.random().toString()

        val now = DateTimeUtils.now()

        val range = DateRangePresets.infinity()

        val createdAt = DateTimeFormatState.of(
            value = now,
            range = range,
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        )

        val total = TrainingTotalState(
            volume = VolumeFormatState.Empty(),
            repetitions = RepetitionsFormatState.Empty(),
            intensity = IntensityFormatState.Empty(),
        )

        return ExerciseState(
            id = id,
            name = example.name,
            iterations = persistentListOf(),
            exerciseExample = example,
            createdAt = createdAt,
            total = total,
        )
    }

    fun acceptLibrarySelection(example: ExerciseExampleState) {
        analytics.trackEvent(TrainingEvent.Recording.LibrarySelected)

        val exercise = createExerciseFromExample(example.value)

        navigateTo(
            TrainingRecordingDirection.ToExercise(exercise = exercise, resultKey = EXERCISE_RESULT_KEY)
        )
    }

    fun updateExercise(value: ExerciseState) {
        analytics.trackEvent(TrainingEvent.Recording.ExerciseUpdated)

        val refreshed = recalculateTotal(value)

        update { current ->
            val index = current.exercises.orEmpty().indexOfFirst { it.id == refreshed.id }

            val updated = if (index >= 0) {
                current.exercises.orEmpty().toMutableList().apply {
                    this[index] = refreshed
                }
            } else {
                current.exercises.orEmpty() + refreshed
            }

            val exercises = updated.toPersistentList()

            current.copy(exercises = exercises)
        }

        saveDraftTraining()
    }

    fun removeExercise(id: String) {
        update { current ->
            val exercises = current.exercises.orEmpty()
                .filterNot { it.id == id }
                .toPersistentList()

            current.copy(exercises = exercises)
        }

        if (state.value.exercises.orEmpty().isEmpty()) {
            clearDraftTraining()
        } else {
            saveDraftTraining()
        }
    }

    override fun onAddExercise() {
        analytics.trackEvent(TrainingEvent.Recording.AddExercise)
        openExerciseLibrary()
    }

    override fun onEditExercise(id: String) {
        val exercise = state.value.exercises.orEmpty().find { it.id == id } ?: return

        analytics.trackEvent(TrainingEvent.Recording.EditExercise)

        navigateTo(
            TrainingRecordingDirection.ToExercise(exercise = exercise, resultKey = EXERCISE_RESULT_KEY)
        )
    }

    override fun onDeleteExercise(id: String) {
        analytics.trackEvent(TrainingEvent.Recording.DeleteExercise)
        removeExercise(id)
    }

    override fun onStartReorderExercises(fromId: String, toId: String) {
        if (fromId == toId) {
            return
        }

        update { current ->
            val exercises = current.exercises.orEmpty()

            val from = exercises.indexOfFirst { it.id == fromId }

            val to = exercises.indexOfFirst { it.id == toId }

            if (from < 0 || to < 0) {
                return@update current
            }

            val mutable = exercises.toMutableList()

            val exercise = mutable.removeAt(from)

            mutable.add(index = to, element = exercise)

            val reordered = mutable.toPersistentList()

            current.copy(exercises = reordered)
        }
    }

    override fun onEndReorderExercises() {
        if (state.value.exercises.orEmpty().isEmpty()) {
            return
        }

        analytics.trackEvent(TrainingEvent.Recording.ReorderExercises)
        saveDraftTraining()
    }

    override fun onSave() {
        analytics.trackEvent(TrainingEvent.Recording.Save)

        val hasPending = state.value.exercises.orEmpty().any { exercise ->
            exercise.iterations.any { it.isPending }
        }

        if (!hasPending) {
            showCompletionDialog()
            return
        }

        val dialog = DialogConfig.Confirmation(
            title = UiText.Res(Res.string.planned_sets_unfinished_title),
            description = UiText.Res(Res.string.planned_sets_unfinished_description),
            onResult = {
                analytics.trackEvent(TrainingEvent.Recording.UnfinishedConfirmed)
                showCompletionDialog()
            },
        )

        analytics.trackEvent(TrainingEvent.Recording.UnfinishedOpened)
        dialogController.open(dialog)
    }

    override fun onBack() {
        analytics.trackEvent(TrainingEvent.Recording.DismissRequested)

        if (state.value.exercises.orEmpty().isEmpty()) {
            discardDraftAndExit()
            return
        }

        val dialog = DialogConfig.Confirmation(
            title = UiText.Res(Res.string.training_progress_lost_title),
            description = UiText.Res(Res.string.training_progress_lost_description),
            onResult = {
                analytics.trackEvent(TrainingEvent.Recording.DiscardConfirmed)
                discardDraftAndExit()
            },
        )

        analytics.trackEvent(TrainingEvent.Recording.DiscardOpened)
        dialogController.open(dialog)
    }

    companion object {
        val LIBRARY_RESULT_KEY: ResultKey<Result<ExerciseExampleState>> = ResultKeys.create("exercise-library-add")
        val EXERCISE_RESULT_KEY: ResultKey<Result<TrainingRouter.Exercise.Action>> = ResultKeys.create("exercise")
        private val START_TRAINING_DIALOG_DELAY = 300.milliseconds
    }
}
