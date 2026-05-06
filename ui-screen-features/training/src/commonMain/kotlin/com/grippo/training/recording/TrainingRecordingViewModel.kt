package com.grippo.training.recording

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.stage.TrainingSeed
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.metrics.volume.TrainingTotalUseCase
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.GeneratePresetTrainingUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.notification_forgot_training_description
import com.grippo.design.resources.provider.notification_forgot_training_title
import com.grippo.design.resources.provider.planned_sets_unfinished_description
import com.grippo.design.resources.provider.planned_sets_unfinished_title
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.training_progress_lost_description
import com.grippo.design.resources.provider.training_progress_lost_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.metrics.volume.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.domain.state.training.toState
import com.grippo.screen.api.deeplink.Deeplink
import com.grippo.services.firebase.FirebaseProvider
import com.grippo.state.domain.training.toDomain
import com.grippo.state.domain.training.toDraftDomain
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationKey
import com.grippo.toolkit.local.notification.NotificationManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

internal class TrainingRecordingViewModel(
    stage: StageState,
    seed: TrainingSeed,
    muscleFeature: MuscleFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider,
    private val trainingTotalUseCase: TrainingTotalUseCase,
    private val notificationManager: NotificationManager,
    private val generatePresetTrainingUseCase: GeneratePresetTrainingUseCase,
) : BaseViewModel<TrainingRecordingState, TrainingRecordingDirection, TrainingRecordingLoader>(
    TrainingRecordingState(stage = stage)
), TrainingRecordingContract {

    private companion object {
        private const val EMPTY_BOOTSTRAP_DELAY_MS = 400L
    }

    init {
        FirebaseProvider.logEvent(FirebaseProvider.Event.WORKOUT_STARTED)

        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { it.exercises.map { exercise -> exercise.exerciseExample.id }.distinct() }
            .distinctUntilChanged()
            .flatMapLatest(exerciseExampleFeature::observeExerciseExamples)
            .onEach(::provideExerciseExamples)
            .safeLaunch()

        safeLaunch { bootstrap(seed) }
    }

    private suspend fun bootstrap(seed: TrainingSeed) {
        when (val stage = state.value.stage) {
            StageState.Add -> bootstrapAdd(seed)

            is StageState.Edit -> {
                trainingFeature.deleteDraftTraining().getOrThrow()
                val training = trainingFeature.observeTraining(stage.id).firstOrNull()
                provideTraining(training)
            }

            StageState.Draft -> {
                exerciseExampleFeature.getExerciseExamples().getOrThrow()
                val draft = trainingFeature.getDraftTraining().firstOrNull()
                provideDraftTraining(draft)
            }
        }
    }

    private suspend fun bootstrapAdd(seed: TrainingSeed) {
        when (seed) {
            TrainingSeed.Blank -> {
                trainingFeature.deleteDraftTraining().getOrThrow()
                delay(EMPTY_BOOTSTRAP_DELAY_MS)
                if (state.value.exercises.isEmpty()) onAddExercise()
            }

            TrainingSeed.FromPreset -> {
                trainingFeature.deleteDraftTraining().getOrThrow()
                val preset = generatePresetTrainingUseCase.execute(DateTimeUtils.now())
                if (preset != null) {
                    seedFromPreset(preset)
                } else {
                    delay(EMPTY_BOOTSTRAP_DELAY_MS)
                    if (state.value.exercises.isEmpty()) onAddExercise()
                }
            }
        }
    }

    private fun provideDraftTraining(value: DraftTraining?) {
        value ?: return

        // Drafts carry no aggregates — rebuild totals from completed
        // iterations so the recording header reflects real progress on resume.
        val exercises = value.exercises.toState().map { exercise ->
            val completed = exercise.iterations.filterNot { it.isPending }.toDomain()
            val totals = trainingTotalUseCase.fromSetIterations(completed).toState()
            exercise.copy(total = totals)
        }

        val startAt = DateTimeUtils.minus(DateTimeUtils.now(), value.duration)

        update {
            it.copy(
                stage = when (val trainingId = value.trainingId) {
                    null -> StageState.Add
                    else -> StageState.Edit(trainingId)
                },
                exercises = exercises.toPersistentList(),
                startAt = startAt,
            )
        }
    }

    private fun provideTraining(value: Training?) {
        value ?: return
        val exercises = value.exercises.toState()
        val startAt = DateTimeUtils.minus(DateTimeUtils.now(), value.duration)
        update { it.copy(exercises = exercises, startAt = startAt) }
    }

    private fun seedFromPreset(value: SetTraining) {
        // Preset arrives with weights as planning hints; strip them so each
        // iteration shows up as Pending (null weights). Exercise totals reset
        // to zero — nothing has been performed yet.
        val seedExercises = value.exercises.map { exercise ->
            exercise.copy(
                iterations = exercise.iterations.map { iteration ->
                    iteration.copy(
                        externalWeight = null,
                        extraWeight = null,
                        assistWeight = null,
                        bodyWeight = null,
                    )
                },
                volume = 0f,
                intensity = 0f,
                repetitions = 0,
            )
        }
        update {
            it.copy(
                exercises = seedExercises.toState(),
                startAt = DateTimeUtils.now(),
            )
        }
    }

    private fun provideMuscles(value: List<MuscleGroup>) {
        update { it.copy(muscles = value.toState()) }
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        update { it.copy(examples = value.toState()) }
    }

    override fun onAddExercise() {
        val dialog = DialogConfig.ExerciseExamplePicker(
            targetMuscleGroupId = lastTargetMuscleGroupId(),
            onResult = { example ->
                val exercise = createExerciseFromExample(example.value)
                navigateTo(TrainingRecordingDirection.ToExercise(exercise))
            }
        )
        dialogController.show(dialog)
    }

    override fun onEditExercise(id: String) {
        val exercise = state.value.exercises.find { it.id == id } ?: return
        navigateTo(TrainingRecordingDirection.ToExercise(exercise))
    }

    fun updateExercise(value: ExerciseState) {
        update { current ->
            val index = current.exercises.indexOfFirst { it.id == value.id }
            val updated = if (index >= 0) {
                current.exercises.toMutableList().apply { this[index] = value }
            } else {
                current.exercises + value
            }
            current.copy(exercises = updated.toPersistentList())
        }

        saveDraftTraining()
    }

    override fun onDeleteExercise(id: String) {
        update { current ->
            val exercises = current.exercises
                .filterNot { it.id == id }
                .toPersistentList()
            current.copy(exercises = exercises)
        }

        safeLaunch {
            if (state.value.exercises.isEmpty()) {
                trainingFeature.deleteDraftTraining().getOrThrow()
            } else {
                saveDraftTraining()
            }
        }
    }

    override fun onSave() {
        val hasPending = state.value.exercises.any { exercise ->
            exercise.iterations.any { it.isPending }
        }

        if (!hasPending) {
            completeAndShowSummary()
            return
        }

        safeLaunch {
            val dialog = DialogConfig.Confirmation(
                title = stringProvider.get(Res.string.planned_sets_unfinished_title),
                description = stringProvider.get(Res.string.planned_sets_unfinished_description),
                onResult = ::completeAndShowSummary
            )
            dialogController.show(dialog)
        }
    }

    /**
     * Strips Pending iterations and exercises that no longer have any sets,
     * then opens the completion dialog. The server save downstream receives
     * only what was actually performed.
     */
    private fun completeAndShowSummary() {
        update { current ->
            val cleaned = current.exercises
                .map { exercise ->
                    exercise.copy(
                        iterations = exercise.iterations
                            .filterNot { it.isPending }
                            .toPersistentList()
                    )
                }
                .filter { it.iterations.isNotEmpty() }
                .toPersistentList()
            current.copy(exercises = cleaned)
        }

        showCompletionDialog()
    }

    private fun showCompletionDialog() {
        val duration = DateTimeUtils.ago(state.value.startAt)

        val dialog = DialogConfig.ConfirmTrainingCompletion(
            initial = duration,
            onResult = { result ->
                val startAt = DateTimeUtils.minus(DateTimeUtils.now(), result)
                update { it.copy(startAt = startAt) }
                cancelNotificationReminder()
                toCompleteTraining()
            }
        )

        dialogController.show(dialog)
    }

    private fun toCompleteTraining() {
        val direction = TrainingRecordingDirection.ToCompleted(
            stage = state.value.stage,
            exercises = state.value.exercises,
            startAt = state.value.startAt
        )
        navigateTo(direction)
    }

    override fun onBack() {
        safeLaunch {
            if (state.value.exercises.isEmpty()) {
                trainingFeature.deleteDraftTraining().getOrThrow()
                navigateTo(TrainingRecordingDirection.Back)
                return@safeLaunch
            }

            val dialog = DialogConfig.Confirmation(
                title = stringProvider.get(Res.string.training_progress_lost_title),
                description = stringProvider.get(Res.string.training_progress_lost_description),
                onResult = {
                    safeLaunch {
                        trainingFeature.deleteDraftTraining().getOrThrow()
                        navigateTo(TrainingRecordingDirection.Back)
                    }
                }
            )
            dialogController.show(dialog)
        }
    }

    private fun saveDraftTraining() {
        safeLaunch {
            val draft = DraftTraining(
                trainingId = state.value.stage.id,
                duration = DateTimeUtils.ago(state.value.startAt),
                exercises = state.value.exercises.toDraftDomain(),
            )
            trainingFeature.setDraftTraining(draft).getOrThrow()
            scheduleNotificationReminder()
        }
    }

    private suspend fun scheduleNotificationReminder() {
        val notification = AppNotification(
            id = NotificationKey.FinishWorkout,
            title = stringProvider.get(Res.string.notification_forgot_training_title),
            body = stringProvider.get(Res.string.notification_forgot_training_description),
            deeplink = Deeplink.TrainingDraft.key
        )
        notificationManager.show(notification = notification, delay = 45.minutes)
    }

    private fun cancelNotificationReminder() {
        notificationManager.cancel(NotificationKey.FinishWorkout)
    }

    /**
     * Walks exercises from newest to oldest and returns the muscle group of the
     * dominant muscle for the most recent exercise that resolves cleanly. Used
     * to pre-select the picker on the next "Add exercise" tap.
     */
    private fun lastTargetMuscleGroupId(): String? {
        val exercises = state.value.exercises.reversed()
        val examples = state.value.examples
        val muscles = state.value.muscles

        for (exercise in exercises) {
            val example = examples.firstOrNull { it.value.id == exercise.exerciseExample.id }
                ?: continue

            val muscleId = example.bundles
                .maxByOrNull { (it.percentage as? PercentageFormatState.Valid)?.value ?: 0 }
                ?.muscle
                ?.id ?: continue

            val muscleGroupId = muscles
                .find { group -> group.muscles.any { it.value.id == muscleId } }
                ?.id

            if (muscleGroupId != null) return muscleGroupId
        }

        return null
    }

    private fun createExerciseFromExample(example: ExerciseExampleValueState): ExerciseState {
        return ExerciseState(
            id = Uuid.random().toString(),
            name = example.name,
            iterations = persistentListOf(),
            exerciseExample = example,
            createdAt = DateTimeFormatState.of(
                value = DateTimeUtils.now(),
                range = DateRangePresets.infinity(),
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            total = TrainingTotalState(
                volume = VolumeFormatState.Empty(),
                repetitions = RepetitionsFormatState.Empty(),
                intensity = IntensityFormatState.Empty(),
            ),
        )
    }
}
