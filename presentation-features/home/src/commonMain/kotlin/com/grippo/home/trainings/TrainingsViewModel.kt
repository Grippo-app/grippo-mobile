package com.grippo.home.trainings

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import kotlinx.datetime.LocalDateTime

internal class TrainingsViewModel(
    private val trainingFeature: TrainingFeature
) : BaseViewModel<TrainingsState, TrainingsDirection, TrainingsLoader>(
    TrainingsState
), TrainingsContract {

    init {
        safeLaunch(loader = TrainingsLoader.Trainings) {
            trainingFeature.getTrainings(
                start = LocalDateTime(2024, 1, 1, 11, 11, 11, 11),
                endDate = LocalDateTime(2026, 1, 1, 11, 11, 11, 11)
            ).getOrThrow()
        }
    }
}