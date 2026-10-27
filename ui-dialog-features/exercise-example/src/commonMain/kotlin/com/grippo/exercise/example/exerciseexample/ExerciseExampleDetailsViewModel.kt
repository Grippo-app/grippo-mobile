package com.grippo.exercise.example.exerciseexample

import com.grippo.analytics.exercise.example.ExerciseExampleAnalytics
import com.grippo.analytics.exercise.example.models.ExerciseExampleEvent
import com.grippo.analytics.exercise.example.models.ExerciseExampleOperation
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.metrics.ExerciseMetricsFeature
import com.grippo.data.features.api.metrics.distribution.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.performance.EstimatedOneRepMaxUseCase
import com.grippo.data.features.api.metrics.volume.VolumeSeriesUseCase
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.domain.state.achievements.toState
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.metrics.distribution.toState
import com.grippo.domain.state.metrics.performance.toState
import com.grippo.domain.state.metrics.volume.toState
import com.grippo.domain.state.training.toState
import com.grippo.exercise.example.exerciseexample.analytics.toAnalytics
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

public class ExerciseExampleDetailsViewModel(
    id: String,
    mode: ExerciseExampleDetailsModeState,
    private val analytics: ExerciseExampleAnalytics,
    exerciseExampleFeature: ExerciseExampleFeature,
    private val exerciseMetricsFeature: ExerciseMetricsFeature,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val volumeSeriesUseCase: VolumeSeriesUseCase,
    private val estimatedOneRepMaxUseCase: EstimatedOneRepMaxUseCase,
) : BaseViewModel<ExerciseExampleDetailsState, ExerciseExampleDetailsDirection, ExerciseExampleDetailsLoader>(
    ExerciseExampleDetailsState(mode = mode)
), ExerciseExampleDetailsContract {

    init {
        val example = exerciseExampleFeature.observeExerciseExample(id)

        example
            .onEach(::provideExerciseExample)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        example
            .combine(observeRecentExercises(id)) { example, exercises ->
                example to exercises
            }
            .onEach(::provideEstimatedOneRepMax)
            .safeLaunch(processing = Processing.Infinity, recovery = ErrorRecovery.Retry)

        safeLaunch(recovery = ErrorRecovery.Retry) {
            analytics.trackOperation(ExerciseExampleOperation.LoadExample) {
                exerciseExampleFeature.getExerciseExampleById(id)
            }
        }

        safeLaunch(loader = ExerciseExampleDetailsLoader.Achievements, recovery = ErrorRecovery.Retry) {
            analytics.trackOperation(ExerciseExampleOperation.LoadAchievements) {
                val achievements = exerciseMetricsFeature.getAchievementsByExampleId(id)
                provideAchievements(achievements)
            }
        }
    }

    private fun observeRecentExercises(id: String): Flow<List<Exercise>> = flow {
        val exercises = withLoader(ExerciseExampleDetailsLoader.RecentExercises) {
            analytics.trackOperation(ExerciseExampleOperation.LoadRecentExercises) {
                val exercises = exerciseMetricsFeature.getRecentExercisesByExampleId(id)
                provideRecentExercises(exercises)
                exercises
            }
        }
        emit(exercises)
    }

    private fun provideEstimatedOneRepMax(value: Pair<ExerciseExample?, List<Exercise>>) {
        val (example, exercises) = value
        val oneRepMax = example?.let {
            estimatedOneRepMaxUseCase.fromExercises(
                exercises = exercises,
                exerciseExample = it,
            ).toState()
        }

        update { it.copy(estimatedOneRepMax = oneRepMax) }
    }

    private fun provideRecentExercises(value: List<Exercise>) {
        val chronological = value.sortedWith(compareBy<Exercise> { it.createdAt }.thenBy { it.id })

        val exercisesState = chronological.asReversed().toState()

        val exerciseVolume = volumeSeriesUseCase
            .fromExercises(chronological)
            .toState()

        update {
            it.copy(
                recent = exercisesState,
                exerciseVolume = exerciseVolume,
            )
        }
    }

    private fun provideAchievements(value: List<Achievement>) {
        val achievements = value.toState().toPersistentList()

        update { it.copy(achievements = achievements) }
    }

    private fun provideExerciseExample(value: ExerciseExample?) {
        val exampleState = value?.toState() ?: return

        val muscleLoad = muscleLoadingSummaryUseCase
            .fromExerciseExample(value)
            .toState()

        update { it.copy(example = exampleState, muscleLoad = muscleLoad) }
    }

    override fun onTabSelected(value: ExerciseExampleDetailsTabState) {
        val tab = value.toAnalytics()

        analytics.trackEvent(ExerciseExampleEvent.TabSelected(tab = tab))
    }

    override fun onDismiss() {
        analytics.trackEvent(ExerciseExampleEvent.DismissRequested)
        navigateTo(ExerciseExampleDetailsDirection.Back)
    }

    override fun onAction() {
        analytics.trackEvent(ExerciseExampleEvent.ActionClicked)
        navigateTo(ExerciseExampleDetailsDirection.Action)
    }
}
