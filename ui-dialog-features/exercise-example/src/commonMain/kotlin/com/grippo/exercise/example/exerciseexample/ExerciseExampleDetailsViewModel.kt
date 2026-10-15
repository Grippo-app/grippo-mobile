package com.grippo.exercise.example.exerciseexample

import com.grippo.analytics.exercise.example.ExerciseExampleAnalytics
import com.grippo.analytics.exercise.example.models.ExerciseExampleEvent
import com.grippo.analytics.exercise.example.models.ExerciseExampleOperation
import com.grippo.core.foundation.BaseViewModel
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
import kotlinx.coroutines.flow.onEach

public class ExerciseExampleDetailsViewModel(
    private val analytics: ExerciseExampleAnalytics,
    id: String,
    mode: ExerciseExampleDetailsModeState,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val exerciseMetricsFeature: ExerciseMetricsFeature,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val volumeSeriesUseCase: VolumeSeriesUseCase,
    private val estimatedOneRepMaxUseCase: EstimatedOneRepMaxUseCase
) : BaseViewModel<ExerciseExampleDetailsState, ExerciseExampleDetailsDirection, ExerciseExampleDetailsLoader>(
    ExerciseExampleDetailsState(mode = mode)
), ExerciseExampleDetailsContract {

    init {
        exerciseExampleFeature.observeExerciseExample(id)
            .onEach(::provideExerciseExample)
            .safeLaunch()

        safeLaunch {
            analytics.trackOperation(ExerciseExampleOperation.LoadExample) {
                exerciseExampleFeature.getExerciseExampleById(id).getOrThrow()
            }
        }

        safeLaunch(loader = ExerciseExampleDetailsLoader.RecentExercises) {
            analytics.trackOperation(ExerciseExampleOperation.LoadRecentExercises) {
                val exercises = exerciseMetricsFeature.getRecentExercisesByExampleId(id).getOrThrow()
                provideRecentExercises(exercises)
            }
        }

        safeLaunch(loader = ExerciseExampleDetailsLoader.Achievements) {
            analytics.trackOperation(ExerciseExampleOperation.LoadAchievements) {
                val exercises = exerciseMetricsFeature.getAchievementsByExampleId(id).getOrThrow()
                provideAchievements(exercises)
            }
        }
    }

    private fun provideRecentExercises(value: List<Exercise>) {
        val exercisesState = value.toState()

        val exerciseVolume = volumeSeriesUseCase
            .fromExercises(value.asReversed())
            .toState()

        val oneRepMax = estimatedOneRepMaxUseCase
            .fromExercises(value)
            .toState()

        update {
            it.copy(
                recent = exercisesState,
                exerciseVolume = exerciseVolume,
                estimatedOneRepMax = oneRepMax
            )
        }
    }

    private fun provideAchievements(value: List<Achievement>) {
        val achievements = value.toState()

        update { it.copy(achievements = achievements.toPersistentList()) }
    }

    private suspend fun provideExerciseExample(value: ExerciseExample?) {
        val exampleState = value?.toState() ?: return

        val muscleLoad = muscleLoadingSummaryUseCase
            .fromExerciseExample(exampleState.value.id)
            .toState()

        update { current -> current.copy(example = exampleState, muscleLoad = muscleLoad) }
    }

    override fun onTabSelected(value: ExerciseExampleDetailsTabState) {
        analytics.trackEvent(ExerciseExampleEvent.TabSelected(value.toAnalytics()))
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
