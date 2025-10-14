package com.grippo.training.recording

import com.grippo.calculation.AnalyticsApi
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.TrainingMetrics
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.training_progress_lost_description
import com.grippo.design.resources.provider.training_progress_lost_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.domain.state.training.toState
import com.grippo.state.domain.training.toDomain
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class)
internal class TrainingRecordingViewModel(
    stage: StageState,
    muscleFeature: MuscleFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider,
    colorProvider: ColorProvider,
) : BaseViewModel<TrainingRecordingState, TrainingRecordingDirection, TrainingRecordingLoader>(
    TrainingRecordingState(stage = stage)
), TrainingRecordingContract {

    private val analytics = AnalyticsApi(stringProvider, colorProvider)

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { it.exercises.map { exercise -> exercise.exerciseExample.id }.toSet().toList() }
            .distinctUntilChanged()
            .flatMapLatest(exerciseExampleFeature::observeExerciseExamples)
            .onEach(::provideExerciseExamples)
            .safeLaunch()

        state
            .map { Triple(it.exercises, it.examples, it.muscles) }
            .debounce(200)
            .distinctUntilChanged()
            .onEach { generateStatistics() }
            .safeLaunch()

        safeLaunch {
            when (val stage = state.value.stage) {
                StageState.Add -> {
                    trainingFeature.deleteDraftTraining().getOrThrow()
                }

                is StageState.Edit -> {
                    trainingFeature.deleteDraftTraining().getOrThrow()
                    val training = trainingFeature.observeTraining(stage.id).firstOrNull()
                    provideTraining(training)
                }

                StageState.Draft -> {
                    val training = trainingFeature.getDraftTraining().firstOrNull()
                    provideDraftTraining(training)
                }
            }
        }
    }

    private fun provideDraftTraining(value: SetDraftTraining?) {
        val exercises = value?.training?.exercises?.toState() ?: return
        val startAt = DateTimeUtils.minus(DateTimeUtils.now(), value.training.duration)
        update {
            it.copy(
                stage = when (val trainingId = value.trainingId) {
                    null -> StageState.Add
                    else -> StageState.Edit(trainingId)
                },
                exercises = exercises.toPersistentList(),
                startAt = startAt
            )
        }
    }

    private fun provideTraining(value: Training?) {
        val exercises = value?.exercises?.toState() ?: return
        val startAt = DateTimeUtils.minus(DateTimeUtils.now(), value.duration)
        update { it.copy(exercises = exercises.toPersistentList(), startAt = startAt) }
    }

    private fun provideMuscles(value: List<MuscleGroup>) {
        val muscles = value.toState()
        update { it.copy(muscles = muscles) }
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val examples = value.toState()
        update { it.copy(examples = examples) }
    }

    override fun onAddExercise() {
        val lastExampleId = state.value.exercises
            .lastOrNull()
            ?.exerciseExample
            ?.id

        val lastTargetMuscleId = state.value.examples
            .firstOrNull { it.value.id == lastExampleId }
            ?.bundles
            ?.maxByOrNull { (it.percentage as? PercentageFormatState.Valid)?.value ?: 0 }
            ?.muscle
            ?.id

        val lastTargetMuscleGroupId = state.value.muscles
            .find { it.muscles.any { a -> a.value.id == lastTargetMuscleId } }
            ?.id

        val dialog = DialogConfig.ExerciseExamplePicker(
            targetMuscleGroupId = lastTargetMuscleGroupId,
            onResult = { example ->
                val exercise = ExerciseState(
                    id = Uuid.random().toString(),
                    name = example.value.name,
                    iterations = persistentListOf(),
                    exerciseExample = example.value,
                    metrics = TrainingMetrics(
                        volume = VolumeFormatState.of(0f),
                        repetitions = RepetitionsFormatState.of(0),
                        intensity = IntensityFormatState.of(0f),
                    ),
                )

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
        update {
            val exercises = it.exercises

            val index = exercises.indexOfFirst { exercise -> exercise.id == value.id }

            val updatedExercises = if (index >= 0) {
                exercises.toMutableList().apply { this[index] = value }
            } else {
                exercises + value
            }

            it.copy(exercises = updatedExercises.toPersistentList())
        }

        saveDraftTraining()
    }

    override fun onSelectTab(tab: RecordingTab) {
        update { it.copy(tab = tab) }
    }

    override fun onDeleteExercise(id: String) {
        update { s ->
            val exercises = s.exercises
                .toMutableList()
                .apply {
                    val idx = indexOfFirst { it.id == id }
                    if (idx >= 0) removeAt(idx)
                }.toPersistentList()

            s.copy(exercises = exercises)
        }

        safeLaunch {
            trainingFeature.getDraftTraining().firstOrNull()
            if (state.value.exercises.isEmpty()) {
                trainingFeature.deleteDraftTraining().getOrThrow()
            } else {
                saveDraftTraining()
            }
        }
    }

    override fun onSave() {
        val direction = TrainingRecordingDirection.ToCompleted(
            stage = state.value.stage,
            exercises = state.value.exercises,
            startAt = state.value.startAt
        )

        navigateTo(direction)
    }

    override fun onBack() {
        if (state.value.exercises.isEmpty()) {
            safeLaunch {
                trainingFeature.deleteDraftTraining().getOrThrow()
                navigateTo(TrainingRecordingDirection.Back)
            }
        } else {
            safeLaunch {
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
    }

    private fun saveDraftTraining() {
        safeLaunch {
            val exercises = state.value.exercises
            val duration = DateTimeUtils.ago(state.value.startAt)
            val totals = analytics.metricsFromExercises(exercises)

            val training = SetTraining(
                exercises = exercises.toDomain(),
                duration = duration,
                volume = totals.volume.value ?: 0f,
                intensity = totals.intensity.value ?: 0f,
                repetitions = totals.repetitions.value ?: 0
            )

            val trainingId = state.value.stage.id

            val draft = SetDraftTraining(
                trainingId = trainingId,
                training = training
            )

            trainingFeature.setDraftTraining(draft).getOrThrow()
        }
    }

    private suspend fun generateStatistics() {
        val exercises = state.value.exercises
        val examples = state.value.examples
        val muscles = state.value.muscles

        if (exercises.isEmpty()) {
            update {
                it.copy(
                    totalMetrics = null,
                    exerciseVolume = null,
                    categoryDistribution = null,
                    forceTypeDistribution = null,
                    weightTypeDistribution = null,
                    muscleLoad = null,
                )
            }
            return
        }

        val totalMetrics = analytics.metricsFromExercises(
            exercises = exercises
        )

        val categoryDistribution = analytics.categoryDistributionFromExercises(
            exercises = exercises
        )

        val weightTypeDistribution = analytics.weightTypeDistributionFromExercises(
            exercises = exercises
        )

        val forceTypeDistribution = analytics.forceTypeDistributionFromExercises(
            exercises = exercises
        )

        val exerciseVolume = analytics.volumeFromExercises(
            exercises = exercises
        )

        val muscleLoad = analytics.muscleLoadFromExercises(
            exercises = exercises,
            examples = examples,
            groups = muscles,
        )

        update {
            it.copy(
                totalMetrics = totalMetrics,
                exerciseVolume = exerciseVolume,
                categoryDistribution = categoryDistribution,
                weightTypeDistribution = weightTypeDistribution,
                forceTypeDistribution = forceTypeDistribution,
                muscleLoad = muscleLoad,
            )
        }
    }
}
