package com.grippo.home.trainings

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.mapper.training.toState
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime

internal class TrainingsViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController
) : BaseViewModel<TrainingsState, TrainingsDirection, TrainingsLoader>(
    TrainingsState()
), TrainingsContract {

    init {
        trainingFeature.observeTrainings(
            start = LocalDateTime(2024, 1, 1, 11, 11, 11, 11),
            end = LocalDateTime(2026, 1, 1, 11, 11, 11, 11)
        )
            .onEach(::provideTrainings)
            .safeLaunch()

        safeLaunch(loader = TrainingsLoader.Trainings) {
            trainingFeature.getTrainings(
                start = LocalDateTime(2024, 1, 1, 11, 11, 11, 11),
                end = LocalDateTime(2026, 1, 1, 11, 11, 11, 11)
            ).getOrThrow()
        }
    }

    private fun provideTrainings(list: List<Training>) {
        val trainings = list.toState()
        update { it.copy(trainings = trainings) }
    }

    override fun openExerciseExample(id: String) {
        val dialog = DialogConfig.ExerciseExample(
            id = id,
            onResult = { value -> }
        )

        dialogController.show(dialog)
    }
}