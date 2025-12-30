package com.grippo.exercise.example.exerciseexample

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.metrics.ExerciseMetricsFeature
import com.grippo.data.features.api.muscle.MuscleLoadingUseCase
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.domain.state.achievements.toState
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.metrics.toState
import com.grippo.domain.state.training.toState
import com.grippo.toolkit.calculation.AnalyticsApi
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

public class ExerciseExampleViewModel(
    id: String,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val exerciseMetricsFeature: ExerciseMetricsFeature,
    private val muscleLoadingUseCase: MuscleLoadingUseCase,
    stringProvider: StringProvider,
    colorProvider: ColorProvider,
) : BaseViewModel<ExerciseExampleState, ExerciseExampleDirection, ExerciseExampleLoader>(
    ExerciseExampleState()
), ExerciseExampleContract {

    private val analytics = AnalyticsApi(stringProvider, colorProvider)

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

    private suspend fun provideRecentExercises(value: List<Exercise>) {
        val exercises = value.toState()

        val exerciseVolume = analytics.volumeFromExercises(
            exercises = exercises,
        )

        update { it.copy(recent = exercises, exerciseVolume = exerciseVolume) }
    }

    private fun provideAchievements(value: List<Achievement>) {
        val achievements = value.toState()

        update { it.copy(achievements = achievements.toPersistentList()) }
    }

    private suspend fun provideExerciseExample(value: ExerciseExample?) {
        val exampleState = value?.toState() ?: return

        val muscleLoad = muscleLoadingUseCase
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
