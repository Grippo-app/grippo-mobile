package com.grippo.exercise

import com.grippo.analytics.exercise.ExerciseAnalytics
import com.grippo.analytics.exercise.models.ExerciseEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.domain.state.training.toState
import kotlinx.coroutines.flow.onEach

public class ExerciseViewModel(
    id: String,
    private val analytics: ExerciseAnalytics,
    trainingFeature: TrainingFeature,
) : BaseViewModel<ExerciseState, ExerciseDirection, ExerciseLoader>(
    ExerciseState()
), ExerciseContract {

    init {
        trainingFeature.observeExercise(id)
            .onEach(::provideExercise)
            .safeLaunch()
    }

    private fun provideExercise(value: Exercise?) {
        val exercise = value?.toState() ?: return
        update { it.copy(exercise = exercise) }
    }

    override fun onDismiss() {
        analytics.trackEvent(ExerciseEvent.DismissRequested)
        navigateTo(ExerciseDirection.Back)
    }

    override fun onExampleDetailsClick() {
        val id = state.value.exercise?.exerciseExample?.id ?: return

        analytics.trackEvent(ExerciseEvent.ExampleDetailsClicked)
        navigateTo(ExerciseDirection.ShowExampleDetails(id))
    }
}
