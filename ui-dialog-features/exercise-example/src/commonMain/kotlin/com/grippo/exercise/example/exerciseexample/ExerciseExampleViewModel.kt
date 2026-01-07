package com.grippo.exercise.example.exerciseexample

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.metrics.ExerciseMetricsFeature
import com.grippo.data.features.api.metrics.EstimatedOneRepMaxUseCase
import com.grippo.data.features.api.metrics.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.VolumeSeriesUseCase
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.domain.state.achievements.toState
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.metrics.toState
import com.grippo.domain.state.training.toState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

public class ExerciseExampleViewModel(
    id: String,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val exerciseMetricsFeature: ExerciseMetricsFeature,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val volumeSeriesUseCase: VolumeSeriesUseCase,
    private val estimatedOneRepMaxUseCase: EstimatedOneRepMaxUseCase
) : BaseViewModel<ExerciseExampleState, ExerciseExampleDirection, ExerciseExampleLoader>(
    ExerciseExampleState()
), ExerciseExampleContract {

    init {
        exerciseExampleFeature.observeExerciseExample(id)
            .onEach(::provideExerciseExample)
            .safeLaunch()

        safeLaunch {
            exerciseExampleFeature.getExerciseExampleById(id).getOrThrow()
        }

        safeLaunch {
            val exercises = exerciseMetricsFeature.getRecentExercisesByExampleId(id).getOrThrow()
            provideRecentExercises(exercises)
        }

        safeLaunch {
            val exercises = exerciseMetricsFeature.getAchievementsByExampleId(id).getOrThrow()
            provideAchievements(exercises)
        }
    }

    private fun provideRecentExercises(value: List<Exercise>) {
        val exercisesState = value.toState()

        val exerciseVolume = volumeSeriesUseCase
            .fromExercises(value)
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

        update { current ->
            current.copy(
                example = exampleState,
                muscleLoad = muscleLoad
            )
        }
    }

    override fun onDismiss() {
        navigateTo(ExerciseExampleDirection.Back)
    }
}
