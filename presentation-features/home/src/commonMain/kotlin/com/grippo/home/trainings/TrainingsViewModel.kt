package com.grippo.home.trainings

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime

internal class TrainingsViewModel(
    private val trainingFeature: TrainingFeature
) : BaseViewModel<TrainingsState, TrainingsDirection, TrainingsLoader>(
    TrainingsState
), TrainingsContract {

    init {
        trainingFeature.observeTrainings(
            start = LocalDateTime(2024, 1, 1, 11, 11, 11, 11),
            end = LocalDateTime(2026, 1, 1, 11, 11, 11, 11)
        ).onEach {
            println("TRAININGS = $it")
        }.safeLaunch()

        safeLaunch(loader = TrainingsLoader.Trainings) {
            trainingFeature.getTrainings(
                start = LocalDateTime(2024, 1, 1, 11, 11, 11, 11),
                end = LocalDateTime(2026, 1, 1, 11, 11, 11, 11)
            ).getOrThrow()
        }
    }
}