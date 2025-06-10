package com.grippo.exercise.example.exerciseexample

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.domain.mapper.exercise.example.toState
import kotlinx.coroutines.flow.onEach

public class ExerciseExampleViewModel(
    id: String,
    private val exerciseExampleFeature: ExerciseExampleFeature
) : BaseViewModel<ExerciseExampleState, ExerciseExampleDirection, ExerciseExampleLoader>(
    ExerciseExampleState()
), ExerciseExampleContract {

    init {
        exerciseExampleFeature.observeExerciseExample(id)
            .onEach(::provideExerciseExample)
            .safeLaunch()

        safeLaunch {
            exerciseExampleFeature.getExerciseExampleById(id)
        }
    }

    private fun provideExerciseExample(value: ExerciseExample?) {
        update { it.copy(example = value?.toState()) }
    }

    override fun dismiss() {
        navigateTo(ExerciseExampleDirection.Dismiss)
    }
}