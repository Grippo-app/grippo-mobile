package com.grippo.exercise

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.state.exercise.examples.ExerciseExampleDialogView
import kotlinx.coroutines.flow.onEach

public class ExerciseViewModel(
    id: String,
    trainingFeature: TrainingFeature,
    private val dialogController: DialogController
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
        navigateTo(ExerciseDirection.Back)
    }

    override fun onExampleDetailsClick(id: String) {
        val dialog = DialogConfig.ExerciseExample(
            id = id,
            view = ExerciseExampleDialogView.READ
        )

        dialogController.show(dialog)
    }
}
